package com.angel.ibanez.dehabitos.utils

import android.content.Context
import android.media.MediaPlayer
import com.angel.ibanez.dehabitos.R

/**
 * Reproduce el tema musical de fondo en modo niño en bucle.
 * Se prepara con el contexto de la aplicación para evitar fugas de memoria.
 */
object MusicaFondoManager {

    private var mediaPlayer: MediaPlayer? = null

    fun preparar(context: Context) {
        if (mediaPlayer != null) return
        val mp = MediaPlayer.create(context.applicationContext, R.raw.thema)
        mp?.let {
            it.isLooping = true
            it.setVolume(0.5f, 0.5f)
            mediaPlayer = it
        }
    }

    fun reproducir() {
        mediaPlayer?.takeIf { !it.isPlaying }?.start()
    }

    fun pausar() {
        mediaPlayer?.takeIf { it.isPlaying }?.pause()
    }

    fun detener() {
        mediaPlayer?.takeIf { it.isPlaying }?.apply {
            pause()
            seekTo(0)
        }
    }

    fun liberar() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun estaReproduciendo(): Boolean = mediaPlayer?.isPlaying ?: false
}
