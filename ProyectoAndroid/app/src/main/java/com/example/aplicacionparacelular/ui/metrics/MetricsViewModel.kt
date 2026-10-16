package com.example.aplicacionparacelular.ui.metrics

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.aplicacionparacelular.network.ApiResult
import com.example.aplicacionparacelular.network.RobotConnectionManager

class MetricsViewModel : ViewModel() {
    private val gate = MetricsRequestGate()
    private val _screen = MutableLiveData<MetricsScreen>()
    val screen: LiveData<MetricsScreen> = _screen

    fun loadTodayMetrics() = loadRangeMetrics(1)

    fun loadWeekMetrics() = loadRangeMetrics(7)

    fun loadRangeMetrics(days: Int) {
        val request = gate.begin()
        _screen.value = MetricsScreen(days, ActivityPhase.LOADING, null)
        RobotConnectionManager.fetchTelemetryRange(days) { result ->
            if (!gate.isCurrent(request)) return@fetchTelemetryRange
            val snap = (result as? ApiResult.Success)?.let { MetricsPeriodParser.parse(it.data) }
            val phase = MetricsLoad.phase(
                connected = RobotConnectionManager.isConnected.value == true,
                success = result is ApiResult.Success,
                summaryKnown = snap?.summaryKnown == true,
            )
            _screen.value = MetricsScreen(
                requestedDays = days,
                phase = phase,
                snapshot = if (phase == ActivityPhase.READY) snap else null,
            )
        }
    }
}
