package com.example.aplicacionparacelular.ui.alerts

import java.util.Calendar
import java.util.TimeZone

object NoticeTime {
    fun label(eventAtMillis: Long?, nowMillis: Long, zone: TimeZone = TimeZone.getDefault()): String? {
        if (eventAtMillis == null) return null
        val event = Calendar.getInstance(zone).apply { timeInMillis = eventAtMillis }
        val now = Calendar.getInstance(zone).apply { timeInMillis = nowMillis }
        val time = "%02d:%02d".format(event.get(Calendar.HOUR_OF_DAY), event.get(Calendar.MINUTE))
        val sameDay = event.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            event.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
        return if (sameDay) {
            "Hoy · $time"
        } else {
            "%02d/%02d · %s".format(event.get(Calendar.DAY_OF_MONTH), event.get(Calendar.MONTH) + 1, time)
        }
    }
}
