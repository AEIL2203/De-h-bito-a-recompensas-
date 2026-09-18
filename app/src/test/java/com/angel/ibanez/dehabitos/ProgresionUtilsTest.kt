package com.angel.ibanez.dehabitos

import com.angel.ibanez.dehabitos.models.Nino
import com.angel.ibanez.dehabitos.utils.ProgresionUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgresionUtilsTest {
    @Test
    fun xpAcumuladaSigueLaFormulaProgresiva() {
        assertEquals(0, ProgresionUtils.xpAcumuladaParaNivel(1))
        assertEquals(100, ProgresionUtils.xpAcumuladaParaNivel(2))
        assertEquals(1000, ProgresionUtils.xpAcumuladaParaNivel(5))
        assertEquals(19000, ProgresionUtils.xpAcumuladaParaNivel(20))
        assertEquals(19000, ProgresionUtils.xpAcumuladaParaNivel(25))
    }

    @Test
    fun nivelNuncaSuperaElMaximo() {
        assertEquals(ProgresionUtils.NIVEL_MAXIMO, ProgresionUtils.nivelDesdeXp(1_000_000))
    }

    @Test
    fun porcentajeDeProgresoDentroDelNivel() {
        assertEquals(0, ProgresionUtils.porcentajeProgreso(0))
        assertEquals(50, ProgresionUtils.porcentajeProgreso(50))
        assertEquals(0, ProgresionUtils.porcentajeProgreso(100))
        assertEquals(75, ProgresionUtils.porcentajeProgreso(250))
    }

    @Test
    fun insigniasSeOtorganSegunTareasRachaYNivel() {
        val insignias = ProgresionUtils.calcularInsignias(Nino(tareasAcademicas = 3, tareasDomesticas = 2, racha = 3, nivel = 5))

        assertTrue(insignias.containsAll(listOf("primera_tarea", "cinco_tareas", "racha_3", "nivel_5")))
        assertFalse(insignias.contains("veinte_tareas"))
        assertFalse(insignias.contains("racha_7"))
        assertFalse(insignias.contains("nivel_10"))
    }

    @Test
    fun insigniasPreviasNoSePierden() {
        assertTrue(ProgresionUtils.calcularInsignias(Nino(insignias = listOf("racha_7"), racha = 0)).contains("racha_7"))
    }

    @Test
    fun estrellasPorNivelesNoSonNegativas() {
        assertEquals(0, ProgresionUtils.estrellasPorNiveles(5, 3))
    }
}
