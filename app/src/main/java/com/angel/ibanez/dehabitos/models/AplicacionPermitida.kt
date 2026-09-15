package com.angel.ibanez.dehabitos.models

data class AplicacionPermitida(
    var id: String = "",
    var ninoId: String = "",
    var tutorId: String = "",
    var packageName: String = "",
    var nombre: String = "",
    var iconoBase64: String = "",
    var precio15min: Int = 0,
    var precio30min: Int = 0,
    var precio60min: Int = 0
)
