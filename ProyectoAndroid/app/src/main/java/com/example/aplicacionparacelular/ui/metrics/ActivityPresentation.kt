package com.example.aplicacionparacelular.ui.metrics

enum class ActivityPhase { LOADING, READY, FAILED, OFFLINE }

data class MetricsScreen(
    val requestedDays: Int,
    val phase: ActivityPhase,
    val snapshot: MetricsPeriodSnapshot?,
)

data class ActivityDayBar(val label: String, val value: Int)

data class ActivityAreaBar(val label: String, val value: Int)

data class ActivityAreaLabels(
    val emotional: String,
    val cognitive: String,
    val social: String,
    val autonomy: String,
)

data class ActivityPresentation(
    val requestedDays: Int,
    val phase: ActivityPhase,
    val interactions: Int?,
    val games: Int?,
    val routines: Int?,
    val crises: Int?,
    val days: List<ActivityDayBar>?,
    val areas: List<ActivityAreaBar>?,
    val newWords: Int?,
    val vocabularyTotal: Int?,
)

class MetricsRequestGate {
    private var seq = 0

    fun begin(): Int {
        seq += 1
        return seq
    }

    fun isCurrent(request: Int): Boolean = request == seq
}

object MetricsLoad {
    fun phase(connected: Boolean, success: Boolean, summaryKnown: Boolean): ActivityPhase = when {
        success && summaryKnown -> ActivityPhase.READY
        !connected -> ActivityPhase.OFFLINE
        else -> ActivityPhase.FAILED
    }
}

object ActivityCopy {
    fun rangeLabel(days: Int): String = when (days) {
        1 -> "Hoy"
        7 -> "Últimos 7 días"
        30 -> "Últimos 30 días"
        else -> "Últimos $days días"
    }

    fun dayLabel(iso: String): String? {
        val parts = iso.split("-")
        if (parts.size != 3) return null
        val month = parts[1].toIntOrNull() ?: return null
        val day = parts[2].toIntOrNull() ?: return null
        if (month !in 1..12 || day !in 1..31) return null
        return "%02d/%02d".format(day, month)
    }

    fun wordsLine(words: Int?): String = if (words == null) "Sin datos" else "$words palabras nuevas"

    fun periodLine(days: Int): String = when (days) {
        1 -> "Hoy"
        7 -> "En los últimos 7 días"
        30 -> "En los últimos 30 días"
        else -> "En los últimos $days días"
    }

    fun counterLine(value: Int?, label: String): String = if (value == null) {
        "Sin datos · $label"
    } else {
        "$value $label"
    }

    fun totalLine(total: Int?): String = if (total == null) {
        "Total acumulado: Sin datos"
    } else {
        "Total acumulado: $total"
    }

    fun countText(value: Int?): String = value?.toString() ?: "Sin datos"
}

object ActivitySnapshots {
    fun of(screen: MetricsScreen, labels: ActivityAreaLabels): ActivityPresentation {
        val snap = screen.snapshot
        if (screen.phase != ActivityPhase.READY || snap == null || !snap.summaryKnown) {
            val phase = if (screen.phase == ActivityPhase.READY) ActivityPhase.FAILED else screen.phase
            return ActivityPresentation(screen.requestedDays, phase, null, null, null, null, null, null, null, null)
        }
        return ActivityPresentation(
            requestedDays = screen.requestedDays,
            phase = ActivityPhase.READY,
            interactions = snap.headline.interactions.takeIf { snap.interactionsKnown },
            games = snap.headline.gamesPlayed.takeIf { snap.gamesKnown },
            routines = snap.headline.routinesCompleted.takeIf { snap.routinesKnown },
            crises = snap.headline.crisisCount.takeIf { snap.crisesKnown },
            days = if (snap.daysKnown) {
                snap.days.map { ActivityDayBar(ActivityCopy.dayLabel(it.date).orEmpty(), it.interactions) }
            } else {
                null
            },
            areas = if (snap.pillarsKnown) {
                listOf(
                    ActivityAreaBar(labels.emotional, snap.pillars.emocional),
                    ActivityAreaBar(labels.cognitive, snap.pillars.cognitivo),
                    ActivityAreaBar(labels.social, snap.pillars.vincular),
                    ActivityAreaBar(labels.autonomy, snap.pillars.autonomia),
                )
            } else {
                null
            },
            newWords = snap.newWords.takeIf { snap.newWordsKnown },
            vocabularyTotal = snap.vocabularyTotal,
        )
    }
}
