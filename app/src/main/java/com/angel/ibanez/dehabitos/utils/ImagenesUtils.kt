package com.angel.ibanez.dehabitos.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream

object ImagenesUtils {

    private const val LADO_MAXIMO = 720
    private const val LIMITE_BASE64 = 600 * 1024

    fun comprimirABase64(bitmap: Bitmap): String {
        val escalado = escalar(bitmap)
        var calidad = 70
        var salida = ByteArrayOutputStream()
        escalado.compress(Bitmap.CompressFormat.JPEG, calidad, salida)
        while (Base64.encodeToString(salida.toByteArray(), Base64.DEFAULT).length > LIMITE_BASE64 && calidad > 10) {
            calidad -= 10
            salida = ByteArrayOutputStream()
            escalado.compress(Bitmap.CompressFormat.JPEG, calidad, salida)
        }
        return Base64.encodeToString(salida.toByteArray(), Base64.DEFAULT)
    }

    fun decodificar(base64: String): Bitmap? = runCatching {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()

    private fun escalar(bitmap: Bitmap): Bitmap {
        val mayor = maxOf(bitmap.width, bitmap.height)
        if (mayor <= LADO_MAXIMO) return bitmap
        val escala = LADO_MAXIMO.toFloat() / mayor
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * escala).toInt().coerceAtLeast(1),
            (bitmap.height * escala).toInt().coerceAtLeast(1),
            true
        )
    }
}
