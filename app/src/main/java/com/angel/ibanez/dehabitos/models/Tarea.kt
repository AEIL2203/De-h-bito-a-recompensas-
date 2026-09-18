package com.angel.ibanez.dehabitos.models

data class Tarea(
    var id: String = "",
    var ninoId: String = "",
    var tutorId: String = "",
    var nombre: String = "",
    var categoria: String = CATEGORIA_ACADEMICA,
    var puntos: Int = 10,
    var estado: String = ESTADO_PENDIENTE,
    var fechaAsignacion: String = "",
    var fechaCompletada: String = "",
    var fechaLimite: String = "",
    var esDiaria: Boolean = false,
    var evidencia: String = ""
) {
    companion object {
        const val ESTADO_PENDIENTE = "pendiente"
        const val ESTADO_COMPLETADA = "completada"
        const val ESTADO_VALIDADA = "validada"

        const val CATEGORIA_ACADEMICA = "academica"
        const val CATEGORIA_DOMESTICA = "domestica"
        const val CATEGORIA_PERSONAL = "personal"
    }
}
