package com.angel.ibanez.dehabitos

import com.angel.ibanez.dehabitos.models.Recompensa
import com.angel.ibanez.dehabitos.models.ResumenSemanal
import com.angel.ibanez.dehabitos.models.SesionApp
import com.angel.ibanez.dehabitos.models.Tarea
import com.angel.ibanez.dehabitos.utils.EstadisticasUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EstadisticasSemanalesTest {
    private val dia = 24 * 60 * 60 * 1000L
    private val ahora: Long = Calendar.getInstance().apply {
        set(2026, Calendar.SEPTEMBER, 16, 10, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun fechaTarea(millis: Long) = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(millis)

    @Test
    fun inicioSemanaEsElLunesALasCero() {
        val calendario = Calendar.getInstance().apply { timeInMillis = EstadisticasUtils.inicioSemana(ahora) }

        assertEquals(Calendar.MONDAY, calendario.get(Calendar.DAY_OF_WEEK))
        assertEquals(0, calendario.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calendario.get(Calendar.MINUTE))
        assertTrue(calendario.timeInMillis <= ahora)
    }

    @Test
    fun resumenDevuelveSemanasOrdenadasYLaUltimaContieneAhora() {
        val semanas = EstadisticasUtils.resumenSemanal(emptyList(), emptyList(), emptyList(), ahora = ahora)

        assertEquals(4, semanas.size)
        assertTrue(semanas.zipWithNext().all { (a, b) -> a.inicio < b.inicio && a.fin == b.inicio })
        assertTrue(ahora in semanas.last().inicio until semanas.last().fin)
    }

    @Test
    fun tareasSeCuentanSoloSiEstanValidadasYEnSuSemana() {
        val tareas = listOf(
            Tarea(estado = Tarea.ESTADO_VALIDADA, fechaCompletada = fechaTarea(ahora)),
            Tarea(estado = Tarea.ESTADO_PENDIENTE, fechaCompletada = fechaTarea(ahora)),
            Tarea(estado = Tarea.ESTADO_VALIDADA, fechaCompletada = fechaTarea(ahora - 8 * dia))
        )

        val semanas = EstadisticasUtils.resumenSemanal(tareas, emptyList(), emptyList(), ahora = ahora)

        assertEquals(1, semanas.last().tareasValidadas)
        assertEquals(1, semanas[semanas.size - 2].tareasValidadas)
        assertEquals(0, semanas.first().tareasValidadas)
    }

    @Test
    fun sesionesYCanjesSeSumanEnLaSemanaCorrecta() {
        val sesiones = listOf(SesionApp(inicio = ahora, duracionMs = 90 * 60_000L))
        val canjes = listOf(Recompensa(fechaCanje = ahora - 8 * dia, estrellasGastadas = 4))

        val semanas = EstadisticasUtils.resumenSemanal(emptyList(), canjes, sesiones, ahora = ahora)

        assertEquals(5_400_000L, semanas.last().tiempoUsadoMs)
        assertEquals(0, semanas.last().canjes)
        assertEquals(1, semanas[semanas.size - 2].canjes)
        assertEquals(4, semanas[semanas.size - 2].estrellasGastadas)
    }

    @Test
    fun variacionPorcentualManejaCasosLimite() {
        assertEquals(50, EstadisticasUtils.variacionPorcentual(150, 100))
        assertEquals(-50, EstadisticasUtils.variacionPorcentual(50, 100))
        assertNull(EstadisticasUtils.variacionPorcentual(10, 0))
    }

    @Test
    fun csvTieneCabeceraYUnaFilaPorSemana() {
        val semanas = EstadisticasUtils.resumenSemanal(emptyList(), emptyList(), emptyList(), ahora = ahora)
        val lineas = EstadisticasUtils.generarCsv("Emely", semanas).lines()

        assertEquals(5, lineas.size)
        assertEquals("nino,semana_inicio,semana_fin,tareas_validadas,horas_uso,canjes,estrellas_gastadas", lineas.first())
        assertTrue(lineas[1].startsWith("Emely,"))
    }

    @Test
    fun fechaMillisDevuelveCeroSiNoSePuedeParsear() {
        assertEquals(0L, EstadisticasUtils.fechaMillis(""))
        assertEquals(0L, EstadisticasUtils.fechaMillis("fecha inválida"))
        assertEquals(ahora, EstadisticasUtils.fechaMillis(fechaTarea(ahora)))
    }

    @Test
    fun resumenSemanalEsCompatibleConElModelo() {
        val semana = ResumenSemanal(0, dia * 7, 2, 0, 1, 3)
        assertEquals(2, semana.tareasValidadas)
    }
}
