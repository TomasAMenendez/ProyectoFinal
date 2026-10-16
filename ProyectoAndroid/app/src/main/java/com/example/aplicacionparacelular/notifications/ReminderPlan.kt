package com.example.aplicacionparacelular.notifications

import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.net.URLEncoder

data class PlannedAlarm(
    val requestCode: Int,
    val atMillis: Long,
    val kind: String,
    val title: String,
    val message: String,
    val recordId: String,
)

data class AppointmentReminder(
    val id: String,
    val title: String,
    val atMillis: Long,
    val label: String,
)

object ReminderPlan {
    const val BASE_ROUTINE = 42000
    const val BASE_VACCINE = 43000
    const val BASE_APPT = 44000

    fun routineCode(id: String): Int = BASE_ROUTINE + (id.hashCode() and 0x0FFF)

    fun routineCodes(json: String): Set<Int> {
        val arr = runCatching { JSONArray(json) }.getOrDefault(JSONArray())
        return buildSet {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                add(routineCode(obj.optString("id")))
            }
        }
    }

    fun routineAlarmIdentity(id: String): String =
        "teo-reminder://routine/" + URLEncoder.encode(id, "UTF-8")

    fun storedRoutines(items: List<Triple<String, String, Int>>, token: () -> String): String {
        val arr = JSONArray()
        items.filter { it.third in 0 until 24 * 60 }.forEach { (id, name, minutes) ->
            arr.put(
                JSONObject().put("id", id).put("name", name).put("minutes", minutes)
                    .put("alarmToken", token()),
            )
        }
        return arr.toString()
    }

    fun legacyRoutineCodes(json: String): Set<Int> {
        val arr = runCatching { JSONArray(json) }.getOrDefault(JSONArray())
        return buildSet {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                if (obj.optString("alarmToken").isBlank()) add(routineCode(obj.optString("id")))
            }
        }
    }

    fun routineLegacyCodesToCancel(routines: String, vaccines: String, appointments: String): List<Int> {
        val medicalCodes = codesToCancel(vaccines, emptySet()) { legacyVaccineCodes(it.optString("id")) } +
            codesToCancel(appointments, emptySet()) { legacyAppointmentCodes(it.optString("title")) }
        return legacyRoutineCodes(routines).filterNot { it in medicalCodes }
    }

    fun isCurrentRoutine(
        json: String,
        requestCode: Int,
        hhmm: String?,
        recordId: String? = null,
        token: String? = null,
    ): Boolean {
        if (recordId == null || token.isNullOrBlank() || requestCode != BASE_ROUTINE) return false
        val parts = hhmm?.split(":") ?: return false
        if (parts.size != 2) return false
        val hour = parts[0].toIntOrNull()?.takeIf { it in 0..23 } ?: return false
        val minute = parts[1].toIntOrNull()?.takeIf { it in 0..59 } ?: return false
        val minutesOfDay = hour * 60 + minute
        val arr = runCatching { JSONArray(json) }.getOrDefault(JSONArray())
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            if (obj.optString("id") == recordId && obj.optString("alarmToken") == token &&
                obj.optInt("minutes", -1) == minutesOfDay
            ) {
                return true
            }
        }
        return false
    }

    fun legacyAppointmentCodes(title: String): List<Int> {
        val code = BASE_APPT + (title.hashCode() and 0x0FFF)
        return listOf(code, code + 9000)
    }

    fun legacyVaccineCodes(id: String): List<Int> {
        val code = BASE_VACCINE + (id.hashCode() and 0x0FFF)
        return listOf(code, code + 8000)
    }

    fun codesToCancel(previousJson: String, protectedCodes: Set<Int>, legacyOf: (JSONObject) -> List<Int>): List<Int> {
        val found = linkedSetOf<Int>()
        val arr = runCatching { JSONArray(previousJson) }.getOrDefault(JSONArray())
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val stored = obj.optJSONArray("alarmCodes")
            if (stored != null) {
                for (j in 0 until stored.length()) found.add(stored.optInt(j))
            }
            legacyOf(obj).forEach { found.add(it) }
        }
        return found.filter { it !in protectedCodes }
    }

    fun planAppointments(
        items: List<AppointmentReminder>,
        now: Long,
        codeOf: (String) -> Int,
    ): List<PlannedAlarm> {
        val alarms = mutableListOf<PlannedAlarm>()
        items.forEach { item ->
            if (item.atMillis <= now) return@forEach
            alarms.add(
                PlannedAlarm(
                    codeOf("appt:${item.id}:at"),
                    item.atMillis,
                    ReminderReceiver.KIND_TURNO,
                    "TEO: turno médico",
                    "${item.title} · ${item.label}",
                    item.id,
                ),
            )
            val beforeCal = Calendar.getInstance()
            beforeCal.timeInMillis = item.atMillis
            beforeCal.add(Calendar.HOUR_OF_DAY, -1)
            val before = beforeCal.timeInMillis
            if (before > now) {
                alarms.add(
                    PlannedAlarm(
                        codeOf("appt:${item.id}:before"),
                        before,
                        ReminderReceiver.KIND_TURNO,
                        "TEO: turno médico",
                        "En una hora: ${item.title}",
                        item.id,
                    ),
                )
            }
        }
        return alarms
    }

    fun planVaccines(
        items: List<ReminderScheduler.VaccineReminder>,
        now: Long,
        codeOf: (String) -> Int,
    ): List<PlannedAlarm> {
        val alarms = mutableListOf<PlannedAlarm>()
        items.forEach { item ->
            if (item.applied || item.dueDateIso.isBlank()) return@forEach
            val morning = atHour(item.dueDateIso, 9) ?: return@forEach
            if (morning > now) {
                alarms.add(
                    PlannedAlarm(
                        codeOf("vac:${item.id}:day"),
                        morning,
                        ReminderReceiver.KIND_VACUNA,
                        "TEO: vacunación",
                        "Hoy toca la vacuna ${item.name}",
                        item.id,
                    ),
                )
            }
            val previous = dayBefore(item.dueDateIso) ?: return@forEach
            if (previous > now) {
                alarms.add(
                    PlannedAlarm(
                        codeOf("vac:${item.id}:eve"),
                        previous,
                        ReminderReceiver.KIND_VACUNA,
                        "TEO: vacunación mañana",
                        "Mañana es la vacuna ${item.name}",
                        item.id,
                    ),
                )
            }
        }
        return alarms
    }

    fun storedAppointments(items: List<AppointmentReminder>, alarms: List<PlannedAlarm>): String {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject()
                    .put("id", item.id)
                    .put("title", item.title)
                    .put("date", item.label)
                    .put("at", item.atMillis)
                    .put("alarmCodes", codesOf(alarms, item.id)),
            )
        }
        return arr.toString()
    }

    fun storedVaccines(items: List<ReminderScheduler.VaccineReminder>, alarms: List<PlannedAlarm>): String {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject()
                    .put("id", item.id)
                    .put("name", item.name)
                    .put("dueDate", item.dueDateIso)
                    .put("applied", item.applied)
                    .put("alarmCodes", codesOf(alarms, item.id)),
            )
        }
        return arr.toString()
    }

    fun atHour(iso: String, hour: Int): Long? = calendarOf(iso)?.let { cal ->
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }

    fun dayBefore(iso: String): Long? = calendarOf(iso)?.let { cal ->
        cal.set(Calendar.HOUR_OF_DAY, 9)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.DAY_OF_YEAR, -1)
        cal.timeInMillis
    }

    private fun codesOf(alarms: List<PlannedAlarm>, recordId: String): JSONArray {
        val arr = JSONArray()
        alarms.filter { it.recordId == recordId }.forEach { arr.put(it.requestCode) }
        return arr
    }

    private fun calendarOf(iso: String): Calendar? {
        val parts = iso.split("-")
        if (parts.size < 3) return null
        val year = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val day = parts[2].toIntOrNull() ?: return null
        val cal = Calendar.getInstance()
        cal.isLenient = false
        return try {
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month - 1)
            cal.set(Calendar.DAY_OF_MONTH, day)
            cal.timeInMillis
            cal
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
