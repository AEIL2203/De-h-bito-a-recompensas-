package com.angel.ibanez.dehabitos

import com.angel.ibanez.dehabitos.models.Recompensa
import com.angel.ibanez.dehabitos.utils.ConsumoRecompensas
import com.angel.ibanez.dehabitos.utils.ProgresionUtils
import com.angel.ibanez.dehabitos.utils.EstadisticasUtils
import com.angel.ibanez.dehabitos.models.Nino
import com.angel.ibanez.dehabitos.models.Tarea
import com.angel.ibanez.dehabitos.models.SesionApp
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun estrellasSeOtorganPorCadaNivelAlcanzado() {
        assertEquals(0, ProgresionUtils.estrellasPorNiveles(2, 2))
        assertEquals(3, ProgresionUtils.estrellasPorNiveles(2, 3))
        assertEquals(9, ProgresionUtils.estrellasPorNiveles(2, 5))
    }

    @Test
    fun nivelCambiaExactamenteAlAlcanzarLaMetaDeXp() {
        assertEquals(1, ProgresionUtils.nivelDesdeXp(99))
        assertEquals(2, ProgresionUtils.nivelDesdeXp(100))
        assertEquals(2, ProgresionUtils.nivelDesdeXp(299))
        assertEquals(3, ProgresionUtils.nivelDesdeXp(300))
    }

    @Test
    fun compensaEstrellasDeNivelesAnterioresSinDuplicarlas() {
        assertEquals(9, ProgresionUtils.compensacionEstrellas(4, 0))
        assertEquals(6, ProgresionUtils.compensacionEstrellas(4, 3))
        assertEquals(0, ProgresionUtils.compensacionEstrellas(4, 9))
    }

    @Test
    fun consumoUsaLasRecompensasEnOrden() {
        val primera = Recompensa(id = "primera", minutosOtorgados = 1, fechaCanje = 10, milisegundosConsumidos = 50_000)
        val segunda = Recompensa(id = "segunda", minutosOtorgados = 1, fechaCanje = 20)

        assertEquals(
            linkedMapOf("primera" to 10_000L, "segunda" to 15_000L),
            ConsumoRecompensas.asignar(listOf(segunda, primera), 25_000)
        )
    }

    @Test
    fun estadisticasAgrupanTareasCanjesYSesiones() {
        val nino = Nino(nivel = 4)
        val tareas = listOf(Tarea(categoria = Tarea.CATEGORIA_ACADEMICA, estado = Tarea.ESTADO_VALIDADA), Tarea(categoria = Tarea.CATEGORIA_DOMESTICA, estado = Tarea.ESTADO_PENDIENTE))
        val canjes = listOf(Recompensa(packageName = "app.a", minutosOtorgados = 15, estrellasGastadas = 2), Recompensa(packageName = "app.a", minutosOtorgados = 30, estrellasGastadas = 4))
        val sesiones = listOf(SesionApp(inicio = 2, duracionMs = 60_000), SesionApp(inicio = 1, duracionMs = 120_000))

        val resultado = EstadisticasUtils.agregar(nino, tareas, canjes, sesiones)

        assertEquals(3, resultado.nivelesAvanzados)
        assertEquals(1, resultado.tareasValidadas)
        assertEquals(1, resultado.tareasPorCategoria[Tarea.CATEGORIA_ACADEMICA])
        assertEquals(2, resultado.canjesTotales)
        assertEquals(45, resultado.canjesPorApp.single().minutos)
        assertEquals(6, resultado.estrellasGastadas)
        assertEquals(180_000, resultado.tiempoUsadoMs)
        assertEquals(2, resultado.sesionesRecientes.first().inicio)
    }

    @Test
    fun consumoNoSuperaElTiempoDisponible() {
        val recompensa = Recompensa(id = "unica", minutosOtorgados = 1, milisegundosConsumidos = 55_000)

        assertEquals(mapOf("unica" to 5_000L), ConsumoRecompensas.asignar(listOf(recompensa), 20_000))
        assertEquals(5_000L, ConsumoRecompensas.disponibles(listOf(recompensa)))
    }
}
