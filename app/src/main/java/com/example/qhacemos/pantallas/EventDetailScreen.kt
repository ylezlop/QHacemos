package com.example.qhacemos.pantallas

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.qhacemos.datos.ResultadoEventos
import com.example.qhacemos.datos.cargarEventos
import com.example.qhacemos.modelo.Evento
import com.example.qhacemos.navigation.AppScreens
import java.util.Locale

@Composable
fun EventDetailScreen(
    eventoId: Long,
    navController: NavController
) {
    val context = LocalContext.current
    var evento by remember { mutableStateOf<Evento?>(null) }
    var cargandoEvento by remember { mutableStateOf(true) }
    var mensajeError by remember { mutableStateOf<String?>(null) }
    var mostrarDialogo by remember { mutableStateOf(false) }
    var intentoCarga by remember { mutableStateOf(0) }

    LaunchedEffect(eventoId, context, intentoCarga) {
        cargandoEvento = true
        mensajeError = null

        when (val resultado = cargarEventos(context)) {
            is ResultadoEventos.Exito -> {
                evento = resultado.eventos.find { it.id == eventoId }
            }

            is ResultadoEventos.Error -> {
                mensajeError = resultado.mensaje
                evento = resultado.eventosLocales.find { it.id == eventoId }
            }
        }
        cargandoEvento = false
    }

    if (cargandoEvento) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF03A9F4))
        }
        return
    }

    val eventoActual = evento
    if (eventoActual == null) {
        EstadoDetalleEvento(
            titulo = "Evento no encontrado",
            mensaje = mensajeError ?: "No encontramos la informacion de este evento.",
            textoAccion = "Reintentar",
            onAccion = { intentoCarga++ },
            textoSecundario = "Volver al inicio",
            onSecundaria = { navController.navigate(AppScreens.Home.route) }
        )
        return
    }

    if (!eventoActual.esVisibleParaUsuarios()) {
        EstadoDetalleEvento(
            titulo = "Este evento ya no esta disponible",
            mensaje = "El organizador pudo haberlo eliminado, pausado o enviado nuevamente a revision.",
            textoAccion = "Volver a la lista",
            onAccion = { navController.navigate(AppScreens.Home.route) }
        )
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(eventoActual.colorFondo)
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                mensajeError?.let { error ->
                    MensajeErrorDetalle(
                        mensaje = error,
                        onRetry = { intentoCarga++ }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Badge(eventoActual.categoria)
                    Badge(
                        eventoActual.costoTexto,
                        if (eventoActual.esGratis) Color(0xFF4CAF50) else Color(0xFFFF7A1A)
                    )
                    if (eventoActual.estado.isNotBlank()) {
                        Badge(eventoActual.estado, Color(0xFF607D8B))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = eventoActual.titulo,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )

                    TextButton(onClick = { mostrarDialogo = true }) {
                        Text("Calificar")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                InfoEvento("Fecha", eventoActual.fechaHora)
                InfoEvento("Lugar", eventoActual.ubicacion)
                if (eventoActual.direccionCompleta.isNotBlank()) {
                    InfoEvento("Direccion", eventoActual.direccionCompleta)
                }
                InfoEvento(
                    "Organiza",
                    eventoActual.organizadorNombre.ifBlank { "Organizador no disponible" }
                )
                if (eventoActual.contactoOrganizador.isNotBlank()) {
                    InfoEvento("Contacto", eventoActual.contactoOrganizador)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Descripcion", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = eventoActual.descripcion.ifBlank {
                        "Evento de categoria ${eventoActual.categoria}. No te lo pierdas."
                    },
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(18.dp))

                RatingEvento(eventoActual)

                Spacer(modifier = Modifier.height(20.dp))

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

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

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
fun EstadoDetalleEvento(
    titulo: String,
    mensaje: String,
    textoAccion: String,
    onAccion: () -> Unit,
    textoSecundario: String? = null,
    onSecundaria: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = mensaje,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(onClick = onAccion) {
                Text(textoAccion)
            }
            if (textoSecundario != null && onSecundaria != null) {
                TextButton(onClick = onSecundaria) {
                    Text(textoSecundario)
                }
            }
        }
    }
}

@Composable
fun MensajeErrorDetalle(
    mensaje: String,
    onRetry: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFFFEBEE),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = mensaje,
                color = Color(0xFFB3261E),
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRetry) {
                Text("Reintentar", color = Color(0xFFB3261E))
            }
        }
    }
}

@Composable
fun InfoEvento(
    etiqueta: String,
    valor: String
) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            text = etiqueta,
            color = Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Text(text = valor, color = Color.DarkGray, fontSize = 14.sp)
    }
}

@Composable
fun RatingEvento(evento: Evento) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF5F5F5))
            .padding(16.dp)
    ) {
        Column {
            Text("Calificacion de organizador", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (evento.organizadorTotalCalificaciones > 0) {
                        String.format(Locale.US, "%.1f", evento.organizadorCalificacionPromedio)
                    } else {
                        "-"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = Color(0xFFFF9800)
                )

                Column {
                    RatingStarsLectura(evento.organizadorCalificacionPromedio)
                    Text(
                        text = if (evento.organizadorTotalCalificaciones > 0) {
                            "${evento.organizadorTotalCalificaciones} valoraciones del organizador"
                        } else {
                            "Aun no hay valoraciones"
                        },
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun RatingStarsLectura(rating: Double) {
    Row {
        val estrellasLlenas = rating.toInt().coerceIn(0, 5)
        for (i in 1..5) {
            Icon(
                imageVector = if (i <= estrellasLlenas) Icons.Default.Star else Icons.Outlined.StarBorder,
                contentDescription = null,
                tint = Color(0xFFFFC107),
                modifier = Modifier.size(22.dp)
            )
        }
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
                    .makeText(context, "Calificacion enviada: $rating estrellas", Toast.LENGTH_SHORT)
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
