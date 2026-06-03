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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.qhacemos.datos.GestorUsuarios
import com.example.qhacemos.modelo.PerfilUsuario
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionUsuariosScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    var consulta by remember { mutableStateOf("") }
    var usuarios by remember { mutableStateOf<List<PerfilUsuario>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var mensajeError by remember { mutableStateOf<String?>(null) }
    var usuarioEditar by remember { mutableStateOf<PerfilUsuario?>(null) }
    var usuarioConfirmar by remember { mutableStateOf<PerfilUsuario?>(null) }
    var accionConfirmar by remember { mutableStateOf("") }

    fun cargarUsuarios() {
        scope.launch {
            cargando = true
            mensajeError = null
            GestorUsuarios.buscarUsuarios(consulta)
                .onSuccess { usuarios = it }
                .onFailure { mensajeError = it.message ?: "No se pudieron cargar los usuarios" }
            cargando = false
        }
    }

    LaunchedEffect(Unit) {
        cargarUsuarios()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestionar usuarios") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = consulta,
                    onValueChange = { consulta = it },
                    label = { Text("Buscar por nombre, correo o rol") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Button(onClick = { cargarUsuarios() }) {
                    Text("Buscar")
                }
            }

            mensajeError?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            if (cargando) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (usuarios.isEmpty()) {
                        item {
                            Text("No hay usuarios para mostrar.", color = Color.Gray)
                        }
                    }

                    items(usuarios, key = { it.id }) { usuario ->
                        TarjetaGestionUsuario(
                            usuario = usuario,
                            onEditar = { usuarioEditar = usuario },
                            onSuspender = {
                                accionConfirmar = if (usuario.estaSuspendido) "reactivar" else "suspender"
                                usuarioConfirmar = usuario
                            },
                            onEliminar = {
                                accionConfirmar = "eliminar"
                                usuarioConfirmar = usuario
                            }
                        )
                    }
                }
            }
        }
    }

    usuarioEditar?.let { usuario ->
        DialogoEditarUsuario(
            usuario = usuario,
            onDismiss = { usuarioEditar = null },
            onGuardar = { actualizado ->
                scope.launch {
                    GestorUsuarios.actualizarUsuario(actualizado)
                        .onSuccess {
                            usuarioEditar = null
                            Toast.makeText(context, "Usuario actualizado", Toast.LENGTH_SHORT).show()
                            cargarUsuarios()
                        }
                        .onFailure {
                            Toast.makeText(context, it.message ?: "No se pudo actualizar", Toast.LENGTH_LONG).show()
                        }
                }
            }
        )
    }

    usuarioConfirmar?.let { usuario ->
        DialogoConfirmarGestionUsuario(
            usuario = usuario,
            accion = accionConfirmar,
            onDismiss = { usuarioConfirmar = null },
            onConfirmar = {
                scope.launch {
                    val resultado = when (accionConfirmar) {
                        "suspender" -> GestorUsuarios.suspenderUsuario(usuario.id)
                        "reactivar" -> GestorUsuarios.reactivarUsuario(usuario.id)
                        else -> GestorUsuarios.eliminarUsuario(usuario.id)
                    }

                    resultado
                        .onSuccess {
                            usuarioConfirmar = null
                            Toast.makeText(context, "Accion completada", Toast.LENGTH_SHORT).show()
                            cargarUsuarios()
                        }
                        .onFailure {
                            Toast.makeText(context, it.message ?: "No se pudo completar la accion", Toast.LENGTH_LONG).show()
                        }
                }
            }
        )
    }
}

@Composable
fun TarjetaGestionUsuario(
    usuario: PerfilUsuario,
    onEditar: () -> Unit,
    onSuspender: () -> Unit,
    onEliminar: () -> Unit
) {
    val colorEstado = when {
        usuario.estaSuspendido -> Color(0xFFFFA000)
        usuario.estado.equals("activo", ignoreCase = true) -> Color(0xFF2E7D32)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
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
                    Text(usuario.nombre.ifBlank { "Usuario sin nombre" }, fontWeight = FontWeight.Bold)
                    Text(usuario.email, color = Color.Gray)
                }
                Text(usuario.rolLegible, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }

            Text("Estado: ${usuario.estado}", color = colorEstado)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onEditar) {
                    Text("Editar")
                }
                Button(
                    onClick = onSuspender,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000))
                ) {
                    Text(if (usuario.estaSuspendido) "Reactivar" else "Suspender")
                }
                TextButton(onClick = onEliminar) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun DialogoEditarUsuario(
    usuario: PerfilUsuario,
    onDismiss: () -> Unit,
    onGuardar: (PerfilUsuario) -> Unit
) {
    var nombre by remember(usuario.id) { mutableStateOf(usuario.nombre) }
    var email by remember(usuario.id) { mutableStateOf(usuario.email) }
    var rol by remember(usuario.id) { mutableStateOf(usuario.rol) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar usuario") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = rol,
                    onValueChange = { rol = it.lowercase().trim() },
                    label = { Text("Rol") },
                    singleLine = true
                )
                Text("Roles validos: admin o usuario", color = Color.Gray)
            }
        },
        confirmButton = {
            Button(
                enabled = nombre.isNotBlank() && email.isNotBlank() && rol in listOf("admin", "usuario"),
                onClick = {
                    onGuardar(
                        usuario.copy(
                            nombre = nombre.trim(),
                            email = email.trim(),
                            rol = rol
                        )
                    )
                }
            ) {
                Text("Guardar")
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
fun DialogoConfirmarGestionUsuario(
    usuario: PerfilUsuario,
    accion: String,
    onDismiss: () -> Unit,
    onConfirmar: () -> Unit
) {
    val textoAccion = when (accion) {
        "suspender" -> "suspender"
        "reactivar" -> "reactivar"
        else -> "eliminar"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirmar accion") },
        text = {
            Column {
                Text("Deseas $textoAccion a este usuario?")
                Spacer(modifier = Modifier.height(8.dp))
                Text(usuario.nombre.ifBlank { usuario.email }, fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {
            Button(onClick = onConfirmar) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
