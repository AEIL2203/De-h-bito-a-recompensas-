package com.angel.ibanez.dehabitos

import com.angel.ibanez.dehabitos.utils.FechasUtils
import com.angel.ibanez.dehabitos.utils.FechasUtils.EstadoVencimiento
import org.junit.Assert.assertEquals
import org.junit.Test

class FechasUtilsTest {

    private val hoy = "2026-09-16"

    @Test
    fun sinLimiteCuandoFechaVacia() {
        assertEquals(EstadoVencimiento.SIN_LIMITE, FechasUtils.estadoVencimiento("", hoy))
    }

    @Test
    fun fechaFuturaEsATiempo() {
        assertEquals(EstadoVencimiento.A_TIEMPO, FechasUtils.estadoVencimiento("2026-09-20", hoy))
    }

    @Test
    fun fechaIgualAHoyVenceHoy() {
        assertEquals(EstadoVencimiento.VENCE_HOY, FechasUtils.estadoVencimiento(hoy, hoy))
    }

    @Test
    fun fechaAnteriorEstaVencida() {
        assertEquals(EstadoVencimiento.VENCIDA, FechasUtils.estadoVencimiento("2026-09-10", hoy))
    }

    @Test
    fun formatearFechaCortaDevuelveDiaMes() {
        assertEquals("16/09", FechasUtils.formatearFechaCorta("2026-09-16"))
    }
}
