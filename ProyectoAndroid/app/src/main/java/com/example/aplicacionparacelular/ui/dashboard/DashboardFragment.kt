package com.example.aplicacionparacelular.ui.dashboard

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.aplicacionparacelular.R
import com.example.aplicacionparacelular.databinding.FragmentDashboardBinding
import com.example.aplicacionparacelular.network.RobotConnectionManager
import com.google.android.material.snackbar.Snackbar
import org.json.JSONObject

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: DashboardViewModel
    private val tickHandler = Handler(Looper.getMainLooper())
    private var playtimeClock: PlaytimeClock? = null
    private val playtimeTicker = object : Runnable {
        override fun run() {
            renderPlaytimeClock()
            tickHandler.postDelayed(this, 1000L)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(DashboardViewModel::class.java)
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observe connection status
        RobotConnectionManager.isConnected.observe(viewLifecycleOwner) { connected ->
            if (connected) {
                binding.chipConnectionStatus.text = getString(R.string.dashboard_connection_status_online)
                binding.chipConnectionStatus.setChipBackgroundColorResource(R.color.card_connection_online)
            } else {
                binding.chipConnectionStatus.text = getString(R.string.dashboard_connection_status_offline)
                binding.chipConnectionStatus.setChipBackgroundColorResource(R.color.card_connection_offline)
            }
        }

        // Observe robot status for power state and alerts
        RobotConnectionManager.robotStatus.observe(viewLifecycleOwner) { status ->
            if (status != null) {
                val powerOn = status.optBoolean("power_on", false)
                updatePowerUI(powerOn)
                viewModel.applyRobotStatus(status)
                absorbPlaytimeFromPi(status)
            }
            refreshAlerts()
        }

        viewModel.bellyWakeEnabled.observe(viewLifecycleOwner) { enabled ->
            if (binding.chipBellyWake.isChecked != enabled) {
                binding.chipBellyWake.isChecked = enabled
            }
        }

        // Observe ViewModel data
        viewModel.totalInteractions.observe(viewLifecycleOwner) { value ->
            binding.txtInteractionsValue.text = value
        }

        viewModel.playtimeMinutes.observe(viewLifecycleOwner) { value ->
            binding.txtPlaytimeValue.text = value
        }

        viewModel.moodLabel.observe(viewLifecycleOwner) { value ->
            binding.txtMoodValue.text = value
        }

        viewModel.nowPlaying.observe(viewLifecycleOwner) { name ->
            binding.txtNowPlaying.text = if (name.isNullOrBlank()) {
                getString(R.string.dashboard_music_idle)
            } else {
                name
            }
        }

        viewModel.songCount.observe(viewLifecycleOwner) { count ->
            val storyOn = viewModel.storyPlaying.value == true
            binding.btnMusicPlay.isEnabled = storyOn || count > 0
            binding.txtMusicHint.visibility =
                if (!storyOn && count == 0) View.VISIBLE else View.GONE
        }
        viewModel.storyPlaying.observe(viewLifecycleOwner) { storyOn ->
            val count = viewModel.songCount.value ?: -1
            binding.btnMusicPlay.isEnabled = storyOn || count > 0
            binding.txtMusicHint.visibility =
                if (!storyOn && count == 0) View.VISIBLE else View.GONE
        }

        viewModel.musicMessage.observe(viewLifecycleOwner) { msg ->
            if (!msg.isNullOrBlank()) {
                Snackbar.make(view, msg, Snackbar.LENGTH_SHORT).show()
            }
        }

        RobotConnectionManager.parentAlerts.observe(viewLifecycleOwner) {
            refreshAlerts()
        }

        binding.cardCelebrate.setOnClickListener {
            RobotConnectionManager.celebrate { result ->
                val msg = when (result) {
                    is com.example.aplicacionparacelular.network.ApiResult.Success ->
                        "🎉 ¡Celebración enviada al peluche!"
                    is com.example.aplicacionparacelular.network.ApiResult.Error ->
                        "No se pudo enviar. ¿Está conectado el peluche?"
                }
                view.let { v ->
                    Snackbar.make(v, msg, Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        // Power toggle button
        binding.btnPowerToggle.setOnClickListener {
            viewModel.togglePower()
        }

        binding.chipBellyWake.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setBellyWakeEnabled(isChecked)
        }

        binding.btnMusicPlay.setOnClickListener {
            viewModel.playNow()
        }
        binding.btnMusicStop.setOnClickListener {
            viewModel.stopNow()
        }
        binding.btnMetricsDetail.setOnClickListener {
            findNavController().navigate(R.id.nav_metrics)
        }

        viewModel.refreshTelemetry()
        viewModel.refreshMusic()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshTelemetry()
        viewModel.refreshMusic()
        tickHandler.removeCallbacks(playtimeTicker)
        tickHandler.post(playtimeTicker)
    }

    override fun onPause() {
        tickHandler.removeCallbacks(playtimeTicker)
        super.onPause()
    }

    private fun absorbPlaytimeFromPi(status: JSONObject) {
        val limit = status.optInt("playtime_limit_minutes", 0)
        val remaining = if (status.has("playtime_remaining_seconds") && !status.isNull("playtime_remaining_seconds")) {
            status.optInt("playtime_remaining_seconds", 0)
        } else {
            limit * 60
        }
        val running = status.optBoolean("models_ready", false) &&
            status.optBoolean("power_on", false) &&
            !status.optBoolean("pending_wake", false)
        val now = System.currentTimeMillis()
        playtimeClock = playtimeClock?.absorb(limit, remaining, now, running)
            ?: PlaytimeClock.fromPi(limit, remaining, now, running)
        renderPlaytimeClock()
    }

    private fun renderPlaytimeClock() {
        val b = _binding ?: return
        val clock = playtimeClock
        if (clock == null || clock.limitMinutes <= 0) {
            b.cardPlaytimeRing.visibility = View.GONE
            return
        }
        val remaining = clock.remainingAt(System.currentTimeMillis())
        val ring = PlaytimeRing.from(clock.limitMinutes, remaining)
        b.cardPlaytimeRing.visibility = View.VISIBLE
        b.playtimeRing.bind(ring)
        b.txtPlaytimeRemaining.text = ring.label
    }

    private fun updatePowerUI(powerOn: Boolean) {
        binding.btnPowerToggle.text = if (powerOn) {
            getString(R.string.dashboard_power_on)
        } else {
            getString(R.string.dashboard_power_off)
        }
    }

    private fun refreshAlerts() {
        val live = RobotConnectionManager.parentAlerts.value ?: emptyList()
        updateAlerts(live)
    }

    private fun updateAlerts(alerts: List<String>) {
        val container = binding.alertsContainer
        container.removeAllViews()

        if (alerts.isEmpty()) {
            val tv = TextView(requireContext()).apply {
                text = getString(R.string.dashboard_no_alerts)
                setPadding(32, 24, 32, 24)
                setTextColor(resources.getColor(R.color.text_hint, null))
            }
            container.addView(tv)
        } else {
            for (alert in alerts) {
                val tv = TextView(requireContext()).apply {
                    text = alert
                    setPadding(32, 16, 32, 16)
                    textSize = 14f
                }
                container.addView(tv)
            }
        }
    }

    override fun onDestroyView() {
        tickHandler.removeCallbacks(playtimeTicker)
        super.onDestroyView()
        _binding = null
    }
}
