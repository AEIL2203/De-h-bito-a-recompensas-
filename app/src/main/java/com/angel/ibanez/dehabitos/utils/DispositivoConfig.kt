package com.angel.ibanez.dehabitos.utils

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

object DispositivoConfig {
    private const val PREFS_NAME = "dispositivo_config"
    private const val KEY_TUTOR_ID = "tutor_id"
    private const val KEY_NINO_ID = "nino_id"
    private const val KEY_NOMBRE_NINO = "nombre_nino"
    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_MODO = "modo"
    private const val KEY_CATALOGO_HASH = "catalogo_apps_hash"
    private const val KEY_CATALOGO_FECHA = "catalogo_apps_fecha"

    private lateinit var prefs: SharedPreferences

    fun inicializar(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_DEVICE_ID, null) == null) {
            prefs.edit().putString(KEY_DEVICE_ID, UUID.randomUUID().toString()).apply()
        }
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
