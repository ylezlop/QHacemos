package com.example.qhacemos.datos

import com.example.qhacemos.modelo.Evento
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val TABLA_ASISTENCIAS = "asistencias_eventos"
private const val TABLA_CALIFICACIONES = "calificaciones_organizadores"

@Serializable
data class AsistenciaEvento(
    val id: Long? = null,
    @SerialName("usuario_id")
    val usuarioId: String,
    @SerialName("evento_id")
    val eventoId: Long,
    val estado: String = "asistire"
)

@Serializable
private data class NuevaAsistenciaEvento(
    @SerialName("usuario_id")
    val usuarioId: String,
    @SerialName("evento_id")
    val eventoId: Long,
    val estado: String = "asistire"
)

@Serializable
data class CalificacionOrganizador(
    val id: Long? = null,
    @SerialName("usuario_id")
    val usuarioId: String,
    @SerialName("evento_id")
    val eventoId: Long,
    @SerialName("organizador_id")
    val organizadorId: String,
    val calificacion: Int
)

@Serializable
private data class NuevaCalificacionOrganizador(
    @SerialName("usuario_id")
    val usuarioId: String,
    @SerialName("evento_id")
    val eventoId: Long,
    @SerialName("organizador_id")
    val organizadorId: String,
    val calificacion: Int
)

object GestorAsistencias {
    private val asistenciasDemo = mutableListOf<AsistenciaEvento>()
    private val calificacionesDemo = mutableListOf<CalificacionOrganizador>()

    suspend fun cargarAsistenciasUsuario(): Result<List<AsistenciaEvento>> {
        val perfil = GestorAutenticacion.cargarPerfilActual().getOrThrow()
            ?: return Result.failure(IllegalStateException("Debes iniciar sesion."))

        if (!SupabaseCliente.estaConfigurado) {
            return Result.success(asistenciasDemo.filter { it.usuarioId == perfil.id })
        }

        return runCatching {
            SupabaseCliente.cliente
                .from(TABLA_ASISTENCIAS)
                .select {
                    filter {
                        eq("usuario_id", perfil.id)
                    }
                }
                .decodeList<AsistenciaEvento>()
        }
    }

    suspend fun asistiraAEvento(eventoId: Long): Result<Boolean> {
        return cargarAsistenciasUsuario().map { asistencias ->
            asistencias.any { it.eventoId == eventoId && it.estado != "cancelado" }
        }
    }

    suspend fun registrarAsistencia(evento: Evento): Result<Unit> {
        val perfil = GestorAutenticacion.cargarPerfilActual().getOrThrow()
            ?: return Result.failure(IllegalStateException("Debes iniciar sesion."))

        if (evento.yaOcurrio) {
            return Result.failure(IllegalStateException("Este evento ya paso."))
        }

        if (asistiraAEvento(evento.id).getOrDefault(false)) {
            return Result.success(Unit)
        }

        val asistencia = NuevaAsistenciaEvento(
            usuarioId = perfil.id,
            eventoId = evento.id
        )

        if (!SupabaseCliente.estaConfigurado) {
            asistenciasDemo.add(
                AsistenciaEvento(
                    id = asistenciasDemo.size.toLong() + 1L,
                    usuarioId = asistencia.usuarioId,
                    eventoId = asistencia.eventoId,
                    estado = asistencia.estado
                )
            )
            return Result.success(Unit)
        }

        return runCatching {
            SupabaseCliente.cliente
                .from(TABLA_ASISTENCIAS)
                .insert(asistencia)
        }
    }

    suspend fun cargarCalificacionesUsuario(): Result<List<CalificacionOrganizador>> {
        val perfil = GestorAutenticacion.cargarPerfilActual().getOrThrow()
            ?: return Result.failure(IllegalStateException("Debes iniciar sesion."))

        if (!SupabaseCliente.estaConfigurado) {
            return Result.success(calificacionesDemo.filter { it.usuarioId == perfil.id })
        }

        return runCatching {
            SupabaseCliente.cliente
                .from(TABLA_CALIFICACIONES)
                .select {
                    filter {
                        eq("usuario_id", perfil.id)
                    }
                }
                .decodeList<CalificacionOrganizador>()
        }
    }

    suspend fun calificarOrganizador(
        evento: Evento,
        calificacion: Int
    ): Result<Unit> {
        val perfil = GestorAutenticacion.cargarPerfilActual().getOrThrow()
            ?: return Result.failure(IllegalStateException("Debes iniciar sesion."))

        if (!evento.yaOcurrio) {
            return Result.failure(IllegalStateException("Podras calificar cuando el evento ya haya ocurrido."))
        }

        if (!asistiraAEvento(evento.id).getOrDefault(false)) {
            return Result.failure(IllegalStateException("Solo puedes calificar eventos a los que marcaste asistencia."))
        }

        if (evento.organizadorId.isBlank()) {
            return Result.failure(IllegalStateException("Este evento no tiene organizador asociado."))
        }

        val yaCalificado = cargarCalificacionesUsuario()
            .getOrDefault(emptyList())
            .any { it.eventoId == evento.id }

        if (yaCalificado) {
            return Result.failure(IllegalStateException("Ya calificaste a este organizador por este evento."))
        }

        val nuevaCalificacion = NuevaCalificacionOrganizador(
            usuarioId = perfil.id,
            eventoId = evento.id,
            organizadorId = evento.organizadorId,
            calificacion = calificacion.coerceIn(1, 5)
        )

        if (!SupabaseCliente.estaConfigurado) {
            calificacionesDemo.add(
                CalificacionOrganizador(
                    id = calificacionesDemo.size.toLong() + 1L,
                    usuarioId = nuevaCalificacion.usuarioId,
                    eventoId = nuevaCalificacion.eventoId,
                    organizadorId = nuevaCalificacion.organizadorId,
                    calificacion = nuevaCalificacion.calificacion
                )
            )
            return Result.success(Unit)
        }

        return runCatching {
            SupabaseCliente.cliente
                .from(TABLA_CALIFICACIONES)
                .insert(nuevaCalificacion)
        }
    }
}
