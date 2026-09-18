package com.angel.ibanez.dehabitos.workers

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ProgramadorTrabajos {

    private const val TRABAJO_RECORDATORIO = "recordatorio_diario"
    private const val TRABAJO_MISION = "mision_diaria"
    private const val HORA_RECORDATORIO = 17
    private const val HORA_MISION = 7

    fun programar(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.enqueueUniquePeriodicWork(
            TRABAJO_RECORDATORIO,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<RecordatorioWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(retrasoHasta(HORA_RECORDATORIO), TimeUnit.MILLISECONDS)
                .build()
        )
        workManager.enqueueUniquePeriodicWork(
            TRABAJO_MISION,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<MisionDiariaWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(retrasoHasta(HORA_MISION), TimeUnit.MILLISECONDS)
                .build()
        )
    }

    private fun retrasoHasta(hora: Int): Long {
        val ahora = Calendar.getInstance()
        val objetivo = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hora)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= ahora.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
        }
        return objetivo.timeInMillis - ahora.timeInMillis
    }
}
