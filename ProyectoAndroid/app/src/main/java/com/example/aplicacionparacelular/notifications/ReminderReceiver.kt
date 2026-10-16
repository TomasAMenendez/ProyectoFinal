package com.example.aplicacionparacelular.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra(EXTRA_KIND) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "TEO"
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: return
        val id = intent.getIntExtra(EXTRA_ID, kind.hashCode())
        val channel = when (kind) {
            KIND_VACUNA -> AppNotificationHelper.CHANNEL_VACUNA
            KIND_TURNO -> AppNotificationHelper.CHANNEL_TURNO
            KIND_RUTINA -> AppNotificationHelper.CHANNEL_RUTINA
            else -> AppNotificationHelper.CHANNEL_PELUCHE
        }
        if (kind == KIND_RUTINA) {
            ReminderScheduler.deliverRoutine(context, intent) {
                AppNotificationHelper.show(context, channel, id, title, message, notificationTag = intent.dataString)
            }
        } else {
            AppNotificationHelper.show(context, channel, id, title, message)
        }
    }

    companion object {
        const val ACTION_REMINDER = "com.example.aplicacionparacelular.REMINDER"
        const val EXTRA_KIND = "kind"
        const val EXTRA_TITLE = "title"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_ID = "id"
        const val EXTRA_ROUTINE_HHMM = "hhmm"
        const val EXTRA_ROUTINE_ID = "routine_id"
        const val EXTRA_ROUTINE_TOKEN = "routine_token"
        const val KIND_VACUNA = "vacuna"
        const val KIND_TURNO = "turno"
        const val KIND_RUTINA = "rutina"
    }
}
