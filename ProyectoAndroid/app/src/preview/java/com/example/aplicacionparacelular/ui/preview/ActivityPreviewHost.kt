package com.example.aplicacionparacelular.ui.preview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.example.aplicacionparacelular.R
import com.example.aplicacionparacelular.databinding.FragmentMetricsBinding
import com.example.aplicacionparacelular.ui.SystemBarInsets
import com.example.aplicacionparacelular.ui.metrics.ActivityAreaBar
import com.example.aplicacionparacelular.ui.metrics.ActivityAreaLabels
import com.example.aplicacionparacelular.ui.metrics.ActivityDayBar
import com.example.aplicacionparacelular.ui.metrics.ActivityPhase
import com.example.aplicacionparacelular.ui.metrics.ActivityPresentation
import com.example.aplicacionparacelular.ui.metrics.ActivityScreen
import java.util.Calendar

enum class ActivityPreviewScenario(val labelRes: Int) {
    DATA(R.string.preview_activity_data),
    EMPTY(R.string.preview_activity_empty),
    LOADING(R.string.preview_activity_loading),
    ERROR(R.string.preview_activity_error),
    OFFLINE(R.string.preview_activity_offline),
}

object ActivityPreviewSamples {
    fun presentation(scenario: ActivityPreviewScenario, days: Int): ActivityPresentation {
        val phase = when (scenario) {
            ActivityPreviewScenario.LOADING -> ActivityPhase.LOADING
            ActivityPreviewScenario.ERROR -> ActivityPhase.FAILED
            ActivityPreviewScenario.OFFLINE -> ActivityPhase.OFFLINE
            else -> ActivityPhase.READY
        }
        if (phase != ActivityPhase.READY) {
            return ActivityPresentation(days, phase, null, null, null, null, null, null, null, null)
        }
        val labels = dayLabels(days)
        if (scenario == ActivityPreviewScenario.EMPTY) {
            return ActivityPresentation(
                requestedDays = days,
                phase = ActivityPhase.READY,
                interactions = 0,
                games = 0,
                routines = 0,
                crises = 0,
                days = labels.map { ActivityDayBar(it, 0) },
                areas = listOf(
                    ActivityAreaBar("Emocional", 0),
                    ActivityAreaBar("Cognitivo", 0),
                    ActivityAreaBar("Vincular", 0),
                    ActivityAreaBar("Autonomía", 0),
                ),
                newWords = 0,
                vocabularyTotal = null,
            )
        }
        val values = when (days) {
            1 -> listOf(3)
            30 -> List(23) { (it % 4) + 1 } + listOf(8, 12, 15, 10, 14, 13, 12)
            else -> listOf(8, 12, 15, 10, 14, 13, 12)
        }
        val bars = labels.zip(values).map { ActivityDayBar(it.first, it.second) }
        return ActivityPresentation(
            requestedDays = days,
            phase = ActivityPhase.READY,
            interactions = values.sum(),
            games = when (days) {
                1 -> 1
                7 -> 6
                else -> 14
            },
            routines = when (days) {
                1 -> 0
                7 -> 12
                else -> 20
            },
            crises = when (days) {
                1 -> 0
                7 -> 2
                else -> 3
            },
            days = bars,
            areas = if (days == 1) {
                listOf(
                    ActivityAreaBar("Emocional", 2),
                    ActivityAreaBar("Cognitivo", 1),
                    ActivityAreaBar("Vincular", 0),
                    ActivityAreaBar("Autonomía", 0),
                )
            } else if (days == 7) {
                listOf(
                    ActivityAreaBar("Emocional", 30),
                    ActivityAreaBar("Cognitivo", 24),
                    ActivityAreaBar("Vincular", 18),
                    ActivityAreaBar("Autonomía", 12),
                )
            } else {
                listOf(
                    ActivityAreaBar("Emocional", 40),
                    ActivityAreaBar("Cognitivo", 28),
                    ActivityAreaBar("Vincular", 16),
                    ActivityAreaBar("Autonomía", 9),
                )
            },
            newWords = if (days == 1) 1 else 5,
            vocabularyTotal = null,
        )
    }

    private fun dayLabels(count: Int): List<String> {
        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 13, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -(count - 1))
        }
        return List(count) {
            val label = "%02d/%02d".format(
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.MONTH) + 1,
            )
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            label
        }
    }
}

class ActivityPreviewHost(
    activity: AppCompatActivity,
    parent: ViewGroup,
    private val onBack: () -> Unit,
    private val onChanged: () -> Unit = {},
) {
    private val binding = FragmentMetricsBinding.inflate(LayoutInflater.from(activity), parent, true)
    val root: View = binding.root
    var scenario = ActivityPreviewScenario.DATA
    var days = 7
    private var applying = false
    val scenarioLabel: String get() = binding.root.context.getString(scenario.labelRes)

    init {
        SystemBarInsets.applyBottom(root)
        binding.btnBack.setOnClickListener { onBack() }
        binding.btnRetry.setOnClickListener {
            scenario = ActivityPreviewScenario.DATA
            render()
            onChanged()
        }
        binding.periodGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked || applying) return@addOnButtonCheckedListener
            days = when (checkedId) {
                R.id.btn_today -> 1
                R.id.btn_month -> 30
                else -> 7
            }
            render()
            onChanged()
        }
        render()
    }

    fun choose(next: ActivityPreviewScenario) {
        scenario = next
        render()
    }

    fun render() {
        applying = true
        val button = when (days) {
            1 -> R.id.btn_today
            30 -> R.id.btn_month
            else -> R.id.btn_week
        }
        if (binding.periodGroup.checkedButtonId != button) binding.periodGroup.check(button)
        applying = false
        val labels = ActivityAreaLabels(
            binding.root.context.getString(R.string.activity_area_emotional),
            binding.root.context.getString(R.string.activity_area_cognitive),
            binding.root.context.getString(R.string.activity_area_social),
            binding.root.context.getString(R.string.activity_area_autonomy),
        )
        val sample = ActivityPreviewSamples.presentation(scenario, days)
        val names = listOf(labels.emotional, labels.cognitive, labels.social, labels.autonomy)
        val areas = sample.areas?.mapIndexed { index, area ->
            area.copy(label = names[index])
        }
        ActivityScreen.render(binding, sample.copy(areas = areas))
    }
}
