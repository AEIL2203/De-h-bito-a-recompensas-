package com.angel.ibanez.dehabitos.repository

import com.angel.ibanez.dehabitos.firebase.FirebaseFirestoreService
import com.angel.ibanez.dehabitos.models.EstadisticasNino
import com.angel.ibanez.dehabitos.models.SesionApp
import com.angel.ibanez.dehabitos.models.TokenFcm
import com.angel.ibanez.dehabitos.utils.EstadisticasUtils

class EstadisticasRepository(private val service: FirebaseFirestoreService = FirebaseFirestoreService()) {
    suspend fun iniciarSesion(sesion: SesionApp) = service.iniciarSesionApp(sesion)
    suspend fun finalizarSesion(id: String, fin: Long, duracionMs: Long) = service.finalizarSesionApp(id, fin, duracionMs)

    suspend fun obtener(ninoId: String, desde: Long?): EstadisticasNino {
        val nino = service.obtenerNinoPorId(ninoId) ?: error("Niño no encontrado")
        val tareas = service.obtenerTareasPorNino(ninoId).filter { desde == null || fechaMillis(it.fechaCompletada) >= desde }
        val canjes = service.obtenerRecompensasPorNino(ninoId).filter { desde == null || it.fechaCanje >= desde }
        val sesiones = service.obtenerSesionesPorNino(ninoId).filter { desde == null || it.inicio >= desde }
        return EstadisticasUtils.agregar(nino, tareas, canjes, sesiones)
    }

    suspend fun guardarToken(tutorId: String, token: String) = service.guardarTokenTutor(TokenFcm(token, tutorId, System.currentTimeMillis()))

    private fun fechaMillis(valor: String): Long = runCatching {
        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).parse(valor)?.time ?: 0
    }.getOrDefault(0)
}
