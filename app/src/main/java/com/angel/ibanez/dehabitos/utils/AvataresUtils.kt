package com.angel.ibanez.dehabitos.utils

import android.widget.ImageView
import com.bumptech.glide.Glide

object AvataresUtils {
    data class Avatar(val id: String, val nombre: String, val archivo: String)

    const val DEFAULT = "cat"

    val lista = listOf(
        Avatar("cat", "Gato", "avatar--cat-100.png"),
        Avatar("dog", "Perro", "avatar--dog-100.png"),
        Avatar("panda", "Panda", "avatar--panda-100.png"),
        Avatar("rabbit", "Conejo", "avatar--rabbit-100.png"),
        Avatar("pig", "Cerdito", "avatar--pig-100.png"),
        Avatar("chicken", "Pollito", "avatar--chicken-100.png"),
        Avatar("penguin", "Pingüino", "avatar--christmas-penguin-100.png"),
        Avatar("octopus", "Pulpo", "avatar--octopus-100.png"),
        Avatar("star", "Estrella", "avatar--falling-star-100.png"),
        Avatar("sun", "Sol", "avatar--sun-100.png"),
        Avatar("pizza", "Pizza", "avatar--pizza-100.png")
    )

    fun obtener(id: String): Avatar = lista.firstOrNull { it.id == id } ?: lista.first { it.id == DEFAULT }

    fun cargar(imageView: ImageView, id: String) {
        Glide.with(imageView)
            .load("file:///android_asset/${obtener(id).archivo}")
            .into(imageView)
    }
}
