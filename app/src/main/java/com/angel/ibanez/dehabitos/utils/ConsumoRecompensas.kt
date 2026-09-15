package com.angel.ibanez.dehabitos.utils

import com.angel.ibanez.dehabitos.models.Recompensa

object ConsumoRecompensas {
    fun asignar(recompensas: List<Recompensa>, milisegundos: Long): Map<String, Long> {
        var restante = milisegundos.coerceAtLeast(0)
        val consumos = linkedMapOf<String, Long>()
        recompensas.sortedWith(compareBy<Recompensa> { it.fechaCanje }.thenBy { it.id }).forEach { recompensa ->
            if (restante == 0L) return@forEach
            val consumo = restante.coerceAtMost(recompensa.milisegundosRestantes)
            if (consumo > 0) {
                consumos[recompensa.id] = consumo
                restante -= consumo
            }
        }
        return consumos
    }

    fun disponibles(recompensas: List<Recompensa>): Long = recompensas.sumOf { it.milisegundosRestantes }
}
