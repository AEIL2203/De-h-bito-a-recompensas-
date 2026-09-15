package com.angel.ibanez.dehabitos.models

data class Nino(
    var id: String = "",
    var nombre: String = "",
    var edad: Int = 0,
    var pin: String = "",
    var tutorId: String = "",
    var avatar: String = "",
    var nivel: Int = 1,
    var experiencia: Int = 0,
    var racha: Int = 0,
    var ultimoDiaCompletado: String = "",
    var insignias: List<String> = emptyList(),
    var tareasAcademicas: Int = 0,
    var tareasDomesticas: Int = 0,
    var tareasPersonales: Int = 0,
    var estrellas: Int = 0,
    var compensacionEstrellasAplicada: Boolean = false,
    var gemas: Int = 0,
    var minutosExtra: Int = 0
)
