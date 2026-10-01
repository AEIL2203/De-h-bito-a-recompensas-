package com.angel.ibanez.dehabitos.data.local

import android.content.Context
import com.angel.ibanez.dehabitos.models.Nino
import com.angel.ibanez.dehabitos.models.Tarea
import com.angel.ibanez.dehabitos.utils.DispositivoConfig
import com.google.gson.Gson
import java.security.SecureRandom
import android.util.Base64
import android.util.Log

object LocalCacheManager {

    private const val TAG = "LocalCacheManager"
    private const val COLECCION_NINOS = "ninos"
    private const val COLECCION_TAREAS = "tareas"

    private var db: OfflineCacheDb? = null
    private val gson = Gson()

    fun inicializar(context: Context) {
        if (db != null) return
        val passphrase = DispositivoConfig.obtenerDbPassphrase()
            ?: generarPassphrase().also { DispositivoConfig.guardarDbPassphrase(it) }
        db = OfflineCacheDb(context.applicationContext, passphrase)
    }

    private fun generarPassphrase(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun requireDb(): OfflineCacheDb = db ?: throw IllegalStateException("LocalCacheManager no inicializado")

    fun guardarNino(nino: Nino) {
        runCatching {
            requireDb().guardar(COLECCION_NINOS, nino.id, nino.id, gson.toJson(nino))
        }.onFailure { Log.e(TAG, "No se pudo guardar el niño en caché", it) }
    }

    fun obtenerNino(ninoId: String): Nino? = runCatching {
        val json = requireDb().obtener(COLECCION_NINOS, ninoId) ?: return@runCatching null
        gson.fromJson(json, Nino::class.java)
    }.onFailure { Log.e(TAG, "No se pudo leer el niño de la caché", it) }.getOrNull()

    fun guardarTareas(ninoId: String, tareas: List<Tarea>) {
        runCatching {
            val db = requireDb()
            db.eliminarPorNino(COLECCION_TAREAS, ninoId)
            tareas.forEach { db.guardar(COLECCION_TAREAS, it.id, ninoId, gson.toJson(it)) }
        }.onFailure { Log.e(TAG, "No se pudieron guardar las tareas en caché", it) }
    }

    fun obtenerTareas(ninoId: String): List<Tarea> = runCatching {
        requireDb().obtenerPorNino(COLECCION_TAREAS, ninoId).mapNotNull {
            runCatching { gson.fromJson(it, Tarea::class.java) }.getOrNull()
        }
    }.onFailure { Log.e(TAG, "No se pudieron leer las tareas de la caché", it) }.getOrDefault(emptyList())
}
