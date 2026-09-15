package com.angel.ibanez.dehabitos.models

data class Dispositivo(
    var id: String = "",
    var tutorId: String = "",
    var ninoId: String = "",
    var nombreNino: String = "",
    var desbloqueado: Boolean = false,
    var fechaVinculacion: String = ""
)
