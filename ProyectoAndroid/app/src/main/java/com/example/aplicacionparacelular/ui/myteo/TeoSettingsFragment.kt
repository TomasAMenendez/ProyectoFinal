package com.example.aplicacionparacelular.ui.myteo

import android.text.Editable
import android.text.TextWatcher
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
        binding.editPlaytime.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (rendering) return
                viewModel.onPlaytime(s?.toString().orEmpty())
            }
        })
        binding.btnApplyPlaytime.setOnClickListener { viewModel.applyPlaytime() }
        binding.groupVoice.setOnCheckedChangeListener { _, checkedId ->
            if (rendering) return@setOnCheckedChangeListener
            val voice = when (checkedId) {
                R.id.radio_voice_topa -> "topa"
                R.id.radio_voice_micaela -> "micaela"
                else -> return@setOnCheckedChangeListener
            }
            if (!viewModel.onVoice(voice)) render(viewModel.snapshot.value)
        }
        binding.switchVoiceLocal.setOnCheckedChangeListener { _, checked ->
            if (rendering) return@setOnCheckedChangeListener
            if (!viewModel.onLocalVoice(checked)) render(viewModel.snapshot.value)
        }
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
        binding.tilPlaytime.isEnabled = snapshot.playtimeEnabled
        binding.editPlaytime.isEnabled = snapshot.playtimeEnabled
        binding.btnApplyPlaytime.isEnabled = snapshot.canApplyPlaytime
        binding.txtPlaytimeConfirmed.text = playtimeLabel(snapshot.playtimeMinutes)
        if (binding.editPlaytime.text?.toString() != snapshot.playtimeField) {
            binding.editPlaytime.setText(snapshot.playtimeField)
        }
        val voiceId = when (snapshot.voice) {
            "micaela" -> R.id.radio_voice_micaela
            "topa" -> R.id.radio_voice_topa
            else -> -1
        }
        binding.radioVoiceTopa.isEnabled = snapshot.voiceEnabled
        binding.radioVoiceMicaela.isEnabled = snapshot.voiceEnabled
        if (voiceId != -1 && binding.groupVoice.checkedRadioButtonId != voiceId) {
            binding.groupVoice.check(voiceId)
        }
        binding.switchVoiceLocal.isEnabled = snapshot.localVoiceEnabled
        if (snapshot.localVoice != null && binding.switchVoiceLocal.isChecked != snapshot.localVoice) {
            binding.switchVoiceLocal.isChecked = snapshot.localVoice
        }
        binding.switchNight.isEnabled = snapshot.night != null
        if (snapshot.night != null && binding.switchNight.isChecked != snapshot.night) {
            binding.switchNight.isChecked = snapshot.night
        }
        rendering = false
    }

    private fun playtimeLabel(minutes: Int?): String = when (minutes) {
        null -> getString(R.string.settings_unknown)
        0 -> getString(R.string.settings_playtime_unlimited)
        else -> getString(R.string.settings_playtime_minutes, minutes)
    }

    private fun noticeText(notice: SettingsNotice): Int = when (notice) {
        SettingsNotice.LOADING -> R.string.settings_loading
        SettingsNotice.FAILED -> R.string.settings_failed
        SettingsNotice.APPLIED -> R.string.settings_applied
        SettingsNotice.APPLY_ERROR -> R.string.settings_apply_error
        SettingsNotice.NIGHT_ON -> R.string.settings_night_on
        SettingsNotice.NIGHT_OFF -> R.string.settings_night_off
        SettingsNotice.NIGHT_ERROR -> R.string.settings_night_error
        SettingsNotice.PLAYTIME_APPLIED -> R.string.settings_playtime_applied
        SettingsNotice.PLAYTIME_ERROR -> R.string.settings_playtime_error
        SettingsNotice.VOICE_APPLIED -> R.string.settings_voice_applied
        SettingsNotice.VOICE_ERROR -> R.string.settings_voice_error
        SettingsNotice.LOCAL_VOICE_ON -> R.string.settings_voice_local_on
        SettingsNotice.LOCAL_VOICE_OFF -> R.string.settings_voice_local_off
        SettingsNotice.LOCAL_VOICE_ERROR -> R.string.settings_voice_local_error
        SettingsNotice.NONE -> R.string.my_teo_settings_hint
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
