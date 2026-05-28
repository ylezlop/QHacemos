package com.example.qhacemos.pantallas

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.qhacemos.datos.obtenerEventos
import com.example.qhacemos.modelo.Evento
import com.example.qhacemos.navigation.AppScreens
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.launch

// Centro geográfico inicial
private val CENTRO_XALAPA = LatLng(19.5438, -96.9102)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapaEventosScreen(navController: NavController) {
    val contexto = LocalContext.current
    val scope = rememberCoroutineScope()

    var listaEventos by remember { mutableStateOf<List<Evento>>(emptyList()) }
    var eventoSeleccionado by remember { mutableStateOf<Evento?>(null) }
    var cargando by remember { mutableStateOf(true) }

    val permissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(CENTRO_XALAPA, 13f)
    }

    LaunchedEffect(Unit) {
        if (!permissionState.status.isGranted) {
            permissionState.launchPermissionRequest()
        }

        listaEventos = obtenerEventos(contexto).filter { it.tieneCoordenadas && !it.yaOcurrio }
        cargando = false
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                val rutaActual = AppScreens.Mapa.route

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                    label = { Text("Inicio") },
                    selected = false,
                    onClick = {
                        navController.navigate(AppScreens.Home.route) {
                            popUpTo(AppScreens.Home.route) {
                                inclusive = false
                            }
                            launchSingleTop = true                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.LocationOn, contentDescription = "Mapa") },
                    label = { Text("Mapa") },
                    selected = rutaActual == AppScreens.Mapa.route, // Marcamos esta pestaña como activa
                    onClick = { }
                )
                NavigationBarItem(
                    icon = {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFF03A9F4))
                                .padding(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Crear", tint = Color.White)
                        }
                    },
                    label = { Text("Crear") },
                    selected = false,
                    onClick = {
                        navController.navigate(AppScreens.CrearEvento.route) { launchSingleTop = true }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Cuenta") },
                    label = { Text("Cuenta") },
                    selected = false,
                    onClick = {
                        navController.navigate(AppScreens.Account.route) { launchSingleTop = true }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Explore, contentDescription = "Brujula") },
                    label = { Text("Brujula") },
                    selected = false,
                    onClick = {
                        navController.navigate(AppScreens.Compass.route) { launchSingleTop = true }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Esto asegura que el mapa no se oculte detrás de la barra inferior
        ) {
            if (cargando) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        isMyLocationEnabled = permissionState.status.isGranted,
                        mapType = MapType.NORMAL
                    ),
                    uiSettings = MapUiSettings(
                        myLocationButtonEnabled = true,
                        zoomControlsEnabled = false,
                        mapToolbarEnabled = false,
                        // Configuraciones explícitas para permitir la navegación táctil del usuario
                        zoomGesturesEnabled = true,
                        scrollGesturesEnabled = true,
                        rotationGesturesEnabled = true,
                        tiltGesturesEnabled = true
                    ),
                    onMapClick = {
                        eventoSeleccionado = null
                    }
                ) {
                    listaEventos.forEach { evento ->
                        val posicion = LatLng(evento.latitud!!, evento.longitud!!)
                        Marker(
                            state = rememberMarkerState(position = posicion),
                            title = evento.titulo,
                            onClick = {
                                eventoSeleccionado = evento
                                scope.launch {
                                    val offsetPosicion = LatLng(posicion.latitude - 0.005, posicion.longitude)
                                    cameraPositionState.animate(CameraUpdateFactory.newLatLng(offsetPosicion))
                                }
                                true
                            }
                        )
                    }
                }

                eventoSeleccionado?.let { evento ->
                    TarjetaFlotanteResumen(
                        evento = evento,
                        onClick = { navController.navigate("${AppScreens.EventDetail.route}/${evento.id}") },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TarjetaFlotanteResumen(
    evento: Evento,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(evento.colorFondo),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White)
            }

            Spacer(modifier = Modifier.size(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = evento.titulo,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = evento.fechaTexto,
                    color = Color.Gray,
                    fontSize = 13.sp
                )
                Text(
                    text = evento.costoTexto,
                    color = if (evento.esGratis) Color(0xFF4CAF50) else Color(0xFFFF7A1A),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Ver detalles",
                tint = Color.Gray
            )
        }
    }
}