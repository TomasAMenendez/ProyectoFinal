package com.example.aplicacionparacelular.notifications

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.example.aplicacionparacelular.network.RobotConnectionManager

/**
 * Mantiene el poll de /api/notifications aunque la Activity se cierre.
 * Sin FCM: es un servicio local en la misma red WiFi que el peluche.
 */
class TeoAlertPollService : Service() {

    private var ownsPolling = false

    override fun onCreate() {
        super.onCreate()
        AppNotificationHelper.ensureChannels(this)
        val notification = AppNotificationHelper.foregroundBuilder(this).build()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    FOREGROUND_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
                )
            } else {
                startForeground(FOREGROUND_ID, notification)
            }
        } catch (e: IllegalStateException) {
            Log.w(TAG, "El sistema no permitió mantener la conexión en segundo plano", e)
            stopSelf()
            return
        } catch (e: SecurityException) {
            Log.w(TAG, "El sistema no concedió el permiso del servicio de conexión", e)
            stopSelf()
            return
        }
        RobotConnectionManager.init(this)
        RobotConnectionManager.setAlertServiceActive(true)
        ownsPolling = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        if (ownsPolling) START_STICKY else START_NOT_STICKY

    override fun onDestroy() {
        releasePolling()
        super.onDestroy()
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        releasePolling()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun releasePolling() {
        if (!ownsPolling) return
        ownsPolling = false
        RobotConnectionManager.setAlertServiceActive(false)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "TeoAlertPollService"
        private const val FOREGROUND_ID = 71001

        fun start(context: Context): Boolean {
            val intent = Intent(context, TeoAlertPollService::class.java)
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                true
            } catch (e: IllegalStateException) {
                Log.w(TAG, "El sistema pospuso el servicio de conexión", e)
                false
            } catch (e: SecurityException) {
                Log.w(TAG, "No se pudo iniciar el servicio de conexión", e)
                false
            }
        }
    }
}
