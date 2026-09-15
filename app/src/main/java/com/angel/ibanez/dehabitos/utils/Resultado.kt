package com.angel.ibanez.dehabitos.utils

sealed class Resultado<out T> {
    object Cargando : Resultado<Nothing>()
    data class Exito<T>(val data: T) : Resultado<T>()
    data class Error(val mensaje: String) : Resultado<Nothing>()
}
