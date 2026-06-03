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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.qhacemos.datos.GestorEventosOrganizador
import com.example.qhacemos.modelo.Evento
import com.example.qhacemos.modelo.PerfilUsuario
import com.example.qhacemos.navigation.AppScreens
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisEventosScreen(
    navController: NavController,
    perfilActual: PerfilUsuario?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var eventos by remember { mutableStateOf<List<Evento>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var mensajeError by remember { mutableStateOf<String?>(null) }
    var intentoCarga by remember { mutableStateOf(0) }
    var eventoAEliminar by remember { mutableStateOf<Evento?>(null) }
    var eliminando by remember { mutableStateOf(false) }
    var eventoADestacar by remember { mutableStateOf<Evento?>(null) }
    var destacando by remember { mutableStateOf(false) }

    LaunchedEffect(perfilActual?.id, intentoCarga) {
        val perfil = perfilActual
        if (perfil == null) {
            cargando = false
            mensajeError = "Debes iniciar sesion para ver tus eventos."
            return@LaunchedEffect
        }

        cargando = true
        mensajeError = null

        GestorEventosOrganizador.obtenerEventosPorOrganizador(perfil.id)
            .onSuccess { lista ->
                eventos = lista.sortedWith(
                    compareBy<Evento> { it.yaOcurrio }
                        .thenBy { it.fechaInicioParseada }
                )
            }
            .onFailure { error ->
                mensajeError = error.message ?: "No se pudieron cargar tus eventos."
                eventos = emptyList()
            }

        cargando = false
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis eventos") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF4F7FB),
                    titleContentColor = Color(0xFF0F172A),
                    navigationIconContentColor = Color(0xFF0F172A)
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                    }
                }
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Tus publicaciones", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF0F172A))
                    Text(
                        "Consulta tus eventos, edita informacion y destaca publicaciones.",
                        color = Color(0xFF64748B),
                        fontSize = 13.sp
                    )
                }
            }

            Button(
                onClick = { navController.navigate(AppScreens.CrearEvento.route) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03A9F4))
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text("Crear evento")
            }

            mensajeError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }

            if (cargando) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            if (eventos.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Aun no has creado eventos.", fontWeight = FontWeight.Bold)
                        Text("Cuando publiques uno nuevo aparecera aqui.", color = Color(0xFF64748B), fontSize = 13.sp)
                    }
                }
                return@Column
            }

            eventos.forEach { evento ->
                TarjetaEventoPropio(
                    evento = evento,
                    onEditar = {
                        navController.navigate("${AppScreens.EditarEvento.route}/${evento.id}")
                    },
                    onEliminar = {
                        eventoAEliminar = evento
                    },
                    onDestacar = {
                        eventoADestacar = evento
                    },
                    onVer = {
                        navController.navigate("${AppScreens.EventDetail.route}/${evento.id}/propio")
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    eventoAEliminar?.let { evento ->
        AlertDialog(
            onDismissRequest = { eventoAEliminar = null },
            containerColor = Color.White,
            shape = RoundedCornerShape(22.dp),
            title = { Text("Eliminar publicacion", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "El evento '${evento.titulo}' pasara a estado inactivo y ya no sera visible para usuarios.",
                    color = Color(0xFF64748B)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            GestorEventosOrganizador.eliminarEvento(evento.id)
                                .onSuccess { exito ->
                                    if (exito) {
                                        Toast.makeText(context, "Publicación eliminada correctamente", Toast.LENGTH_SHORT).show()
                                        eventoAEliminar = null
                                        intentoCarga++ // Dispara el LaunchedEffect para refrescar la lista visualmente
                                    } else {
                                        Toast.makeText(context, "No se pudo completar la acción", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .onFailure { error ->
                                    Toast.makeText(context, error.message ?: "Fallo de conexión", Toast.LENGTH_SHORT).show()
                                    eventoAEliminar = null
                                }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E))
                ) {
                    Text("Confirmar eliminación")
                }
            },
            dismissButton = {
                TextButton(onClick = { eventoAEliminar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    eventoADestacar?.let { evento ->
        AlertDialog(
            onDismissRequest = { if (!destacando) eventoADestacar = null },
            containerColor = Color.White,
            shape = RoundedCornerShape(22.dp),
            title = { Text("Destacar evento", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        if (evento.estado.equals("publicado", ignoreCase = true)) {
                            "El evento aparecera en la seccion de destacados durante una semana."
                        } else {
                            "El evento quedara marcado como destacado y aparecera cuando sea publicado."
                        },
                        color = Color(0xFF64748B)
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
                            Text("Destaque semanal", color = Color(0xFF64748B), fontSize = 13.sp)
                            Text("$99.00 MXN", fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !destacando,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03A9F4)),
                    onClick = {
                        scope.launch {
                            destacando = true
                            GestorEventosOrganizador.destacarEvento(evento.id, "semanal", 99.0)
                                .onSuccess { actualizado ->
                                    if (actualizado) {
                                        eventos = eventos.map {
                                            if (it.id == evento.id) it.copy(esDestacado = true, tipoPublicacion = "destacada") else it
                                        }
                                        Toast.makeText(context, "Evento destacado", Toast.LENGTH_LONG).show()
                                        eventoADestacar = null
                                    } else {
                                        Toast.makeText(context, "No se encontro el evento", Toast.LENGTH_LONG).show()
                                    }
                                }
                                .onFailure { error ->
                                    Toast.makeText(
                                        context,
                                        error.message ?: "No se pudo destacar el evento",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            destacando = false
                        }
                    }
                ) {
                    if (destacando) {
                        CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Destacar")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !destacando,
                    onClick = { eventoADestacar = null }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun TarjetaEventoPropio(
    evento: Evento,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    onDestacar: () -> Unit,
    onVer: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                ImagenEvento(
                    evento = evento,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(evento.titulo, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(evento.fechaTexto, color = Color.Gray, fontSize = 12.sp)
                    Text(evento.ubicacion, color = Color.Gray, fontSize = 12.sp)
                }
                Text(
                    evento.estado.replace("_", " ").uppercase(),
                    color = Color(0xFF0284C7),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DatoEstadistica("Vistas", evento.vistas.toString(), Modifier.weight(1f))
                DatoEstadistica("Clicks", evento.clicks.toString(), Modifier.weight(1f))
                DatoEstadistica("Guardados", evento.guardados.toString(), Modifier.weight(1f))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onVer, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Visibility, contentDescription = null)
                    Spacer(modifier = Modifier.padding(horizontal = 3.dp))
                    Text("Ver")
                }
                OutlinedButton(
                    onClick = onEditar,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.padding(horizontal = 3.dp))
                    Text("Editar")
                }
            }

            OutlinedButton(
                onClick = onEliminar,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB3261E))
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.padding(horizontal = 3.dp))
                Text("Eliminar")
            }

            if (!evento.esDestacado && !evento.estado.equals("eliminado", ignoreCase = true)) {
                Button(
                    onClick = onDestacar,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
                ) {
                    Icon(Icons.Default.Star, contentDescription = null)
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    Text("Destacar")
                }
            } else if (evento.esDestacado) {
                Text("Destacado activo", color = Color(0xFF1B5E20), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun DatoEstadistica(
    etiqueta: String,
    valor: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF4F7FB)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(valor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(etiqueta, color = Color.Gray, fontSize = 11.sp)
        }
    }
}
