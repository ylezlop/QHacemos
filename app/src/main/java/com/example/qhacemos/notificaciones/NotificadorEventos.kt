package com.example.qhacemos.notificaciones

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.qhacemos.MainActivity
import com.example.qhacemos.R
import androidx.core.net.toUri

class NotificadorEventos(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CANAL_ID = "canal_eventos_qhacemos"
        const val CANAL_NOMBRE = "Notificaciones de Eventos"
    }

    init {
        crearCanalDeNotificacion()
    }

    private fun crearCanalDeNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL_ID,
                CANAL_NOMBRE,
                NotificationManager.IMPORTANCE_HIGH // IMPORTANCE_HIGH hace que suene y aparezca como un banner en la parte superior
            ).apply {
                description = "Recordatorios, nuevos eventos y alertas geográficas"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(canal)
        }
    }

    fun mostrarNotificacionBasica(titulo: String, mensaje: String, eventoId: Long? = null) {
        val uri = if (eventoId != null) "qhacemos://evento/$eventoId".toUri() else null

        val intent = Intent(Intent.ACTION_VIEW, uri, context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            eventoId?.toInt() ?: 0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificacion = NotificationCompat.Builder(context, CANAL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val idNotificacion = eventoId?.toInt() ?: System.currentTimeMillis().toInt()
        notificationManager.notify(idNotificacion, notificacion)
    }
}