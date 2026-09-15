package com.angel.ibanez.dehabitos.utils

import com.angel.ibanez.dehabitos.models.EstadisticasNino
import com.angel.ibanez.dehabitos.models.GrupoCanjes
import com.angel.ibanez.dehabitos.models.Nino
import com.angel.ibanez.dehabitos.models.Recompensa
import com.angel.ibanez.dehabitos.models.SesionApp
import com.angel.ibanez.dehabitos.models.Tarea

object EstadisticasUtils {
    fun agregar(nino: Nino, tareas: List<Tarea>, canjes: List<Recompensa>, sesiones: List<SesionApp>): EstadisticasNino {
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
            sesionesRecientes = sesiones.sortedByDescending { it.inicio }.take(10)
        )
    }
}
