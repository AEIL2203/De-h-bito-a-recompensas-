package com.angel.ibanez.dehabitos.utils

import com.angel.ibanez.dehabitos.models.HorarioSueno
import java.util.Calendar

object HorarioSuenoUtils {

    fun estaEnHorarioSueno(horario: HorarioSueno?): Boolean {
        if (horario == null) return false
        val cal = Calendar.getInstance()
        val hoy = cal.get(Calendar.DAY_OF_WEEK)
        val manana = ((hoy - Calendar.SUNDAY + 1) % 7) + Calendar.SUNDAY
        val minutosActual = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val dormir = parseMinutos(horario.horaDormir) ?: return false
        val despertar = parseMinutos(horario.horaDespertar) ?: return false
        val activoHoy = horario.diasActivos.contains(hoy)
        val activoManana = horario.diasActivos.contains(manana)

        return if (dormir < despertar) {
            activoHoy && minutosActual in dormir until despertar
        } else {
            (activoHoy && minutosActual >= dormir) || (activoManana && minutosActual < despertar)
        }
    }

    fun formatearHorario(minutos: Int): String {
        val h = minutos / 60
        val m = minutos % 60
        return "%02d:%02d".format(h, m)
    }

    private fun parseMinutos(hora: String): Int? {
        val partes = hora.split(":")
        if (partes.size != 2) return null
        val h = partes[0].toIntOrNull() ?: return null
        val m = partes[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
    }
}
