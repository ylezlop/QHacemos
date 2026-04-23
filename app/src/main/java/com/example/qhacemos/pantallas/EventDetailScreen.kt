package com.example.qhacemos.pantallas

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.qhacemos.datos.leerEventos
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.ui.graphics.toArgb

@Composable
fun EventDetailScreen(
    eventoId: Int,
    navController: NavController
) {
    val context = LocalContext.current
    val listaEventos = remember { leerEventos(context) }
    val evento = listaEventos.find { it.id == eventoId }

    var mostrarDialogo by remember { mutableStateOf(false) }

    if (evento == null) {
        Text("Evento no encontrado")
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // 🔶 HEADER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(Color(evento.colorFondo.toArgb()))
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
            }
        }

        // 🔳 CONTENIDO
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            // 🔹 BADGES
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Badge("${evento.categoria}")
                if (evento.esGratis) Badge("Gratis", Color(0xFF4CAF50))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 🔹 TITULO + CALIFICAR
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = evento.titulo,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )

                TextButton(onClick = { mostrarDialogo = true }) {
                    Text("Calificar")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 🔹 INFO
            Text("📅 ${evento.fechaHora}", color = Color.Gray)
            Text("📍 ${evento.ubicacion}", color = Color.Gray)

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Evento de categoría ${evento.categoria}. ¡No te lo pierdas!",
                color = Color.DarkGray
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 🔹 BOTONES
            Row(modifier = Modifier.fillMaxWidth()) {

                Button(
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7A1A))
                ) {
                    Text("Guardar evento")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81D4FA))
                ) {
                    Text("Compartir")
                }
            }
        }
    }

    // ⭐ DIALOGO
    if (mostrarDialogo) {
        DialogoCalificacion(
            onDismiss = { mostrarDialogo = false }
        )
    }
}

@Composable
fun Badge(text: String, color: Color = Color(0xFF81D4FA)) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
fun RatingStars(
    rating: Int,
    onRatingChanged: (Int) -> Unit
) {
    Row {
        for (i in 1..5) {
            Icon(
                imageVector = if (i <= rating) Icons.Default.Star else Icons.Outlined.StarBorder,
                contentDescription = null,
                tint = Color(0xFFFFC107),
                modifier = Modifier
                    .size(32.dp)
                    .clickable { onRatingChanged(i) }
            )
        }
    }
}

@Composable
fun DialogoCalificacion(
    onDismiss: () -> Unit
) {
    var rating by remember { mutableStateOf(0) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Calificar evento") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                RatingStars(rating) {
                    rating = it
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                Toast
                    .makeText(context, "Calificación enviada: $rating ⭐", Toast.LENGTH_SHORT)
                    .show()
                onDismiss()
            }) {
                Text("Enviar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}