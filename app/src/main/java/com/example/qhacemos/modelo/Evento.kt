package com.example.qhacemos.modelo

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime
import java.util.Locale

@Serializable
data class Evento(
    val id: Int = 0,
    val titulo: String = "",
    val descripcion: String = "",
    @SerialName("fecha_inicio")
    val fechaInicio: String = "",
    @SerialName("fecha_hora")
    val fechaHora: String = "",
    val ubicacion: String = "",
    @SerialName("direccion_completa")
    val direccionCompleta: String = "",
    val latitud: Double? = null,
    val longitud: Double? = null,
    val categoria: String = "",
    @SerialName("organizador_id")
    val organizadorId: String = "",
    @SerialName("organizador_nombre")
    val organizadorNombre: String = "",
    @SerialName("contacto_organizador")
    val contactoOrganizador: String = "",
    @SerialName("organizador_calificacion_promedio")
    val organizadorCalificacionPromedio: Double = 0.0,
    @SerialName("organizador_total_calificaciones")
    val organizadorTotalCalificaciones: Int = 0,
    @SerialName("costo_mxn")
    val costoMxn: Double = 0.0,
    val moneda: String = "MXN",
    @SerialName("es_destacado")
    val esDestacado: Boolean = false,
    @SerialName("es_gratis")
    val esGratis: Boolean = false,
    @SerialName("tipo_publicacion")
    val tipoPublicacion: String = "gratuita",
    val estado: String = "publicado",
    @SerialName("imagen_principal_url")
    val imagenPrincipalUrl: String = "",
    val imagenes: List<String> = emptyList(),
    val vistas: Int = 0,
    val clicks: Int = 0,
    val guardados: Int = 0,
    @SerialName("color_hex")
    val colorHex: String = "#9E9E9E"
) {
    // Propiedad calculada que transforma el texto "#FFFFFF" al Color de Compose
    val colorFondo: Color
        get() = try {
            Color(android.graphics.Color.parseColor(colorHex))
        } catch (e: Exception) {
            Color.Gray // Color por defecto si hay un error de escritura en el JSON
        }

    val costoTexto: String
        get() = if (esGratis || costoMxn <= 0.0) {
            "Gratis"
        } else {
            "$${String.format(Locale.US, "%.2f", costoMxn)} $moneda"
        }

    val tieneCoordenadas: Boolean
        get() = latitud != null && longitud != null

    val fechaInicioParseada: OffsetDateTime?
        get() = try {
            if (fechaInicio.isBlank()) null else OffsetDateTime.parse(fechaInicio)
        } catch (_: Exception) {
            null
        }

    fun esVisibleParaUsuarios(): Boolean {
        return estado.isBlank() || estado.equals("publicado", ignoreCase = true)
    }
}
