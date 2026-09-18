package com.angel.ibanez.dehabitos.utils

import com.angel.ibanez.dehabitos.models.EncuestaSatisfaccion
import com.angel.ibanez.dehabitos.models.EstadisticasNino
import com.angel.ibanez.dehabitos.models.GrupoCanjes
import com.angel.ibanez.dehabitos.models.Nino
import com.angel.ibanez.dehabitos.models.Recompensa
import com.angel.ibanez.dehabitos.models.ResumenSemanal
import com.angel.ibanez.dehabitos.models.SesionApp
import com.angel.ibanez.dehabitos.models.Tarea
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object EstadisticasUtils {
    private const val FORMATO_FECHA_TAREA = "yyyy-MM-dd HH:mm:ss"
    private const val SEMANA_MS = 7 * 24 * 60 * 60 * 1000L

    fun agregar(
        nino: Nino,
        tareas: List<Tarea>,
        canjes: List<Recompensa>,
        sesiones: List<SesionApp>,
        semanas: List<ResumenSemanal> = emptyList(),
        encuestas: List<EncuestaSatisfaccion> = emptyList()
    ): EstadisticasNino {
        val validadas = tareas.filter { it.estado == Tarea.ESTADO_VALIDADA }
        val grupos = canjes.groupBy { it.packageName }.map { (paquete, lista) ->
            GrupoCanjes(paquete, lista.size, lista.sumOf { it.minutosOtorgados })
        }.sortedByDescending { it.minutos }
        return EstadisticasNino(
            nino = nino,
            nivelesAvanzados = (nino.nivel - 1).coerceAtLeast(0),
            tareasValidadas = validadas.size,
            tareasPorCategoria = validadas.groupingBy { it.categoria }.eachCount(),
            canjesTotales = canjes.size,
            canjesPorApp = grupos,
            estrellasGastadas = canjes.sumOf { it.estrellasGastadas },
            tiempoUsadoMs = sesiones.sumOf { it.duracionMs },
            sesionesRecientes = sesiones.sortedByDescending { it.inicio }.take(10),
            semanas = semanas,
            encuestas = encuestas.size,
            satisfaccionPromedio = if (encuestas.isEmpty()) 0.0 else encuestas.map { it.promedio() }.average()
        )
    }

    fun fechaMillis(valor: String): Long = runCatching {
        if (valor.isBlank()) 0L else SimpleDateFormat(FORMATO_FECHA_TAREA, Locale.US).parse(valor)?.time ?: 0L
    }.getOrDefault(0L)

    fun inicioSemana(millis: Long): Long {
        val calendario = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diasDesdeLunes = (calendario.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
        calendario.add(Calendar.DAY_OF_MONTH, -diasDesdeLunes)
        return calendario.timeInMillis
    }

    fun resumenSemanal(
        tareas: List<Tarea>,
        canjes: List<Recompensa>,
        sesiones: List<SesionApp>,
        semanas: Int = 4,
        ahora: Long = System.currentTimeMillis()
    ): List<ResumenSemanal> {
        val inicioActual = inicioSemana(ahora)
        return (semanas - 1 downTo 0).map { desplazamiento ->
            val inicio = inicioActual - desplazamiento * SEMANA_MS
            val fin = inicio + SEMANA_MS
            fun enSemana(millis: Long) = millis in inicio until fin
            val validadas = tareas.filter { it.estado == Tarea.ESTADO_VALIDADA && enSemana(fechaMillis(it.fechaCompletada)) }
            val canjesSemana = canjes.filter { enSemana(it.fechaCanje) }
            ResumenSemanal(
                inicio = inicio,
                fin = fin,
                tareasValidadas = validadas.size,
                tiempoUsadoMs = sesiones.filter { enSemana(it.inicio) }.sumOf { it.duracionMs },
                canjes = canjesSemana.size,
                estrellasGastadas = canjesSemana.sumOf { it.estrellasGastadas }
            )
        }
    }

    fun variacionPorcentual(actual: Long, anterior: Long): Int? =
        if (anterior == 0L) null else ((actual - anterior) * 100 / anterior).toInt()

    fun generarCsv(nombreNino: String, semanas: List<ResumenSemanal>): String {
        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val filas = semanas.map { semana ->
            val horas = String.format(Locale.US, "%.2f", semana.tiempoUsadoMs / 3_600_000.0)
            listOf(
                nombreNino,
                formato.format(semana.inicio),
                formato.format(semana.fin - 24 * 60 * 60 * 1000L),
                semana.tareasValidadas,
                horas,
                semana.canjes,
                semana.estrellasGastadas
            ).joinToString(",")
        }
        return (listOf("nino,semana_inicio,semana_fin,tareas_validadas,horas_uso,canjes,estrellas_gastadas") + filas).joinToString("\n")
    }
}
