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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.qhacemos.datos.GestorAutenticacion
import com.example.qhacemos.modelo.PerfilUsuario
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun CuentaScreen(
    perfil: PerfilUsuario,
    navController: NavController,
    scope: CoroutineScope,
    onLogout: () -> Unit
) {
    val backStackEntry by navController.currentBackStackEntryAsState()

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

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Acceso actual", fontWeight = FontWeight.Bold)
                    Text(
                        text = if (perfil.esAdmin) {
                            "Tu cuenta puede revisar y administrar el sistema."
                        } else {
                            "Tu cuenta puede explorar la app y despues organizara eventos como usuario creador."
                        },
                        color = Color.Gray
                    )
                    if (GestorAutenticacion.usaModoDemo()) {
                        Text(
                            text = "Esta sesion viene del modo demo local y no persiste al cerrar la app.",
                            color = Color(0xFF0D47A1),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            if (perfil.esAdmin) {
                AvisoCuenta(
                    texto = "En las siguientes iteraciones podemos usar este rol para validacion de eventos, gestion de usuarios y metricas globales."
                )
            } else {
                AvisoCuenta(
                    texto = "Este perfil seguira siendo el que organiza, publica y administra sus propios eventos."
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
