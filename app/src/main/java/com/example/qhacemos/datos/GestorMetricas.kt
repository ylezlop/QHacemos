package com.example.qhacemos.datos

import android.content.Context
import com.example.qhacemos.modelo.Evento
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

data class FiltrosMetricas(
    val estado: String = "Todos",
    val categoria: String = "Todas",
    val organizador: String = "Todos",
    val periodo: String = "Todo"
)

data class PuntoGrafica(
    val etiqueta: String,
    val valor: Int
)

data class MetricasSistema(
    val totalUsuarios: Int,
    val totalEventos: Int,
    val eventosPublicados: Int,
    val eventosPendientes: Int,
    val eventosDestacados: Int,
    val vistasTotales: Int,
    val clicksTotales: Int,
    val guardadosTotales: Int,
    val suscripcionesActivas: Int,
    val ingresosEstimadosMxn: Double,
    val categoriasDisponibles: List<String>,
    val organizadoresDisponibles: List<String>,
    val interacciones: List<PuntoGrafica>,
    val eventosPorCategoria: List<PuntoGrafica>,
    val eventosPorOrganizador: List<PuntoGrafica>
)

object GestorMetricas {

    suspend fun registrarVista(evento: Evento): Result<Unit> {
        return actualizarMetrica(evento, vistas = evento.vistas + 1)
    }

    suspend fun registrarClick(evento: Evento): Result<Unit> {
        return actualizarMetrica(evento, clicks = evento.clicks + 1)
    }

    suspend fun registrarGuardado(evento: Evento): Result<Unit> {
        return actualizarMetrica(evento, guardados = evento.guardados + 1)
    }

    suspend fun obtenerMetricasSistema(
        context: Context,
        filtros: FiltrosMetricas = FiltrosMetricas()
    ): Result<MetricasSistema> = withContext(Dispatchers.IO) {
        runCatching {
            val eventos = cargarEventos(context).let { resultado ->
                when (resultado) {
                    is ResultadoEventos.Exito -> resultado.eventos
                    is ResultadoEventos.Error -> resultado.eventosLocales
                }
            }
            val eventosFiltrados = eventos.aplicarFiltros(filtros)

            val suscripcionesActivas = contarSuscripcionesActivas()
            MetricasSistema(
                totalUsuarios = contarUsuarios(),
                totalEventos = eventosFiltrados.size,
                eventosPublicados = eventosFiltrados.count { it.estado.equals("publicado", ignoreCase = true) },
                eventosPendientes = eventosFiltrados.count { it.estado.equals("pendiente_validacion", ignoreCase = true) },
                eventosDestacados = eventosFiltrados.count { it.esDestacado },
                vistasTotales = eventosFiltrados.sumOf { it.vistas },
                clicksTotales = eventosFiltrados.sumOf { it.clicks },
                guardadosTotales = eventosFiltrados.sumOf { it.guardados },
                suscripcionesActivas = suscripcionesActivas,
                ingresosEstimadosMxn = estimarIngresos(eventosFiltrados, suscripcionesActivas),
                categoriasDisponibles = eventos.map { it.categoria.ifBlank { "Sin categoria" } }
                    .distinct()
                    .sorted(),
                organizadoresDisponibles = eventos.map { it.etiquetaOrganizador() }
                    .distinct()
                    .sorted(),
                interacciones = listOf(
                    PuntoGrafica("Vistas", eventosFiltrados.sumOf { it.vistas }),
                    PuntoGrafica("Clicks", eventosFiltrados.sumOf { it.clicks }),
                    PuntoGrafica("Guardados", eventosFiltrados.sumOf { it.guardados })
                ),
                eventosPorCategoria = eventosFiltrados
                    .groupingBy { it.categoria.ifBlank { "Sin categoria" } }
                    .eachCount()
                    .map { PuntoGrafica(it.key, it.value) }
                    .sortedByDescending { it.valor }
                    .take(6),
                eventosPorOrganizador = eventosFiltrados
                    .groupingBy { it.etiquetaOrganizador() }
                    .eachCount()
                    .map { PuntoGrafica(it.key, it.value) }
                    .sortedByDescending { it.valor }
                    .take(6)
            )
        }
    }

    private fun List<Evento>.aplicarFiltros(filtros: FiltrosMetricas): List<Evento> {
        return filter { evento ->
            val coincideEstado = filtros.estado == "Todos" ||
                evento.estado.equals(filtros.estado, ignoreCase = true)

            val categoriaEvento = evento.categoria.ifBlank { "Sin categoria" }
            val coincideCategoria = filtros.categoria == "Todas" ||
                categoriaEvento.equals(filtros.categoria, ignoreCase = true)

            val coincideOrganizador = filtros.organizador == "Todos" ||
                evento.etiquetaOrganizador().equals(filtros.organizador, ignoreCase = true)

            val coincidePeriodo = when (filtros.periodo) {
                "Ultimos 30 dias" -> evento.fechaInicioParseada?.isAfter(
                    OffsetDateTime.now().minus(30, ChronoUnit.DAYS)
                ) ?: false
                "Ultimos 90 dias" -> evento.fechaInicioParseada?.isAfter(
                    OffsetDateTime.now().minus(90, ChronoUnit.DAYS)
                ) ?: false
                else -> true
            }

            coincideEstado && coincideCategoria && coincideOrganizador && coincidePeriodo
        }
    }

    private fun Evento.etiquetaOrganizador(): String {
        return organizadorNombre.ifBlank {
            organizadorId.ifBlank { "Sin organizador" }
        }
    }

    private suspend fun actualizarMetrica(
        evento: Evento,
        vistas: Int? = null,
        clicks: Int? = null,
        guardados: Int? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (!SupabaseCliente.estaConfigurado) {
                GestorEventosOrganizador.actualizarMetricasDemo(
                    eventoId = evento.id,
                    vistas = vistas,
                    clicks = clicks,
                    guardados = guardados
                )
                return@runCatching
            }

            SupabaseCliente.cliente.from("eventos").update({
                vistas?.let { set("vistas", it) }
                clicks?.let { set("clicks", it) }
                guardados?.let { set("guardados", it) }
            }) {
                filter { eq("id", evento.id) }
            }
        }
    }

    private suspend fun contarUsuarios(): Int {
        if (!SupabaseCliente.estaConfigurado) return 2

        return runCatching {
            SupabaseCliente.cliente
                .from("perfiles")
                .select()
                .decodeList<Map<String, JsonElement>>()
                .size
        }.getOrDefault(0)
    }

    private suspend fun contarSuscripcionesActivas(): Int {
        if (!SupabaseCliente.estaConfigurado) {
            return if (GestorSuscripciones.suscripcionDemoActiva()) 1 else 0
        }

        return runCatching {
            SupabaseCliente.cliente
                .from("suscripciones")
                .select {
                    filter { eq("estado", "activa") }
                }
                .decodeList<Map<String, JsonElement>>()
                .size
        }.getOrDefault(0)
    }

    private fun estimarIngresos(eventos: List<Evento>, suscripcionesActivas: Int): Double {
        val ingresosPublicacion = eventos.count {
            it.tipoPublicacion.equals("pago", ignoreCase = true) ||
                it.tipoPublicacion.equals("pago_individual", ignoreCase = true)
        } * 50.0
        val ingresosDestacados = eventos.count { it.esDestacado } * 99.0
        val ingresosSuscripciones = suscripcionesActivas * 299.0

        return ingresosPublicacion + ingresosDestacados + ingresosSuscripciones
    }
}
