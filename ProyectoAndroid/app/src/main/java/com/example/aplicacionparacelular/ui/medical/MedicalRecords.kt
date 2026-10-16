package com.example.aplicacionparacelular.ui.medical

import org.json.JSONArray
import org.json.JSONObject

object MedicalRecords {
    fun appointmentsFrom(json: String): List<Appointment>? {
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<Appointment>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val date = obj.optString("date")
                val stored = obj.optLong("at", 0L)
                list.add(
                    Appointment(
                        id = obj.optString("id").ifBlank { "turno-$i" },
                        title = obj.optString("title"),
                        date = date,
                        notes = obj.optString("notes", ""),
                        atMillis = if (stored > 0L) stored else AgendaRules.millisOf(date),
                    ),
                )
            }
            list
        } catch (_: Exception) {
            null
        }
    }

    fun appointmentsTo(items: List<Appointment>): String {
        val arr = JSONArray()
        items.forEach { appt ->
            arr.put(
                JSONObject()
                    .put("id", appt.id)
                    .put("title", appt.title)
                    .put("date", appt.date)
                    .put("notes", appt.notes)
                    .put("at", appt.atMillis),
            )
        }
        return arr.toString()
    }

    fun vaccinesFrom(json: String): List<Vaccine>? {
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<Vaccine>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    Vaccine(
                        id = obj.optString("id"),
                        name = obj.optString("name"),
                        scheduledAge = obj.optString("scheduled_age"),
                        applied = obj.optBoolean("applied", false),
                        appliedDate = obj.optString("applied_date", ""),
                        dueDate = obj.optString("due_date", ""),
                    ),
                )
            }
            list
        } catch (_: Exception) {
            null
        }
    }

    fun vaccinesTo(items: List<Vaccine>): String {
        val arr = JSONArray()
        items.forEach { vac ->
            arr.put(
                JSONObject()
                    .put("id", vac.id)
                    .put("name", vac.name)
                    .put("scheduled_age", vac.scheduledAge)
                    .put("applied", vac.applied)
                    .put("applied_date", vac.appliedDate)
                    .put("due_date", vac.dueDate),
            )
        }
        return arr.toString()
    }
}
