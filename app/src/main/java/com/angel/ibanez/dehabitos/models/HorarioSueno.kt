package com.angel.ibanez.dehabitos.models

import java.util.Calendar

data class HorarioSueno(
    var horaDormir: String = "21:00",
    var horaDespertar: String = "07:00",
    var diasActivos: List<Int> = listOf(
        Calendar.MONDAY,
        Calendar.TUESDAY,
        Calendar.WEDNESDAY,
        Calendar.THURSDAY,
        Calendar.FRIDAY,
        Calendar.SATURDAY,
        Calendar.SUNDAY
    )
)
