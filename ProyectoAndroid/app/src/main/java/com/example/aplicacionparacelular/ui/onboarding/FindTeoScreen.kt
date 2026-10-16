package com.example.aplicacionparacelular.ui.onboarding

import android.view.View
import androidx.core.view.isVisible
import com.example.aplicacionparacelular.R
import com.example.aplicacionparacelular.databinding.FragmentOnboardingRetryBinding
import com.example.aplicacionparacelular.onboarding.FindHint
import com.example.aplicacionparacelular.onboarding.FindTeoPhase
import com.example.aplicacionparacelular.onboarding.FindTeoRules
import com.example.aplicacionparacelular.ui.dashboard.TeoCharacterPresenter

object FindTeoScreen {
    fun render(binding: FragmentOnboardingRetryBinding, phase: FindTeoPhase) {
        val hints = FindTeoRules.hints(phase)
        TeoCharacterPresenter.applyWave(binding.imgCharacter, null)
        binding.progressRetry.isVisible = phase == FindTeoPhase.SEARCHING
        binding.txtSearchStatus.isVisible = phase == FindTeoPhase.SEARCHING
        binding.cardHints.isVisible = phase != FindTeoPhase.SEARCHING
        binding.rowPower.isVisible = FindHint.POWER in hints
        binding.rowBluetooth.isVisible = FindHint.BLUETOOTH in hints
        binding.rowWifi.isVisible = FindHint.WIFI in hints
        binding.txtPermission.isVisible = phase == FindTeoPhase.PERMISSION_NEEDED ||
            phase == FindTeoPhase.PERMISSION_BLOCKED
        binding.btnRetryDiscovery.isEnabled = FindTeoRules.canStartSearch(phase)
        binding.btnRetryBluetoothSettings.isVisible =
            phase == FindTeoPhase.NOT_FOUND || phase == FindTeoPhase.BLUETOOTH_OFF
        binding.btnRetryAppSettings.isVisible = phase == FindTeoPhase.PERMISSION_BLOCKED
        val title = when (phase) {
            FindTeoPhase.NOT_FOUND -> R.string.onboarding_find_missing_title
            FindTeoPhase.BLUETOOTH_OFF -> R.string.onboarding_find_bluetooth_off_title
            FindTeoPhase.PERMISSION_NEEDED -> R.string.onboarding_find_permission_title
            FindTeoPhase.PERMISSION_BLOCKED -> R.string.onboarding_find_permission_blocked_title
            FindTeoPhase.SEARCHING -> 0
        }
        if (title != 0) binding.txtCardTitle.setText(title)
        val permission = when (phase) {
            FindTeoPhase.PERMISSION_NEEDED -> R.string.onboarding_find_permission
            FindTeoPhase.PERMISSION_BLOCKED -> R.string.onboarding_find_permission_blocked
            else -> 0
        }
        if (permission != 0) binding.txtPermission.setText(permission)
        binding.btnRetryDiscovery.visibility = View.VISIBLE
    }
}
