package com.example.aplicacionparacelular.ui.stories

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.aplicacionparacelular.R
import com.example.aplicacionparacelular.databinding.FragmentStoryFormBinding
import com.example.aplicacionparacelular.ui.SystemBarInsets

class StoryFormFragment : Fragment() {
    private var _binding: FragmentStoryFormBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: StoriesViewModel
    private var filled = false

    private val pdfPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val uri = result.data?.data ?: return@registerForActivityResult
        val context = context ?: return@registerForActivityResult
        ContentFiles.describe(context, uri, "cuento.pdf") { summary ->
            if (_binding == null) return@describe
            if (summary == null) {
                showError(getString(R.string.content_read_failed))
                return@describe
            }
            val problem = ContentRules.pdfProblem(summary.name, summary.sizeBytes)
            if (problem != null) {
                showError(problem)
                return@describe
            }
            viewModel.rememberPdf(uri, summary.name, summary.sizeBytes)
            binding.txtFormError.isVisible = false
            paintPdf()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewModel = ViewModelProvider(requireActivity())[StoriesViewModel::class.java]
        _binding = FragmentStoryFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        SystemBarInsets.applyVertical(binding.formScroll)
        val editing = arguments?.getString(ARG_MODE) == MODE_EDIT
        val storyId = arguments?.getString(ARG_ID).orEmpty()
        binding.txtFormTitle.setText(if (editing) R.string.content_edit_story else R.string.content_add_story)
        binding.txtFormIntro.setText(if (editing) R.string.content_edit_story_intro else R.string.content_add_story_intro)
        binding.cardPdf.isVisible = !editing
        binding.groupCreateNotes.isVisible = !editing
        binding.txtTextLabel.isVisible = editing
        binding.inputText.isVisible = editing
        binding.txtTitleHelp.setText(if (editing) R.string.content_title_edit_help else R.string.content_title_create_help)
        binding.btnSave.setText(if (editing) R.string.routines_save_changes else R.string.content_upload_story)
        binding.btnBack.setOnClickListener { close() }
        binding.btnCancel.setOnClickListener { close() }
        binding.btnChangePdf.setOnClickListener { openPdf() }
        if (!editing && savedInstanceState == null) {
            viewModel.clearPdf()
        } else if (!editing && viewModel.pdfName == null) {
            restorePdf(savedInstanceState)
        }
        paintPdf()
        if (savedInstanceState != null) filled = true
        binding.btnSave.setOnClickListener {
            if (editing) {
                viewModel.saveStory(storyId, binding.editTitle.text?.toString().orEmpty(), binding.editText.text?.toString().orEmpty())
            } else {
                submitPdf()
            }
        }
        if (editing && savedInstanceState == null) viewModel.loadStory(storyId)
        viewModel.storyToEdit.observe(viewLifecycleOwner) { detail ->
            if (!editing || detail == null || detail.id != storyId || filled) return@observe
            binding.editTitle.setText(detail.title)
            binding.editText.setText(detail.text)
            filled = true
        }
        viewModel.formMessage.observe(viewLifecycleOwner) { message ->
            if (message.isNullOrBlank()) {
                binding.txtFormError.isVisible = false
            } else {
                showError(message)
            }
        }
        viewModel.formAccepted.observe(viewLifecycleOwner) { accepted ->
            if (accepted == true) close()
        }
        viewModel.pending.observe(viewLifecycleOwner) { pending ->
            binding.btnSave.isEnabled = pending != true
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_URI, viewModel.pdfUri?.toString())
        outState.putString(STATE_NAME, viewModel.pdfName)
        outState.putLong(STATE_SIZE, viewModel.pdfSize)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun openPdf() {
        pdfPicker.launch(ContentPickers.pdf())
    }

    private fun paintPdf() {
        ContentPickers.bindPdf(
            binding.txtPdfName,
            binding.txtPdfMeta,
            binding.btnChangePdf,
            viewModel.pdfName,
            viewModel.pdfSize,
        )
    }

    private fun restorePdf(state: Bundle?) {
        val uri = state?.getString(STATE_URI)?.let(Uri::parse) ?: return
        val name = state.getString(STATE_NAME) ?: return
        val size = state.getLong(STATE_SIZE, -1)
        if (size < 0) return
        viewModel.rememberPdf(uri, name, size)
    }

    private fun submitPdf() {
        val uri = viewModel.pdfUri
        val name = viewModel.pdfName
        if (uri == null || name == null) {
            showError(getString(R.string.content_pdf_empty))
            return
        }
        val problem = ContentRules.pdfProblem(name, viewModel.pdfSize)
        if (problem != null) {
            showError(problem)
            return
        }
        val title = binding.editTitle.text?.toString()
        val context = context ?: return
        ContentFiles.read(context, uri, name) { picked ->
            if (picked == null) {
                if (_binding != null) showError(getString(R.string.content_read_failed))
                return@read
            }
            viewModel.uploadPdf(picked.name, picked.bytes, title)
        }
    }

    private fun showError(message: String) {
        binding.txtFormError.text = message
        binding.txtFormError.isVisible = true
    }

    private fun close() {
        viewModel.clearStoryToEdit()
        findNavController().popBackStack()
    }

    companion object {
        const val ARG_MODE = "mode"
        const val ARG_ID = "storyId"
        const val MODE_CREATE = "create"
        const val MODE_EDIT = "edit"
        private const val STATE_URI = "pdf_uri"
        private const val STATE_NAME = "pdf_name"
        private const val STATE_SIZE = "pdf_size"
    }
}
