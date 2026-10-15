package com.example.aplicacionparacelular.network

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.example.aplicacionparacelular.onboarding.OnboardingGate
import com.example.aplicacionparacelular.ui.character.TeoAppearance
import com.example.aplicacionparacelular.ui.character.ProfileAppearanceGate
import com.example.aplicacionparacelular.ui.character.TeoAppearanceRules
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Gestor de la conexión con la Raspberry Pi.
 *
 * Mantiene un polling periódico del estado del robot y expone LiveData
 * reactivos que los Fragments pueden observar para actualizar la UI.
 *
 * Flujo de conexión:
 * 1. init() carga la IP guardada en SharedPreferences.
 * 2. Si no hay IP, inicia descubrimiento automático (UDP beacon + fallback HTTP).
 * 3. Cuando se configura una IP, arranca el polling automáticamente.
 * 4. El polling consulta /api/status cada N segundos y actualiza los LiveData.
 */
object RobotConnectionManager {

    private const val TAG = "RobotConnMgr"
    private const val MUSIC_PREFS = "teo_music"
    private const val KEY_LAST_SONG = "last_played_song"

    private val executor = Executors.newScheduledThreadPool(2)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pollingFuture: ScheduledFuture<*>? = null

    private var appContext: Context? = null
    private var initialized = false
    private val profileAppearanceGate = ProfileAppearanceGate()
    private val connectionSession = ConnectionSession()

    private val _parentAlerts = MutableLiveData<List<ParentNotice>>(emptyList())
    val parentAlerts: LiveData<List<ParentNotice>> = _parentAlerts

    private val recentNotices = mutableListOf<ParentNotice>()

    // ------------------------------------------------------------------
    // LiveData observables
    // ------------------------------------------------------------------

    private val _isConnected = MutableLiveData(false)
    val isConnected: LiveData<Boolean> = _isConnected

    private val _robotStatus = MutableLiveData<JSONObject?>()
    val robotStatus: LiveData<JSONObject?> = _robotStatus

    private val _lastError = MutableLiveData<String?>()
    val lastError: LiveData<String?> = _lastError

    // Default true: Pi vieja sin el campo no debe bloquear la app para siempre.
    private val _hasChildProfile = MutableLiveData(true)
    val hasChildProfile: LiveData<Boolean> = _hasChildProfile

    // ------------------------------------------------------------------
    // Inicialización y Polling
    // ------------------------------------------------------------------

    /**
     * Inicializa el manager y el cliente API.
     * Debe llamarse una vez desde Application.onCreate() o MainActivity.
     * Si no hay IP configurada, intenta descubrir el robot automáticamente.
     *
     * Es seguro llamar múltiples veces — solo la primera tiene efecto.
     */
    fun init(context: Context) {
        if (initialized) {
            Log.d(TAG, "init() ya fue llamado, ignorando llamada duplicada")
            return
        }
        initialized = true

        val ctx = context.applicationContext
        appContext = ctx
        TeoAppearance.init(ctx)
        RobotApiClient.init(ctx)
        com.example.aplicacionparacelular.notifications.AppNotificationHelper.ensureChannels(ctx)

        attachDiscoveryObserver()
        if (RobotApiClient.isConfigured()) {
            Log.d(TAG, "IP ya configurada, arrancando polling")
            val revision = connectionSession.revision
            executor.execute {
                RobotApiClient.refreshPairingToken(ctx)
                postToMain { if (connectionSession.accepts(revision)) startPolling() }
            }
        } else {
            Log.d(TAG, "No hay IP configurada, iniciando descubrimiento automático")
            startLanRediscovery()
        }
    }

    private var discoveryObserverAttached = false

    private val discoveryObserver = Observer<RobotDiscovery.DiscoveredRobot?> { robot ->
        val ctx = appContext ?: return@Observer
        val hasLan = _isConnected.value == true
        if (robot != null && BeaconIp.isUsable(robot.ip) && OnboardingGate.shouldAdoptLanDiscovery(hasLan)) {
            if (!connectionSession.hasOwner) return@Observer
            Log.d(TAG, "Robot descubierto: ${robot.ip}:${robot.port} (${robot.deviceName})")
            val revision = resetConnectionAttempt()
            RobotApiClient.setRobotAddress(ctx, robot.ip, robot.port)
            if (robot.pairingToken.isNotBlank()) {
                RobotApiClient.setPairingToken(ctx, robot.pairingToken)
            }
            executor.execute {
                RobotApiClient.refreshPairingToken(ctx)
                postToMain { if (connectionSession.accepts(revision)) startPolling() }
            }
        }
    }

