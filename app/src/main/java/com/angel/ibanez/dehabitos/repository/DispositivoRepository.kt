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
            emit(Resultado.Exito(dispositivo))
        } catch (e: Exception) {
            emit(Resultado.Error(e.message ?: "Error al vincular el dispositivo"))
        }
    }

    fun obtenerDispositivosPorTutor(tutorId: String): Flow<Resultado<List<Dispositivo>>> = flow {
        emit(Resultado.Cargando)
        try {
            val lista = firestoreService.obtenerDispositivosPorTutor(tutorId)
            emit(Resultado.Exito(lista))
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
