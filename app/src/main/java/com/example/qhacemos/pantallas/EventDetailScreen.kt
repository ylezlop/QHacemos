package com.example.qhacemos.pantallas

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.qhacemos.datos.GestorAsistencias
import com.example.qhacemos.datos.GestorAutenticacion
import com.example.qhacemos.datos.GestorEventosOrganizador
import com.example.qhacemos.datos.GestorMetricas
import com.example.qhacemos.datos.GestorReportes
import com.example.qhacemos.datos.ResultadoEventos
import com.example.qhacemos.datos.cargarEventos
import com.example.qhacemos.modelo.Evento
import com.example.qhacemos.modelo.PerfilUsuario
import com.example.qhacemos.navigation.AppScreens
import com.example.qhacemos.notificaciones.GestorRecordatorios
import kotlinx.coroutines.launch
import java.util.Locale
import com.example.qhacemos.notificaciones.NotificadorEventos

@Composable
fun EventDetailScreen(
    eventoId: Long,
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var evento by remember { mutableStateOf<Evento?>(null) }
    var cargandoEvento by remember { mutableStateOf(true) }
    var mensajeError by remember { mutableStateOf<String?>(null) }
    var mostrarDialogoCompartir by remember { mutableStateOf(false) }
    var asistiraEvento by remember { mutableStateOf(false) }
    var guardandoAsistencia by remember { mutableStateOf(false) }
    var intentoCarga by remember { mutableStateOf(0) }
    var vistaRegistrada by remember(eventoId) { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var perfilActual by remember { mutableStateOf<PerfilUsuario?>(null) }
    var mostrarDialogoReporte by remember { mutableStateOf(false) }
    var motivoReporte by remember { mutableStateOf("") }
    var enviandoReporte by remember { mutableStateOf(false) }
    var conteoReportes by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var mostrarConfirmacionEliminar by remember { mutableStateOf(false) }

    LaunchedEffect(eventoId) {
        perfilActual = GestorAutenticacion.cargarPerfilActual().getOrNull()
        if (perfilActual?.esAdmin == true) {
            conteoReportes = GestorReportes.obtenerConteoReportes(eventoId).getOrDefault(emptyMap())
        }
    }

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

        evento?.let { eventoEncontrado ->
            if (!vistaRegistrada && eventoEncontrado.esVisibleParaUsuarios()) {
                GestorMetricas.registrarVista(eventoEncontrado)
                evento = eventoEncontrado.copy(vistas = eventoEncontrado.vistas + 1)
                vistaRegistrada = true
            }
        }

        asistiraEvento = GestorAsistencias.asistiraAEvento(eventoId).getOrDefault(false)
        cargandoEvento = false
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                intentoCarga++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
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
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = eventoActual.titulo,
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(12.dp))

                InfoEvento("Fecha", eventoActual.fechaTexto)
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
                   val puedeInteractuarAsistencia = !eventoActual.yaOcurrio && !guardandoAsistencia

                    Button(
                        onClick = {
                            scope.launch {
                                guardandoAsistencia = true
                                if (asistiraEvento) {
                                    GestorAsistencias.eliminarAsistencia(eventoActual.id)
                                        .onSuccess {
                                            asistiraEvento = false
                                            GestorRecordatorios.cancelarRecordatorio(context, eventoActual.id)
                                            Toast.makeText(context, "Asistencia cancelada exitosamente", Toast.LENGTH_SHORT).show()
                                        }
                                        .onFailure { error ->
                                            Toast.makeText(
                                                context,
                                                error.message ?: "No se pudo cancelar tu asistencia",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                } else {
                                    val resultado = GestorAsistencias.registrarAsistencia(eventoActual)
                                    resultado
                                        .onSuccess {
                                            asistiraEvento = true
                                            GestorRecordatorios.programarRecordatorio(context, eventoActual)
                                            GestorMetricas.registrarGuardado(eventoActual)
                                            evento = eventoActual.copy(guardados = eventoActual.guardados + 1)
                                            Toast.makeText(context, "Evento agregado a tus asistencias", Toast.LENGTH_SHORT).show()
                                        }
                                        .onFailure { error ->
                                            Toast.makeText(
                                                context,
                                                error.message ?: "No se pudo registrar tu asistencia",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                }
                                guardandoAsistencia = false
                            }
                        },
                        enabled = puedeInteractuarAsistencia,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (asistiraEvento) Color.Gray else Color(0xFFFF7A1A)
                        )
                    ) {
                        Text(
                            when {
                                eventoActual.yaOcurrio -> "Evento pasado"
                                guardandoAsistencia -> "Procesando..."
                                asistiraEvento -> "¡Asistiré! ✓" // Texto indicativo de que está seleccionado
                                else -> "Asistiré"
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                GestorMetricas.registrarClick(eventoActual)
                                evento = eventoActual.copy(clicks = eventoActual.clicks + 1)
                            }
                            mostrarDialogoCompartir = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81D4FA))
                    ) {
                        Text("Compartir")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color.LightGray)
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "¿Notas algo mal? Reportar evento",
                    color = Color.Red,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { mostrarDialogoReporte = true }
                        .padding(vertical = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                )

                if (perfilActual?.esAdmin == true) {

                    Button(
                        onClick = {
                            val notificador = NotificadorEventos(context)
                            notificador.mostrarNotificacionBasica(
                                titulo = "Prueba de notificación",
                                mensaje = "Click para abrir: ${eventoActual.titulo}",
                                eventoId = eventoActual.id // Pasamos el ID del evento actual
                            )
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                    ) {
                        Text("Probar Notificación de este evento")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Panel de Administrador - Reportes", fontWeight = FontWeight.Bold, color = Color.Red)
                            Spacer(modifier = Modifier.height(8.dp))

                            if (conteoReportes.isEmpty()) {
                                Text("No hay reportes para este evento.", fontSize = 14.sp, color = Color.DarkGray)
                            } else {
                                conteoReportes.forEach { (motivo, cantidad) ->
                                    Text("- $motivo: $cantidad", fontSize = 14.sp, color = Color.DarkGray)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { mostrarConfirmacionEliminar = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Red,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("ELIMINAR EVENTO", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (mostrarDialogoCompartir) {
        DialogoCompartirEvento(
            evento = eventoActual,
            onDismiss = { mostrarDialogoCompartir = false }
        )
    }

    if (mostrarDialogoReporte) {
        val opciones = listOf("Información incorrecta", "Fraude", "Cancelación", "Contenido inapropiado")

        AlertDialog(
            onDismissRequest = {
                if (!enviandoReporte) mostrarDialogoReporte = false
            },
            title = { Text("Reportar Evento") },
            text = {
                Column {
                    Text("Selecciona el motivo del reporte:")
                    Spacer(modifier = Modifier.height(8.dp))
                    opciones.forEach { opcion ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { motivoReporte = opcion }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = motivoReporte == opcion,
                                onClick = { motivoReporte = opcion },
                                colors = RadioButtonDefaults.colors(selectedColor = Color.Red)
                            )
                            Text(text = opcion, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            enviandoReporte = true
                            GestorReportes.enviarReporte(
                                eventoId = eventoId,
                                usuarioId = perfilActual?.id ?: "usuario_anonimo",
                                motivo = motivoReporte
                            )
                            enviandoReporte = false
                            mostrarDialogoReporte = false
                            motivoReporte = ""
                            Toast.makeText(context, "Reporte enviado exitosamente", Toast.LENGTH_SHORT).show()

                            if (perfilActual?.esAdmin == true) {
                                conteoReportes = GestorReportes.obtenerConteoReportes(eventoId).getOrDefault(emptyMap())
                            }
                        }
                    },
                    enabled = motivoReporte.isNotEmpty() && !enviandoReporte,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    if (enviandoReporte) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Enviar Reporte")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { mostrarDialogoReporte = false },
                    enabled = !enviandoReporte
                ) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }

    if (mostrarConfirmacionEliminar) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacionEliminar = false },
            title = { Text("¿Eliminar evento?") },
            text = { Text("Esta acción ocultará permanentemente el evento para los usuarios de la plataforma.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            GestorEventosOrganizador.eliminarEvento(eventoActual.id)
                                .onSuccess { exito ->
                                    if (exito) {
                                        Toast.makeText(context, "Evento eliminado con éxito", Toast.LENGTH_SHORT).show()
                                        mostrarConfirmacionEliminar = false
                                        // Limpiamos la pila y regresamos al Home para no quedarnos en una pantalla fantasma
                                        navController.navigate(AppScreens.Home.route) {
                                            popUpTo(AppScreens.Home.route) { inclusive = true }
                                        }
                                    } else {
                                        Toast.makeText(context, "No se pudo eliminar el evento", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .onFailure { error ->
                                    Toast.makeText(context, error.message ?: "Error de red al intentar eliminar", Toast.LENGTH_SHORT).show()
                                    mostrarConfirmacionEliminar = false
                                }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacionEliminar = false }) {
                    Text("Cancelar")
                }
            }
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

@Composable
fun DialogoCompartirEvento(
    evento: Evento,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val link = evento.linkCompartir()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Compartir evento") },
        text = {
            Column {
                Text(
                    text = "Link para compartir evento",
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFF5F5F5),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = link,
                        modifier = Modifier.padding(12.dp),
                        color = Color(0xFF0277BD),
                        fontSize = 13.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                compartirEvento(context, evento)
                onDismiss()
            }) {
                Text("Compartir")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                copiarLinkEvento(context, link)
                onDismiss()
            }) {
                Text("Copiar link")
            }
        }
    )
}

fun Evento.linkCompartir(): String {
    return "qhacemos://evento/$id"
}

fun compartirEvento(context: Context, evento: Evento) {
    val texto = buildString {
        appendLine(evento.titulo)
        if (evento.fechaTexto.isNotBlank()) appendLine(evento.fechaTexto)
        if (evento.ubicacion.isNotBlank()) appendLine(evento.ubicacion)
        appendLine()
        append("Abrir en QHacemos: ${evento.linkCompartir()}")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, evento.titulo)
        putExtra(Intent.EXTRA_TEXT, texto)
    }

    context.startActivity(Intent.createChooser(intent, "Compartir evento"))
}

fun copiarLinkEvento(context: Context, link: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Link del evento", link))
    Toast.makeText(context, "Link copiado", Toast.LENGTH_SHORT).show()
}
