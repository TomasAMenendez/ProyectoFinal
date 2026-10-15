package com.example.aplicacionparacelular.ui.alerts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class NoticeTimeTest {
    @Test
    fun showsTodayOrTheEventDateAndOmitsAMissingInstant() {
        val zone = TimeZone.getTimeZone("UTC")
        val now = calendar(zone, 2026, Calendar.OCTOBER, 13, 18, 0)
        val today = calendar(zone, 2026, Calendar.OCTOBER, 13, 14, 30)
        val yesterday = calendar(zone, 2026, Calendar.OCTOBER, 12, 9, 5)
        assertEquals("Hoy · 14:30", NoticeTime.label(today, now, zone))
        assertEquals("12/10 · 09:05", NoticeTime.label(yesterday, now, zone))
        assertNull(NoticeTime.label(null, now, zone))
    }

    private fun calendar(zone: TimeZone, year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return Calendar.getInstance(zone).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
