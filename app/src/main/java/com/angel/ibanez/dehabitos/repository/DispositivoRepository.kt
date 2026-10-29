package com.angel.ibanez.dehabitos.repository

import com.angel.ibanez.dehabitos.firebase.FirebaseFirestoreService
import com.angel.ibanez.dehabitos.models.Dispositivo
import com.angel.ibanez.dehabitos.utils.Resultado
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class DispositivoRepository(
    private val firestoreService: FirebaseFirestoreService = FirebaseFirestoreService()
) {

    fun vincularDispositivo(dispositivo: Dispositivo): Flow<Resultado<Dispositivo>> = flow {
        emit(Resultado.Cargando)
        try {
            firestoreService.guardarDispositivo(dispositivo)
            runCatching {
                firestoreService.eliminarDispositivosDuplicados(
                    dispositivo.tutorId, dispositivo.ninoId, dispositivo.nombreDispositivo, dispositivo.id
                )
            }
            emit(Resultado.Exito(dispositivo))
        } catch (e: Exception) {
            emit(Resultado.Error(e.message ?: "Error al vincular el dispositivo"))
        }
    }

    fun obtenerDispositivosPorTutor(tutorId: String): Flow<Resultado<List<Dispositivo>>> = flow {
        emit(Resultado.Cargando)
        try {
            val todos = firestoreService.obtenerDispositivosPorTutor(tutorId)
            // Limpia los duplicados ya existentes: por niño + modelo se queda el vínculo más reciente
            val (lista, fantasmas) = todos
                .groupBy { it.ninoId to it.nombreDispositivo }
                .values
                .map { grupo -> grupo.sortedByDescending { it.fechaVinculacion } }
                .let { grupos -> grupos.map { it.first() } to grupos.flatMap { it.drop(1) } }
            fantasmas.forEach { runCatching { firestoreService.desvincularDispositivo(it.id) } }
            emit(Resultado.Exito(lista.sortedBy { it.nombreNino }))
        } catch (e: Exception) {
            emit(Resultado.Error(e.message ?: "Error al cargar dispositivos"))
        }
    }

    fun cambiarEstadoDesbloqueo(dispositivoId: String, desbloqueado: Boolean): Flow<Resultado<Unit>> = flow {
        emit(Resultado.Cargando)
        try {
            firestoreService.actualizarDesbloqueo(dispositivoId, desbloqueado)
            emit(Resultado.Exito(Unit))
        } catch (e: Exception) {
            emit(Resultado.Error(e.message ?: "Error al cambiar el estado"))
        }
    }

    fun desvincularDispositivo(dispositivoId: String): Flow<Resultado<Unit>> = flow {
        emit(Resultado.Cargando)
        try {
            firestoreService.desvincularDispositivo(dispositivoId)
            emit(Resultado.Exito(Unit))
        } catch (e: Exception) {
            emit(Resultado.Error(e.message ?: "Error al desvincular el dispositivo"))
        }
    }

    fun escucharDispositivo(dispositivoId: String, onUpdate: (Dispositivo?) -> Unit): ListenerRegistration {
        return firestoreService.escucharDispositivo(dispositivoId, onUpdate)
    }
}
