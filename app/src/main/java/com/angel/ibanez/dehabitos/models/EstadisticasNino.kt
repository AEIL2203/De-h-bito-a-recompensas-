package com.angel.ibanez.dehabitos.models

data class GrupoCanjes(val packageName: String, val cantidad: Int, val minutos: Int)

data class ResumenSemanal(
    val inicio: Long,
    val fin: Long,
    val tareasValidadas: Int,
    val tiempoUsadoMs: Long,
    val canjes: Int,
    val estrellasGastadas: Int
)

data class EstadisticasNino(
    val nino: Nino,
    val nivelesAvanzados: Int,
    val tareasValidadas: Int,
    val tareasPorCategoria: Map<String, Int>,
    val canjesTotales: Int,
    val canjesPorApp: List<GrupoCanjes>,
    val estrellasGastadas: Int,
    val tiempoUsadoMs: Long,
    val sesionesRecientes: List<SesionApp>,
    val semanas: List<ResumenSemanal> = emptyList(),
    val encuestas: Int = 0,
    val satisfaccionPromedio: Double = 0.0
)
