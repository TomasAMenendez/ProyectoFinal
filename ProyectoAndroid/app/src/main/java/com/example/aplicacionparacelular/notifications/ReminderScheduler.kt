package com.example.aplicacionparacelular.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale
import java.util.UUID

/**
 * Agenda alarmas locales (sin internet) para vacunas y rutinas.
 * Persistimos un JSON mínimo para rearmarlas tras un reinicio.
 */
object ReminderScheduler {

    private const val PREFS = "teo_reminders"
    private const val KEY_ROUTINES = "routines_json"
    private const val KEY_VACCINES = "vaccines_json"
    private const val KEY_APPOINTMENTS = "appointments_json"
    private const val KEY_CODES = "alarm_codes"
    private const val KEY_NEXT = "next_alarm_code"

    fun restoreAll(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        fun storedCollection(key: String): JSONArray? {
            return runCatching { JSONArray(prefs.getString(key, "[]") ?: "[]") }.getOrNull()
                ?.takeIf { array -> (0 until array.length()).all { array.optJSONObject(it) != null } }
        }
        storedCollection(KEY_ROUTINES)?.let { storedRoutines ->
            val routines = mutableListOf<Triple<String, String, Int>>()
            for (i in 0 until storedRoutines.length()) {
                val obj = storedRoutines.getJSONObject(i)
                routines.add(Triple(obj.optString("id"), obj.optString("name", "Rutina"), obj.optInt("minutes", -1)))
            }
            saveAndScheduleRoutines(context, routines)
        }
        storedCollection(KEY_VACCINES)?.let { scheduleVaccinesFromJson(context, it.toString()) }
        storedCollection(KEY_APPOINTMENTS)?.let { scheduleAppointmentsFromJson(context, it.toString()) }
    }

    @Synchronized
    fun saveAndScheduleRoutines(
        context: Context,
        items: List<Triple<String, String, Int>>,
    ) {
        val json = ReminderPlan.storedRoutines(items) { UUID.randomUUID().toString() }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val previous = prefs.getString(KEY_ROUTINES, "[]") ?: "[]"
        cancelRoutineIdentities(context, previous)
        // Los códigos antiguos compartían namespace con recordatorios médicos.
        // Si colisionan, la migración médica se encarga de cancelarlos/rearmarlos.
        cancelCodes(
            context,
            ReminderPlan.routineLegacyCodesToCancel(
                previous,
                prefs.getString(KEY_VACCINES, "[]") ?: "[]",
                prefs.getString(KEY_APPOINTMENTS, "[]") ?: "[]",
            ),
        )
        prefs.edit().putString(KEY_ROUTINES, json).apply()
        scheduleRoutinesFromJson(context, json)
    }

