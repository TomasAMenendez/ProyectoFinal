package com.example.aplicacionparacelular.ui.metrics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.aplicacionparacelular.R
import com.example.aplicacionparacelular.databinding.FragmentMetricsBinding
import com.example.aplicacionparacelular.ui.SystemBarInsets

class MetricsFragment : Fragment() {
    private var _binding: FragmentMetricsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: MetricsViewModel
    private var applyingPeriod = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        viewModel = ViewModelProvider(this)[MetricsViewModel::class.java]
        _binding = FragmentMetricsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        SystemBarInsets.applyVertical(binding.metricsScroll)
        binding.btnBack.setOnClickListener {
            val controller = findNavController()
            if (!controller.popBackStack()) controller.navigate(R.id.nav_dashboard)
        }
        binding.btnRetry.setOnClickListener {
            viewModel.loadRangeMetrics(viewModel.screen.value?.requestedDays ?: 7)
        }
        binding.periodGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked || applyingPeriod) return@addOnButtonCheckedListener
            viewModel.loadRangeMetrics(
                when (checkedId) {
                    R.id.btn_today -> 1
                    R.id.btn_month -> 30
                    else -> 7
                },
            )
        }
        viewModel.screen.observe(viewLifecycleOwner) { screen ->
            applyingPeriod = true
            val button = when (screen.requestedDays) {
                1 -> R.id.btn_today
                30 -> R.id.btn_month
                else -> R.id.btn_week
            }
            if (binding.periodGroup.checkedButtonId != button) binding.periodGroup.check(button)
            applyingPeriod = false
            ActivityScreen.render(
                binding,
                ActivitySnapshots.of(
                    screen,
                    ActivityAreaLabels(
                        getString(R.string.activity_area_emotional),
                        getString(R.string.activity_area_cognitive),
                        getString(R.string.activity_area_social),
                        getString(R.string.activity_area_autonomy),
                    ),
                ),
            )
        }
        if (viewModel.screen.value == null) viewModel.loadRangeMetrics(7)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
