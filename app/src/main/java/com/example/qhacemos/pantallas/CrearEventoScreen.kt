package com.example.qhacemos.pantallas

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.qhacemos.datos.GestorEventosOrganizador
import com.example.qhacemos.modelo.Evento
import com.example.qhacemos.modelo.PerfilUsuario
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearEventoScreen(navController: NavController, perfilActual: PerfilUsuario?) {
    val contexto = LocalContext.current
    val scope = rememberCoroutineScope()

    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf("") }
    var ubicacion by remember { mutableStateOf("") }
    var costoTexto by remember { mutableStateOf("0") }

    var mostrandoConfirmacionPago by remember { mutableStateOf(false) }
    var procesandoPublicacion by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Publicar Nuevo Evento") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = titulo,
                onValueChange = { titulo = it },
                label = { Text("Título del Evento *") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Descripción") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            OutlinedTextField(
                value = fecha,
                onValueChange = { fecha = it },
                label = { Text("Fecha (ej. 20/05/2026) *") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = ubicacion,
                onValueChange = { ubicacion = it },
                label = { Text("Ubicación en Xalapa *") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = costoTexto,
                onValueChange = { costoTexto = it },
                label = { Text("Costo de Entrada ($ MXN)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (titulo.isBlank() || fecha.isBlank() || ubicacion.isBlank()) {
                        Toast.makeText(contexto, "Por favor, completa todos los campos requeridos (*)", Toast.LENGTH_LONG).show()
                    } else {
                        mostrandoConfirmacionPago = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Proceder al Pago ($50.00 MXN)")
            }
        }
    }

    if (mostrandoConfirmacionPago) {
        AlertDialog(
            onDismissRequest = { mostrandoConfirmacionPago = false },
            title = { Text("Confirmar Pasarela de Pago") },
            text = { Text("Se realizará un cargo de $50.00 MXN a tu método de pago registrado para la publicación de este evento.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrandoConfirmacionPago = false
                        procesandoPublicacion = true

                        val costoEntrada = costoTexto.toDoubleOrNull() ?: 0.0
                        val nuevoEvento = Evento(
                            id = 0L,
                            titulo = titulo,
                            descripcion = descripcion,
                            fechaInicio = fecha,
                            ubicacion = ubicacion,
                            organizadorId = perfilActual?.id ?: "anonimo",
                            organizadorNombre = perfilActual?.nombre ?: "Organizador",
                            costoMxn = costoEntrada,
                            esGratis = costoEntrada == 0.0,
                            esDestacado = false,
                            tipoPublicacion = "pago",
                            estado = "pendiente_validacion",
                            imagenPrincipalUrl = "",
                            vistas = 0,
                            clicks = 0,
                            guardados = 0
                        )

                        scope.launch {
                            val resultado = GestorEventosOrganizador.publicarEvento(nuevoEvento, 50.0)
                            procesandoPublicacion = false
                            if (resultado.isSuccess) {
                                Toast.makeText(contexto, "Evento guardado con éxito. Pendiente de validación", Toast.LENGTH_LONG).show()
                                navController.popBackStack()
                            } else {
                                Toast.makeText(contexto, "Error al guardar los datos: ${resultado.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) {
                    if (procesandoPublicacion) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Text("Confirmar Pago")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrandoConfirmacionPago = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}