    fun saveAndScheduleVaccines(
        context: Context,
        items: List<VaccineReminder>,
    ) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val previous = prefs.getString(KEY_VACCINES, "[]") ?: "[]"
        cancelCodes(
            context,
            ReminderPlan.codesToCancel(previous, routineCodes(prefs)) { obj ->
                ReminderPlan.legacyVaccineCodes(obj.optString("id"))
            },
        )
        val alarms = ReminderPlan.planVaccines(items, System.currentTimeMillis()) { stableCode(prefs, it) }
        alarms.forEach { setAlarm(context, it.requestCode, it.atMillis, it.kind, it.title, it.message, null) }
        prefs.edit().putString(KEY_VACCINES, ReminderPlan.storedVaccines(items, alarms)).apply()
    }

    fun saveAndScheduleAppointments(
        context: Context,
        items: List<AppointmentReminder>,
    ) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val previous = prefs.getString(KEY_APPOINTMENTS, "[]") ?: "[]"
        cancelCodes(
            context,
            ReminderPlan.codesToCancel(previous, routineCodes(prefs)) { obj ->
                ReminderPlan.legacyAppointmentCodes(obj.optString("title"))
            },
        )
        val alarms = ReminderPlan.planAppointments(items, System.currentTimeMillis()) { stableCode(prefs, it) }
        alarms.forEach { setAlarm(context, it.requestCode, it.atMillis, it.kind, it.title, it.message, null) }
        prefs.edit().putString(KEY_APPOINTMENTS, ReminderPlan.storedAppointments(items, alarms)).apply()
    }

    fun isCurrentRoutine(context: Context, intent: Intent): Boolean {
        val recordId = intent.getStringExtra(ReminderReceiver.EXTRA_ROUTINE_ID) ?: return false
        if (intent.dataString != ReminderPlan.routineAlarmIdentity(recordId)) return false
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ROUTINES, "[]") ?: "[]"
        return ReminderPlan.isCurrentRoutine(
            json,
            intent.getIntExtra(ReminderReceiver.EXTRA_ID, 0),
            intent.getStringExtra(ReminderReceiver.EXTRA_ROUTINE_HHMM),
            recordId,
            intent.getStringExtra(ReminderReceiver.EXTRA_ROUTINE_TOKEN),
        )
    }

    @Synchronized
    fun deliverRoutine(context: Context, intent: Intent, show: () -> Unit) {
        if (!isCurrentRoutine(context, intent)) return
        show()
        rescheduleRoutineFromIntent(context, intent)
    }

    @Synchronized
    fun rescheduleRoutineFromIntent(context: Context, intent: Intent) {
        if (!isCurrentRoutine(context, intent)) return
        val hhmm = intent.getStringExtra(ReminderReceiver.EXTRA_ROUTINE_HHMM) ?: return
        val parts = hhmm.split(":")
        if (parts.size < 2) return
        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return
        val id = intent.getIntExtra(ReminderReceiver.EXTRA_ID, 0)
        val title = intent.getStringExtra(ReminderReceiver.EXTRA_TITLE) ?: "Rutina"
        val message = intent.getStringExtra(ReminderReceiver.EXTRA_MESSAGE) ?: return
        setAlarm(
            context,
            id,
            nextOccurrence(hour, minute),
            ReminderReceiver.KIND_RUTINA,
            title,
            message,
            String.format(Locale.US, "%02d:%02d", hour, minute),
            intent.getStringExtra(ReminderReceiver.EXTRA_ROUTINE_ID),
            intent.getStringExtra(ReminderReceiver.EXTRA_ROUTINE_TOKEN),
        )
    }

    private fun scheduleRoutinesFromJson(context: Context, json: String) {
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val minutes = obj.optInt("minutes", -1)
            if (minutes !in 0 until 24 * 60) continue
            val hour = minutes / 60
            val minute = minutes % 60
            val name = obj.optString("name", "Rutina")
            val id = obj.optString("id")
            val token = obj.optString("alarmToken")
            if (token.isBlank()) continue
            setAlarm(
                context,
                ReminderPlan.BASE_ROUTINE,
                nextOccurrence(hour, minute),
                ReminderReceiver.KIND_RUTINA,
                "TEO: rutina",
                "Se acerca $name (${String.format(Locale.US, "%02d:%02d", hour, minute)})",
                String.format(Locale.US, "%02d:%02d", hour, minute),
                id,
                token,
            )
        }
    }

    private fun scheduleVaccinesFromJson(context: Context, json: String) {
        val items = mutableListOf<VaccineReminder>()
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            items.add(
                VaccineReminder(
                    id = obj.optString("id"),
                    name = obj.optString("name"),
                    dueDateIso = obj.optString("dueDate"),
                    applied = obj.optBoolean("applied"),
                ),
            )
        }
        saveAndScheduleVaccines(context, items)
    }

    private fun scheduleAppointmentsFromJson(context: Context, json: String) {
        val items = mutableListOf<AppointmentReminder>()
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val at = obj.optLong("at", 0L).takeIf { it > 0L }
                ?: parseAppointmentDate(obj.optString("date"))?.timeInMillis
                ?: 0L
            items.add(
                AppointmentReminder(
                    id = obj.optString("id").ifBlank { "legacy-$i" },
                    title = obj.optString("title"),
                    atMillis = at,
                    label = obj.optString("date"),
                ),
            )
        }
        saveAndScheduleAppointments(context, items)
    }

    private fun setAlarm(
        context: Context,
        requestCode: Int,
        whenCal: Calendar,
        kind: String,
        title: String,
        message: String,
        hhmm: String?,
        routineId: String? = null,
        routineToken: String? = null,
    ) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMINDER
            if (routineId != null) data = Uri.parse(ReminderPlan.routineAlarmIdentity(routineId))
            putExtra(ReminderReceiver.EXTRA_KIND, kind)
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_MESSAGE, message)
            putExtra(ReminderReceiver.EXTRA_ID, requestCode)
            if (hhmm != null) putExtra(ReminderReceiver.EXTRA_ROUTINE_HHMM, hhmm)
            if (routineId != null) putExtra(ReminderReceiver.EXTRA_ROUTINE_ID, routineId)
            if (routineToken != null) putExtra(ReminderReceiver.EXTRA_ROUTINE_TOKEN, routineToken)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val at = whenCal.timeInMillis
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarm.canScheduleExactAlarms()) {
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
            } else {
                @Suppress("DEPRECATION")
                alarm.setExact(AlarmManager.RTC_WAKEUP, at, pending)
            }
        } catch (_: SecurityException) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        }
    }

    private fun nextOccurrence(hour: Int, minute: Int): Calendar {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (cal.timeInMillis <= System.currentTimeMillis() + 15_000) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal
    }

    private fun parseIsoDate(iso: String): Calendar? {
        val parts = iso.split("-")
        if (parts.size < 3) return null
        val y = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        val d = parts[2].toIntOrNull() ?: return null
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, y)
            set(Calendar.MONTH, m - 1)
            set(Calendar.DAY_OF_MONTH, d)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
    }

    fun parseAppointmentDate(text: String): Calendar? {
        val slash = Regex("""(\d{1,2})/(\d{1,2})/(\d{4})\s+(\d{1,2}):(\d{2})""").find(text)
        if (slash != null) {
            val (d, m, y, h, min) = slash.destructured
            return Calendar.getInstance().apply {
                set(Calendar.YEAR, y.toInt())
                set(Calendar.MONTH, m.toInt() - 1)
                set(Calendar.DAY_OF_MONTH, d.toInt())
                set(Calendar.HOUR_OF_DAY, h.toInt())
                set(Calendar.MINUTE, min.toInt())
                set(Calendar.SECOND, 0)
            }
        }
        val months = mapOf(
            "enero" to 0, "febrero" to 1, "marzo" to 2, "abril" to 3,
            "mayo" to 4, "junio" to 5, "julio" to 6, "agosto" to 7,
            "septiembre" to 8, "octubre" to 9, "noviembre" to 10, "diciembre" to 11,
        )
        val named = Regex(
            """(\d{1,2})\s+de\s+([a-záéíóúñ]+)\s+de\s+(\d{4}).*?(\d{1,2}):(\d{2})""",
            RegexOption.IGNORE_CASE,
        ).find(text) ?: return null
        val (day, monthName, year, hour, minute) = named.destructured
        val month = months[monthName.lowercase(Locale("es"))] ?: return null
        return Calendar.getInstance().apply {
            isLenient = false
            set(Calendar.YEAR, year.toInt())
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day.toInt())
            set(Calendar.HOUR_OF_DAY, hour.toInt())
            set(Calendar.MINUTE, minute.toInt())
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun cancelCodes(context: Context, codes: List<Int>) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        codes.distinct().forEach { code ->
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = ReminderReceiver.ACTION_REMINDER
            }
            val pending = PendingIntent.getBroadcast(
                context,
                code,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            ) ?: return@forEach
            alarm.cancel(pending)
            pending.cancel()
        }
    }

    private fun routineCodes(prefs: SharedPreferences): Set<Int> {
        return ReminderPlan.legacyRoutineCodes(prefs.getString(KEY_ROUTINES, "[]") ?: "[]")
    }

    private fun cancelRoutineIdentities(context: Context, json: String) {
        val arr = runCatching { JSONArray(json) }.getOrDefault(JSONArray())
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = ReminderReceiver.ACTION_REMINDER
                data = Uri.parse(ReminderPlan.routineAlarmIdentity(obj.optString("id")))
            }
            val pending = PendingIntent.getBroadcast(
                context,
                ReminderPlan.BASE_ROUTINE,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            ) ?: continue
            alarm.cancel(pending)
            pending.cancel()
        }
    }

    private fun stableCode(prefs: SharedPreferences, key: String): Int {
        val map = JSONObject(prefs.getString(KEY_CODES, "{}") ?: "{}")
        if (map.has(key)) return map.getInt(key)
        val next = prefs.getInt(KEY_NEXT, 80_000)
        map.put(key, next)
        prefs.edit().putString(KEY_CODES, map.toString()).putInt(KEY_NEXT, next + 1).commit()
        return next
    }

    private fun setAlarm(
        context: Context,
        requestCode: Int,
        atMillis: Long,
        kind: String,
        title: String,
        message: String,
        hhmm: String?,
    ) {
        val whenCal = Calendar.getInstance()
        whenCal.timeInMillis = atMillis
        setAlarm(context, requestCode, whenCal, kind, title, message, hhmm)
    }

    data class VaccineReminder(
        val id: String,
        val name: String,
        val dueDateIso: String,
        val applied: Boolean,
    )
}
