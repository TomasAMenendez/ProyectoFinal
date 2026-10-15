package com.example.aplicacionparacelular.ui.onboarding

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.aplicacionparacelular.R
import com.example.aplicacionparacelular.databinding.FragmentWifiSetupBinding
import com.example.aplicacionparacelular.databinding.ItemWifiNetworkBinding
import com.example.aplicacionparacelular.network.ApiResult
import com.example.aplicacionparacelular.network.RobotApiClient
import com.example.aplicacionparacelular.network.RobotConnectionManager
import com.example.aplicacionparacelular.onboarding.BlePermissionUi
import com.example.aplicacionparacelular.onboarding.ScanListAction
import com.example.aplicacionparacelular.onboarding.TeoBleClient
import com.example.aplicacionparacelular.onboarding.TeoBleStatus
import com.example.aplicacionparacelular.onboarding.TeoBleUuids
import com.example.aplicacionparacelular.onboarding.TeoStatusEffect
import com.example.aplicacionparacelular.onboarding.WifiNetworkUi
import com.example.aplicacionparacelular.onboarding.WifiScanPolicy

/**
 * Pantalla bloqueante que conecta al peluche a una red WiFi por BLE.
 *
 * Pasos: PIN del LCD → lista de redes → contraseña → conectando. Cuando la Pi
 * avisa `connected`, la app olvida la IP vieja y vuelve a descubrirla en la LAN.
 */
class WifiSetupFragment : Fragment() {

    private var _binding: FragmentWifiSetupBinding? = null
    private val binding get() = _binding!!

    private var client: TeoBleClient? = null
    private var selectedNetwork: WifiNetworkUi? = null

    /** Última lista de redes que el padre tiene (o tuvo) en pantalla. */
    private var shownNetworks: List<WifiNetworkUi> = emptyList()
    private var readInFlight = false
    private var autoReads = 0
    private var pendingReadIsManual = false

    private var pinVerified = false
    private var credentialsSent = false
    private var sawDisconnected = false

    /**
     * True si llegamos desde Config → "Cambiar red WiFi" (la Pi está en la LAN).
     * En ese caso, al abandonar la pantalla hay que avisarle que corte el BLE.
     */
    private var startedFromConfig = false
    private var fromMyTeo = false
    private var joinedWifi = false

