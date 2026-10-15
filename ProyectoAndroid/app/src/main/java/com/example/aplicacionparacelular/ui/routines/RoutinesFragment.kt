package com.example.aplicacionparacelular.ui.routines

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.aplicacionparacelular.R
import com.example.aplicacionparacelular.databinding.FragmentRoutinesBinding
import com.example.aplicacionparacelular.ui.SystemBarInsets
import com.google.android.material.snackbar.Snackbar

class RoutinesFragment : Fragment() {

    private var _binding: FragmentRoutinesBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: RoutinesViewModel
    private lateinit var adapter: RoutineAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        viewModel = ViewModelProvider(requireActivity())[RoutinesViewModel::class.java]
        _binding = FragmentRoutinesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        SystemBarInsets.applyTop(binding.routinesRoot)
        adapter = RoutineAdapter(
            onToggle = { item -> viewModel.toggleRoutine(item.id) },
            onEdit = { item -> openEditor(edit = true, id = item.id) },
            onDelete = { item ->
                RoutineDeleteDialog.show(requireContext(), item.name) {
                    viewModel.removeRoutine(item.id)
                }
            },
        )
        binding.recyclerRoutines.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerRoutines.adapter = adapter
        binding.btnAdd.setOnClickListener { openEditor(edit = false, id = "") }
        binding.btnAlertsBell.setOnClickListener { findNavController().navigate(R.id.nav_alerts) }
        binding.btnMyTeo.setOnClickListener { findNavController().navigate(R.id.nav_config) }
        viewModel.routines.observe(viewLifecycleOwner) { routines ->
            adapter.submitList(routines)
            val empty = routines.isEmpty()
            binding.txtEmpty.visibility = if (empty) View.VISIBLE else View.GONE
            binding.recyclerRoutines.visibility = if (empty) View.GONE else View.VISIBLE
        }
        viewModel.syncStatus.observe(viewLifecycleOwner) { status ->
            if (!status.isNullOrBlank()) {
                Snackbar.make(binding.root, status, Snackbar.LENGTH_LONG).show()
                viewModel.consumeStatus()
            }
        }
        viewModel.attachContext(requireContext())
        viewModel.loadRoutines()
    }

    private fun openEditor(edit: Boolean, id: String) {
        findNavController().navigate(
            R.id.nav_routine_editor,
            bundleOf(
                "mode" to if (edit) "edit" else "create",
                "routineId" to id,
            ),
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
