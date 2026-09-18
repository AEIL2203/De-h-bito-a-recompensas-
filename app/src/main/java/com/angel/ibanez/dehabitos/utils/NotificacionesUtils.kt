package com.angel.ibanez.dehabitos.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.angel.ibanez.dehabitos.MainActivity
import com.angel.ibanez.dehabitos.R

object NotificacionesUtils {

    const val CANAL_TAREAS = "tareas"
    const val CANAL_USO_APPS = "uso_apps"
    const val CANAL_RECORDATORIOS = "recordatorios"

    fun crearCanales(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(CANAL_TAREAS, "Misiones", NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(CANAL_USO_APPS, "Uso de aplicaciones", NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(CANAL_RECORDATORIOS, "Recordatorios", NotificationManager.IMPORTANCE_DEFAULT)
            )
        )
    }

    fun notificar(context: Context, canal: String, titulo: String, cuerpo: String, id: Int) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val intent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        manager.notify(
            id,
            NotificationCompat.Builder(context, canal)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(titulo)
                .setContentText(cuerpo)
                .setAutoCancel(true)
                .setContentIntent(intent)
                .build()
        )
    }
}
