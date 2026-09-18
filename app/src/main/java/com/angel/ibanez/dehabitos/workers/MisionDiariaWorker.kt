package com.angel.ibanez.dehabitos.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.angel.ibanez.dehabitos.repository.TareaRepository
import com.angel.ibanez.dehabitos.utils.DispositivoConfig

class MisionDiariaWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (DispositivoConfig.obtenerModo() != DispositivoConfig.ModoDispositivo.HIJO) return Result.success()
        val ninoId = DispositivoConfig.obtenerNinoId()
        val tutorId = DispositivoConfig.obtenerTutorId()
        if (ninoId.isBlank() || tutorId.isBlank()) return Result.success()
        return try {
            TareaRepository().asegurarMisionDiaria(ninoId, tutorId)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
