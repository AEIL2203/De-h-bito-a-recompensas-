package com.angel.ibanez.dehabitos.repository

import com.angel.ibanez.dehabitos.firebase.FirebaseFirestoreService
import com.angel.ibanez.dehabitos.models.Nino
import com.angel.ibanez.dehabitos.utils.Resultado
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class NinoRepository(
    private val firestoreService: FirebaseFirestoreService = FirebaseFirestoreService()
) {

    fun crearNino(nino: Nino): Flow<Resultado<Nino>> = flow {
        emit(Resultado.Cargando)
        try {
            firestoreService.guardarNino(nino)
            emit(Resultado.Exito(nino))
        } catch (e: Exception) {
            emit(Resultado.Error(e.message ?: "Error desconocido"))
        }
    }

    fun actualizarAvatar(ninoId: String, avatar: String): Flow<Resultado<String>> = flow {
        emit(Resultado.Cargando)
        try {
            firestoreService.actualizarAvatarNino(ninoId, avatar)
            emit(Resultado.Exito(avatar))
        } catch (e: Exception) {
            emit(Resultado.Error(e.message ?: "No se pudo actualizar el avatar"))
        }
    }

    fun obtenerNinosPorTutor(tutorId: String): Flow<Resultado<List<Nino>>> = flow {
        emit(Resultado.Cargando)
        try {
            val ninos = firestoreService.obtenerNinosPorTutor(tutorId)
            emit(Resultado.Exito(ninos))
        } catch (e: Exception) {
            emit(Resultado.Error(e.message ?: "Error desconocido"))
        }
    }

    fun obtenerNinoPorId(ninoId: String): Flow<Resultado<Nino>> = flow {
        emit(Resultado.Cargando)
        try {
            val nino = firestoreService.obtenerNinoPorId(ninoId)
                ?: throw Exception("Niño no encontrado")
            emit(Resultado.Exito(nino))
        } catch (e: Exception) {
            emit(Resultado.Error(e.message ?: "Error desconocido"))
        }
    }

    fun escucharNino(
        ninoId: String,
        onResultado: (Resultado<Nino>) -> Unit
    ): ListenerRegistration = firestoreService.escucharNino(
        ninoId,
        { onResultado(Resultado.Exito(it)) },
        { onResultado(Resultado.Error(it)) }
    )

    fun iniciarSesionNino(pin: String): Flow<Resultado<Nino>> = flow {
        emit(Resultado.Cargando)
        try {
            val nino = firestoreService.obtenerNinoPorPin(pin)
                ?: throw Exception("PIN no encontrado")
            emit(Resultado.Exito(nino))
        } catch (e: Exception) {
            emit(Resultado.Error(e.message ?: "Error desconocido"))
        }
    }
}
