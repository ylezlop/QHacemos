package com.example.qhacemos.datos

import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReporteEvento(
    val id: Long? = null,
    @SerialName("evento_id")
    val eventoId: Long,
    @SerialName("usuario_id")
    val usuarioId: String,
    val motivo: String,
    @SerialName("fecha_reporte")
    val fechaReporte: String? = null
)

object GestorReportes {
    private const val TABLA_REPORTES = "reportes_eventos"

    private val reportesDemo = mutableListOf<ReporteEvento>()


    suspend fun enviarReporte(eventoId: Long, usuarioId: String, motivo: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                val nuevoReporte = ReporteEvento(
                    eventoId = eventoId,
                    usuarioId = usuarioId,
                    motivo = motivo
                )

                if (!SupabaseCliente.estaConfigurado) {
                    reportesDemo.add(nuevoReporte.copy(id = (reportesDemo.size + 1).toLong()))
                    return@runCatching true
                }

                SupabaseCliente.cliente.from(TABLA_REPORTES).insert(nuevoReporte)
                true
            }
        }

    suspend fun obtenerConteoReportes(eventoId: Long): Result<Map<String, Int>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val reportesDelEvento = if (!SupabaseCliente.estaConfigurado) {
                    reportesDemo.filter { it.eventoId == eventoId }
                } else {
                    // Consulta relacional filtrando por evento_id
                    SupabaseCliente.cliente.from(TABLA_REPORTES)
                        .select {
                            filter {
                                eq("evento_id", eventoId)
                            }
                        }
                        .decodeList<ReporteEvento>()
                }

                // Transforma la lista en un mapa de frecuencias: [Motivo -> Cantidad]
                reportesDelEvento.groupingBy { it.motivo }.eachCount()
            }
        }
}