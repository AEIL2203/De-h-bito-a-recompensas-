@file:Suppress("DEPRECATION")
package com.angel.ibanez.dehabitos.services

import com.angel.ibanez.dehabitos.repository.EstadisticasRepository
import com.angel.ibanez.dehabitos.utils.DispositivoConfig
import com.angel.ibanez.dehabitos.utils.NotificacionesUtils
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class HabitosMessagingService : FirebaseMessagingService() {

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Deprecated("FCM tokens are deprecated; kept for compatibility")
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val tutorId = DispositivoConfig.obtenerTutorId()
        if (tutorId.isBlank()) return
        val modo = if (DispositivoConfig.obtenerModo() == DispositivoConfig.ModoDispositivo.HIJO) "hijo" else "tutor"
        val ninoId = if (modo == "hijo") DispositivoConfig.obtenerNinoId() else ""
        alcance.launch {
            runCatching { EstadisticasRepository().guardarToken(tutorId, token, ninoId, modo) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        NotificacionesUtils.crearCanales(this)
        val canal = message.data["canal"] ?: NotificacionesUtils.CANAL_USO_APPS
        val titulo = message.notification?.title ?: message.data["titulo"] ?: "Actividad de aplicaciones"
        val cuerpo = message.notification?.body ?: message.data["cuerpo"] ?: "Hay nueva actividad"
        NotificacionesUtils.notificar(
            this, canal, titulo, cuerpo,
            (message.messageId ?: System.currentTimeMillis().toString()).hashCode()
        )
    }
}
