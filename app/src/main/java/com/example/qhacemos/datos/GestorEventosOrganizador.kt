package com.example.qhacemos.datos

import com.example.qhacemos.modelo.Evento
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class EventoPublicacion(
    val titulo: String,
    val descripcion: String,
    @SerialName("fecha_inicio")
    val fechaInicio: String?,
    @SerialName("fecha_hora")
    val fechaHora: String,
    val ubicacion: String,
    @SerialName("direccion_completa")
    val direccionCompleta: String,
    val latitud: Double?,
    val longitud: Double?,
    val categoria: String,
    @SerialName("organizador_id")
    val organizadorId: String,
    @SerialName("organizador_nombre")
    val organizadorNombre: String,
    @SerialName("contacto_organizador")
    val contactoOrganizador: String,
    @SerialName("costo_mxn")
    val costoMxn: Double,
    val moneda: String,
    @SerialName("es_destacado")
    val esDestacado: Boolean,
    @SerialName("es_gratis")
    val esGratis: Boolean,
    @SerialName("tipo_publicacion")
    val tipoPublicacion: String,
    val estado: String,
    @SerialName("imagen_principal_url")
    val imagenPrincipalUrl: String,
    val imagenes: List<String>,
    @SerialName("color_hex")
    val colorHex: String
)

object GestorEventosOrganizador {

    private val eventosDemo = mutableListOf<Evento>()

    fun obtenerEventosDemo(): List<Evento> = eventosDemo

    suspend fun publicarEvento(evento: Evento, montoPago: Double): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            if (!SupabaseCliente.estaConfigurado) {
                eventosDemo.add(evento.copy(id = (eventosDemo.size + 1).toLong(), estado = "pendiente_validacion"))
                return@runCatching true
            }
            SupabaseCliente.cliente.from("eventos").insert(evento.aPublicacion("pendiente_validacion"))
            true
        }
    }

    suspend fun actualizarEvento(evento: Evento): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            if (!SupabaseCliente.estaConfigurado) {
                val index = eventosDemo.indexOfFirst { it.id == evento.id }
                if (index != -1) {
                    eventosDemo[index] = evento.copy(estado = "pendiente_validacion")
                }
                return@runCatching index != -1
            }

            val datos = evento.aPublicacion("pendiente_validacion")
            SupabaseCliente.cliente.from("eventos").update({
                set("titulo", datos.titulo)
                set("descripcion", datos.descripcion)
                set("fecha_inicio", datos.fechaInicio)
                set("fecha_hora", datos.fechaHora)
                set("ubicacion", datos.ubicacion)
                set("direccion_completa", datos.direccionCompleta)
                set("latitud", datos.latitud)
                set("longitud", datos.longitud)
                set("categoria", datos.categoria)
                set("contacto_organizador", datos.contactoOrganizador)
                set("costo_mxn", datos.costoMxn)
                set("moneda", datos.moneda)
                set("es_gratis", datos.esGratis)
                set("tipo_publicacion", datos.tipoPublicacion)
                set("estado", datos.estado)
                set("imagen_principal_url", datos.imagenPrincipalUrl)
                set("imagenes", datos.imagenes)
                set("color_hex", datos.colorHex)
            }) {
                filter { eq("id", evento.id) }
            }
            true
        }
    }

    suspend fun eliminarEvento(eventoId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            if (!SupabaseCliente.estaConfigurado) {
                val index = eventosDemo.indexOfFirst { it.id == eventoId }
                if (index != -1) eventosDemo[index] = eventosDemo[index].copy(estado = "eliminado")
                return@runCatching index != -1
            }

            SupabaseCliente.cliente.from("eventos").update({
                set("estado", "eliminado")
            }) {
                filter { eq("id", eventoId) }
            }
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
                return@runCatching eventosDemo.filter {
                    it.organizadorId == organizadorId && !it.estado.equals("eliminado", ignoreCase = true)
                }
            }
            SupabaseCliente.cliente.from("eventos")
                .select { filter { eq("organizador_id", organizadorId) } }
                .decodeList<Evento>()
                .filter { !it.estado.equals("eliminado", ignoreCase = true) }
        }
    }

    suspend fun obtenerEventoPorId(eventoId: Long): Result<Evento?> = withContext(Dispatchers.IO) {
        runCatching {
            if (!SupabaseCliente.estaConfigurado) {
                return@runCatching eventosDemo.firstOrNull { it.id == eventoId }
            }

            SupabaseCliente.cliente.from("eventos")
                .select { filter { eq("id", eventoId) } }
                .decodeList<Evento>()
                .firstOrNull()
        }
    }

    private fun Evento.aPublicacion(estadoPublicacion: String): EventoPublicacion {
        return EventoPublicacion(
            titulo = titulo,
            descripcion = descripcion,
            fechaInicio = fechaInicio.takeIf { it.isNotBlank() },
            fechaHora = fechaHora,
            ubicacion = ubicacion,
            direccionCompleta = direccionCompleta,
            latitud = latitud,
            longitud = longitud,
            categoria = categoria,
            organizadorId = organizadorId,
            organizadorNombre = organizadorNombre,
            contactoOrganizador = contactoOrganizador,
            costoMxn = costoMxn,
            moneda = moneda,
            esDestacado = esDestacado,
            esGratis = esGratis,
            tipoPublicacion = tipoPublicacion,
            estado = estadoPublicacion,
            imagenPrincipalUrl = imagenPrincipalUrl,
            imagenes = imagenes,
            colorHex = colorHex
        )
    }
}
