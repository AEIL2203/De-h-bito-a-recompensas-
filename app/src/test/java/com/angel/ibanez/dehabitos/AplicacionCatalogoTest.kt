package com.angel.ibanez.dehabitos

import com.angel.ibanez.dehabitos.models.AplicacionCatalogo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AplicacionCatalogoTest {
    @Test
    fun crearIdEsDeterministaYValidoParaFirestore() {
        val id = AplicacionCatalogo.crearId("nino/1", "device/1", "com.example/app")

        assertEquals(id, AplicacionCatalogo.crearId("nino/1", "device/1", "com.example/app"))
        assertEquals(64, id.length)
        assertTrue(id.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun crearIdDistingueDispositivoYPaquete() {
        val base = AplicacionCatalogo.crearId("nino", "device-1", "com.example.one")

        assertNotEquals(base, AplicacionCatalogo.crearId("nino", "device-2", "com.example.one"))
        assertNotEquals(base, AplicacionCatalogo.crearId("nino", "device-1", "com.example.two"))
    }
}
