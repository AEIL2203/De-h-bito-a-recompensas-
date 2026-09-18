package com.angel.ibanez.dehabitos

import com.angel.ibanez.dehabitos.models.Tarea
import com.angel.ibanez.dehabitos.utils.MisionesDiariasUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MisionesDiariasUtilsTest {

    @Test
    fun misionParaFechaEsDeterminista() {
        assertEquals(
            MisionesDiariasUtils.misionParaFecha("2026-09-16"),
            MisionesDiariasUtils.misionParaFecha("2026-09-16")
        )
    }

    @Test
    fun fechasConsecutivasRecorrenLaLista() {
        val misiones = (1..MisionesDiariasUtils.MISIONES.size + 1).map { dia ->
            MisionesDiariasUtils.misionParaFecha("2026-01-%02d".format(dia))
        }
        assertEquals(misiones.first(), misiones.last())
        assertEquals(MisionesDiariasUtils.MISIONES.size, misiones.dropLast(1).toSet().size)
    }

    @Test
    fun diasDistintosPuedenDarMisionesDistintas() {
        assertNotEquals(
            MisionesDiariasUtils.misionParaFecha("2026-01-01"),
            MisionesDiariasUtils.misionParaFecha("2026-01-02")
        )
    }

    @Test
    fun idMisionDiariaTieneFormatoCorrecto() {
        assertEquals("diaria_nino1_2026-09-16", MisionesDiariasUtils.idMisionDiaria("nino1", "2026-09-16"))
    }

    @Test
    fun todasLasMisionesTienenPuntosPositivosYCategoriaValida() {
        val categorias = setOf(Tarea.CATEGORIA_ACADEMICA, Tarea.CATEGORIA_DOMESTICA, Tarea.CATEGORIA_PERSONAL)
        MisionesDiariasUtils.MISIONES.forEach { mision ->
            assertTrue(mision.puntos > 0)
            assertTrue(mision.categoria in categorias)
            assertTrue(mision.nombre.isNotBlank())
        }
    }
}
