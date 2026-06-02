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
import androidx.navigation.NavController
import com.example.qhacemos.datos.GestorEventosOrganizador
import com.example.qhacemos.modelo.Evento
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ValidarEventosScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var eventos by remember { mutableStateOf<List<Evento>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var mensajeError by remember { mutableStateOf<String?>(null) }
    var eventoSeleccionado by remember { mutableStateOf<Evento?>(null) }
    var accionPendiente by remember { mutableStateOf<String?>(null) }
    var procesando by remember { mutableStateOf(false) }
    var intentoCarga by remember { mutableStateOf(0) }

    LaunchedEffect(intentoCarga) {
        cargando = true
        mensajeError = null

        GestorEventosOrganizador.obtenerEventosPendientes()
            .onSuccess { lista ->
                eventos = lista.sortedBy { it.fechaInicioParseada }
            }
            .onFailure { error ->
                eventos = emptyList()
                mensajeError = error.message ?: "No se pudieron cargar los eventos pendientes."
            }

        cargando = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Validar eventos") },
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
            mensajeError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                Button(onClick = { intentoCarga++ }) {
                    Text("Reintentar")
                }
            }

            if (cargando) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
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
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("No hay eventos pendientes de validacion.", fontWeight = FontWeight.Bold)
                    }
                }
                return@Column
            }

            eventos.forEach { evento ->
                TarjetaEventoPendiente(
                    evento = evento,
                    onAprobar = {
                        eventoSeleccionado = evento
                        accionPendiente = "aprobar"
                    },
                    onRechazar = {
                        eventoSeleccionado = evento
                        accionPendiente = "rechazar"
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    val evento = eventoSeleccionado
    val accion = accionPendiente
    if (evento != null && accion != null) {
        AlertDialog(
            onDismissRequest = {
                if (!procesando) {
                    eventoSeleccionado = null
                    accionPendiente = null
                }
            },
            title = { Text(if (accion == "aprobar") "Aprobar evento" else "Rechazar evento") },
            text = {
                Text(
                    if (accion == "aprobar") {
                        "El evento cambiara a Publicado y sera visible para los usuarios."
                    } else {
                        "El evento cambiara a Rechazado y no se mostrara en la aplicacion."
                    }
                )
            },
            confirmButton = {
                Button(
                    enabled = !procesando,
                    onClick = {
                        scope.launch {
                            procesando = true
                            val resultado = if (accion == "aprobar") {
                                GestorEventosOrganizador.aprobarEvento(evento.id)
                            } else {
                                GestorEventosOrganizador.rechazarEvento(evento.id)
                            }

                            resultado
                                .onSuccess { actualizado ->
                                    if (actualizado) {
                                        eventos = eventos.filterNot { it.id == evento.id }
                                        Toast.makeText(
                                            context,
                                            if (accion == "aprobar") "Evento aprobado" else "Evento rechazado",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        eventoSeleccionado = null
                                        accionPendiente = null
                                    } else {
                                        Toast.makeText(context, "No se encontro el evento", Toast.LENGTH_LONG).show()
                                    }
                                }
                                .onFailure { error ->
                                    Toast.makeText(
                                        context,
                                        error.message ?: "No se pudo actualizar el evento",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }

                            procesando = false
                        }
                    }
                ) {
                    if (procesando) {
                        CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Confirmar")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !procesando,
                    onClick = {
                        eventoSeleccionado = null
                        accionPendiente = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun TarjetaEventoPendiente(
    evento: Evento,
    onAprobar: () -> Unit,
    onRechazar: () -> Unit
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
            Text(evento.titulo.ifBlank { "Evento sin titulo" }, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(evento.fechaTexto, color = Color.Gray, fontSize = 12.sp)
            Text(evento.ubicacion.ifBlank { "Ubicacion no disponible" }, color = Color.Gray, fontSize = 12.sp)
            Text(
                evento.descripcion.ifBlank { "Sin descripcion." },
                color = Color.DarkGray,
                fontSize = 13.sp
            )
            Text(
                "Organiza: ${evento.organizadorNombre.ifBlank { "Organizador no disponible" }}",
                color = Color.Gray,
                fontSize = 12.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onRechazar, modifier = Modifier.weight(1f)) {
                    Text("Rechazar")
                }
                Button(onClick = onAprobar, modifier = Modifier.weight(1f)) {
                    Text("Aprobar")
                }
            }
        }
    }
}
