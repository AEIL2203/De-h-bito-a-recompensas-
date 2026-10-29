package com.angel.ibanez.dehabitos.firebase

import com.angel.ibanez.dehabitos.models.AplicacionCatalogo
import com.angel.ibanez.dehabitos.models.AplicacionPermitida
import com.angel.ibanez.dehabitos.models.Dispositivo
import com.angel.ibanez.dehabitos.models.EncuestaSatisfaccion
import com.angel.ibanez.dehabitos.models.Nino
import com.angel.ibanez.dehabitos.models.Recompensa
import com.angel.ibanez.dehabitos.models.Tarea
import com.angel.ibanez.dehabitos.models.Tutor
import com.angel.ibanez.dehabitos.models.SesionApp
import com.angel.ibanez.dehabitos.models.TokenFcm
import com.angel.ibanez.dehabitos.utils.ProgresionUtils
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class FirebaseFirestoreService {

    private val db = FirebaseFirestore.getInstance()

    companion object {
        const val COLECCION_TUTORES = "tutores"
        const val COLECCION_NINOS = "ninos"
        const val COLECCION_DISPOSITIVOS = "dispositivos"
        const val COLECCION_TAREAS = "tareas"
        const val COLECCION_APPS_PERMITIDAS = "apps_permitidas"
        const val COLECCION_CATALOGO_APPS = "catalogo_apps"
        const val COLECCION_RECOMPENSAS = "recompensas"
        const val COLECCION_SESIONES = "sesiones_apps"
        const val COLECCION_TOKENS = "tokens_fcm"
        const val COLECCION_ENCUESTAS = "encuestas_satisfaccion"
        private const val TIEMPO_ESPERA_MS = 3000L
    }

    private suspend fun <T> Task<T>.esperarOEncolar(): T? = withTimeoutOrNull(TIEMPO_ESPERA_MS) { await() }

    private suspend fun DocumentReference.obtenerConRespaldo(): DocumentSnapshot {
        val errorServidor = runCatching {
            return withTimeoutOrNull(TIEMPO_ESPERA_MS) { get(Source.SERVER).await() }
                ?: throw com.google.firebase.firestore.FirebaseFirestoreException(
                    "Tiempo de espera agotado",
                    com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAVAILABLE
                )
        }.exceptionOrNull()
        return try {
            get(Source.CACHE).await()
        } catch (e: Exception) {
            throw errorServidor ?: e
        }
    }

    private suspend fun Query.obtenerConRespaldo(): QuerySnapshot {
        val errorServidor = runCatching {
            return withTimeoutOrNull(TIEMPO_ESPERA_MS) { get(Source.SERVER).await() }
                ?: throw com.google.firebase.firestore.FirebaseFirestoreException(
                    "Tiempo de espera agotado",
                    com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAVAILABLE
                )
        }.exceptionOrNull()
        return try {
            get(Source.CACHE).await()
        } catch (e: Exception) {
            throw errorServidor ?: e
        }
    }

    suspend fun guardarTutor(tutor: Tutor) {
        val id = tutor.id.ifBlank { db.collection(COLECCION_TUTORES).document().id }
        val tutorConId = tutor.copy(id = id)
        db.collection(COLECCION_TUTORES).document(id).set(tutorConId).await()
    }

    suspend fun obtenerTutor(uid: String): Tutor? {
        val documento = db.collection(COLECCION_TUTORES).document(uid).get().await()
        return documento.toObject(Tutor::class.java)
    }

    suspend fun guardarNino(nino: Nino) {
        val id = nino.id.ifBlank { db.collection(COLECCION_NINOS).document().id }
        val ninoConId = nino.copy(id = id)
        db.collection(COLECCION_NINOS).document(id).set(ninoConId).esperarOEncolar()
    }

    suspend fun actualizarAvatarNino(ninoId: String, avatar: String) {
        db.collection(COLECCION_NINOS).document(ninoId).update("avatar", avatar).esperarOEncolar()
    }

    suspend fun actualizarHorarioSueno(ninoId: String, horario: com.angel.ibanez.dehabitos.models.HorarioSueno) {
        db.collection(COLECCION_NINOS).document(ninoId).update("horarioSueno", horario).esperarOEncolar()
    }

    suspend fun obtenerNinoPorId(ninoId: String): Nino? {
        val documento = db.collection(COLECCION_NINOS).document(ninoId).obtenerConRespaldo()
        return documento.toObject(Nino::class.java)?.let { compensarEstrellasAnteriores(it) }
    }

    suspend fun obtenerNinoPorPin(pin: String, tutorId: String): Nino? {
        val consulta = db.collection(COLECCION_NINOS)
            .whereEqualTo("tutorId", tutorId)
            .whereEqualTo("pin", pin)
            .limit(1)
            .obtenerConRespaldo()
        return if (consulta.isEmpty) null else consulta.documents[0].toObject(Nino::class.java)?.let { compensarEstrellasAnteriores(it) }
    }

    suspend fun obtenerNinosPorTutor(tutorId: String): List<Nino> {
        val consulta = db.collection(COLECCION_NINOS)
            .whereEqualTo("tutorId", tutorId)
            .obtenerConRespaldo()
        return consulta.toObjects(Nino::class.java).map { compensarEstrellasAnteriores(it) }
    }

    private suspend fun compensarEstrellasAnteriores(nino: Nino): Nino {
        if (nino.compensacionEstrellasAplicada) return nino
        val referencia = db.collection(COLECCION_NINOS).document(nino.id)
        return runCatching {
            db.runTransaction { transaction ->
                val actual = transaction.get(referencia).toObject(Nino::class.java) ?: nino
                if (actual.compensacionEstrellasAplicada) return@runTransaction actual
                val compensacion = ProgresionUtils.compensacionEstrellas(actual.nivel, actual.estrellas)
                val actualizado = actual.copy(
                    estrellas = actual.estrellas + compensacion,
                    compensacionEstrellasAplicada = true
                )
                transaction.update(
                    referencia,
                    mapOf(
                        "estrellas" to actualizado.estrellas,
                        "compensacionEstrellasAplicada" to true
                    )
                )
                actualizado
            }.await()
        }.getOrDefault(nino)
    }

    suspend fun guardarDispositivo(dispositivo: Dispositivo) {
        val id = dispositivo.id.ifBlank { db.collection(COLECCION_DISPOSITIVOS).document().id }
        val conId = dispositivo.copy(id = id)
        db.collection(COLECCION_DISPOSITIVOS).document(id).set(conId).esperarOEncolar()
    }

    suspend fun obtenerDispositivo(dispositivoId: String): Dispositivo? {
        val doc = db.collection(COLECCION_DISPOSITIVOS).document(dispositivoId).obtenerConRespaldo()
        return doc.toObject(Dispositivo::class.java)
    }

    suspend fun obtenerDispositivosPorTutor(tutorId: String): List<Dispositivo> {
        val consulta = db.collection(COLECCION_DISPOSITIVOS)
            .whereEqualTo("tutorId", tutorId)
            .obtenerConRespaldo()
        return consulta.documents.mapNotNull { documento ->
            documento.toObject(Dispositivo::class.java)?.copy(id = documento.id)
        }
    }

    /**
     * Borra registros fantasma del mismo teléfono (mismo niño + mismo modelo) que quedaron
     * con un id anterior. Conserva [conservarId].
     */
    suspend fun eliminarDispositivosDuplicados(tutorId: String, ninoId: String, nombreDispositivo: String, conservarId: String) {
        obtenerDispositivosPorTutor(tutorId)
            .filter { it.id != conservarId && it.ninoId == ninoId && it.nombreDispositivo == nombreDispositivo }
            .forEach { desvincularDispositivo(it.id) }
    }

    suspend fun actualizarDesbloqueo(dispositivoId: String, desbloqueado: Boolean) {
        db.collection(COLECCION_DISPOSITIVOS).document(dispositivoId)
            .update("desbloqueado", desbloqueado)
            .esperarOEncolar()
    }

    suspend fun desvincularDispositivo(dispositivoId: String) {
        val catalogo = db.collection(COLECCION_CATALOGO_APPS)
            .whereEqualTo("deviceId", dispositivoId)
            .get()
            .await()
        val batch = db.batch()
        catalogo.documents.forEach { batch.delete(it.reference) }
        batch.delete(db.collection(COLECCION_DISPOSITIVOS).document(dispositivoId))
        batch.commit().await()
    }

    suspend fun guardarTarea(tarea: Tarea) {
        val id = tarea.id.ifBlank { db.collection(COLECCION_TAREAS).document().id }
        val conId = tarea.copy(id = id)
        db.collection(COLECCION_TAREAS).document(id).set(conId).esperarOEncolar()
    }

    suspend fun obtenerTareasPorNino(ninoId: String): List<Tarea> {
        val consulta = db.collection(COLECCION_TAREAS)
            .whereEqualTo("ninoId", ninoId)
            .obtenerConRespaldo()
        return consulta.toObjects(Tarea::class.java)
    }

    suspend fun obtenerTareasPorTutor(tutorId: String): List<Tarea> {
        val consulta = db.collection(COLECCION_TAREAS)
            .whereEqualTo("tutorId", tutorId)
            .obtenerConRespaldo()
        return consulta.toObjects(Tarea::class.java)
    }

    fun escucharTareasPorNino(
        ninoId: String,
        onUpdate: (List<Tarea>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration = db.collection(COLECCION_TAREAS)
        .whereEqualTo("ninoId", ninoId)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error.message ?: "Error al actualizar las misiones")
                return@addSnapshotListener
            }
            onUpdate(snapshot?.toObjects(Tarea::class.java) ?: emptyList())
        }

    fun escucharTareasPorTutor(
        tutorId: String,
        onUpdate: (List<Tarea>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration = db.collection(COLECCION_TAREAS)
        .whereEqualTo("tutorId", tutorId)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error.message ?: "Error al actualizar las tareas")
                return@addSnapshotListener
            }
            onUpdate(snapshot?.toObjects(Tarea::class.java) ?: emptyList())
        }

    suspend fun obtenerTareasPorNinoDesdeServidor(ninoId: String): List<Tarea> {
        val consulta = db.collection(COLECCION_TAREAS)
            .whereEqualTo("ninoId", ninoId)
            .get(Source.SERVER)
            .await()
        return consulta.toObjects(Tarea::class.java)
    }

    fun escucharNino(
        ninoId: String,
        onUpdate: (Nino) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration = db.collection(COLECCION_NINOS).document(ninoId)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error.message ?: "Error al actualizar el perfil")
                return@addSnapshotListener
            }
            snapshot?.toObject(Nino::class.java)?.let(onUpdate)
        }

    suspend fun actualizarEstadoTarea(tareaId: String, estado: String, fechaCompletada: String, evidencia: String? = null) {
        val cambios = mutableMapOf<String, Any>("estado" to estado, "fechaCompletada" to fechaCompletada)
        if (evidencia != null) cambios["evidencia"] = evidencia
        db.collection(COLECCION_TAREAS).document(tareaId)
            .update(cambios)
            .esperarOEncolar()
    }

    suspend fun crearTareaSiNoExiste(tarea: Tarea): Boolean {
        val ref = db.collection(COLECCION_TAREAS).document(tarea.id)
        return db.runTransaction { transaction ->
            if (transaction.get(ref).exists()) {
                false
            } else {
                transaction.set(ref, tarea)
                true
            }
        }.await()
    }

    suspend fun eliminarDatosNino(ninoId: String) {
        val referencias = mutableListOf<com.google.firebase.firestore.DocumentReference>()
        listOf(
            COLECCION_TAREAS,
            COLECCION_RECOMPENSAS,
            COLECCION_SESIONES,
            COLECCION_APPS_PERMITIDAS,
            COLECCION_CATALOGO_APPS,
            COLECCION_ENCUESTAS,
            COLECCION_DISPOSITIVOS,
            COLECCION_TOKENS
        ).forEach { coleccion ->
            val consulta = db.collection(coleccion).whereEqualTo("ninoId", ninoId).get().await()
            consulta.documents.forEach { referencias.add(it.reference) }
        }
        referencias.chunked(450).forEach { grupo ->
            val batch = db.batch()
            grupo.forEach { batch.delete(it) }
            batch.commit().await()
        }
        db.collection(COLECCION_NINOS).document(ninoId).delete().await()
    }

    suspend fun actualizarProgresoNino(nino: Nino) {
        db.collection(COLECCION_NINOS).document(nino.id)
            .update(
                mapOf(
                    "experiencia" to nino.experiencia,
                    "nivel" to nino.nivel,
                    "estrellas" to nino.estrellas,
                    "racha" to nino.racha,
                    "ultimoDiaCompletado" to nino.ultimoDiaCompletado,
                    "insignias" to nino.insignias,
                    "tareasAcademicas" to nino.tareasAcademicas,
                    "tareasDomesticas" to nino.tareasDomesticas,
                    "tareasPersonales" to nino.tareasPersonales
                )
            )
            .esperarOEncolar()
    }

    fun escucharDispositivo(dispositivoId: String, onUpdate: (Dispositivo?) -> Unit): ListenerRegistration {
        return db.collection(COLECCION_DISPOSITIVOS).document(dispositivoId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onUpdate(null)
                    return@addSnapshotListener
                }
                val dispositivo = snapshot?.toObject(Dispositivo::class.java)
                onUpdate(dispositivo)
            }
    }

    suspend fun sincronizarCatalogoApps(ninoId: String, deviceId: String, apps: List<AplicacionCatalogo>) {
        val collection = db.collection(COLECCION_CATALOGO_APPS)
        val existentes = collection
            .whereEqualTo("ninoId", ninoId)
            .get()
            .await()
        val idsActuales = apps.mapTo(mutableSetOf()) { it.id }
        val operaciones = buildList {
            apps.forEach { app -> add(collection.document(app.id) to app) }
            existentes.documents
                .filter { it.getString("deviceId") == deviceId }
                .filterNot { it.id in idsActuales }
                .forEach { add(collection.document(it.id) to null) }
        }
        operaciones.chunked(450).forEach { grupo ->
            val batch = db.batch()
            grupo.forEach { (ref, app) ->
                if (app == null) batch.delete(ref) else batch.set(ref, app)
            }
            batch.commit().await()
        }
    }

    suspend fun obtenerCatalogoAppsPorNino(ninoId: String): List<AplicacionCatalogo> {
        val consulta = db.collection(COLECCION_CATALOGO_APPS)
            .whereEqualTo("ninoId", ninoId)
            .obtenerConRespaldo()
        return consulta.toObjects(AplicacionCatalogo::class.java)
            .groupBy { it.packageName }
            .mapNotNull { (_, versiones) -> versiones.maxByOrNull { it.actualizadoEn } }
            .sortedBy { it.nombre.lowercase() }
    }

    suspend fun guardarAplicacionPermitida(app: AplicacionPermitida) {
        val id = app.id.ifBlank { db.collection(COLECCION_APPS_PERMITIDAS).document().id }
        val conId = app.copy(id = id)
        db.collection(COLECCION_APPS_PERMITIDAS).document(id).set(conId).esperarOEncolar()
    }

    suspend fun eliminarAplicacionPermitida(appId: String) {
        db.collection(COLECCION_APPS_PERMITIDAS).document(appId).delete().esperarOEncolar()
    }

    suspend fun obtenerAppsPermitidasPorNino(ninoId: String): List<AplicacionPermitida> {
        val consulta = db.collection(COLECCION_APPS_PERMITIDAS)
            .whereEqualTo("ninoId", ninoId)
            .obtenerConRespaldo()
        return consulta.toObjects(AplicacionPermitida::class.java)
    }

    suspend fun canjearRecompensa(
        nino: Nino,
        app: AplicacionPermitida,
        minutos: Int,
        estrellasCosto: Int,
        fechaCanje: Long
    ): Recompensa {
        val ninoRef = db.collection(COLECCION_NINOS).document(nino.id)
        val recompensaId = db.collection(COLECCION_RECOMPENSAS).document().id
        val recompensa = Recompensa(
            id = recompensaId,
            ninoId = nino.id,
            tutorId = nino.tutorId,
            packageName = app.packageName,
            minutosOtorgados = minutos,
            estrellasGastadas = estrellasCosto,
            fechaCanje = fechaCanje
        )
        db.runTransaction { transaction ->
            val ninoActual = transaction.get(ninoRef).toObject(Nino::class.java)
                ?: throw IllegalStateException("Niño no encontrado")
            if (ninoActual.estrellas < estrellasCosto) {
                throw IllegalStateException("No tienes suficientes monedas")
            }
            transaction.update(
                ninoRef,
                mapOf(
                    "estrellas" to ninoActual.estrellas - estrellasCosto,
                    "minutosExtra" to ninoActual.minutosExtra + minutos
                )
            )
            transaction.set(db.collection(COLECCION_RECOMPENSAS).document(recompensaId), recompensa)
        }.await()
        return recompensa
    }

    suspend fun consumirRecompensas(consumos: Map<String, Long>) {
        if (consumos.isEmpty()) return
        db.runTransaction { transaction ->
            val actuales = consumos.filterValues { it > 0 }.mapNotNull { (recompensaId, milisegundos) ->
                val ref = db.collection(COLECCION_RECOMPENSAS).document(recompensaId)
                transaction.get(ref).toObject(Recompensa::class.java)?.let { Triple(ref, it, milisegundos) }
            }
            actuales.forEach { (ref, actual, milisegundos) ->
                val consumidos = (actual.milisegundosConsumidos + milisegundos)
                    .coerceAtMost(actual.minutosOtorgados * 60_000L)
                transaction.update(ref, "milisegundosConsumidos", consumidos)
            }
        }.await()
    }

    fun escucharAppsPermitidasPorNino(
        ninoId: String,
        onUpdate: (List<AplicacionPermitida>) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration {
        return db.collection(COLECCION_APPS_PERMITIDAS)
            .whereEqualTo("ninoId", ninoId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                val lista = snapshot?.toObjects(AplicacionPermitida::class.java) ?: emptyList()
                onUpdate(lista)
            }
    }

    suspend fun iniciarSesionApp(sesion: SesionApp): String {
        val ref = if (sesion.id.isBlank()) db.collection(COLECCION_SESIONES).document() else db.collection(COLECCION_SESIONES).document(sesion.id)
        val conId = sesion.copy(id = ref.id)
        db.runTransaction { transaction ->
            if (!transaction.get(ref).exists()) transaction.set(ref, conId)
        }.await()
        return ref.id
    }

    suspend fun finalizarSesionApp(sesionId: String, fin: Long, duracionMs: Long) {
        val ref = db.collection(COLECCION_SESIONES).document(sesionId)
        db.runTransaction { transaction ->
            val actual = transaction.get(ref).toObject(SesionApp::class.java)
            if (actual != null && actual.estado == SesionApp.ESTADO_INICIADA) {
                transaction.update(ref, mapOf("fin" to fin, "duracionMs" to duracionMs.coerceAtLeast(0), "estado" to SesionApp.ESTADO_FINALIZADA))
            }
        }.await()
    }

    suspend fun obtenerSesionesPorNino(ninoId: String): List<SesionApp> = db.collection(COLECCION_SESIONES)
        .whereEqualTo("ninoId", ninoId).get().await().toObjects(SesionApp::class.java)

    suspend fun obtenerRecompensasPorNino(ninoId: String): List<Recompensa> = db.collection(COLECCION_RECOMPENSAS)
        .whereEqualTo("ninoId", ninoId).get().await().toObjects(Recompensa::class.java)

    suspend fun guardarTokenTutor(token: TokenFcm) {
        db.collection(COLECCION_TOKENS).document(token.token).set(token).esperarOEncolar()
    }

    suspend fun guardarEncuesta(encuesta: EncuestaSatisfaccion) {
        val id = encuesta.id.ifBlank { db.collection(COLECCION_ENCUESTAS).document().id }
        db.collection(COLECCION_ENCUESTAS).document(id).set(encuesta.copy(id = id)).await()
    }

    suspend fun obtenerEncuestasPorNino(ninoId: String, tutorId: String): List<EncuestaSatisfaccion> = db.collection(COLECCION_ENCUESTAS)
        .whereEqualTo("tutorId", tutorId)
        .whereEqualTo("ninoId", ninoId)
        .get().await().toObjects(EncuestaSatisfaccion::class.java)

    fun escucharRecompensasPorNino(
        ninoId: String,
        onUpdate: (List<Recompensa>) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration {
        return db.collection(COLECCION_RECOMPENSAS)
            .whereEqualTo("ninoId", ninoId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                val lista = snapshot?.toObjects(Recompensa::class.java) ?: emptyList()
                onUpdate(lista)
            }
    }
}