    private val mainHandler = Handler(Looper.getMainLooper())

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (_binding == null) return@registerForActivityResult
        if (grants.values.all { it }) {
            startBle()
            return@registerForActivityResult
        }
        val blocked = BlePermissionUi.permanentlyDenied(grants) { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), permission)
        }
        showPermissionProblem(blocked)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentWifiSetupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Con LAN viva solo se llega acá desde Config: el onboarding real no
        // tiene red. Se mira una vez, antes de que la Pi cambie de red, y
        // sobrevive a una recreación de la Activity.
        startedFromConfig = if (savedInstanceState?.containsKey(KEY_FROM_CONFIG) == true) {
            savedInstanceState.getBoolean(KEY_FROM_CONFIG)
        } else {
            RobotConnectionManager.isConnected.value == true
        }
        fromMyTeo = if (savedInstanceState?.containsKey(KEY_FROM_MY_TEO) == true) {
            savedInstanceState.getBoolean(KEY_FROM_MY_TEO)
        } else {
            arguments?.getBoolean(KEY_FROM_MY_TEO) == true
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Si hay LAN llegamos desde "Cambiar red WiFi" y volver es
                    // legítimo. Sin LAN esta pantalla es una pared.
                    if (fromMyTeo || RobotConnectionManager.isConnected.value == true) {
                        findNavController().popBackStack()
                    } else {
                        requireActivity().moveTaskToBack(true)
                    }
                }
            },
        )

        RobotConnectionManager.isConnected.observe(viewLifecycleOwner) { connected ->
            if (!connected) {
                sawDisconnected = true
            } else if (sawDisconnected) {
                // El peluche volvió a aparecer en la LAN: el onboarding sigue
                // en MainActivity (perfil del chico o app normal).
                if (fromMyTeo) {
                    findNavController().popBackStack()
                } else {
                    findNavController().popBackStack(R.id.nav_dashboard, false)
                }
            }
        }

        binding.editWifiPin.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                binding.btnVerifyPin.isEnabled = TeoBleUuids.isValidPin(s?.toString().orEmpty())
            }
        })
        binding.btnVerifyPin.setOnClickListener { verifyPin() }
        binding.btnRefreshNetworks.setOnClickListener { requestNetworks(manual = true) }
        binding.btnWifiConnect.setOnClickListener { sendCredentials() }
        binding.btnWifiRetry.setOnClickListener { restart() }
        binding.btnWifiBluetoothSettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        }
        binding.btnWifiAppSettings.setOnClickListener { openAppSettings() }

        ensurePermissionsAndStart()
    }

    /**
     * Única salida cuando el padre eligió "no preguntar de nuevo": los ajustes
     * de la app, donde el permiso se puede volver a activar a mano.
     */
    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", requireContext().packageName, null),
        )
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "no pude abrir los ajustes de la app: ${e.message}")
        }
    }

    private fun showPermissionProblem(blocked: Boolean) {
        if (_binding == null) return
        setBusy(false, "")
        if (blocked) {
            showError(getString(R.string.wifi_setup_error_permission_blocked), allowRetry = true)
            binding.btnWifiAppSettings.visibility = View.VISIBLE
        } else {
            showError(getString(R.string.wifi_setup_error_permission), allowRetry = true)
        }
    }

    // ------------------------------------------------------------------
    // BLE
    // ------------------------------------------------------------------

    private fun ensurePermissionsAndStart() {
        val missing = TeoBleClient.missingPermissions(requireContext())
        if (missing.isEmpty()) {
            startBle()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun startBle() {
        if (_binding == null) return
        val ble = TeoBleClient(requireContext())
        client = ble
        ble.onScanStarted = { setBusy(true, getString(R.string.wifi_setup_searching)) }
        ble.onReady = {
            setBusy(false, getString(R.string.wifi_setup_connected_ble))
            binding.sectionPin.visibility = View.VISIBLE
        }
        ble.onStatus = { handleStatus(it) }
        ble.onNetworks = { showNetworks(it) }
        ble.onFailure = { handleFailure(it) }
        ble.startScanAndConnect()
    }

    private fun restart() {
        client?.close()
        client = null
        cancelStallWatchdog()
        pinVerified = false
        credentialsSent = false
        selectedNetwork = null
        forgetNetworkList()
        binding.sectionPin.visibility = View.GONE
        binding.sectionNetworks.visibility = View.GONE
        binding.sectionPassword.visibility = View.GONE
        binding.txtWifiError.visibility = View.GONE
        binding.btnWifiRetry.visibility = View.GONE
        binding.btnWifiBluetoothSettings.visibility = View.GONE
        binding.btnWifiAppSettings.visibility = View.GONE
        ensurePermissionsAndStart()
    }

    private fun verifyPin() {
        val pin = binding.editWifiPin.text?.toString().orEmpty()
        if (!TeoBleUuids.isValidPin(pin)) return
        binding.txtWifiError.visibility = View.GONE
        setBusy(true, getString(R.string.wifi_setup_pin_verify))
        armStallWatchdog()
        client?.writePin(pin)
    }

    /**
     * Lee CHAR_SCAN. [manual] es el botón "Buscar redes de nuevo": renueva el
     * presupuesto de lecturas automáticas y su respuesta manda, aunque venga
     * vacía.
     */
    private fun requestNetworks(manual: Boolean) {
        if (!pinVerified) return
        if (manual) {
            if (!WifiScanPolicy.allowsManualRead(readInFlight)) return
            autoReads = 0
        } else {
            autoReads++
        }
        pendingReadIsManual = manual
        readInFlight = true
        setBusy(true, getString(R.string.wifi_setup_scanning))
        armStallWatchdog()
        client?.readScan()
    }

    /** True mientras el padre esté eligiendo una red o tipeando la contraseña. */
    private fun isUserChoosing(): Boolean {
        if (_binding == null) return false
        return selectedNetwork != null || binding.editWifiPassword.text?.isNotEmpty() == true
    }

    private fun forgetNetworkList() {
        shownNetworks = emptyList()
        readInFlight = false
        autoReads = 0
        pendingReadIsManual = false
    }

    private fun sendCredentials() {
        val network = selectedNetwork ?: return
        val password = binding.editWifiPassword.text?.toString().orEmpty()
        binding.txtWifiError.visibility = View.GONE
        setBusy(true, getString(R.string.wifi_setup_connecting, network.ssid))
        binding.btnWifiConnect.isEnabled = false
        credentialsSent = true
        armStallWatchdog()
        client?.writeConnect(network.ssid, password)
    }

    // ------------------------------------------------------------------
    // Estados que llegan de la Pi
    // ------------------------------------------------------------------

    /**
     * Traduce los nueve estados del contrato (y cualquier valor inesperado) a
     * pasos de la pantalla. El mapeo vive en [TeoBleStatus.effectOf], que se
     * testea sin radio.
     */
    private fun handleStatus(status: String) {
        if (_binding == null) return
        when (TeoBleStatus.effectOf(status)) {
            TeoStatusEffect.WAIT_PIN -> {
                // La sesión volvió a esperar el PIN (o todavía no arrancó).
                if (!pinVerified) {
                    cancelStallWatchdog()
                    setBusy(false, getString(R.string.wifi_setup_waiting_pin))
                    binding.sectionPin.visibility = View.VISIBLE
                }
            }
            TeoStatusEffect.PIN_ACCEPTED -> {
                pinVerified = true
                // La Pi repite `pin_ok` cada vez que su sesión vuelve a reposo.
                // Releer acá siempre era el ping-pong: read → la Pi rescanea →
                // `scanning` → `pin_ok` → read…
                val action = WifiScanPolicy.onPinOk(
                    hasNetworks = shownNetworks.isNotEmpty(),
                    userChoosing = isUserChoosing(),
                    readInFlight = readInFlight,
                    autoReads = autoReads,
                )
                if (action != ScanListAction.KEEP) {
                    binding.sectionPin.visibility = View.GONE
                    binding.sectionNetworks.visibility = View.VISIBLE
                }
                when (action) {
                    ScanListAction.READ -> requestNetworks(manual = false)
                    ScanListAction.SETTLE -> settleNetworkList()
                    else -> Unit
                }
            }
            TeoStatusEffect.SCANNING_NETWORKS -> {
                // La Pi salió a buscar redes. Si ya hay una lista usable en
                // pantalla no la tapamos con el spinner: solo avisamos.
                when (WifiScanPolicy.onScanning(shownNetworks.isNotEmpty(), isUserChoosing())) {
                    ScanListAction.WAIT -> {
                        setBusy(true, getString(R.string.wifi_setup_scanning))
                        armStallWatchdog()
                    }
                    ScanListAction.REFRESH_HINT ->
                        setBusy(false, getString(R.string.wifi_setup_refresh_hint))
                    else -> Unit
                }
            }
            TeoStatusEffect.CONNECTING_WIFI -> {
                val ssid = selectedNetwork?.ssid
                setBusy(
                    true,
                    if (ssid != null) {
                        getString(R.string.wifi_setup_connecting, ssid)
                    } else {
                        getString(R.string.wifi_setup_connecting_generic)
                    },
                )
                armStallWatchdog()
            }
            TeoStatusEffect.JOINED_WIFI -> onRobotJoinedWifi()
            TeoStatusEffect.RETRY_PIN -> {
                cancelStallWatchdog()
                setBusy(false, getString(R.string.wifi_setup_connected_ble))
                binding.sectionPin.visibility = View.VISIBLE
                showError(getString(R.string.wifi_setup_error_pin), allowRetry = false)
            }
            TeoStatusEffect.RESTART_SESSION -> {
                cancelStallWatchdog()
                pinVerified = false
                selectedNetwork = null
                forgetNetworkList()
                setBusy(false, getString(R.string.wifi_setup_connected_ble))
                binding.sectionPin.visibility = View.VISIBLE
                binding.sectionNetworks.visibility = View.GONE
                binding.sectionPassword.visibility = View.GONE
                showError(getString(R.string.wifi_setup_error_session), allowRetry = false)
            }
            TeoStatusEffect.RETRY_PASSWORD -> {
                // El PIN sigue valiendo: solo hay que reescribir la contraseña.
                cancelStallWatchdog()
                credentialsSent = false
                setBusy(false, "")
                binding.sectionPassword.visibility = View.VISIBLE
                binding.btnWifiConnect.isEnabled = true
                binding.editWifiPassword.text?.clear()
                showError(getString(R.string.wifi_setup_error_auth), allowRetry = false)
            }
            TeoStatusEffect.SESSION_EXPIRED -> {
                // Se cumplieron los 10 minutos: la Pi apagó el BLE y borró el
                // PIN, así que hay que empezar de cero (no solo reintentar).
                cancelStallWatchdog()
                pinVerified = false
                credentialsSent = false
                selectedNetwork = null
                forgetNetworkList()
                setBusy(false, "")
                binding.sectionPin.visibility = View.GONE
                binding.sectionNetworks.visibility = View.GONE
                binding.sectionPassword.visibility = View.GONE
                showError(getString(R.string.wifi_setup_error_expired), allowRetry = true)
            }
            TeoStatusEffect.UNKNOWN_FAILURE -> {
                cancelStallWatchdog()
                setBusy(false, "")
                binding.btnWifiConnect.isEnabled = true
                showError(getString(R.string.wifi_setup_error_generic), allowRetry = true)
            }
            TeoStatusEffect.UNKNOWN -> {
                // No sabemos qué quiso decir: no lo tratamos como error, pero
                // dejamos el Reintentar a mano para no encerrar al padre.
                Log.w(TAG, "estado desconocido de TEO: '$status'")
                binding.txtWifiError.text = getString(R.string.wifi_setup_error_unknown_status)
                binding.txtWifiError.visibility = View.VISIBLE
                binding.btnWifiRetry.visibility = View.VISIBLE
            }
        }
    }

    private fun onRobotJoinedWifi() {
        joinedWifi = true
        cancelStallWatchdog()
        setBusy(true, getString(R.string.wifi_setup_done))
        binding.sectionPin.visibility = View.GONE
        binding.sectionNetworks.visibility = View.GONE
        binding.sectionPassword.visibility = View.GONE
        binding.txtWifiError.visibility = View.GONE
        // La Pi corta el BLE apenas entra a la red; soltamos la radio nosotros también.
        client?.close()
        client = null
        RobotConnectionManager.forgetAndRediscover(requireContext())
    }

    /** Respuesta de CHAR_SCAN. */
    private fun showNetworks(answer: List<WifiNetworkUi>) {
        if (_binding == null) return
        cancelStallWatchdog()
        readInFlight = false
        val manual = pendingReadIsManual
        pendingReadIsManual = false
        shownNetworks = WifiScanPolicy.listToShow(shownNetworks, answer, manual)
        if (isUserChoosing() && !manual) {
            // Llegó tarde, mientras el padre elige o tipea: guardamos la lista
            // nueva pero no le movemos la pantalla abajo de las manos.
            setBusy(false, "")
            return
        }
        settleNetworkList()
    }

    /** Apaga el spinner y dibuja [shownNetworks]. */
    private fun settleNetworkList() {
        if (_binding == null) return
        cancelStallWatchdog()
        setBusy(false, getString(R.string.wifi_setup_networks_title))
        binding.sectionNetworks.visibility = View.VISIBLE
        binding.containerNetworks.removeAllViews()
        binding.txtNetworksEmpty.visibility =
            if (shownNetworks.isEmpty()) View.VISIBLE else View.GONE
        val inflater = LayoutInflater.from(requireContext())
        for (network in shownNetworks) {
            val item = ItemWifiNetworkBinding.inflate(inflater, binding.containerNetworks, false)
            item.txtNetworkSsid.text = network.ssid
            val lock = if (network.secure) {
                "🔒 " + getString(R.string.wifi_setup_secure)
            } else {
                getString(R.string.wifi_setup_open_network)
            }
            item.txtNetworkDetail.text =
                "$lock · " + getString(R.string.wifi_setup_signal, network.signal)
            item.root.setOnClickListener { selectNetwork(network) }
            binding.containerNetworks.addView(item.root)
        }
    }

    private fun selectNetwork(network: WifiNetworkUi) {
        selectedNetwork = network
        binding.txtWifiError.visibility = View.GONE
        if (!network.secure) {
            binding.sectionPassword.visibility = View.GONE
            setBusy(true, getString(R.string.wifi_setup_connecting, network.ssid))
            credentialsSent = true
            armStallWatchdog()
            client?.writeConnect(network.ssid, "")
            return
        }
        binding.sectionPassword.visibility = View.VISIBLE
        binding.btnWifiConnect.isEnabled = true
        binding.txtPasswordTitle.text = getString(R.string.wifi_setup_password_title, network.ssid)
        binding.editWifiPassword.text?.clear()
        binding.editWifiPassword.requestFocus()
    }

    private fun handleFailure(failure: TeoBleClient.Failure) {
        if (_binding == null) return
        cancelStallWatchdog()
        readInFlight = false
        pendingReadIsManual = false
        setBusy(false, "")
        binding.btnWifiConnect.isEnabled = true
        when (failure) {
            TeoBleClient.Failure.BLUETOOTH_OFF -> {
                showError(getString(R.string.wifi_setup_error_bluetooth_off), allowRetry = true)
                binding.btnWifiBluetoothSettings.visibility = View.VISIBLE
            }
            TeoBleClient.Failure.NO_PERMISSION -> {
                // Si ya pedimos el permiso y el sistema no deja explicarlo de
                // nuevo, la única salida son los ajustes de la app.
                val blocked = TeoBleClient.missingPermissions(requireContext()).any { permission ->
                    !ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), permission)
                }
                showPermissionProblem(blocked)
            }
            TeoBleClient.Failure.NOT_FOUND ->
                showError(getString(R.string.wifi_setup_error_not_found), allowRetry = true)
            TeoBleClient.Failure.DISCONNECTED -> {
                if (credentialsSent) {
                    // La Pi corta el BLE apenas entra a la red, así que un corte
                    // después de mandar la clave puede ser justamente el éxito.
                    credentialsSent = false
                    setBusy(true, getString(R.string.wifi_setup_disconnected_checking))
                    RobotConnectionManager.forgetAndRediscover(requireContext())
                    showError(getString(R.string.wifi_setup_error_disconnected), allowRetry = true)
                } else {
                    showError(getString(R.string.wifi_setup_error_disconnected), allowRetry = true)
                }
            }
            else ->
                showError(getString(R.string.wifi_setup_error_generic), allowRetry = true)
        }
    }

    // ------------------------------------------------------------------
    // UI helpers
    // ------------------------------------------------------------------

    private fun setBusy(busy: Boolean, message: String) {
        binding.progressWifi.visibility = if (busy) View.VISIBLE else View.GONE
        if (message.isNotEmpty()) binding.txtWifiStatus.text = message
    }

    private fun showError(message: String, allowRetry: Boolean) {
        cancelStallWatchdog()
        binding.txtWifiError.text = message
        binding.txtWifiError.visibility = View.VISIBLE
        binding.btnWifiRetry.visibility = if (allowRetry) View.VISIBLE else View.GONE
    }

    // ------------------------------------------------------------------
    // Perro guardián: ninguna espera queda girando para siempre
    // ------------------------------------------------------------------

    private val stallWatchdog = Runnable {
        if (_binding == null || joinedWifi) return@Runnable
        readInFlight = false
        pendingReadIsManual = false
        setBusy(false, "")
        binding.btnWifiConnect.isEnabled = true
        showError(getString(R.string.wifi_setup_error_stalled), allowRetry = true)
    }

    private fun armStallWatchdog() {
        mainHandler.removeCallbacks(stallWatchdog)
        mainHandler.postDelayed(stallWatchdog, STALL_TIMEOUT_MS)
    }

    private fun cancelStallWatchdog() {
        mainHandler.removeCallbacks(stallWatchdog)
    }

    override fun onDestroyView() {
        client?.close()
        client = null
        cancelStallWatchdog()
        cancelProvisioningIfAbandoned()
        super.onDestroyView()
        _binding = null
    }

    /**
     * Si el padre abandona la pantalla que abrió desde Config, le avisamos a la
     * Pi que corte el BLE en lugar de dejarla anunciándose hasta su timeout de
     * 10 minutos.
     *
     * Solo aplica al caso "Cambiar red WiFi": en el onboarding real no hay LAN
     * para pegarle a la API. Si la Pi ya entró a la red nueva, [joinedWifi] es
     * true y no hay nada que cancelar (además la IP vieja ya no sirve).
     */
    private fun cancelProvisioningIfAbandoned() {
        if (!startedFromConfig || joinedWifi) return
        // Rotar la pantalla también destruye la vista: ahí no hay que cancelar
        // nada, el padre sigue en el flujo.
        val leaving = isRemoving || activity?.isFinishing == true
        if (!leaving) return
        if (!RobotApiClient.isConfigured()) return
        RobotConnectionManager.setWifiProvisioning(false) { result ->
            Log.d(TAG, "corté el provisioning al salir (ok=${result is ApiResult.Success})")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_FROM_CONFIG, startedFromConfig)
        outState.putBoolean(KEY_FROM_MY_TEO, fromMyTeo)
    }

    companion object {
        private const val TAG = "WifiSetupFragment"
        private const val KEY_FROM_CONFIG = "started_from_config"
        const val KEY_FROM_MY_TEO = "fromMyTeo"

        /** Máximo que esperamos una respuesta de la Pi antes de ofrecer salida. */
        private const val STALL_TIMEOUT_MS = 45_000L
    }
}
