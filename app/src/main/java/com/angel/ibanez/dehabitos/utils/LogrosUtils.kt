package com.angel.ibanez.dehabitos.utils

import com.angel.ibanez.dehabitos.R
import com.angel.ibanez.dehabitos.models.Nino

/** Catálogo visual de hitos, basado únicamente en progreso persistido. */
object LogrosUtils {
    data class Contexto(
        val nino: Nino,
        val totalTareasValidadas: Int = nino.tareasAcademicas + nino.tareasDomesticas + nino.tareasPersonales,
        val totalCanjes: Int = 0,
        val misionDiariaCompletada: Boolean = false
    )

    data class Logro(
        val id: String,
        val nombre: String,
        val descripcion: String,
        val iconoGmr: String,
        val colorRes: Int,
        val objetivo: Int,
        val progreso: (Contexto) -> Int
    ) {
        fun progresoActual(contexto: Contexto): Int = progreso(contexto).coerceAtLeast(0)
        fun cumplido(contexto: Contexto): Boolean = progresoActual(contexto) >= objetivo
    }

    private fun progresoPersistido(contexto: Contexto, id: String, valor: Int, objetivo: Int): Int =
        if (id in contexto.nino.insignias) maxOf(valor, objetivo) else valor

    val CATALOGO = listOf(
        Logro("primera_tarea", "Primer paso", "Valida tu primera misión", "gmr-thumb-up", R.color.game_green, 1) { progresoPersistido(it, "primera_tarea", it.totalTareasValidadas, 1) },
        Logro("cinco_tareas", "En marcha", "Valida 5 misiones", "gmr-military-tech", R.color.game_blue, 5) { progresoPersistido(it, "cinco_tareas", it.totalTareasValidadas, 5) },
        Logro("diez_tareas", "Imparable", "Valida 10 misiones", "gmr-rocket-launch", R.color.game_purple, 10) { it.totalTareasValidadas },
        Logro("veinte_tareas", "Veterano", "Valida 20 misiones", "gmr-star", R.color.game_yellow_dark, 20) { progresoPersistido(it, "veinte_tareas", it.totalTareasValidadas, 20) },
        Logro("primera_academica", "Mente brillante", "Completa una misión académica", "gmr-school", R.color.game_blue, 1) { it.nino.tareasAcademicas },
        Logro("primera_domestica", "Héroe del hogar", "Completa una misión doméstica", "gmr-home", R.color.game_orange, 1) { it.nino.tareasDomesticas },
        Logro("primera_personal", "Mejor versión", "Completa una misión personal", "gmr-self-improvement", R.color.game_pink, 1) { it.nino.tareasPersonales },
        Logro("racha_3", "Buen ritmo", "Mantén una racha de 3 días", "gmr-local-fire-department", R.color.game_orange, 3) { progresoPersistido(it, "racha_3", it.nino.racha, 3) },
        Logro("racha_7", "Semana perfecta", "Mantén una racha de 7 días", "gmr-bolt", R.color.game_yellow_dark, 7) { progresoPersistido(it, "racha_7", it.nino.racha, 7) },
        Logro("nivel_5", "Aventurero", "Alcanza el nivel 5", "gmr-grade", R.color.game_green_dark, 5) { progresoPersistido(it, "nivel_5", it.nino.nivel, 5) },
        Logro("nivel_10", "Experto", "Alcanza el nivel 10", "gmr-auto-awesome", R.color.game_purple, 10) { progresoPersistido(it, "nivel_10", it.nino.nivel, 10) },
        Logro("nivel_15", "Leyenda", "Alcanza el nivel 15", "gmr-emoji-events", R.color.game_yellow_dark, 15) { it.nino.nivel },
        Logro("xp_100", "Centenario", "Consigue 100 XP", "gmr-flare", R.color.game_blue_dark, 100) { it.nino.experiencia },
        Logro("xp_500", "Maestro de XP", "Consigue 500 XP", "gmr-workspace-premium", R.color.game_purple_dark, 500) { it.nino.experiencia },
        Logro("primer_canje", "Primer premio", "Realiza tu primer canje", "gmr-card-giftcard", R.color.game_coral, 1) { if (it.totalCanjes >= 1 || it.nino.minutosExtra > 0) 1 else 0 }
    )

    fun logrosDesbloqueados(nino: Nino, extras: Contexto = Contexto(nino)): List<Logro> {
        val contexto = extras.copy(nino = nino)
        return CATALOGO.filter { it.cumplido(contexto) }
    }
}
