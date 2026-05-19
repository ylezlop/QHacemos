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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
            Button(
                onClick = { navController.navigate(AppScreens.CrearEvento.route) },
                modifier = Modifier.fillMaxWidth()
            ) {
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
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Aun no has creado eventos.", fontWeight = FontWeight.Bold)
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
                    onVer = {
                        navController.navigate("${AppScreens.EventDetail.route}/${evento.id}")
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    eventoAEliminar?.let { evento ->
        AlertDialog(
            onDismissRequest = { if (!eliminando) eventoAEliminar = null },
            title = { Text("Eliminar evento") },
            text = {
                Text("El evento dejara de aparecer para los usuarios. Esta accion no elimina asistencias ni calificaciones relacionadas.")
            },
            confirmButton = {
                Button(
                    enabled = !eliminando,
                    onClick = {
                        scope.launch {
                            eliminando = true
                            GestorEventosOrganizador.eliminarEvento(evento.id)
                                .onSuccess {
                                    eventos = eventos.filterNot { it.id == evento.id }
                                    Toast.makeText(context, "Evento eliminado", Toast.LENGTH_LONG).show()
                                    eventoAEliminar = null
                                }
                                .onFailure { error ->
                                    Toast.makeText(
                                        context,
                                        error.message ?: "No se pudo eliminar el evento",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            eliminando = false
                        }
                    }
                ) {
                    if (eliminando) {
                        CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Eliminar")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !eliminando,
                    onClick = { eventoAEliminar = null }
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
    onVer: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                    color = Color(0xFF0277BD),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onVer, modifier = Modifier.weight(1f)) {
                    Text("Ver")
                }
                Button(onClick = onEditar, modifier = Modifier.weight(1f)) {
                    Text("Editar")
                }
                OutlinedButton(onClick = onEliminar, modifier = Modifier.weight(1f)) {
                    Text("Eliminar")
                }
            }
        }
    }
}
