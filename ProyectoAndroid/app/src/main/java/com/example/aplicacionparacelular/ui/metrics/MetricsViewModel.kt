package com.example.aplicacionparacelular.ui.metrics

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.aplicacionparacelular.network.ApiResult
import com.example.aplicacionparacelular.network.RobotConnectionManager

class MetricsViewModel : ViewModel() {

    private val _pillars = MutableLiveData(PillarData())
    val pillars: LiveData<PillarData> = _pillars

    private val _todaySummary = MutableLiveData<DailySummary?>()
    val todaySummary: LiveData<DailySummary?> = _todaySummary

    private val _weekSummaries = MutableLiveData<List<DailySummary>>(emptyList())
    val weekSummaries: LiveData<List<DailySummary>> = _weekSummaries

    private val _vocabularyTotal = MutableLiveData(0)
    val vocabularyTotal: LiveData<Int> = _vocabularyTotal

    private val _newWordsWeek = MutableLiveData(0)
    val newWordsWeek: LiveData<Int> = _newWordsWeek

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private var loadSeq = 0

    fun loadTodayMetrics() {
        loadRangeMetrics(1)
    }

    fun loadWeekMetrics() {
        loadRangeMetrics(7)
    }

    fun loadRangeMetrics(days: Int) {
        val seq = ++loadSeq
        _isLoading.value = true
        RobotConnectionManager.fetchTelemetryRange(days) { result ->
            if (seq != loadSeq) return@fetchTelemetryRange
            _isLoading.value = false
            when (result) {
                is ApiResult.Success -> applySnapshot(MetricsPeriodParser.parse(result.data))
                is ApiResult.Error -> { }
            }
        }
    }

    private fun applySnapshot(snap: MetricsPeriodSnapshot) {
        _todaySummary.value = snap.headline
        _pillars.value = snap.pillars
        _weekSummaries.value = snap.days
        _newWordsWeek.value = snap.newWords
        if (snap.vocabularyTotal > 0) {
            _vocabularyTotal.value = snap.vocabularyTotal
        }
    }
}
