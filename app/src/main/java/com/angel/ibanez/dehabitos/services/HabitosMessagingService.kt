package com.angel.ibanez.dehabitos.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.angel.ibanez.dehabitos.MainActivity
import com.angel.ibanez.dehabitos.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class HabitosMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CANAL, "Uso de aplicaciones", NotificationManager.IMPORTANCE_DEFAULT))
        val intent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val titulo = message.notification?.title ?: message.data["titulo"] ?: "Actividad de aplicaciones"
        val cuerpo = message.notification?.body ?: message.data["cuerpo"] ?: "Hay nueva actividad"
        manager.notify((message.messageId ?: System.currentTimeMillis().toString()).hashCode(), NotificationCompat.Builder(this, CANAL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(titulo).setContentText(cuerpo).setAutoCancel(true).setContentIntent(intent).build())
    }

    companion object { const val CANAL = "uso_apps" }
}
