package com.angel.ibanez.dehabitos.utils

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.app.NotificationManagerCompat
import com.angel.ibanez.dehabitos.services.AppMonitorService

object PermisosUtils {

    fun tieneAccesoUso(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        @Suppress("DEPRECATION")
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun servicioAccesibilidadActivo(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        val serviceName = "${context.packageName}/${AppMonitorService::class.java.canonicalName}"
        return enabledServices?.contains(serviceName) == true && am.isEnabled
    }

    fun abrirConfiguracionUso(context: Context) {
        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun notificacionesActivas(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun abrirConfiguracionNotificaciones(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun abrirConfiguracionAccesibilidad(context: Context): Boolean {
        val componente = ComponentName(context, AppMonitorService::class.java)
        val detalle = Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS").apply {
            putExtra(Intent.EXTRA_COMPONENT_NAME, componente)
            putExtra(":settings:fragment_args_key", componente.flattenToString())
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val general = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(if (detalle.resolveActivity(context.packageManager) != null) detalle else general)
            true
        }.getOrElse {
            runCatching { context.startActivity(general) }.isSuccess
        }
    }
}