    private fun attachDiscoveryObserver() {
        if (discoveryObserverAttached) return
        discoveryObserverAttached = true
        RobotDiscovery.discoveredRobot.observeForever(discoveryObserver)
    }

    private fun startLanRediscovery() {
        if (!connectionSession.hasOwner) return
        val ctx = appContext ?: return
        attachDiscoveryObserver()
        RobotDiscovery.startScan(ctx)
    }

    /**
     * Olvida la IP guardada y vuelve a descubrir el peluche en la LAN.
     *
     * Tras un cambio de red WiFi la Pi casi siempre cambia de IP, así que hay
     * que borrar la vieja **antes** de escanear o el polling sigue pegándole a
     * una dirección muerta.
     */
    fun forgetAndRediscover(context: Context) {
        val ctx = context.applicationContext
        appContext = ctx
        resetConnectionAttempt()
        RobotApiClient.clearAddress(ctx)
        attachDiscoveryObserver()
        RobotDiscovery.stopScan()
        // stopScan apaga isScanning con postValue; sin este post, startScan vería
        // el scan viejo todavía en curso y se iría sin hacer nada.
        postToMain { startLanRediscovery() }
    }

    /**
     * Configura manualmente la IP del robot y arranca el polling.
     * Usar desde la UI de configuración cuando el usuario ingresa la IP.
     */
    fun connectManually(context: Context, ip: String, port: Int = 8080) {
        Log.d(TAG, "Conexión manual a $ip:$port")
        val revision = resetConnectionAttempt()
        RobotDiscovery.stopScan()
        RobotApiClient.setRobotAddress(context, ip, port)
        executor.execute {
            RobotApiClient.refreshPairingToken(context)
            postToMain { if (connectionSession.accepts(revision)) startPolling() }
        }
    }

    private fun resetConnectionAttempt(): Long {
        val revision = connectionSession.changeTarget()
        stopPolling()
        _isConnected.value = false
        _robotStatus.value = null
        _lastError.value = null
        return revision
    }

    fun setUiActive(active: Boolean) {
        connectionSession.uiActive = active
        reconcileConnectionWork()
    }

    fun setAlertServiceActive(active: Boolean) {
        connectionSession.alertServiceActive = active
        reconcileConnectionWork()
    }

    private fun reconcileConnectionWork() {
        if (!connectionSession.hasOwner) {
            stopPolling()
            RobotDiscovery.stopScan()
        } else if (RobotApiClient.isConfigured()) {
            if (pollingFuture == null) startPolling()
        } else {
            startLanRediscovery()
        }
    }

    /**
     * Inicia el polling periódico del estado del robot.
     * El intervalo por defecto es de 5 segundos.
     */
    fun startPolling(intervalSeconds: Long = 5) {
        if (!connectionSession.hasOwner) return
        if (!RobotApiClient.isConfigured()) {
            Log.d(TAG, "startPolling() ignorado: no hay IP configurada")
            return
        }
        stopPolling()
        Log.d(TAG, "Polling iniciado (cada ${intervalSeconds}s)")
        pollingFuture = executor.scheduleWithFixedDelay(
            { pollStatus() },
            0,
            intervalSeconds,
            TimeUnit.SECONDS
        )
    }

    /**
     * Detiene el polling periódico.
     */
    fun stopPolling() {
        pollingFuture?.cancel(false)
        pollingFuture = null
    }

