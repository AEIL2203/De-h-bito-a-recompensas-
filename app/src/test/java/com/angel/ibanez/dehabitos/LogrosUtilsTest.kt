package com.angel.ibanez.dehabitos

import com.angel.ibanez.dehabitos.models.Nino
import com.angel.ibanez.dehabitos.utils.LogrosUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogrosUtilsTest {
    private fun ids(nino: Nino) = LogrosUtils.logrosDesbloqueados(nino).map { it.id }.toSet()

    @Test fun primeraTareaSeDesbloqueaConUnaValidada() {
        assertFalse("primera_tarea" in ids(Nino()))
        assertTrue("primera_tarea" in ids(Nino(tareasAcademicas = 1)))
    }

    @Test fun nivelXpYRachaRespetanUmbrales() {
        val antes = ids(Nino(nivel = 4, experiencia = 99, racha = 2))
        assertFalse("nivel_5" in antes)
        assertFalse("xp_100" in antes)
        assertFalse("racha_3" in antes)
        val justo = ids(Nino(nivel = 5, experiencia = 100, racha = 3))
        assertTrue("nivel_5" in justo)
        assertTrue("xp_100" in justo)
        assertTrue("racha_3" in justo)
    }

    @Test fun progresoReflejaValorActualYObjetivo() {
        val contexto = LogrosUtils.Contexto(Nino(tareasAcademicas = 3, experiencia = 80))
        val cincoTareas = LogrosUtils.CATALOGO.first { it.id == "cinco_tareas" }
        val xp100 = LogrosUtils.CATALOGO.first { it.id == "xp_100" }
        assertEquals(5, cincoTareas.objetivo)
        assertEquals(3, cincoTareas.progresoActual(contexto))
        assertEquals(80, xp100.progresoActual(contexto))
    }

    @Test fun desbloqueosCrecenMonotonicamente() {
        val progresos = listOf(
            Nino(),
            Nino(tareasAcademicas = 1, experiencia = 100),
            Nino(tareasAcademicas = 5, experiencia = 500, nivel = 5, racha = 3),
            Nino(tareasAcademicas = 10, experiencia = 1_000, nivel = 10, racha = 7, minutosExtra = 10)
        ).map(::ids)
        progresos.zipWithNext().forEach { (anterior, siguiente) -> assertTrue(siguiente.containsAll(anterior)) }
    }
}
