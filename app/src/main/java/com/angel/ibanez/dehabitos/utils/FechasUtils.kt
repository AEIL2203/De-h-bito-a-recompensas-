package com.angel.ibanez.dehabitos.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FechasUtils {

    private const val FORMATO_DIA = "yyyy-MM-dd"

    enum class EstadoVencimiento { SIN_LIMITE, A_TIEMPO, VENCE_HOY, VENCIDA }

    fun hoy(): String = SimpleDateFormat(FORMATO_DIA, Locale.US).format(Date())

    fun estadoVencimiento(fechaLimite: String, hoy: String = hoy()): EstadoVencimiento {
        if (fechaLimite.isBlank()) return EstadoVencimiento.SIN_LIMITE
        return when {
            fechaLimite > hoy -> EstadoVencimiento.A_TIEMPO
            fechaLimite == hoy -> EstadoVencimiento.VENCE_HOY
            else -> EstadoVencimiento.VENCIDA
        }
    }

    fun formatearFechaCorta(fecha: String): String = runCatching {
        val parsed = SimpleDateFormat(FORMATO_DIA, Locale.US).parse(fecha) ?: return fecha
        SimpleDateFormat("dd/MM", Locale.US).format(parsed)
    }.getOrDefault(fecha)
}
