package com.angel.ibanez.dehabitos.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.Base64
import com.angel.ibanez.dehabitos.models.AplicacionCatalogo
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

data class AppInfo(
    val packageName: String,
    val nombre: String,
    val icono: Drawable?
)

private val nombresConocidos = mapOf(
    "com.google.android.youtube" to "YouTube",
    "com.google.android.apps.youtube.kids" to "YouTube Kids",
    "com.google.android.apps.youtube.music" to "YouTube Music",
    "com.google.android.youtube.tv" to "YouTube",
    "com.zhiliaoapp.musically" to "TikTok",
    "com.ss.android.ugc.trill" to "TikTok",
    "com.facebook.katana" to "Facebook",
    "com.facebook.orca" to "Messenger",
    "com.instagram.android" to "Instagram",
    "com.whatsapp" to "WhatsApp",
    "com.whatsapp.w4b" to "WhatsApp Business",
    "com.spotify.music" to "Spotify",
    "com.netflix.mediaclient" to "Netflix",
    "com.disney.disneyplus" to "Disney+",
    "com.google.android.apps.nbu.paisa.user" to "Google Pay",
    "com.google.android.gms" to "Servicios de Google Play",
    "com.google.android.gsf" to "Google Services Framework",
    "com.android.vending" to "Google Play Store"
)

object AppsUtils {

    fun listarAppsInstaladas(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        
        val resolveList = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
        
        return resolveList
            .filter { it.activityInfo.packageName != context.packageName } // No incluir esta app
            .sortedBy { it.loadLabel(pm).toString().lowercase() }
            .map {
                AppInfo(
                    packageName = it.activityInfo.packageName,
                    nombre = it.loadLabel(pm).toString(),
                    icono = it.loadIcon(pm)
                )
            }
            .distinctBy { it.packageName }
    }

    fun crearCatalogo(context: Context, ninoId: String, deviceId: String): Pair<List<AplicacionCatalogo>, String> {
        val fecha = System.currentTimeMillis()
        val apps = listarAppsInstaladas(context)
            .distinctBy { it.packageName }
            .map { app ->
                AplicacionCatalogo(
                    id = AplicacionCatalogo.crearId(ninoId, deviceId, app.packageName),
                    ninoId = ninoId,
                    deviceId = deviceId,
                    packageName = app.packageName,
                    nombre = app.nombre,
                    iconoBase64 = drawableABase64(app.icono),
                    actualizadoEn = fecha
                )
            }
        val contenido = apps.joinToString("\u0000") { "${it.packageName}\u0000${it.nombre}\u0000${it.iconoBase64}" }
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(contenido.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return apps to hash
    }

    fun drawableABase64(drawable: Drawable?, width: Int = 96, height: Int = 96): String {
        if (drawable == null) return ""
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val bytes = stream.toByteArray()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    fun base64ADrawable(context: Context, base64: String?): Drawable? {
        if (base64.isNullOrBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Devuelve un nombre legible para el usuario a partir de un packageName.
     * - Si hay un nombre guardado en Firestore/local, lo usa.
     * - Si el paquete está instalado, consulta el nombre real del sistema.
     * - Si no, intenta nombres conocidos de apps populares.
     * - Finalmente, devuelve el packageName original.
     */
    fun obtenerNombreApp(context: Context, packageName: String, nombreGuardado: String? = null): String {
        val guardado = nombreGuardado?.trim().orEmpty()
        if (guardado.isNotEmpty() && !guardado.startsWith("com.") && !guardado.startsWith("org.")) return guardado

        val limpio = packageName.trim().lowercase()
        nombresConocidos[limpio]?.let { return it }

        return try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(limpio, 0)
            pm.getApplicationLabel(info).toString().takeIf { it.isNotBlank() } ?: limpio
        } catch (e: Exception) {
            limpio
        }
    }
}
