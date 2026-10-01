package com.angel.ibanez.dehabitos.utils

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate

/**
 * Preferencia local de tema de la app (no remota, por dispositivo).
 * Valores: "claro", "oscuro" o "sistema" (por defecto).
 */
object TemaConfig {
    private const val PREFS_NAME = "tema_config"
    private const val KEY_TEMA = "modo_tema"

    const val CLARO = "claro"
    const val OSCURO = "oscuro"
    const val SISTEMA = "sistema"

    fun guardar(context: Context, modo: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_TEMA, modo).apply()
    }

    fun obtener(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TEMA, SISTEMA) ?: SISTEMA

    /** Aplica el modo guardado. Llamar antes de setContentView. */
    fun aplicar(context: Context) {
        AppCompatDelegate.setDefaultNightMode(
            when (obtener(context)) {
                CLARO -> AppCompatDelegate.MODE_NIGHT_NO
                OSCURO -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }

    /** Guarda el modo y lo aplica inmediatamente (recrea las actividades). */
    fun cambiar(context: Context, oscuro: Boolean) {
        guardar(context, if (oscuro) OSCURO else CLARO)
        AppCompatDelegate.setDefaultNightMode(
            if (oscuro) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    /** Indica si el modo efectivo actual (UI) es oscuro, sin importar la preferencia. */
    fun esOscuroActual(context: Context): Boolean =
        context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}
