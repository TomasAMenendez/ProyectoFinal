package com.example.aplicacionparacelular.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.aplicacionparacelular.R
import com.example.aplicacionparacelular.databinding.FragmentChildProfileBinding
import com.example.aplicacionparacelular.network.ApiResult
import com.example.aplicacionparacelular.network.RobotConnectionManager
import com.example.aplicacionparacelular.onboarding.ChildProfile
import com.google.android.material.snackbar.Snackbar

class ChildProfileFragment : Fragment() {

    private var _binding: FragmentChildProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var form: ChildProfileForm
    private var saving = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentChildProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    requireActivity().moveTaskToBack(true)
                }
            },
        )

        form = ChildProfileForm(binding.form)
        form.bind { form.refreshSaveEnabled() }
        binding.form.btnSave.setOnClickListener { save() }
        loadProfile()
    }

    private fun loadProfile() {
        RobotConnectionManager.fetchChildProfile { result ->
            if (_binding == null) return@fetchChildProfile
            when (result) {
                is ApiResult.Success -> form.populate(ChildProfile.parse(result.data))
                is ApiResult.Error -> {
                    Snackbar.make(binding.root, R.string.child_profile_load_error, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun save() {
        if (saving) return
        val profile = form.read()
        if (!profile.canSubmit) return
        saving = true
        binding.form.btnSave.isEnabled = false
        RobotConnectionManager.saveChildProfile(form.payload()) { result ->
            if (_binding == null) return@saveChildProfile
            saving = false
            form.refreshSaveEnabled()
            when (result) {
                is ApiResult.Success -> {
                    val saved = ChildProfile.parse(result.data)
                    if (saved.complete) {
                        findNavController().popBackStack(R.id.nav_dashboard, false)
                    } else {
                        Snackbar.make(binding.root, R.string.child_profile_save_error, Snackbar.LENGTH_SHORT).show()
                    }
                }
                is ApiResult.Error -> {
                    Snackbar.make(
                        binding.root,
                        getString(R.string.child_profile_save_error),
                        Snackbar.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
