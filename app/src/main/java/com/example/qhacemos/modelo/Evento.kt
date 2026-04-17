package com.example.qhacemos.modelo

import androidx.compose.ui.graphics.Color

data class Evento(
    val id: Int,
    val titulo: String,
    val fechaHora: String,
    val ubicacion: String,
    val categoria: String,
    val esDestacado: Boolean,
    val esGratis: Boolean,
    val colorHex: String
) {
    // Propiedad calculada que transforma el texto "#FFFFFF" al Color de Compose
    val colorFondo: Color
        get() = try {
            Color(android.graphics.Color.parseColor(colorHex))
        } catch (e: Exception) {
            Color.Gray // Color por defecto si hay un error de escritura en el JSON
        }
}