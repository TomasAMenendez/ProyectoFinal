package com.example.aplicacionparacelular.ui.myteo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.aplicacionparacelular.R
import com.example.aplicacionparacelular.databinding.FragmentTeoSettingsBinding
import com.example.aplicacionparacelular.ui.SystemBarInsets

class TeoSettingsFragment : Fragment() {
    private var _binding: FragmentTeoSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TeoSettingsViewModel by viewModels()
    private var rendering = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTeoSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        SystemBarInsets.applyVertical(binding.settingsScroll)
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.sliderVolume.addOnChangeListener { _, _, fromUser ->
            if (fromUser) viewModel.onSlider(binding.sliderVolume.value.toInt(), binding.sliderBrightness.value.toInt())
        }
        binding.sliderBrightness.addOnChangeListener { _, _, fromUser ->
            if (fromUser) viewModel.onSlider(binding.sliderVolume.value.toInt(), binding.sliderBrightness.value.toInt())
        }
        binding.btnApply.setOnClickListener { viewModel.apply() }
        binding.switchNight.setOnCheckedChangeListener { _, checked ->
            if (rendering) return@setOnCheckedChangeListener
            if (!viewModel.onNight(checked)) render(viewModel.snapshot.value)
        }
        viewModel.snapshot.observe(viewLifecycleOwner) { render(it) }
        viewModel.notice.observe(viewLifecycleOwner) { notice ->
            binding.txtStatus.setText(noticeText(notice))
        }
        viewModel.refresh()
    }

    private fun render(snapshot: SettingsSnapshot?) {
        if (_binding == null || snapshot == null) return
        rendering = true
        val known = snapshot.volume != null && snapshot.brightness != null
        binding.sliderVolume.isEnabled = snapshot.controlsEnabled
        binding.sliderBrightness.isEnabled = snapshot.controlsEnabled
        if (known) {
            if (binding.sliderVolume.value.toInt() != snapshot.volume) {
                binding.sliderVolume.value = snapshot.volume!!.toFloat()
            }
            if (binding.sliderBrightness.value.toInt() != snapshot.brightness) {
                binding.sliderBrightness.value = snapshot.brightness!!.toFloat()
            }
            binding.txtVolume.text = getString(R.string.settings_percent, snapshot.volume)
            binding.txtBrightness.text = getString(R.string.settings_percent, snapshot.brightness)
        } else {
            binding.txtVolume.setText(R.string.settings_unknown)
            binding.txtBrightness.setText(R.string.settings_unknown)
        }
        binding.btnApply.isEnabled = snapshot.canApply
        binding.switchNight.isEnabled = snapshot.night != null
        if (snapshot.night != null && binding.switchNight.isChecked != snapshot.night) {
            binding.switchNight.isChecked = snapshot.night
        }
        rendering = false
    }

    private fun noticeText(notice: SettingsNotice): Int = when (notice) {
        SettingsNotice.LOADING -> R.string.settings_loading
        SettingsNotice.FAILED -> R.string.settings_failed
        SettingsNotice.APPLIED -> R.string.settings_applied
        SettingsNotice.APPLY_ERROR -> R.string.settings_apply_error
        SettingsNotice.NIGHT_ON -> R.string.settings_night_on
        SettingsNotice.NIGHT_OFF -> R.string.settings_night_off
        SettingsNotice.NIGHT_ERROR -> R.string.settings_night_error
        SettingsNotice.NONE -> R.string.my_teo_settings_hint
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