    private fun pollStatus() {
        if (!connectionSession.hasOwner) return
        val revision = connectionSession.revision
        if (!RobotApiClient.isConfigured()) {
            postToMain {
                if (connectionSession.accepts(revision) && connectionSession.hasOwner) _isConnected.value = false
            }
            return
        }

        when (val result = RobotApiClient.getStatus()) {
            is ApiResult.Success -> {
                postToMain {
                    if (!connectionSession.accepts(revision) || !connectionSession.hasOwner) return@postToMain
                    val justConnected = _isConnected.value != true
                    _robotStatus.value = result.data
                    _lastError.value = null
                    // Si el JSON no trae has_child_profile (firmware viejo), true evita
                    // dejar la app bloqueada para siempre. Tras actualizar la Pi, el campo existe.
                    _hasChildProfile.value = result.data.optBoolean("has_child_profile", true)
                    _isConnected.value = true
                    if (justConnected) fetchChildProfile { }
                }
                if (connectionSession.accepts(revision) && connectionSession.hasOwner) pollNotifications(revision)
            }
            is ApiResult.Error -> {
                Log.d(TAG, "Poll falló: ${result.message}")
                postToMain {
                    if (!connectionSession.accepts(revision) || !connectionSession.hasOwner) return@postToMain
                    _isConnected.value = false
                    _robotStatus.value = null
                    _lastError.value = result.message
                    startLanRediscovery()
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Operaciones asíncronas (ejecutadas en background)
    // ------------------------------------------------------------------

    /**
     * Ejecuta una llamada API en background y llama al callback en el main thread.
     */
    fun <T> executeAsync(
        call: () -> ApiResult<T>,
        onResult: (ApiResult<T>) -> Unit
    ) {
        val revision = connectionSession.revision
        executor.execute {
            val result = if (connectionSession.accepts(revision)) call() else connectionChanged()
            postToMain {
                onResult(if (connectionSession.accepts(revision)) result else connectionChanged())
            }
        }
    }

    private fun connectionChanged() = ApiResult.Error("La conexión con TEO cambió. Reintentá la operación.")

    fun fetchStatus(onResult: (ApiResult<JSONObject>) -> Unit) {
        executeAsync({ RobotApiClient.getStatus() }) { result ->
            if (result is ApiResult.Success) {
                _robotStatus.value = result.data
                _lastError.value = null
                _hasChildProfile.value = result.data.optBoolean("has_child_profile", true)
                _isConnected.value = true
            }
            onResult(result)
        }
    }

    /**
     * Envía una señal de celebración al robot.
     */
    fun celebrate(onResult: (ApiResult<JSONObject>) -> Unit) {
        executeAsync({ RobotApiClient.postCelebrate() }, onResult)
    }

    /**
     * Enciende o apaga el robot.
     */
    fun setPower(powerOn: Boolean, onResult: (ApiResult<JSONObject>) -> Unit) {
        executeAsync({ RobotApiClient.postPower(powerOn) }, onResult)
    }

    /** Pone al peluche en modo de provisionamiento WiFi por BLE (PIN en el LCD). */
    fun setWifiProvisioning(enabled: Boolean, onResult: (ApiResult<JSONObject>) -> Unit) {
        executeAsync({ RobotApiClient.postWifiProvisioning(enabled) }, onResult)
    }

    fun fetchWifiNetworks(onResult: (ApiResult<JSONObject>) -> Unit) {
        executeAsync({ RobotApiClient.getWifiNetworks() }, onResult)
    }

    fun connectWifi(ssid: String, password: String, onResult: (ApiResult<JSONObject>) -> Unit) {
        executeAsync({ RobotApiClient.postWifiConnect(ssid, password) }, onResult)
    }

    fun setBellyWake(enabled: Boolean, onResult: (ApiResult<JSONObject>) -> Unit) {
        executeAsync({ RobotApiClient.postBellyWake(enabled) }, onResult)
    }

    /**
     * Actualiza la configuración sensorial del robot.
     */
    fun updateConfig(
        volumeLimit: Int,
        brightness: Float,
        playtimeLimitMinutes: Int = 0,
        onResult: (ApiResult<JSONObject>) -> Unit
    ) {
        executeAsync({ RobotApiClient.postConfig(volumeLimit, brightness, playtimeLimitMinutes) }, onResult)
    }

    fun updateSensory(
        volumeLimit: Int,
        brightnessPercent: Int,
        onResult: (ApiResult<JSONObject>) -> Unit,
    ) {
        executeAsync({ RobotApiClient.postSensory(volumeLimit, brightnessPercent) }, onResult)
    }

    /**
     * Activa/desactiva el modo noche.
     */
    fun setNightMode(enabled: Boolean, onResult: (ApiResult<JSONObject>) -> Unit) {
        executeAsync({ RobotApiClient.postNightMode(enabled) }, onResult)
    }

    /**
     * Obtiene la telemetría del día actual.
     */
    fun fetchTelemetryToday(onResult: (ApiResult<JSONObject>) -> Unit) {
        executeAsync({ RobotApiClient.getTelemetryToday() }, onResult)
    }

    /**
     * Obtiene la telemetría de una fecha.
     */
    fun fetchTelemetry(date: String, onResult: (ApiResult<JSONObject>) -> Unit) {
        executeAsync({ RobotApiClient.getTelemetry(date) }, onResult)
    }

    fun fetchTelemetryRange(days: Int, onResult: (ApiResult<JSONObject>) -> Unit) {
        executeAsync({ RobotApiClient.getTelemetryRange(days) }, onResult)
    }

    /**
     * Obtiene las rutinas configuradas.
     */
    fun fetchRoutines(onResult: (ApiResult<JSONObject>) -> Unit) {
        executeAsync({ RobotApiClient.getRoutines() }, onResult)
    }

    /**
     * Actualiza las rutinas en el robot.
     */
    fun updateRoutines(routinesJson: JSONObject, onResult: (ApiResult<JSONObject>) -> Unit) {
        executeAsync({ RobotApiClient.postRoutines(routinesJson) }, onResult)
    }

    /**
     * Obtiene la lista de canciones del robot.
     */
    fun fetchMusic(onResult: (ApiResult<JSONObject>) -> Unit) {
        executeAsync({ RobotApiClient.getMusic() }, onResult)
    }

    /**
     * Sube una canción al robot.
     */
    fun uploadMusic(filename: String, data: ByteArray, onResult: (ApiResult<JSONObject>) -> Unit) {
        executeAsync({ RobotApiClient.uploadMusic(filename, data) }, onResult)
    }

    /**
     * Elimina una canción del robot.
     */
    fun deleteMusic(filename: String, onResult: (ApiResult<JSONObject>) -> Unit) {
        executeAsync({ RobotApiClient.deleteMusic(filename) }, onResult)
    }

    /**
     * Reproduce una canción en el robot.
     */
    fun playMusic(filename: String? = null, onResult: (ApiResult<JSONObject>) -> Unit = {}) {
        if (!filename.isNullOrBlank()) {
            rememberLastSong(filename)
        }
        executeAsync({ RobotApiClient.playMusic(filename) }, onResult)
    }

    fun rememberLastSong(filename: String) {
        val ctx = appContext ?: return
        ctx.getSharedPreferences(MUSIC_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_SONG, filename)
            .apply()
    }

    fun lastPlayedSong(): String? {
        val ctx = appContext ?: return null
        return ctx.getSharedPreferences(MUSIC_PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LAST_SONG, null)
    }

    /**
     * Detiene la reproducción de música en el robot.
     */
    fun stopMusic(onResult: (ApiResult<JSONObject>) -> Unit = {}) {
        executeAsync({ RobotApiClient.stopMusic() }, onResult)
    }

    fun playStory(storyId: String, onResult: (ApiResult<JSONObject>) -> Unit = {}) {
        executeAsync({ RobotApiClient.playStory(storyId) }, onResult)
    }

    fun stopStory(onResult: (ApiResult<JSONObject>) -> Unit = {}) {
        executeAsync({ RobotApiClient.stopStory() }, onResult)
    }

    fun fetchStories(onResult: (ApiResult<JSONObject>) -> Unit) {
        executeAsync({ RobotApiClient.getStories() }, onResult)
    }

    fun uploadStory(
        filename: String,
        data: ByteArray,
        title: String? = null,
        onResult: (ApiResult<JSONObject>) -> Unit,
    ) {
        executeAsync({ RobotApiClient.uploadStory(filename, data, title) }, onResult)
    }

    fun fetchStory(storyId: String, onResult: (ApiResult<JSONObject>) -> Unit) {
        executeAsync({ RobotApiClient.getStory(storyId) }, onResult)
    }

    fun updateStory(
        storyId: String,
        title: String? = null,
        text: String? = null,
        onResult: (ApiResult<JSONObject>) -> Unit,
    ) {
        executeAsync({ RobotApiClient.updateStory(storyId, title, text) }, onResult)
    }

    fun deleteStory(storyId: String, onResult: (ApiResult<JSONObject>) -> Unit) {
        executeAsync({ RobotApiClient.deleteStory(storyId) }, onResult)
    }

    fun fetchChildProfile(onResult: (ApiResult<JSONObject>) -> Unit) {
        val read = profileAppearanceGate.beginRead()
        executeAsync({ RobotApiClient.getChildProfile() }) { result ->
            if (profileAppearanceGate.accepts(read) && result is ApiResult.Success) {
                TeoAppearance.acceptConfirmed(result.data)
            }
            onResult(result)
        }
    }

    fun saveChildProfile(body: JSONObject, onResult: (ApiResult<JSONObject>) -> Unit) {
        val version = profileAppearanceGate.beginSave()
        executeAsync({ RobotApiClient.putChildProfile(body) }) { result ->
            val confirmed = result is ApiResult.Success && TeoAppearanceRules.confirmedCostume(result.data) != null
            val latestSave = profileAppearanceGate.finishSave(version, confirmed)
            if (result is ApiResult.Success && result.data.optBoolean("complete", false)) {
                _hasChildProfile.value = true
                if (latestSave) TeoAppearance.acceptConfirmed(result.data)
            }
            onResult(result)
        }
    }

    // ------------------------------------------------------------------
    // Notificaciones Raspberry → celular
    // ------------------------------------------------------------------

    private fun pollNotifications(revision: Long) {
        when (val result = RobotApiClient.getNotifications()) {
            is ApiResult.Success -> {
                if (!connectionSession.accepts(revision) || !connectionSession.hasOwner) return
                val arr = result.data.optJSONArray("notifications") ?: return
                if (arr.length() == 0) return
                val incoming = mutableListOf<ParentNotice>()
                val receivedAt = System.currentTimeMillis()
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val type = obj.optString("type", "aviso")
                    val message = obj.optString("message", "")
                    val notice = ParentInbox.notice(
                        type,
                        message,
                        obj.optString("timestamp", ""),
                        receivedAt + i,
                    ) ?: continue
                    incoming.add(notice)
                }
                if (incoming.isEmpty()) return
                postToMain {
                    if (!connectionSession.accepts(revision) || !connectionSession.hasOwner) return@postToMain
                    incoming.forEach { showSystemNotification(it.type, it.message) }
                    val merged = ParentInbox.merge(recentNotices, incoming)
                    recentNotices.clear()
                    recentNotices.addAll(merged)
                    _parentAlerts.value = recentNotices.toList()
                }
            }
            is ApiResult.Error -> {
                Log.d(TAG, "Poll notificaciones falló: ${result.message}")
            }
        }
    }

    private fun showSystemNotification(type: String, message: String) {
        val context = appContext ?: return
        val t = type.lowercase()
        val title = when (t) {
            "crisis" -> "TEO: atención recomendada"
            "pedido" -> "TEO: el nene te necesita"
            "musica" -> "TEO está reproduciendo música"
            "logro" -> "TEO: el nene logró algo"
            "vocabulario" -> "TEO: palabras nuevas"
            "rutina" -> "TEO: rutina"
            "vacuna" -> "TEO: vacunación"
            "turno" -> "TEO: turno médico"
            else -> "Aviso de TEO"
        }
        val channel = when (t) {
            "rutina" -> com.example.aplicacionparacelular.notifications.AppNotificationHelper.CHANNEL_RUTINA
            "vacuna" -> com.example.aplicacionparacelular.notifications.AppNotificationHelper.CHANNEL_VACUNA
            "turno" -> com.example.aplicacionparacelular.notifications.AppNotificationHelper.CHANNEL_TURNO
            else -> com.example.aplicacionparacelular.notifications.AppNotificationHelper.CHANNEL_PELUCHE
        }
        com.example.aplicacionparacelular.notifications.AppNotificationHelper.show(
            context,
            channel,
            (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
            title,
            message,
            highPriority = t == "crisis" || t == "pedido",
        )
    }

    // ------------------------------------------------------------------
    // Utilidad
    // ------------------------------------------------------------------

    private fun postToMain(action: () -> Unit) {
        mainHandler.post(action)
    }
}
