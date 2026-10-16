package com.example.aplicacionparacelular.ui.alerts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.aplicacionparacelular.R
import com.example.aplicacionparacelular.databinding.FragmentAlertsBinding
import com.example.aplicacionparacelular.network.ParentNotice
import com.example.aplicacionparacelular.network.RobotConnectionManager
import com.example.aplicacionparacelular.ui.SystemBarInsets

class AlertsFragment : Fragment() {
    private var _binding: FragmentAlertsBinding? = null
    private val binding get() = _binding!!
    private var notices: List<ParentNotice> = emptyList()
    private var connected = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAlertsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        SystemBarInsets.applyVertical(binding.root)
        binding.btnBack.setOnClickListener {
            val controller = findNavController()
            if (!controller.popBackStack()) controller.navigate(R.id.nav_dashboard)
        }
        binding.txtAlertsStatus.visibility = View.GONE
        binding.btnAlertsRetry.visibility = View.GONE
        RobotConnectionManager.parentAlerts.observe(viewLifecycleOwner) {
            notices = it.orEmpty()
            render()
        }
        RobotConnectionManager.isConnected.observe(viewLifecycleOwner) {
            connected = it == true
            render()
        }
    }

    private fun render() {
        val current = _binding ?: return
        AlertsScreen.bind(current.alertsList, current.txtEmpty, current.txtOffline, notices, connected)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
