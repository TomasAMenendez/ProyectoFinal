package com.example.aplicacionparacelular.ui.myteo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.aplicacionparacelular.BuildConfig
import com.example.aplicacionparacelular.R
import com.example.aplicacionparacelular.databinding.FragmentMyTeoBinding
import com.example.aplicacionparacelular.ui.SystemBarInsets

class MyTeoFragment : Fragment() {
    private var _binding: FragmentMyTeoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMyTeoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        SystemBarInsets.applyVertical(binding.menuScroll)
        binding.txtVersion.text = getString(R.string.my_teo_version, BuildConfig.VERSION_NAME)
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.cardSettings.setOnClickListener { findNavController().navigate(R.id.nav_teo_settings) }
        binding.cardConnection.setOnClickListener { findNavController().navigate(R.id.nav_connection) }
        binding.cardProfile.setOnClickListener { findNavController().navigate(R.id.nav_profile_edit) }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
