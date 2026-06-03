package com.example.qhacemos.notificaciones

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.qhacemos.modelo.Evento
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

object GestorRecordatorios {

    fun programarRecordatorio(context: Context, evento: Evento) {
        val fechaEvento = evento.fechaInicioParseada ?: return
        val ahora = OffsetDateTime.now(ZoneId.of("America/Mexico_City"))

        val tiempoParaRecordatorio = ChronoUnit.MILLIS.between(ahora, fechaEvento.plusMinutes(1))

        if (tiempoParaRecordatorio <= 0) return

        val datos = Data.Builder()
            .putLong("evento_id", evento.id)
            .putString("titulo", "Recordatorio: ${evento.titulo}")
            .putString("mensaje", "¡Tu evento en ${evento.ubicacion} comienza en 2 horas!")
            .build()

        val peticion = OneTimeWorkRequestBuilder<RecordatorioWorker>()
            .setInitialDelay(tiempoParaRecordatorio, TimeUnit.MILLISECONDS) // Esperar el tiempo calculado
            .setInputData(datos)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "recordatorio_evento_${evento.id}",
            ExistingWorkPolicy.REPLACE,
            peticion
        )
    }

    fun cancelarRecordatorio(context: Context, eventoId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork("recordatorio_evento_$eventoId")
    }
}