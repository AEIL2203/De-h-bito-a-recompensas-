package com.angel.ibanez.dehabitos.models

data class SesionApp(
    var id: String = "",
    var ninoId: String = "",
    var tutorId: String = "",
    var packageName: String = "",
    var nombre: String = "",
    var inicio: Long = 0,
    var fin: Long? = null,
    var duracionMs: Long = 0,
    var estado: String = ESTADO_INICIADA
) {
    companion object {
        const val ESTADO_INICIADA = "iniciada"
        const val ESTADO_FINALIZADA = "finalizada"
    }
}
