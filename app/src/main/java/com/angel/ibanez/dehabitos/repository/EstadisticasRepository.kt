package com.angel.ibanez.dehabitos.repository

import com.angel.ibanez.dehabitos.firebase.FirebaseFirestoreService
import com.angel.ibanez.dehabitos.models.EncuestaSatisfaccion
import com.angel.ibanez.dehabitos.models.EstadisticasNino
import com.angel.ibanez.dehabitos.models.SesionApp
import com.angel.ibanez.dehabitos.models.TokenFcm
import com.angel.ibanez.dehabitos.utils.EstadisticasUtils

class EstadisticasRepository(private val service: FirebaseFirestoreService = FirebaseFirestoreService()) {
    suspend fun iniciarSesion(sesion: SesionApp) = service.iniciarSesionApp(sesion)
    suspend fun finalizarSesion(id: String, fin: Long, duracionMs: Long) = service.finalizarSesionApp(id, fin, duracionMs)

    suspend fun obtener(ninoId: String, desde: Long?): EstadisticasNino {
        val nino = service.obtenerNinoPorId(ninoId) ?: error("Niño no encontrado")
        val tareas = service.obtenerTareasPorNino(ninoId)
        val canjes = service.obtenerRecompensasPorNino(ninoId)
        val sesiones = service.obtenerSesionesPorNino(ninoId)
        val encuestas = runCatching { service.obtenerEncuestasPorNino(ninoId, nino.tutorId) }.getOrDefault(emptyList())
        return EstadisticasUtils.agregar(
            nino = nino,
            tareas = tareas.filter { desde == null || EstadisticasUtils.fechaMillis(it.fechaCompletada) >= desde },
            canjes = canjes.filter { desde == null || it.fechaCanje >= desde },
            sesiones = sesiones.filter { desde == null || it.inicio >= desde },
            semanas = EstadisticasUtils.resumenSemanal(tareas, canjes, sesiones),
            encuestas = encuestas
        )
    }

    suspend fun guardarEncuesta(encuesta: EncuestaSatisfaccion) = service.guardarEncuesta(encuesta)

    suspend fun guardarToken(tutorId: String, token: String, ninoId: String = "", modo: String = "tutor") =
        service.guardarTokenTutor(TokenFcm(token, tutorId, System.currentTimeMillis(), ninoId = ninoId, modo = modo))
}
