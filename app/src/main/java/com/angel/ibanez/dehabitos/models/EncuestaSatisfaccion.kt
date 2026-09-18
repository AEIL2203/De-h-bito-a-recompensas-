package com.angel.ibanez.dehabitos.models

import com.google.firebase.firestore.Exclude

data class EncuestaSatisfaccion(
    var id: String = "",
    var tutorId: String = "",
    var ninoId: String = "",
    var fecha: Long = 0,
    var respuestas: Map<String, Int> = emptyMap(),
    var comentario: String = ""
) {
    @Exclude
    fun promedio(): Double = if (respuestas.isEmpty()) 0.0 else respuestas.values.average()

    companion object {
        const val VALOR_MINIMO = 1
        const val VALOR_MAXIMO = 5

        val PREGUNTAS = listOf(
            "motivacion" to "Mi hijo/a se siente motivado/a a cumplir sus tareas",
            "tiempo_pantalla" to "El tiempo de pantalla de mi hijo/a ha disminuido",
            "responsabilidad" to "Mi hijo/a cumple sus responsabilidades con menos recordatorios",
            "facilidad" to "La aplicación es fácil de usar",
            "recomendaria" to "Recomendaría la aplicación a otros padres"
        )
    }
}
