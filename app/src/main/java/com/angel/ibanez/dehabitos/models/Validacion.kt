package com.angel.ibanez.dehabitos.models

/** Resultado de validar una tarea: el niño con su progreso actualizado y el nombre de la tarea aprobada. */
data class Validacion(
    val nino: Nino,
    val nombreTarea: String
)
