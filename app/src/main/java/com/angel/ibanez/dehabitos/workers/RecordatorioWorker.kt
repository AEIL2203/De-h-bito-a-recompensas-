package com.angel.ibanez.dehabitos.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.angel.ibanez.dehabitos.firebase.FirebaseFirestoreService
import com.angel.ibanez.dehabitos.models.Tarea
import com.angel.ibanez.dehabitos.utils.DispositivoConfig
import com.angel.ibanez.dehabitos.utils.FechasUtils
import com.angel.ibanez.dehabitos.utils.NotificacionesUtils
import com.google.firebase.auth.FirebaseAuth

class RecordatorioWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (FirebaseAuth.getInstance().currentUser == null) return Result.success()
        val tutorId = DispositivoConfig.obtenerTutorId()
        if (tutorId.isBlank()) return Result.success()

        val service = FirebaseFirestoreService()
        return try {
            when (DispositivoConfig.obtenerModo()) {
                DispositivoConfig.ModoDispositivo.HIJO -> notificarHijo(service)
                DispositivoConfig.ModoDispositivo.PADRE -> notificarTutor(service, tutorId)
                DispositivoConfig.ModoDispositivo.SIN_CONFIGURAR -> Unit
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun notificarHijo(service: FirebaseFirestoreService) {
        val ninoId = DispositivoConfig.obtenerNinoId()
        if (ninoId.isBlank()) return
        val pendientes = service.obtenerTareasPorNino(ninoId).filter { it.estado == Tarea.ESTADO_PENDIENTE }
        if (pendientes.isEmpty()) return
        val hoy = FechasUtils.hoy()
        val urgentes = pendientes.count {
            FechasUtils.estadoVencimiento(it.fechaLimite, hoy).let { v ->
                v == FechasUtils.EstadoVencimiento.VENCE_HOY || v == FechasUtils.EstadoVencimiento.VENCIDA
            }
        }
        val cuerpo = "Tienes ${pendientes.size} misiones pendientes" +
            if (urgentes > 0) " · $urgentes vencen hoy" else ""
        NotificacionesUtils.notificar(
            applicationContext, NotificacionesUtils.CANAL_RECORDATORIOS,
            "Misiones pendientes", cuerpo, ID_NOTIFICACION
        )
    }

    private suspend fun notificarTutor(service: FirebaseFirestoreService, tutorId: String) {
        val porValidar = service.obtenerTareasPorTutor(tutorId).count { it.estado == Tarea.ESTADO_COMPLETADA }
        if (porValidar == 0) return
        NotificacionesUtils.notificar(
            applicationContext, NotificacionesUtils.CANAL_RECORDATORIOS,
            "Misiones por validar", "$porValidar misiones esperan tu validación", ID_NOTIFICACION
        )
    }

    companion object {
        private const val ID_NOTIFICACION = 4001
    }
}
