package com.example.qhacemos.pantallas

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.qhacemos.datos.GestorAsistencias
import com.example.qhacemos.datos.GestorAutenticacion
import com.example.qhacemos.datos.GestorSuscripciones
import com.example.qhacemos.datos.ResultadoEventos
import com.example.qhacemos.datos.cargarEventos
import com.example.qhacemos.modelo.Evento
import com.example.qhacemos.modelo.PerfilUsuario
import com.example.qhacemos.navigation.AppScreens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun CuentaScreen(
    perfil: PerfilUsuario,
    navController: NavController,
    scope: CoroutineScope,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    var eventosUsuario by remember { mutableStateOf<List<Evento>>(emptyList()) }
    var calificacionesEventoIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var cargandoEventosUsuario by remember { mutableStateOf(true) }
    var mensajeEventosUsuario by remember { mutableStateOf<String?>(null) }
    var eventoParaCalificar by remember { mutableStateOf<Evento?>(null) }
    var intentoEventosUsuario by remember { mutableStateOf(0) }

    var esSuscrito by remember { mutableStateOf(false) }
    var mostrarModalSuscripcion by remember { mutableStateOf(false) }

    LaunchedEffect(perfil.id, intentoEventosUsuario) {
        if (perfil.esAdmin) {
            cargandoEventosUsuario = false
            return@LaunchedEffect
        }

        cargandoEventosUsuario = true
        mensajeEventosUsuario = null

        GestorSuscripciones.verificarSuscripcionActiva(perfil.id)
            .onSuccess { esSuscrito = it }

        val resultadoAsistencias = GestorAsistencias.cargarAsistenciasUsuario()
        val asistencias = resultadoAsistencias.getOrElse { error ->
            mensajeEventosUsuario = error.message ?: "No se pudieron cargar tus eventos."
            emptyList()
        }

        val idsAsistidos = asistencias
            .filter { it.estado != "cancelado" }
            .map { it.eventoId }
            .toSet()

        val eventos = when (val resultado = cargarEventos(context)) {
            is ResultadoEventos.Exito -> resultado.eventos
            is ResultadoEventos.Error -> {
                mensajeEventosUsuario = resultado.mensaje
                resultado.eventosLocales
            }
        }

        eventosUsuario = eventos
            .filter { it.id in idsAsistidos }
            .sortedBy { it.fechaInicioParseada }

        calificacionesEventoIds = GestorAsistencias.cargarCalificacionesUsuario()
            .getOrDefault(emptyList())
            .map { it.eventoId }
            .toSet()

        cargandoEventosUsuario = false
    }

    DisposableEffect(lifecycleOwner, perfil.esAdmin) {
        val observer = LifecycleEventObserver { _, event ->
            if (!perfil.esAdmin && event == Lifecycle.Event.ON_RESUME) {
                intentoEventosUsuario++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        bottomBar = {
            BarraNavegacionInferior(
                navController = navController,
                rutaActual = backStackEntry?.destination?.route
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF4F7FB))
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Mi cuenta",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = perfil.nombre.ifBlank { "Usuario sin nombre" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(perfil.email, color = Color.Gray)
                    Spacer(modifier = Modifier.height(14.dp))
                    EtiquetaRol(perfil.rolLegible, perfil.esAdmin)
                }
            }

            if (!perfil.esAdmin) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    tonalElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = if (esSuscrito) "Estado: suscripcion activa" else "Estado: cuenta gratuita",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (esSuscrito) Color(0xFF1B5E20) else Color.Gray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Con el Plan Premium puedes publicar eventos de forma ilimitada en Xalapa sin cargos por posteo individual.",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                        if (!esSuscrito) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { mostrarModalSuscripcion = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Contratar suscripcion mensual")
                            }
                        }
                    }
                }

                Button(
                    onClick = { navController.navigate(AppScreens.CrearEvento.route) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Publicar nuevo evento")
                }

                Button(
                    onClick = { navController.navigate(AppScreens.MisEventos.route) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Mis eventos")
                }

                SeccionEventosUsuario(
                    cargando = cargandoEventosUsuario,
                    mensajeError = mensajeEventosUsuario,
                    eventos = eventosUsuario,
                    calificacionesEventoIds = calificacionesEventoIds,
                    onEventoClick = { evento ->
                        navController.navigate("${AppScreens.EventDetail.route}/${evento.id}")
                    },
                    onCalificarClick = { evento ->
                        eventoParaCalificar = evento
                    }
                )
            }

            Button(
                onClick = {
                    scope.launch {
                        GestorAutenticacion.cerrarSesion()
                        onLogout()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cerrar sesion")
            }
        }
    }

    eventoParaCalificar?.let { evento ->
        DialogoCalificarOrganizadorPerfil(
            evento = evento,
            onDismiss = { eventoParaCalificar = null },
            onEnviar = { rating ->
                scope.launch {
                    GestorAsistencias.calificarOrganizador(evento, rating)
                        .onSuccess {
                            Toast.makeText(context, "Calificacion registrada", Toast.LENGTH_SHORT).show()
                            eventoParaCalificar = null
                            intentoEventosUsuario++
                        }
                        .onFailure { error ->
                            Toast.makeText(
                                context,
                                error.message ?: "No se pudo registrar la calificacion",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }
        )
    }

    if (mostrarModalSuscripcion) {
        AlertDialog(
            onDismissRequest = { mostrarModalSuscripcion = false },
            title = { Text("Suscripcion mensual creador") },
            text = {
                Column {
                    Text("Beneficios principales:", fontWeight = FontWeight.Bold)
                    Text("- Publicacion de eventos de forma ilimitada durante 30 dias.")
                    Text("- Tus publicaciones pasan a revision con prioridad alta.")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Precio: $299.00 MXN al mes",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val resultado = GestorSuscripciones.contratarSuscripcionMensual(perfil.id, 299.0)
                            if (resultado.isSuccess) {
                                esSuscrito = true
                                mostrarModalSuscripcion = false
                                Toast.makeText(context, "Suscripcion activa.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) {
                    Text("Confirmar pago")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarModalSuscripcion = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun AvisoCuenta(texto: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFE3F2FD)
    ) {
        Text(
            text = texto,
            modifier = Modifier.padding(16.dp),
            color = Color(0xFF0D47A1),
            fontSize = 13.sp
        )
    }
}

@Composable
fun EtiquetaRol(
    rol: String,
    esAdmin: Boolean
) {
    Box(
        modifier = Modifier
            .background(
                color = if (esAdmin) Color(0xFFFFF3E0) else Color(0xFFE8F5E9),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = rol,
            color = if (esAdmin) Color(0xFFE65100) else Color(0xFF1B5E20),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

@Composable
fun SeccionEventosUsuario(
    cargando: Boolean,
    mensajeError: String?,
    eventos: List<Evento>,
    calificacionesEventoIds: Set<Long>,
    onEventoClick: (Evento) -> Unit,
    onCalificarClick: (Evento) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Eventos", fontWeight = FontWeight.Bold, fontSize = 18.sp)

            mensajeError?.let {
                Text(it, color = Color(0xFFB3261E), fontSize = 13.sp)
            }

            if (cargando) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            if (eventos.isEmpty()) {
                Text(
                    text = "Cuando marques Asistire en un evento, aparecera aqui.",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
                return@Column
            }

            val eventosActivos = eventos.filter { !it.yaOcurrio }
            val eventosPasados = eventos.filter { it.yaOcurrio }

            if (eventosActivos.isNotEmpty()) {
                Text("Activos", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                eventosActivos.forEach { evento ->
                    TarjetaEventoCuenta(
                        evento = evento,
                        etiqueta = "Pendiente",
                        onEventoClick = onEventoClick
                    )
                }
            }

            if (eventosPasados.isNotEmpty()) {
                Text("Pasados", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                eventosPasados.forEach { evento ->
                    TarjetaEventoCuenta(
                        evento = evento,
                        etiqueta = if (evento.id in calificacionesEventoIds) "Calificado" else "Por calificar",
                        onEventoClick = onEventoClick,
                        onCalificarClick = if (evento.id in calificacionesEventoIds) {
                            null
                        } else {
                            { onCalificarClick(evento) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TarjetaEventoCuenta(
    evento: Evento,
    etiqueta: String,
    onEventoClick: (Evento) -> Unit,
    onCalificarClick: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFC)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(evento.titulo, fontWeight = FontWeight.Bold)
                    Text(evento.fechaTexto, color = Color.Gray, fontSize = 12.sp)
                    Text(
                        evento.organizadorNombre.ifBlank { "Organizador no disponible" },
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                Text(etiqueta, color = Color(0xFF0277BD), fontSize = 12.sp)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onEventoClick(evento) }) {
                    Text("Ver evento")
                }
                if (onCalificarClick != null) {
                    Button(onClick = onCalificarClick) {
                        Text("Calificar organizador")
                    }
                }
            }
        }
    }
}

@Composable
fun DialogoCalificarOrganizadorPerfil(
    evento: Evento,
    onDismiss: () -> Unit,
    onEnviar: (Int) -> Unit
) {
    var rating by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Calificar organizador") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = evento.organizadorNombre.ifBlank { "Organizador" },
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                RatingStars(rating) {
                    rating = it
                }
            }
        },
        confirmButton = {
            Button(
                enabled = rating in 1..5,
                onClick = { onEnviar(rating) }
            ) {
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
