package com.angel.ibanez.dehabitos.utils

import com.angel.ibanez.dehabitos.models.Tarea
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object MisionesDiariasUtils {

    data class MisionDiaria(val nombre: String, val categoria: String, val puntos: Int)

    val MISIONES = listOf(
        MisionDiaria("Leer 15 minutos", Tarea.CATEGORIA_ACADEMICA, 10),
        MisionDiaria("Tender la cama", Tarea.CATEGORIA_DOMESTICA, 5),
        MisionDiaria("Guardar tus juguetes", Tarea.CATEGORIA_DOMESTICA, 5),
        MisionDiaria("Repasar la tarea de la escuela", Tarea.CATEGORIA_ACADEMICA, 10),
        MisionDiaria("Ayudar a poner la mesa", Tarea.CATEGORIA_DOMESTICA, 5),
        MisionDiaria("Cepillarte los dientes antes de dormir", Tarea.CATEGORIA_PERSONAL, 5),
        MisionDiaria("Tomar 5 vasos de agua", Tarea.CATEGORIA_PERSONAL, 5),
        MisionDiaria("Practicar 10 minutos de lectura en voz alta", Tarea.CATEGORIA_ACADEMICA, 10),
        MisionDiaria("Ordenar tu mochila para mañana", Tarea.CATEGORIA_PERSONAL, 5),
        MisionDiaria("Jugar 20 minutos al aire libre", Tarea.CATEGORIA_PERSONAL, 5)
    )

    fun misionParaFecha(fecha: String): MisionDiaria {
        val diaDelAnio = runCatching {
            val fechaParseada = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(fecha)
            fechaParseada?.let {
                Calendar.getInstance().apply { time = it }.get(Calendar.DAY_OF_YEAR)
            } ?: 0
        }.getOrDefault(0)
        return MISIONES[diaDelAnio % MISIONES.size]
    }

    fun idMisionDiaria(ninoId: String, fecha: String): String = "diaria_${ninoId}_$fecha"
}
