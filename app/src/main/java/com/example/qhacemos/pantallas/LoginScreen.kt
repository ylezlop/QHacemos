package com.example.qhacemos.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qhacemos.datos.GestorAutenticacion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    scope: CoroutineScope,
    onLoginSuccess: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F7FB))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Iniciar sesion",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Entra con tu cuenta para continuar con la cartelera y la gestion de eventos.",
                    color = Color.Gray,
                    fontSize = 14.sp
                )

                if (GestorAutenticacion.usaModoDemo()) {
                    AvisoLogin(
                        texto = "Modo demo activo. Usa admin@qhacemos.test / Admin1234 o usuario@qhacemos.test / Usuario1234."
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        mensajeError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Correo") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        mensajeError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Contrasena") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )

                if (mensajeError != null) {
                    Text(
                        text = mensajeError.orEmpty(),
                        color = Color(0xFFB3261E),
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            mensajeError = "Escribe tu correo y contrasena."
                            return@Button
                        }

                        scope.launch {
                            cargando = true
                            mensajeError = null
                            val resultado = GestorAutenticacion.iniciarSesion(email, password)
                            cargando = false

                            resultado.onSuccess {
                                onLoginSuccess()
                            }.onFailure {
                                mensajeError = it.message ?: "No fue posible iniciar sesion."
                            }
                        }
                    },
                    enabled = !cargando,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (cargando) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text("Entrar")
                    }
                }

                Text(
                    text = "Los usuarios admin y usuario normal comparten esta pantalla; el rol se toma desde tu perfil.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun AvisoLogin(texto: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE3F2FD), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Text(text = texto, color = Color(0xFF0D47A1), fontSize = 13.sp)
    }
}
