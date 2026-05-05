package com.example.qhacemos.datos

import android.content.Context
import com.example.qhacemos.modelo.Evento
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.InputStreamReader

private const val TABLA_EVENTOS = "eventos"

@Serializable
private data class EventoRemoto(
    val id: Long? = null,
    val titulo: String? = null,
    val descripcion: String? = null,
    @SerialName("fecha_inicio")
    val fechaInicio: String? = null,
    @SerialName("fecha_hora")
    val fechaHora: String? = null,
    val ubicacion: String? = null,
    @SerialName("direccion_completa")
    val direccionCompleta: String? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val categoria: String? = null,
    @SerialName("organizador_id")
    val organizadorId: String? = null,
    @SerialName("organizador_nombre")
    val organizadorNombre: String? = null,
    @SerialName("contacto_organizador")
    val contactoOrganizador: String? = null,
    @SerialName("organizador_calificacion_promedio")
    val organizadorCalificacionPromedio: Double? = null,
    @SerialName("organizador_total_calificaciones")
    val organizadorTotalCalificaciones: Int? = null,
    @SerialName("costo_mxn")
    val costoMxn: Double? = null,
    val moneda: String? = null,
    @SerialName("es_destacado")
    val esDestacado: Boolean? = null,
    @SerialName("es_gratis")
    val esGratis: Boolean? = null,
    @SerialName("tipo_publicacion")
    val tipoPublicacion: String? = null,
    val estado: String? = null,
    @SerialName("imagen_principal_url")
    val imagenPrincipalUrl: String? = null,
    val imagenes: List<String>? = null,
    val vistas: Int? = null,
    val clicks: Int? = null,
    val guardados: Int? = null,
    @SerialName("color_hex")
    val colorHex: String? = null
) {
    fun aEvento(): Evento {
        return Evento(
            id = id ?: 0L,
            titulo = titulo.orEmpty(),
            descripcion = descripcion.orEmpty(),
            fechaInicio = fechaInicio.orEmpty(),
            fechaHora = fechaHora.orEmpty(),
            ubicacion = ubicacion.orEmpty(),
            direccionCompleta = direccionCompleta.orEmpty(),
            latitud = latitud,
            longitud = longitud,
            categoria = categoria.orEmpty(),
            organizadorId = organizadorId.orEmpty(),
            organizadorNombre = organizadorNombre.orEmpty(),
            contactoOrganizador = contactoOrganizador.orEmpty(),
            organizadorCalificacionPromedio = organizadorCalificacionPromedio ?: 0.0,
            organizadorTotalCalificaciones = organizadorTotalCalificaciones ?: 0,
            costoMxn = costoMxn ?: 0.0,
            moneda = moneda ?: "MXN",
            esDestacado = esDestacado ?: false,
            esGratis = esGratis ?: false,
            tipoPublicacion = tipoPublicacion ?: "gratuita",
            estado = estado ?: "publicado",
            imagenPrincipalUrl = imagenPrincipalUrl.orEmpty(),
            imagenes = imagenes.orEmpty(),
            vistas = vistas ?: 0,
            clicks = clicks ?: 0,
            guardados = guardados ?: 0,
            colorHex = colorHex ?: "#9E9E9E"
        )
    }
}

sealed class ResultadoEventos {
    data class Exito(val eventos: List<Evento>) : ResultadoEventos()
    data class Error(val mensaje: String, val eventosLocales: List<Evento>) : ResultadoEventos()
}

// Necesitamos el 'Context' para poder acceder a la carpeta de recursos (assets) de la app.
fun leerEventos(contexto: Context): List<Evento> {
    return try {
        // Abrimos el archivo
        val flujoEntrada = contexto.assets.open("eventos.json")
        val lector = InputStreamReader(flujoEntrada)

        // Le indicamos a Gson que queremos transformar el JSON en una Lista de Eventos
        val tipoLista = object : TypeToken<List<Evento>>() {}.type

        // Hacemos la magia de conversión
        Gson().fromJson(lector, tipoLista)

    } catch (e: Exception) {
        // Si el archivo no existe o hay un error, imprimimos el error y devolvemos una lista vacía
        e.printStackTrace()
        emptyList()
    }
}

suspend fun leerEventosSupabase(): List<Evento> {
    if (!SupabaseCliente.estaConfigurado) return emptyList()

    return SupabaseCliente.cliente
        .from(TABLA_EVENTOS)
        .select()
        .decodeList<EventoRemoto>()
        .map { it.aEvento() }
}

suspend fun obtenerEventos(contexto: Context): List<Evento> {
    return when (val resultado = cargarEventos(contexto)) {
        is ResultadoEventos.Exito -> resultado.eventos
        is ResultadoEventos.Error -> resultado.eventosLocales
    }
}

suspend fun cargarEventos(contexto: Context): ResultadoEventos {
    if (!SupabaseCliente.estaConfigurado) {
        return ResultadoEventos.Exito(leerEventos(contexto))
    }

    return try {
        val eventosRemotos = leerEventosSupabase()
        ResultadoEventos.Exito(eventosRemotos)
    } catch (e: Exception) {
        e.printStackTrace()
        val detalle = e.message?.takeIf { it.isNotBlank() } ?: "No se pudo completar la consulta."
        ResultadoEventos.Error(
            mensaje = "Error al cargar los eventos: $detalle",
            eventosLocales = emptyList()
        )
    }
}
