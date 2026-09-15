package com.angel.ibanez.dehabitos.models

data class TokenFcm(
    var token: String = "",
    var tutorId: String = "",
    var actualizadoEn: Long = 0,
    var plataforma: String = "android"
)
