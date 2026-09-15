package com.angel.ibanez.dehabitos.models

import java.security.MessageDigest

data class AplicacionCatalogo(
    val id: String = "",
    val ninoId: String = "",
    val deviceId: String = "",
    val packageName: String = "",
    val nombre: String = "",
    val iconoBase64: String = "",
    val actualizadoEn: Long = 0L
) {
    companion object {
        fun crearId(ninoId: String, deviceId: String, packageName: String): String {
            val clave = "$ninoId\u0000$deviceId\u0000$packageName"
            return MessageDigest.getInstance("SHA-256")
                .digest(clave.toByteArray())
                .joinToString("") { "%02x".format(it) }
        }
    }
}
