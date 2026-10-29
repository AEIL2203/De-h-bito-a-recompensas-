package com.angel.ibanez.dehabitos.utils

import com.angel.ibanez.dehabitos.models.Nino
import com.angel.ibanez.dehabitos.models.Tarea

/**
 * Reglas de progresión del niño.
 * 4 mundos de 15 niveles. Subir un nivel cuesta una cantidad fija según el mundo:
 * mundo 1 = 100 XP, mundo 2 = 125 XP, mundo 3 = 130 XP, mundo 4 = 145 XP.
 */
object ProgresionUtils {

    const val NIVELES_POR_MUNDO = 15
    const val NIVEL_MAXIMO = 60
    private val XP_POR_NIVEL_SEGUN_MUNDO = intArrayOf(100, 125, 130, 145)

    /** XP acumulada necesaria para alcanzar [nivel]. */
    fun xpAcumuladaParaNivel(nivel: Int): Int {
        val n = nivel.coerceIn(1, NIVEL_MAXIMO)
        return (1 until n).sumOf { xpCostoDelNivel(it) }
    }

    /** XP que cuesta pasar de [nivel] al siguiente. */
    fun xpCostoDelNivel(nivel: Int): Int {
        val mundo = ((nivel.coerceIn(1, NIVEL_MAXIMO) - 1) / NIVELES_POR_MUNDO)
        return XP_POR_NIVEL_SEGUN_MUNDO[mundo.coerceIn(0, XP_POR_NIVEL_SEGUN_MUNDO.lastIndex)]
    }

    fun nivelDesdeXp(xp: Int): Int {
        var nivel = 1
        while (nivel < NIVEL_MAXIMO && xp >= xpAcumuladaParaNivel(nivel + 1)) {
            nivel++
        }
        return nivel
    }

    fun estrellasPorNiveles(nivelAnterior: Int, nivelNuevo: Int): Int =
        (nivelNuevo - nivelAnterior).coerceAtLeast(0) * 3

    fun compensacionEstrellas(nivel: Int, estrellasActuales: Int): Int =
        (((nivel - 1).coerceAtLeast(0) * 3) - estrellasActuales).coerceAtLeast(0)

    /** XP ganada dentro del nivel actual. */
    fun xpEnNivelActual(xp: Int): Int = xp - xpAcumuladaParaNivel(nivelDesdeXp(xp))

    /** XP total que requiere el nivel actual para pasar al siguiente. */
    fun xpMetaNivelActual(xp: Int): Int = xpCostoDelNivel(nivelDesdeXp(xp))

    /** Progreso 0-100 hacia el siguiente nivel. */
    fun porcentajeProgreso(xp: Int): Int {
        val meta = xpMetaNivelActual(xp)
        if (meta == 0) return 100
        return (xpEnNivelActual(xp) * 100 / meta).coerceIn(0, 100)
    }

    // ---------- Insignias ----------

    data class Insignia(val id: String, val nombre: String, val icono: String)

    val CATALOGO_INSIGNIAS = listOf(
        Insignia("primera_tarea", "Primera misión", "gmr-thumb-up"),
        Insignia("cinco_tareas", "5 misiones", "gmr-military-tech"),
        Insignia("veinte_tareas", "20 misiones", "gmr-star"),
        Insignia("racha_3", "Racha de 3 días", "gmr-local-fire-department"),
        Insignia("racha_7", "Racha de 7 días", "gmr-flash-on"),
        Insignia("nivel_5", "Nivel 5", "gmr-grade"),
        Insignia("nivel_10", "Nivel 10", "gmr-stars")
    )

    fun calcularInsignias(nino: Nino): List<String> {
        val totalTareas = nino.tareasAcademicas + nino.tareasDomesticas + nino.tareasPersonales
        val ganadas = mutableSetOf<String>()
        ganadas.addAll(nino.insignias)

        if (totalTareas >= 1) ganadas.add("primera_tarea")
        if (totalTareas >= 5) ganadas.add("cinco_tareas")
        if (totalTareas >= 20) ganadas.add("veinte_tareas")
        if (nino.racha >= 3) ganadas.add("racha_3")
        if (nino.racha >= 7) ganadas.add("racha_7")
        if (nino.nivel >= 5) ganadas.add("nivel_5")
        if (nino.nivel >= 10) ganadas.add("nivel_10")

        return ganadas.toList()
    }

    // ---------- Categorías ----------

    fun iconoCategoria(categoria: String): String = when (categoria) {
        Tarea.CATEGORIA_ACADEMICA -> "gmr-school"
        Tarea.CATEGORIA_DOMESTICA -> "gmr-home"
        else -> "gmr-self-improvement"
    }

    fun nombreCategoria(categoria: String): String = when (categoria) {
        Tarea.CATEGORIA_ACADEMICA -> "Académica"
        Tarea.CATEGORIA_DOMESTICA -> "Doméstica"
        else -> "Personal"
    }
}
