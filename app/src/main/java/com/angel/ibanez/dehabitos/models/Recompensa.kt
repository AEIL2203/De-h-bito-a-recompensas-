package com.angel.ibanez.dehabitos.models

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Recompensa(
    var id: String = "",
    var ninoId: String = "",
    var tutorId: String = "",
    var packageName: String = "",
    var minutosOtorgados: Int = 0,
    var estrellasGastadas: Int = 0,
    var fechaCanje: Long = 0,
    var milisegundosConsumidos: Long = 0
) {
    @get:Exclude
    val milisegundosRestantes: Long
        get() = (duracionTotalMs - milisegundosConsumidos).coerceAtLeast(0)

    @get:Exclude
    val duracionTotalMs: Long
        get() = minutosOtorgados * 60_000L
}
