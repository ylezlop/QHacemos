package com.example.qhacemos.datos

import com.example.qhacemos.modelo.Evento
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GestorEventosOrganizador {

    private val eventosDemo = mutableListOf<Evento>()

    fun obtenerEventosDemo(): List<Evento> = eventosDemo

    suspend fun publicarEvento(evento: Evento, montoPago: Double): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            if (!SupabaseCliente.estaConfigurado) {
                eventosDemo.add(evento.copy(id = (eventosDemo.size + 1).toLong(), estado = "pendiente_validacion"))
                return@runCatching true
            }
            SupabaseCliente.cliente.from("eventos").insert(evento.copy(estado = "pendiente_validacion"))
            true
        }
    }

    suspend fun destacarEvento(eventoId: Long, periodo: String, costo: Double): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            if (!SupabaseCliente.estaConfigurado) {
                val index = eventosDemo.indexOfFirst { it.id == eventoId }
                if (index != -1) eventosDemo[index] = eventosDemo[index].copy(esDestacado = true, estado = "destacado")
                return@runCatching true
            }
            SupabaseCliente.cliente.from("eventos").update({
                set("es_destacado", true)
                set("estado", "destacado")
            }) {
                filter { eq("id", eventoId) }
            }
            true
        }
    }

    suspend fun obtenerEventosPorOrganizador(organizadorId: String): Result<List<Evento>> = withContext(Dispatchers.IO) {
        runCatching {
            if (!SupabaseCliente.estaConfigurado) {
                return@runCatching eventosDemo.filter { it.organizadorId == organizadorId }
            }
            SupabaseCliente.cliente.from("eventos")
                .select { filter { eq("organizador_id", organizadorId) } }
                .decodeList<Evento>()
        }
    }
}