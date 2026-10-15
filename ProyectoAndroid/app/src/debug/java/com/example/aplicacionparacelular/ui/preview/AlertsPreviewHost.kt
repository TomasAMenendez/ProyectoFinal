package com.example.aplicacionparacelular.ui.preview

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.aplicacionparacelular.R
import com.example.aplicacionparacelular.databinding.FragmentAlertsBinding
import com.example.aplicacionparacelular.network.ParentInbox
import com.example.aplicacionparacelular.network.ParentNotice
import com.example.aplicacionparacelular.ui.SystemBarInsets
import com.example.aplicacionparacelular.ui.alerts.AlertsScreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class AlertsPreviewScenario(val labelRes: Int) {
    EXAMPLES(R.string.preview_alerts_examples),
    EMPTY(R.string.preview_alerts_empty),
    OFFLINE(R.string.preview_alerts_offline),
    LOADING(R.string.preview_alerts_loading),
    ERROR(R.string.preview_alerts_error),
}

class AlertsPreviewHost(
    private val activity: AppCompatActivity,
    private val binding: FragmentAlertsBinding,
    private val onChanged: () -> Unit = {},
) {
    var scenario = AlertsPreviewScenario.EXAMPLES
    val scenarioLabel: String get() = activity.getString(scenario.labelRes)

    init {
        SystemBarInsets.applyBottom(binding.root)
        binding.btnAlertsRetry.setOnClickListener {
            scenario = AlertsPreviewScenario.EXAMPLES
            render()
            onChanged()
        }
    }

    fun choose(next: AlertsPreviewScenario) {
        scenario = next
        render()
    }

    fun render() {
        when (scenario) {
            AlertsPreviewScenario.LOADING -> showStatus(R.string.preview_alerts_loading, retry = false)
            AlertsPreviewScenario.ERROR -> showStatus(R.string.preview_alerts_error, retry = true)
            else -> {
                binding.txtAlertsStatus.visibility = View.GONE
                binding.btnAlertsRetry.visibility = View.GONE
                val notices = if (scenario == AlertsPreviewScenario.EMPTY) emptyList() else examples()
                AlertsScreen.bind(
                    binding.alertsList,
                    binding.txtEmpty,
                    binding.txtOffline,
                    notices,
                    connected = scenario != AlertsPreviewScenario.OFFLINE,
                )
            }
        }
    }

    private fun showStatus(text: Int, retry: Boolean) {
        binding.txtAlertsStatus.setText(text)
        binding.txtAlertsStatus.visibility = View.VISIBLE
        binding.btnAlertsRetry.visibility = if (retry) View.VISIBLE else View.GONE
        binding.txtOffline.visibility = View.GONE
        binding.txtEmpty.visibility = View.GONE
        binding.alertsList.visibility = View.GONE
        binding.alertsList.removeAllViews()
    }

    private fun examples(): List<ParentNotice> {
        val received = 1_000L
        val notices = listOfNotNull(
            ParentInbox.notice("logro", "Ejemplo: TEO acompañó a guardar los juguetes.", stamp(0, 14, 30), received),
            ParentInbox.notice("pedido", "Ejemplo pidió escuchar una canción.", stamp(0, 11, 15), received + 1),
            ParentInbox.notice("crisis", "Ejemplo: TEO registró un momento de malestar.", stamp(-1, 18, 40), received + 2),
            ParentInbox.notice("aviso", "Ejemplo: hoy leyeron juntos.", stamp(-2, 16, 5), received + 3),
            ParentInbox.notice("aviso", "Ejemplo: este aviso no tiene fecha del evento.", "sin-fecha", received + 4),
        )
        return ParentInbox.merge(emptyList(), notices)
    }

    private fun stamp(dayOffset: Int, hour: Int, minute: Int): String {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, dayOffset)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(calendar.time)
    }
}
