package com.angel.ibanez.dehabitos.repository

import com.angel.ibanez.dehabitos.firebase.FirebaseFirestoreService
import com.angel.ibanez.dehabitos.models.AplicacionCatalogo
import com.angel.ibanez.dehabitos.models.AplicacionPermitida
import com.angel.ibanez.dehabitos.models.Nino
import com.angel.ibanez.dehabitos.models.Recompensa
import com.angel.ibanez.dehabitos.utils.Resultado
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class RecompensaRepository(
    private val firestoreService: FirebaseFirestoreService = FirebaseFirestoreService()
) {

    fun sincronizarCatalogoApps(
        ninoId: String,
        deviceId: String,
        apps: List<AplicacionCatalogo>
    ): Flow<Resultado<Unit>> = flow {
        emit(Resultado.Cargando)
        try {
            firestoreService.sincronizarCatalogoApps(ninoId, deviceId, apps)
            emit(Resultado.Exito(Unit))
        } catch (e: Exception) {
            emit(Resultado.Error(e.message ?: "Error al sincronizar apps"))
        }
    }

    fun obtenerCatalogoAppsPorNino(ninoId: String): Flow<Resultado<List<AplicacionCatalogo>>> = flow {
        emit(Resultado.Cargando)
        try {
            emit(Resultado.Exito(firestoreService.obtenerCatalogoAppsPorNino(ninoId)))
        } catch (e: Exception) {
            emit(Resultado.Error(e.message ?: "Error al cargar el catálogo de apps"))
        }
    }

    fun guardarAplicacionPermitida(app: AplicacionPermitida): Flow<Resultado<AplicacionPermitida>> = flow {
        emit(Resultado.Cargando)
        try {
            firestoreService.guardarAplicacionPermitida(app)
            emit(Resultado.Exito(app))
        } catch (e: Exception) {
            emit(Resultado.Error(e.message ?: "Error al guardar la app"))
        }
    }

    fun eliminarAplicacionPermitida(appId: String): Flow<Resultado<Unit>> = flow {
        emit(Resultado.Cargando)
        try {
            firestoreService.eliminarAplicacionPermitida(appId)
            emit(Resultado.Exito(Unit))
        } catch (e: Exception) {
            emit(Resultado.Error(e.message ?: "Error al eliminar la app"))
        }
    }

    fun obtenerAppsPermitidasPorNino(ninoId: String): Flow<Resultado<List<AplicacionPermitida>>> = flow {
        emit(Resultado.Cargando)
        try {
            val lista = firestoreService.obtenerAppsPermitidasPorNino(ninoId)
            emit(Resultado.Exito(lista))
        } catch (e: Exception) {
            emit(Resultado.Error(e.message ?: "Error al cargar apps"))
        }
    }

    fun canjearRecompensa(
        nino: Nino,
        app: AplicacionPermitida,
        minutos: Int,
        estrellasCosto: Int,
        fechaCanje: Long
    ): Flow<Resultado<Recompensa>> = flow {
        emit(Resultado.Cargando)
        try {
            val recompensa = firestoreService.canjearRecompensa(nino, app, minutos, estrellasCosto, fechaCanje)
            emit(Resultado.Exito(recompensa))
        } catch (e: Exception) {
            emit(Resultado.Error(e.message ?: "Error al canjear"))
        }
    }

    fun escucharAppsPermitidasPorNino(
        ninoId: String,
        onUpdate: (List<AplicacionPermitida>) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration = firestoreService.escucharAppsPermitidasPorNino(ninoId, onUpdate, onError)

    fun escucharRecompensasPorNino(
        ninoId: String,
        onUpdate: (List<Recompensa>) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration = firestoreService.escucharRecompensasPorNino(ninoId, onUpdate, onError)

    suspend fun consumirRecompensas(consumos: Map<String, Long>) {
        firestoreService.consumirRecompensas(consumos)
    }
}
