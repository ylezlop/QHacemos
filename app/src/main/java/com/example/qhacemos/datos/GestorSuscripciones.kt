package com.example.qhacemos.datos

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest

object GestorSuscripciones {

    // Variables en memoria para controlar el estado en Modo Demo Offline
    private var suscripcionActivaDemo = false

    suspend fun verificarSuscripcionActiva(usuarioId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            if (!SupabaseCliente.estaConfigurado) return@runCatching suscripcionActivaDemo
            val lista = SupabaseCliente.cliente.from("suscripciones")
                .select {
                    filter {
                        eq("usuario_id", usuarioId)
                        eq("estado", "activa")
                    }
                }
                .decodeList<Map<String, String>>()
            lista.isNotEmpty()
        }
    }

    suspend fun contratarSuscripcionMensual(usuarioId: String, costo: Double): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (!SupabaseCliente.estaConfigurado) {
                suscripcionActivaDemo = true
                return@withContext Result.success(true)
            }

            val cliente = SupabaseCliente.cliente
            val mapaSuscripcion = mapOf(
                "usuario_id" to usuarioId,
                "estado" to "activa",
                "fecha_inicio" to "2026-05-12", // Fecha actual simulada del sistema
                "fecha_fin" to "2026-06-12"
            )
            cliente.postgrest["suscripciones"].insert(mapaSuscripcion)

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}