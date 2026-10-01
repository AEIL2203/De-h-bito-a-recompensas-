@file:Suppress("DEPRECATION")
package com.angel.ibanez.dehabitos.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

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
    private const val KEY_PIN_PARENTAL_HASH = "pin_parental_hash"
    private const val KEY_PIN_PARENTAL_SALT = "pin_parental_salt"
    private const val KEY_PIN_INTENTOS = "pin_parental_intentos"
    private const val KEY_PIN_BLOQUEO_HASTA = "pin_parental_bloqueo_hasta"
    private const val KEY_DB_PASSPHRASE = "db_passphrase"
    private const val KEY_NOTIF_HIJO_HORA = "notif_hijo_hora"
    private const val KEY_NOTIF_TUTOR_HORA = "notif_tutor_hora"
    private const val KEY_NOTIF_HABILITADAS = "notif_habilitadas"
    private const val MAX_INTENTOS_PIN = 5
    private const val BLOQUEO_PIN_MS = 60_000L

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

    fun tienePinParental(): Boolean = prefs.contains(KEY_PIN_PARENTAL_HASH) && prefs.contains(KEY_PIN_PARENTAL_SALT)

    fun guardarPinParental(pin: String) {
        require(pin.matches(Regex("\\d{6}"))) { "El PIN parental debe tener 6 dígitos" }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = derivarPin(pin, salt)
        prefs.edit()
            .putString(KEY_PIN_PARENTAL_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_PIN_PARENTAL_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .putInt(KEY_PIN_INTENTOS, 0)
            .putLong(KEY_PIN_BLOQUEO_HASTA, 0L)
            .apply()
    }

    fun validarPinParental(pin: String, ahora: Long = System.currentTimeMillis()): ResultadoPin {
        val bloqueoHasta = prefs.getLong(KEY_PIN_BLOQUEO_HASTA, 0L)
        if (bloqueoHasta > ahora) return ResultadoPin.BLOQUEADO
        val salt = prefs.getString(KEY_PIN_PARENTAL_SALT, null)?.let { Base64.decode(it, Base64.NO_WRAP) }
            ?: return ResultadoPin.NO_CONFIGURADO
        val esperado = prefs.getString(KEY_PIN_PARENTAL_HASH, null)?.let { Base64.decode(it, Base64.NO_WRAP) }
            ?: return ResultadoPin.NO_CONFIGURADO
        if (MessageDigest.isEqual(esperado, derivarPin(pin, salt))) {
            prefs.edit().putInt(KEY_PIN_INTENTOS, 0).putLong(KEY_PIN_BLOQUEO_HASTA, 0L).apply()
            return ResultadoPin.CORRECTO
        }
        val intentos = prefs.getInt(KEY_PIN_INTENTOS, 0) + 1
        val editor = prefs.edit().putInt(KEY_PIN_INTENTOS, intentos)
        if (intentos >= MAX_INTENTOS_PIN) {
            editor.putInt(KEY_PIN_INTENTOS, 0).putLong(KEY_PIN_BLOQUEO_HASTA, ahora + BLOQUEO_PIN_MS)
        }
        editor.apply()
        return ResultadoPin.INCORRECTO
    }

    fun segundosBloqueoPin(ahora: Long = System.currentTimeMillis()): Long =
        ((prefs.getLong(KEY_PIN_BLOQUEO_HASTA, 0L) - ahora).coerceAtLeast(0L) + 999L) / 1000L

    private fun derivarPin(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 120_000, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
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

    fun limpiar(preservarPinParental: Boolean = false) {
        val deviceId = obtenerDeviceId()
        val pinHash = prefs.getString(KEY_PIN_PARENTAL_HASH, null)
        val pinSalt = prefs.getString(KEY_PIN_PARENTAL_SALT, null)
        val intentos = prefs.getInt(KEY_PIN_INTENTOS, 0)
        val bloqueoHasta = prefs.getLong(KEY_PIN_BLOQUEO_HASTA, 0L)
        val editor = prefs.edit().clear().putString(KEY_DEVICE_ID, deviceId)
        if (preservarPinParental && pinHash != null && pinSalt != null) {
            editor.putString(KEY_PIN_PARENTAL_HASH, pinHash)
                .putString(KEY_PIN_PARENTAL_SALT, pinSalt)
                .putInt(KEY_PIN_INTENTOS, intentos)
                .putLong(KEY_PIN_BLOQUEO_HASTA, bloqueoHasta)
        }
        editor.apply()
    }

    fun guardarDbPassphrase(passphrase: String) {
        prefs.edit().putString(KEY_DB_PASSPHRASE, passphrase).apply()
    }

    fun obtenerDbPassphrase(): String? = prefs.getString(KEY_DB_PASSPHRASE, null)

    fun guardarConfigNotificaciones(hijoHora: Int, tutorHora: Int, habilitadas: Boolean) {
        prefs.edit()
            .putInt(KEY_NOTIF_HIJO_HORA, hijoHora)
            .putInt(KEY_NOTIF_TUTOR_HORA, tutorHora)
            .putBoolean(KEY_NOTIF_HABILITADAS, habilitadas)
            .apply()
    }

    fun obtenerHoraNotificacionHijo(): Int = prefs.getInt(KEY_NOTIF_HIJO_HORA, 7)
    fun obtenerHoraNotificacionTutor(): Int = prefs.getInt(KEY_NOTIF_TUTOR_HORA, 17)
    fun notificacionesHabilitadas(): Boolean = prefs.getBoolean(KEY_NOTIF_HABILITADAS, true)

    enum class ResultadoPin { CORRECTO, INCORRECTO, BLOQUEADO, NO_CONFIGURADO }

    enum class ModoDispositivo(val valor: String) {
        PADRE("padre"),
        HIJO("hijo"),
        SIN_CONFIGURAR("");

        companion object {
            fun fromString(valor: String) = values().find { it.valor == valor } ?: SIN_CONFIGURAR
        }
    }
}
