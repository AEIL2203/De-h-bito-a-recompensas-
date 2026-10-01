package com.angel.ibanez.dehabitos.utils

import android.content.Context

/**
 * Preferencia local para activar/desactivar la música de fondo.
 */
object AudioConfig {
    private const val PREFS_NAME = "audio_config"
    private const val KEY_MUSICA = "musica_habilitada"

    fun habilitarMusica(context: Context, habilitar: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_MUSICA, habilitar).apply()
    }

    fun musicaHabilitada(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_MUSICA, true)
}
