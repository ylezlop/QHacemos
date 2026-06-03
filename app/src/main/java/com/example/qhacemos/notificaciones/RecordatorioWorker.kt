package com.example.qhacemos.notificaciones

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class RecordatorioWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val eventoId = inputData.getLong("evento_id", -1L)
        val titulo = inputData.getString("titulo") ?: "¡Tu evento está por comenzar!"
        val mensaje = inputData.getString("mensaje") ?: "Abre la app para ver los detalles."

        val notificador = NotificadorEventos(applicationContext)
        notificador.mostrarNotificacionBasica(
            titulo = titulo,
            mensaje = mensaje,
            eventoId = if (eventoId != -1L) eventoId else null
        )

        return Result.success()
    }
}