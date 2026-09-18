package com.angel.ibanez.dehabitos.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.UUID

object DispositivoConfig {
    private const val TAG = "DispositivoConfig"
    private const val PREFS_NAME = "dispositivo_config"
    private const val PREFS_CIFRADAS_NAME = "dispositivo_config_seguro"
    private const val KEY_TUTOR_ID = "tutor_id"
    private const val KEY_NINO_ID = "nino_id"
    private const val KEY_NOMBRE_NINO = "nombre_nino"
    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_MODO = "modo"
    private const val KEY_CATALOGO_HASH = "catalogo_apps_hash"
    private const val KEY_CATALOGO_FECHA = "catalogo_apps_fecha"

    private lateinit var prefs: SharedPreferences

    fun inicializar(context: Context) {
        val planas = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = crearPrefsCifradas(context)?.also { migrar(planas, it) } ?: planas
        if (prefs.getString(KEY_DEVICE_ID, null) == null) {
            prefs.edit().putString(KEY_DEVICE_ID, UUID.randomUUID().toString()).apply()
        }
    }

    private fun crearPrefsCifradas(context: Context): SharedPreferences? = runCatching {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_CIFRADAS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }.onFailure { Log.w(TAG, "No se pudo crear el almacenamiento cifrado; se usan preferencias planas", it) }.getOrNull()

    private fun migrar(planas: SharedPreferences, cifradas: SharedPreferences) {
        val pendientes = planas.all
        if (pendientes.isEmpty() || cifradas.contains(KEY_DEVICE_ID)) return
        val editor = cifradas.edit()
        pendientes.forEach { (clave, valor) ->
            when (valor) {
                is String -> editor.putString(clave, valor)
                is Long -> editor.putLong(clave, valor)
                is Int -> editor.putInt(clave, valor)
                is Boolean -> editor.putBoolean(clave, valor)
            }
        }
        editor.apply()
        planas.edit().clear().apply()
    }

    fun guardarTutorId(tutorId: String) {
        prefs.edit().putString(KEY_TUTOR_ID, tutorId).apply()
    }

    fun obtenerTutorId(): String = prefs.getString(KEY_TUTOR_ID, "") ?: ""

    fun guardarNino(ninoId: String, nombreNino: String) {
        prefs.edit()
            .putString(KEY_NINO_ID, ninoId)
            .putString(KEY_NOMBRE_NINO, nombreNino)
            .apply()
    }

    fun obtenerNinoId(): String = prefs.getString(KEY_NINO_ID, "") ?: ""
    fun obtenerNombreNino(): String = prefs.getString(KEY_NOMBRE_NINO, "") ?: ""
    fun obtenerDeviceId(): String = prefs.getString(KEY_DEVICE_ID, "") ?: ""

    fun debeSincronizarCatalogo(hash: String, ahora: Long = System.currentTimeMillis()): Boolean {
        val mismoCatalogo = prefs.getString(KEY_CATALOGO_HASH, "") == hash
        val ultimaFecha = prefs.getLong(KEY_CATALOGO_FECHA, 0L)
        return !mismoCatalogo || ahora - ultimaFecha >= 24 * 60 * 60 * 1000L
    }

    fun registrarCatalogoSincronizado(hash: String, fecha: Long = System.currentTimeMillis()) {
        prefs.edit()
            .putString(KEY_CATALOGO_HASH, hash)
            .putLong(KEY_CATALOGO_FECHA, fecha)
            .apply()
    }

    fun guardarModo(modo: ModoDispositivo) {
        prefs.edit().putString(KEY_MODO, modo.valor).apply()
    }

    fun obtenerModo(): ModoDispositivo {
        val valor = prefs.getString(KEY_MODO, "") ?: ""
        return ModoDispositivo.fromString(valor)
    }

    fun limpiarNino() {
        prefs.edit()
            .remove(KEY_NINO_ID)
            .remove(KEY_NOMBRE_NINO)
            .apply()
    }

    fun limpiar() {
        val deviceId = obtenerDeviceId()
        prefs.edit().clear().putString(KEY_DEVICE_ID, deviceId).apply()
    }

    enum class ModoDispositivo(val valor: String) {
        PADRE("padre"),
        HIJO("hijo"),
        SIN_CONFIGURAR("");

        companion object {
            fun fromString(valor: String) = values().find { it.valor == valor } ?: SIN_CONFIGURAR
        }
    }
}
