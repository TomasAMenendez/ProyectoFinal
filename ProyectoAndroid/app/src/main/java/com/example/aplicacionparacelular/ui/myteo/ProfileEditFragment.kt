package com.example.aplicacionparacelular.ui.myteo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.findNavController
import com.example.aplicacionparacelular.R
import com.example.aplicacionparacelular.databinding.FragmentProfileEditBinding
import com.example.aplicacionparacelular.network.ApiResult
import com.example.aplicacionparacelular.network.RobotConnectionManager
import com.example.aplicacionparacelular.onboarding.ChildProfile
import com.example.aplicacionparacelular.ui.SystemBarInsets
import com.example.aplicacionparacelular.ui.onboarding.ChildProfileForm
import com.google.android.material.snackbar.Snackbar
import org.json.JSONObject

class ProfileEditViewModel : ViewModel() {
    var draft: ChildProfile? = null
    var dirty: Boolean = false
}

class ProfileEditFragment : Fragment() {
    private var _binding: FragmentProfileEditBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileEditViewModel by viewModels()
    private lateinit var form: ChildProfileForm
    private var saving = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        SystemBarInsets.applyVertical(binding.profileScroll)
        form = ChildProfileForm(binding.form)
        savedInstanceState?.getString("profile_draft")?.let {
            viewModel.draft = ChildProfile.parse(JSONObject(it))
            viewModel.dirty = true
        }
        binding.form.btnSave.setText(R.string.profile_save_changes)
        form.bind {
            viewModel.draft = form.read()
            viewModel.dirty = true
            form.refreshSaveEnabled()
        }
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.form.btnSave.setOnClickListener { save() }
        val draft = viewModel.draft
        if (viewModel.dirty && draft != null) {
            form.restoreDraft(draft)
        } else {
            load()
        }
    }

    private fun load() {
        RobotConnectionManager.fetchChildProfile { result ->
            if (_binding == null || viewModel.dirty) return@fetchChildProfile
            when (result) {
                is ApiResult.Success -> {
                    val profile = ChildProfile.parse(result.data)
                    viewModel.draft = profile
                    form.populate(profile)
                }
                is ApiResult.Error -> {
                    Snackbar.make(binding.root, R.string.child_profile_load_error, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun save() {
        if (saving || !form.read().canSubmit) return
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
                        viewModel.dirty = false
                        viewModel.draft = saved
                        form.markClean()
                        findNavController().popBackStack()
                    } else {
                        Snackbar.make(binding.root, R.string.child_profile_save_error, Snackbar.LENGTH_LONG).show()
                    }
                }
                is ApiResult.Error -> {
                    Snackbar.make(binding.root, R.string.child_profile_save_error, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (_binding != null && form.hasChanges) outState.putString("profile_draft", form.payload().toString())
        super.onSaveInstanceState(outState)
    }
}
