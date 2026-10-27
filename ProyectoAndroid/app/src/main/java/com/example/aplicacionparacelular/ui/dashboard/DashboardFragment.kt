package com.example.aplicacionparacelular.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.aplicacionparacelular.R
import com.example.aplicacionparacelular.ui.character.TeoAppearance
import com.example.aplicacionparacelular.databinding.FragmentDashboardBinding
import com.example.aplicacionparacelular.network.ParentNotice
import com.example.aplicacionparacelular.network.RobotConnectionManager
import com.example.aplicacionparacelular.ui.SystemBarInsets
import com.google.android.material.snackbar.Snackbar

import org.json.JSONObject

class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: DashboardViewModel
    private var connected = false
    private var latestStatus: HomeStatusView = HomeStatus.from(false, false, false, false)
    private var latestRobotStatus: JSONObject? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        viewModel = ViewModelProvider(this)[DashboardViewModel::class.java]
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        SystemBarInsets.applyTop(binding.homeScroll)
        stackIfNeeded()
        binding.imgCharacter.addOnLayoutChangeListener { view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (right - left == oldRight - oldLeft && bottom - top == oldBottom - oldTop) return@addOnLayoutChangeListener
            TeoCharacterPresenter.applyWave(view as ImageView, TeoAppearance.costume.value)
        }
        TeoAppearance.costume.observe(viewLifecycleOwner) { costume ->
            TeoCharacterPresenter.applyWave(binding.imgCharacter, costume)
        }

        RobotConnectionManager.isConnected.observe(viewLifecycleOwner) { isConnected ->
            connected = isConnected == true
            renderStatus()
            renderPlayback()
            renderPlaytime()
        }
        RobotConnectionManager.robotStatus.observe(viewLifecycleOwner) { status ->
            latestRobotStatus = status
            if (status != null && connected) {
                latestStatus = HomeStatus.from(
                    connected = true,
                    powerOn = status.optBoolean("power_on", false),
                    modelsReady = status.optBoolean("models_ready", false),
                    pendingWake = status.optBoolean("pending_wake", false),
                )
                viewModel.applyRobotStatus(status)
            } else if (connected) {
                latestStatus = HomeStatus.from(true, powerOn = false, modelsReady = false, pendingWake = false)
            } else {
                latestStatus = HomeStatus.from(false, false, false, false)
            }
            renderStatus()
            renderPlaytime()
        }
        RobotConnectionManager.parentAlerts.observe(viewLifecycleOwner) { notices ->
            renderNotice(notices.firstOrNull())
        }

        viewModel.interactions.observe(viewLifecycleOwner) { value ->
            HomeScreenRenderer.interactions(binding, value ?: getString(R.string.home_absent))
        }
        viewModel.signal.observe(viewLifecycleOwner) { value ->
            HomeScreenRenderer.signal(
                binding,
                value ?: getString(R.string.home_absent),
                !value.isNullOrBlank(),
            )
        }
        viewModel.playback.observe(viewLifecycleOwner) { renderPlayback() }
        viewModel.catalogCount.observe(viewLifecycleOwner) { renderPlayback() }
        viewModel.busy.observe(viewLifecycleOwner) { waiting ->
            binding.txtWaiting.visibility = if (waiting == true) View.VISIBLE else View.GONE
            renderStatus()
            renderPlayback()
        }
        viewModel.message.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrBlank()) {
                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
            }
        }

        binding.btnPower.setOnClickListener {
            val turnOn = latestStatus.activity != TeoActivity.AWAKE
            viewModel.requestPower(turnOn)
        }
        binding.btnConnection.setOnClickListener { open(R.id.nav_connection) }
        binding.btnMyTeo.setOnClickListener { open(R.id.nav_config) }
        binding.btnActivity.setOnClickListener { open(R.id.nav_metrics) }
        binding.btnContent.setOnClickListener { open(R.id.nav_stories) }
        binding.btnAlertsBell.setOnClickListener { open(R.id.nav_alerts) }
        binding.btnAllAlerts.setOnClickListener { open(R.id.nav_alerts) }
        binding.btnMusicPlay.setOnClickListener { viewModel.playNow() }
        binding.btnMusicStop.setOnClickListener { viewModel.stopNow() }
        binding.btnCelebrate.setOnClickListener { viewModel.celebrate() }

        viewModel.refreshTelemetry()
        viewModel.refreshMusic()
        renderNotice(RobotConnectionManager.parentAlerts.value?.firstOrNull())
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshTelemetry()
        viewModel.refreshMusic()
        stackIfNeeded()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun open(destinationId: Int) {
        findNavController().navigate(destinationId)
    }

    private fun renderStatus() {
        val status = if (connected) latestStatus else HomeStatus.from(false, false, false, false)
        HomeScreenRenderer.status(binding, status, viewModel.busy.value == true)
    }

    private fun renderPlaytime() {
        if (_binding == null) return
        HomeScreenRenderer.playtime(
            binding,
            PlaytimeReading.from(connected, latestRobotStatus),
            getString(R.string.home_absent),
        )
    }

    private fun renderPlayback() {
        val snapshot = viewModel.playback.value ?: PlaybackSnapshot(null, null, null)
        val count = viewModel.catalogCount.value ?: -1
        HomeScreenRenderer.playback(
            binding,
            NowPlaying.view(connected, snapshot, count),
            viewModel.busy.value == true,
        )
    }

    private fun renderNotice(notice: ParentNotice?) {
        HomeScreenRenderer.notice(binding, notice)
    }

    private fun stackIfNeeded() {
        HomeScreenRenderer.stack(
            binding,
            resources.configuration.fontScale,
            resources.configuration.screenWidthDp,
        )
    }
}
