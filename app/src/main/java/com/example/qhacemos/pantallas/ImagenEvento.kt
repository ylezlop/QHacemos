package com.example.qhacemos.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
import com.example.qhacemos.modelo.Evento

@Composable
fun ImagenEvento(
    evento: Evento,
    modifier: Modifier = Modifier
) {
    val imagen = evento.imagenPrincipalUrl.ifBlank { evento.imagenes.firstOrNull().orEmpty() }

    if (imagen.isBlank()) {
        Box(modifier = modifier.background(evento.colorFondo))
        return
    }

    SubcomposeAsyncImage(
        model = imagen,
        contentDescription = evento.titulo,
        modifier = modifier.background(evento.colorFondo),
        contentScale = ContentScale.Crop,
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(evento.colorFondo),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        },
        error = {
            Box(modifier = Modifier.fillMaxSize().background(evento.colorFondo))
        }
    )
}
