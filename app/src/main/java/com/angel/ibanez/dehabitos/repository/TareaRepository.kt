package com.angel.ibanez.dehabitos.repository

import com.angel.ibanez.dehabitos.firebase.FirebaseFirestoreService
import com.angel.ibanez.dehabitos.models.Nino
import com.angel.ibanez.dehabitos.models.Tarea
import com.angel.ibanez.dehabitos.models.Validacion
import com.angel.ibanez.dehabitos.utils.MisionesDiariasUtils
import com.angel.ibanez.dehabitos.utils.ProgresionUtils
import com.angel.ibanez.dehabitos.utils.Resultado
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TareaRepository(
    private val firestoreService: FirebaseFirestoreService = FirebaseFirestoreService()
) {

    private val formatoDia = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val formatoFechaHora = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun crearTarea(tarea: Tarea): Flow<Resultado<Tarea>> = flow {
        emit(Resultado.Cargando)
        try {
            val conFecha = tarea.copy(fechaAsignacion = formatoFechaHora.format(Date()))
            firestoreService.guardarTarea(conFecha)
            emit(Resultado.Exito(conFecha))
        } catch (e: Exception) {
            emit(Resultado.Error(e.message ?: "Error al crear la tarea"))
        }
    }

    fun obtenerTareasPorNino(ninoId: String): Flow<Resultado<List<Tarea>>> = flow {
        emit(Resultado.Cargando)
        try {
            val tareas = firestoreService.obtenerTareasPorNino(ninoId)
            emit(Resultado.Exito(tareas))
        } catch (e: Exception) {
            emit(Resultado.Error(e.message ?: "Error al cargar las misiones"))
        }
    }

    fun obtenerTareasPorTutor(tutorId: String): Flow<Resultado<List<Tarea>>> = flow {
        emit(Resultado.Cargando)
        try {
            val tareas = firestoreService.obtenerTareasPorTutor(tutorId)
            emit(Resultado.Exito(tareas))
        } catch (e: Exception) {
            emit(Resultado.Error(e.message ?: "Error al cargar las tareas"))
        }
    }

    fun escucharTareasPorNino(
        ninoId: String,
        onResultado: (Resultado<List<Tarea>>) -> Unit
    ): ListenerRegistration = firestoreService.escucharTareasPorNino(
        ninoId,
        { onResultado(Resultado.Exito(it)) },
        { onResultado(Resultado.Error(it)) }
    )

    fun escucharTareasPorTutor(
        tutorId: String,
        onResultado: (Resultado<List<Tarea>>) -> Unit
    ): ListenerRegistration = firestoreService.escucharTareasPorTutor(
        tutorId,
        { onResultado(Resultado.Exito(it)) },
        { onResultado(Resultado.Error(it)) }
    )

    /** El niño marca la misión como hecha; queda pendiente de validación del tutor. */
    fun marcarCompletada(tarea: Tarea, evidencia: String = ""): Flow<Resultado<Tarea>> = flow {
        emit(Resultado.Cargando)
        try {
            val fecha = formatoFechaHora.format(Date())
            firestoreService.actualizarEstadoTarea(tarea.id, Tarea.ESTADO_COMPLETADA, fecha, evidencia)
            emit(Resultado.Exito(tarea.copy(estado = Tarea.ESTADO_COMPLETADA, fechaCompletada = fecha, evidencia = evidencia)))
        } catch (e: Exception) {
            emit(Resultado.Error(e.message ?: "Error al completar la misión"))
        }
    }

    /** El tutor rechaza: la tarea vuelve a pendiente. */
    fun rechazarTarea(tarea: Tarea): Flow<Resultado<Tarea>> = flow {
        emit(Resultado.Cargando)
        try {
            firestoreService.actualizarEstadoTarea(tarea.id, Tarea.ESTADO_PENDIENTE, "", "")
            emit(Resultado.Exito(tarea.copy(estado = Tarea.ESTADO_PENDIENTE, fechaCompletada = "", evidencia = "")))
        } catch (e: Exception) {
            emit(Resultado.Error(e.message ?: "Error al rechazar la tarea"))
        }
    }

    suspend fun asegurarMisionDiaria(ninoId: String, tutorId: String): Boolean {
        val hoy = formatoDia.format(Date())
        val mision = MisionesDiariasUtils.misionParaFecha(hoy)
        val tarea = Tarea(
            id = MisionesDiariasUtils.idMisionDiaria(ninoId, hoy),
            ninoId = ninoId,
            tutorId = tutorId,
            nombre = mision.nombre,
            categoria = mision.categoria,
            puntos = mision.puntos,
            estado = Tarea.ESTADO_PENDIENTE,
            fechaAsignacion = formatoFechaHora.format(Date()),
            fechaLimite = hoy,
            esDiaria = true
        )
        return firestoreService.crearTareaSiNoExiste(tarea)
    }

    /**
     * El tutor valida: la tarea pasa a validada y el niño recibe los puntos.
     * Actualiza XP, nivel, racha, insignias y contadores por categoría.
     */
    fun validarTarea(tarea: Tarea): Flow<Resultado<Validacion>> = flow {
        emit(Resultado.Cargando)
        try {
            val nino = firestoreService.obtenerNinoPorId(tarea.ninoId)
                ?: throw Exception("Niño no encontrado")

            firestoreService.actualizarEstadoTarea(
                tarea.id,
                Tarea.ESTADO_VALIDADA,
                tarea.fechaCompletada.ifBlank { formatoFechaHora.format(Date()) }
            )

            val actualizado = aplicarProgreso(nino, tarea)
            firestoreService.actualizarProgresoNino(actualizado)
            emit(Resultado.Exito(Validacion(actualizado, tarea.nombre)))
        } catch (e: Exception) {
            emit(Resultado.Error(e.message ?: "Error al validar la tarea"))
        }
    }

    private fun aplicarProgreso(nino: Nino, tarea: Tarea): Nino {
        val xpNueva = nino.experiencia + tarea.puntos
        val nivelNuevo = ProgresionUtils.nivelDesdeXp(xpNueva)

        val hoy = formatoDia.format(Date())
        val ayer = formatoDia.format(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.time)
        val rachaNueva = when (nino.ultimoDiaCompletado) {
            hoy -> nino.racha.coerceAtLeast(1)
            ayer -> nino.racha + 1
            else -> 1
        }

        val conContadores = nino.copy(
            experiencia = xpNueva,
            nivel = nivelNuevo,
            estrellas = nino.estrellas + ProgresionUtils.estrellasPorNiveles(nino.nivel, nivelNuevo),
            racha = rachaNueva,
            ultimoDiaCompletado = hoy,
            tareasAcademicas = nino.tareasAcademicas + if (tarea.categoria == Tarea.CATEGORIA_ACADEMICA) 1 else 0,
            tareasDomesticas = nino.tareasDomesticas + if (tarea.categoria == Tarea.CATEGORIA_DOMESTICA) 1 else 0,
            tareasPersonales = nino.tareasPersonales + if (tarea.categoria == Tarea.CATEGORIA_PERSONAL) 1 else 0
        )

        return conContadores.copy(insignias = ProgresionUtils.calcularInsignias(conContadores))
    }
}
