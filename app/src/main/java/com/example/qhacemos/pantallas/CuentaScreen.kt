package com.example.qhacemos.pantallas

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    var procesandoSuscripcion by remember { mutableStateOf(false) }

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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            CabeceraCuenta()

            if (perfil.esAdmin) {
                GrupoAccionesCuenta(titulo = "Administracion") {
                    OpcionCuenta(
                        icono = Icons.Default.AdminPanelSettings,
                        titulo = "Validar eventos",
                        descripcion = "Revisa publicaciones pendientes",
                        onClick = { navController.navigate(AppScreens.ValidarEventos.route) }
                    )
                    OpcionCuenta(
                        icono = Icons.Default.QueryStats,
                        titulo = "Metricas del sistema",
                        descripcion = "Consulta actividad y uso de la app",
                        onClick = { navController.navigate(AppScreens.MetricasSistema.route) }
                    )
                    OpcionCuenta(
                        icono = Icons.Default.Groups,
                        titulo = "Gestionar usuarios",
                        descripcion = "Administra cuentas y estados",
                        onClick = { navController.navigate(AppScreens.GestionUsuarios.route) },
                        mostrarDivisor = false
                    )
                }
            }

            if (!perfil.esAdmin) {
                PanelSuscripcionCuenta(
                    esSuscrito = esSuscrito,
                    onContratarClick = { mostrarModalSuscripcion = true }
                )

                GrupoAccionesCuenta(titulo = "Tus herramientas") {
                    OpcionCuenta(
                        icono = Icons.Default.Event,
                        titulo = "Mis eventos",
                        descripcion = "Edita, elimina o revisa tus publicaciones",
                        onClick = { navController.navigate(AppScreens.MisEventos.route) }
                    )
                    OpcionCuenta(
                        icono = Icons.Default.Star,
                        titulo = "Destacar un evento",
                        descripcion = "Impulsa uno de tus eventos publicados",
                        onClick = { navController.navigate(AppScreens.MisEventos.route) },
                        mostrarDivisor = false
                    )
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

            OutlinedButton(
                onClick = {
                    scope.launch {
                        GestorAutenticacion.cerrarSesion()
                        onLogout()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB3261E))
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
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
            containerColor = Color.White,
            shape = RoundedCornerShape(22.dp),
            title = {
                Text(
                    "Premium para creadores",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE0F2FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color(0xFF0284C7))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Publicacion ilimitada", fontWeight = FontWeight.Bold)
                            Text("Durante 30 dias", color = Color(0xFF64748B), fontSize = 13.sp)
                        }
                    }
                    Text(
                        "Tus eventos pasan a revision con prioridad alta y no pagas por publicacion individual.",
                        color = Color(0xFF64748B),
                        fontSize = 13.sp
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF8FAFC)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Mensualidad", color = Color(0xFF64748B), fontSize = 13.sp)
                            Text("$299.00 MXN", fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !procesandoSuscripcion,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03A9F4)),
                    onClick = {
                        scope.launch {
                            procesandoSuscripcion = true
                            val resultado = GestorSuscripciones.contratarSuscripcionMensual(perfil.id, 299.0)
                            resultado
                                .onSuccess {
                                    esSuscrito = true
                                    mostrarModalSuscripcion = false
                                    Toast.makeText(context, "Suscripcion activa.", Toast.LENGTH_LONG).show()
                                }
                                .onFailure { error ->
                                    Toast.makeText(
                                        context,
                                        error.message ?: "No se pudo procesar la suscripcion",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            procesandoSuscripcion = false
                        }
                    }
                ) {
                    if (procesandoSuscripcion) {
                        CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Contratar")
                    }
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
fun CabeceraCuenta() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF0EA5E9),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Yael",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }
    }
}

@Composable
fun PanelSuscripcionCuenta(
    esSuscrito: Boolean,
    onContratarClick: () -> Unit
) {
    val colorEstado = if (esSuscrito) Color(0xFF15803D) else Color(0xFF64748B)
    val fondoIcono = if (esSuscrito) Color(0xFFE7F8EE) else Color(0xFFE0F2FE)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(fondoIcono),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color(0xFF0284C7))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (esSuscrito) "Plan Premium activo" else "Cuenta gratuita",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = if (esSuscrito) {
                            "Puedes publicar eventos sin limite mensual."
                        } else {
                            "Publica eventos de manera ilimitada con Premium"
                        },
                        color = Color(0xFF64748B),
                        fontSize = 13.sp
                    )
                }
                Text(
                    text = if (esSuscrito) "Activo" else "Gratis",
                    color = colorEstado,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            if (!esSuscrito) {
                Button(
                    onClick = onContratarClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03A9F4))
                ) {
                    Text("Contratar suscripción")
                }
            }
        }
    }
}

@Composable
fun GrupoAccionesCuenta(
    titulo: String,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = titulo,
            color = Color(0xFF334155),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                contenido()
            }
        }
    }
}

@Composable
fun OpcionCuenta(
    icono: ImageVector,
    titulo: String,
    descripcion: String,
    onClick: () -> Unit,
    mostrarDivisor: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0F2FE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icono, contentDescription = null, tint = Color(0xFF0284C7))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(descripcion, color = Color(0xFF64748B), fontSize = 12.sp)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8))
        }
        if (mostrarDivisor) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 70.dp),
                color = Color(0xFFE2E8F0)
            )
        }
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
        color = Color.White,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Mi agenda", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Eventos a los que asistiras o asististe", color = Color(0xFF64748B), fontSize = 12.sp)
                }
                if (eventos.isNotEmpty()) {
                    Text(
                        text = eventos.size.toString(),
                        color = Color(0xFF0284C7),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

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
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC)
                ) {
                    Text(
                        text = "Cuando marques Asistire en un evento, aparecera aqui.",
                        modifier = Modifier.padding(16.dp),
                        color = Color(0xFF64748B),
                        fontSize = 13.sp
                    )
                }
                return@Column
            }

            val eventosActivos = eventos.filter { !it.yaOcurrio }
            val eventosPasados = eventos.filter { it.yaOcurrio }

            if (eventosActivos.isNotEmpty()) {
                Text("Proximos", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF334155))
                eventosActivos.forEach { evento ->
                    TarjetaEventoCuenta(
                        evento = evento,
                        etiqueta = "Proximo",
                        onEventoClick = onEventoClick
                    )
                }
            }

            if (eventosPasados.isNotEmpty()) {
                Text("Pasados", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF334155))
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEventoClick(evento) },
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
                    Text(evento.titulo, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Text(evento.fechaTexto, color = Color(0xFF64748B), fontSize = 12.sp)
                    Text(
                        evento.organizadorNombre.ifBlank { "Organizador no disponible" },
                        color = Color(0xFF64748B),
                        fontSize = 12.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .background(Color(0xFFE0F2FE), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(etiqueta, color = Color(0xFF0277BD), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onCalificarClick != null) {
                    Button(
                        onClick = onCalificarClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03A9F4))
                    ) {
                        Text("Calificar organizador")
                    }
                } else {
                    TextButton(onClick = { onEventoClick(evento) }) {
                        Text("Ver detalle")
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
