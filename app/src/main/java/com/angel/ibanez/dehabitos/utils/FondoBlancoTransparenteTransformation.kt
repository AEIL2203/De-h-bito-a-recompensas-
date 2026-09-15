package com.angel.ibanez.dehabitos.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class FondoBlancoTransparenteTransformation : BitmapTransformation() {
    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        val ancho = toTransform.width
        val alto = toTransform.height
        val resultado = pool.get(ancho, alto, Bitmap.Config.ARGB_8888)
        resultado.setHasAlpha(true)
        val pixeles = IntArray(ancho * alto)
        toTransform.getPixels(pixeles, 0, ancho, 0, 0, ancho, alto)
        pixeles.indices.forEach { indice ->
            val color = pixeles[indice]
            val minimo = minOf(Color.red(color), Color.green(color), Color.blue(color))
            val alpha = when {
                minimo >= 245 -> 0
                minimo >= 220 -> Color.alpha(color) * (245 - minimo) / 25
                else -> Color.alpha(color)
            }
            pixeles[indice] = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
        }
        resultado.setPixels(pixeles, 0, ancho, 0, 0, ancho, alto)
        return resultado
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(ID.toByteArray(StandardCharsets.UTF_8))
    }

    override fun equals(other: Any?): Boolean = other is FondoBlancoTransparenteTransformation

    override fun hashCode(): Int = ID.hashCode()

    companion object {
        private const val ID = "com.angel.ibanez.dehabitos.FondoBlancoTransparenteTransformation.v1"
    }
}
