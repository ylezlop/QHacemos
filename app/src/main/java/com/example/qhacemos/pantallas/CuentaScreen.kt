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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.qhacemos.datos.GestorAsistencias
import com.example.qhacemos.datos.GestorAutenticacion
import com.example.qhacemos.datos.ResultadoEventos
import com.example.qhacemos.datos.cargarEventos
import com.example.qhacemos.modelo.Evento
import com.example.qhacemos.modelo.PerfilUsuario
import com.example.qhacemos.navigation.AppScreens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.ui.unit.sp
import com.example.qhacemos.datos.GestorEventosOrganizador
import com.example.qhacemos.datos.GestorSuscripciones

@Composable
fun CuentaScreen(
    perfil: PerfilUsuario,
    navController: NavController,
    scope: CoroutineScope,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    var eventosUsuario by remember { mutableStateOf<List<Evento>>(emptyList()) }
    var calificacionesEventoIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var cargandoEventosUsuario by remember { mutableStateOf(true) }
    var mensajeEventosUsuario by remember { mutableStateOf<String?>(null) }
    var eventoParaCalificar by remember { mutableStateOf<Evento?>(null) }
    var intentoEventosUsuario by remember { mutableStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current

    var listaEventosPropios by remember { mutableStateOf<List<Evento>>(emptyList()) }
    var esSuscrito by remember { mutableStateOf(false) }
    var cargandoGestion by remember { mutableStateOf(true) }

    var mostrarModalSuscripcion by remember { mutableStateOf(false) }
    var eventoADestacar by remember { mutableStateOf<Evento?>(null) }
    var periodoDestacar by remember { mutableStateOf("1 semana") }
    var eventoParaMetricas by remember { mutableStateOf<Evento?>(null) }




    LaunchedEffect(perfil.id, intentoEventosUsuario) {
        if (perfil.esAdmin) {
            cargandoEventosUsuario = false
            cargandoGestion = true

            val resultadoSuscripcion = GestorSuscripciones.verificarSuscripcionActiva(perfil.id)
            if (resultadoSuscripcion.isSuccess) {
                esSuscrito = resultadoSuscripcion.getOrDefault(false)
            }

            if (!perfil.esAdmin) {
                val resultadoEventos = GestorEventosOrganizador.obtenerEventosPorOrganizador(perfil.id)
                if (resultadoEventos.isSuccess) {
                    listaEventosPropios = resultadoEventos.getOrDefault(emptyList())
                }
            }

            cargandoGestion = false
            return@LaunchedEffect
        }

        cargandoEventosUsuario = true
        mensajeEventosUsuario = null

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

            // Sección interactiva para creadores de eventos
            if (!perfil.esAdmin) {
                // CU-16: Tarjeta informativa del estado de la suscripción
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    tonalElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = if (esSuscrito) "Estado: Suscripción Activa" else "Estado: Cuenta Gratuita",
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
                                Text("Contratar Suscripción Mensual")
                            }
                        }
                    }
                }

                // CU-15: Botón de acceso rápido para crear un nuevo evento
                Button(
                    onClick = { navController.navigate(AppScreens.CrearEvento.route) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Publicar Nuevo Evento (CU-15)")
                }

                // CU-17 y CU-18: Historial de autoría y analíticas
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Mis Eventos Creados", fontWeight = FontWeight.Bold, fontSize = 18.sp)

                        if (cargandoGestion) {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else if (listaEventosPropios.isEmpty()) {
                            Text(
                                text = "Aún no has creado eventos en la plataforma.",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        } else {
                            listaEventosPropios.forEach { evento ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(evento.titulo, fontWeight = FontWeight.Bold)
                                        Text("Fecha: ${evento.fechaInicio} | Ubicación: ${evento.ubicacion}", fontSize = 12.sp, color = Color.Gray)
                                        Text("Estado: ${evento.estado.replace("_", " ").uppercase()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0277BD))

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            // CU-17: Destacar publicación con validación si ya ocurrió
                                            OutlinedButton(
                                                onClick = {
                                                    if (evento.yaOcurrio) {
                                                        Toast.makeText(context, "Error: No se puede destacar un evento que ya ocurrió", Toast.LENGTH_LONG).show()
                                                    } else {
                                                        eventoADestacar = evento
                                                    }
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(if (evento.esDestacado) "★ ¡Destacado!" else "Destacar")
                                            }

                                            // CU-18: Panel de interacciones
                                            OutlinedButton(
                                                onClick = { eventoParaMetricas = evento },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Estadísticas")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!perfil.esAdmin) {
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
                            Toast
                                .makeText(context, "Calificacion registrada", Toast.LENGTH_SHORT)
                                .show()
                            eventoParaCalificar = null
                            intentoEventosUsuario++
                        }
                        .onFailure { error ->
                            Toast
                                .makeText(
                                    context,
                                    error.message ?: "No se pudo registrar la calificacion",
                                    Toast.LENGTH_SHORT
                                )
                                .show()
                        }
                }
            }
        )
    }

    // Modales correspondientes a los casos de uso CU-16, CU-17 y CU-18

    // MODAL CU-16: Contratación de Suscripción Mensual
    if (mostrarModalSuscripcion) {
        AlertDialog(
            onDismissRequest = { mostrarModalSuscripcion = false },
            title = { Text("Suscripción Mensual Creador") },
            text = {
                Column {
                    Text("Beneficios principales:", fontWeight = FontWeight.Bold)
                    Text("• Publicación de eventos de forma ilimitada durante 30 días.")
                    Text("• Tus publicaciones pasan a revisión con prioridad alta.")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Precio: $299.00 MXN al mes", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
                                Toast.makeText(context, "Suscripción Activa. ¡Disfruta tus beneficios!", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) { Text("Confirmar Pago") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarModalSuscripcion = false }) { Text("Cancelar") }
            }
        )
    }

    // CU-17
    eventoADestacar?.let { evento ->
        AlertDialog(
            onDismissRequest = { eventoADestacar = null },
            title = { Text("Destacar Evento: ${evento.titulo}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Selecciona el tiempo de promoción para aparecer destacado:")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = periodoDestacar == "1 semana", onClick = { periodoDestacar = "1 semana" })
                        Text("1 Semana ($150.00 MXN)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = periodoDestacar == "1 mes", onClick = { periodoDestacar = "1 mes" })
                        Text("1 Mes ($450.00 MXN)")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val costo = if (periodoDestacar == "1 semana") 150.0 else 450.0
                        scope.launch {
                            val resultado = GestorEventosOrganizador.destacarEvento(evento.id, periodoDestacar, costo)
                            if (resultado.isSuccess) {
                                Toast.makeText(context, "El evento ahora se encuentra destacado", Toast.LENGTH_LONG).show()
                                listaEventosPropios = listaEventosPropios.map {
                                    if (it.id == evento.id) it.copy(esDestacado = true, estado = "destacado") else it
                                }
                                eventoADestacar = null
                            }
                        }
                    }
                ) { Text("Pagar Promoción") }
            },
            dismissButton = {
                TextButton(onClick = { eventoADestacar = null }) { Text("Volver") }
            }
        )
    }

    // MODAL CU-18: Visor de métricas cuantitativas
    eventoParaMetricas?.let { evento ->
        AlertDialog(
            onDismissRequest = { eventoParaMetricas = null },
            title = { Text("Métricas e Interacciones") },
            text = {
                if (evento.vistas == 0 && evento.clicks == 0 && evento.guardados == 0) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("Aún no hay interacciones", fontWeight = FontWeight.Medium, color = Color.Gray)
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Rendimiento acumulado de la publicación:", fontSize = 13.sp, color = Color.Gray)

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Visualizaciones:", fontWeight = FontWeight.SemiBold)
                                Text("${evento.vistas}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Clics en el detalle:", fontWeight = FontWeight.SemiBold)
                                Text("${evento.clicks}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Veces guardado:", fontWeight = FontWeight.SemiBold)
                                Text("${evento.guardados}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { eventoParaMetricas = null }) { Text("Cerrar") }
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
