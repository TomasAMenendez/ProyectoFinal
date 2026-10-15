package com.example.aplicacionparacelular.ui.medical

import java.util.Calendar
import java.util.Locale

enum class RecordLoad {
    READY,
    FAILED,
}

enum class VaccineStatus { APPLIED, PENDING }

data class VaccinePresentation(
    val status: VaccineStatus,
    val dueText: String?,
    val showSchedule: Boolean,
)

object AgendaRules {
    fun titleProblem(title: String): String? {
        if (title.trim().isEmpty()) return "El título es obligatorio."
        return null
    }

    fun whenProblem(hasDate: Boolean, hasTime: Boolean): String? {
        if (!hasDate) return "Elegí la fecha."
        if (!hasTime) return "Elegí la hora."
        return null
    }

    fun atMillis(appointment: Appointment): Long {
        if (appointment.atMillis > 0L) return appointment.atMillis
        return millisOf(appointment.date)
    }

    fun displayWhen(appointment: Appointment): String {
        val millis = atMillis(appointment)
        if (millis > 0L) return formatWhen(millis)
        return appointment.date
    }

    fun sort(items: List<Appointment>): List<Appointment> {
        return items.withIndex().sortedWith(
            compareBy<IndexedValue<Appointment>> { atMillis(it.value) <= 0L }
                .thenBy { if (atMillis(it.value) > 0L) atMillis(it.value) else Long.MAX_VALUE }
                .thenBy { it.index },
        ).map { it.value }
    }

    fun without(items: List<Appointment>, id: String): List<Appointment> {
        return items.filterNot { it.id == id }
    }

    fun formatWhen(millis: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        return String.format(
            Locale.US,
            "%02d/%02d/%04d - %02d:%02d hs",
            cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.YEAR),
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
        )
    }

    fun storedDate(millis: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        return String.format(
            Locale.US,
            "%02d/%02d/%04d %02d:%02d",
            cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.YEAR),
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
        )
    }

    fun millisOf(text: String): Long {
        val slash = Regex("""(\d{1,2})/(\d{1,2})/(\d{4})\s+(\d{1,2}):(\d{2})""").find(text)
        if (slash != null) {
            val (day, month, year, hour, minute) = slash.destructured
            return calendarMillis(year.toInt(), month.toInt(), day.toInt(), hour.toInt(), minute.toInt())
        }
        val named = Regex(
            """(\d{1,2})\s+de\s+([a-záéíóúñ]+)(?:\s+de\s+(\d{4}))?.*?(\d{1,2}):(\d{2})""",
            RegexOption.IGNORE_CASE,
        ).find(text) ?: return 0L
        val year = named.groupValues[3]
        if (year.isBlank()) return 0L
        val month = MONTHS[named.groupValues[2].lowercase(Locale("es"))] ?: return 0L
        return calendarMillis(
            year.toInt(),
            month,
            named.groupValues[1].toInt(),
            named.groupValues[4].toInt(),
            named.groupValues[5].toInt(),
        )
    }

    fun formatDue(iso: String): String {
        val parts = iso.split("-")
        if (parts.size < 3) return iso
        val year = parts[0].toIntOrNull() ?: return iso
        val month = parts[1].toIntOrNull() ?: return iso
        val day = parts[2].toIntOrNull() ?: return iso
        if (calendarMillis(year, month, day, 0, 0) == 0L) return iso
        return String.format(Locale.US, "%02d/%02d/%04d", day, month, year)
    }

    fun vaccine(item: Vaccine): VaccinePresentation {
        if (item.applied) return VaccinePresentation(VaccineStatus.APPLIED, null, false)
        if (item.dueDate.isBlank()) return VaccinePresentation(VaccineStatus.PENDING, null, true)
        return VaccinePresentation(VaccineStatus.PENDING, formatDue(item.dueDate), true)
    }

    fun defaultVaccines(): List<Vaccine> = listOf(
        Vaccine("v1", "BCG", "Al nacer", false),
        Vaccine("v2", "Hepatitis B", "Al nacer", false),
        Vaccine("v3", "Neumococo conjugada (1ra)", "2 meses", false),
        Vaccine("v4", "Quíntuple pentavalente (1ra)", "2 meses", false),
        Vaccine("v5", "IPV/Salk (1ra)", "2 meses", false),
        Vaccine("v6", "Rotavirus (1ra)", "2 meses", false),
        Vaccine("v7", "Neumococo conjugada (2da)", "4 meses", false),
        Vaccine("v8", "Quíntuple pentavalente (2da)", "4 meses", false),
        Vaccine("v9", "IPV/Salk (2da)", "4 meses", false),
        Vaccine("v10", "Rotavirus (2da)", "4 meses", false),
        Vaccine("v11", "Meningococo (1ra)", "3 meses", false),
        Vaccine("v12", "Meningococo (2da)", "5 meses", false),
        Vaccine("v13", "Gripe (1ra)", "6 meses", false),
        Vaccine("v14", "Quíntuple pentavalente (3ra)", "6 meses", false),
        Vaccine("v15", "Hepatitis A", "12 meses", false),
        Vaccine("v16", "Triple viral (SRP) 1ra", "12 meses", false),
        Vaccine("v17", "Varicela", "15 meses", false),
    )

    private fun calendarMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance()
        cal.isLenient = false
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        cal.set(Calendar.DAY_OF_MONTH, day)
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return try {
            cal.timeInMillis
        } catch (_: IllegalArgumentException) {
            0L
        }
    }

    private val MONTHS = mapOf(
        "enero" to 1,
        "febrero" to 2,
        "marzo" to 3,
        "abril" to 4,
        "mayo" to 5,
        "junio" to 6,
        "julio" to 7,
        "agosto" to 8,
        "septiembre" to 9,
        "octubre" to 10,
        "noviembre" to 11,
        "diciembre" to 12,
    )
}
