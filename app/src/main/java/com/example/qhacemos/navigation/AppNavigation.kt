package com.example.qhacemos.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.example.qhacemos.datos.GestorAutenticacion
import com.example.qhacemos.modelo.PerfilUsuario
import com.example.qhacemos.pantallas.BrujulaEventosScreen
import com.example.qhacemos.pantallas.CuentaScreen
import com.example.qhacemos.pantallas.EventDetailScreen
import com.example.qhacemos.pantallas.LoginScreen
import com.example.qhacemos.pantallas.MisEventosScreen
import com.example.qhacemos.pantallas.PantallaPrincipal
import kotlinx.coroutines.launch
import com.example.qhacemos.pantallas.CrearEventoScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val rutaActual = backStackEntry?.destination?.route

    var cargandoSesion by remember { mutableStateOf(true) }
    var perfilActual by remember { mutableStateOf<PerfilUsuario?>(null) }

    LaunchedEffect(Unit) {
        perfilActual = GestorAutenticacion.cargarPerfilActual().getOrNull()
        cargandoSesion = false
    }

    LaunchedEffect(perfilActual, rutaActual, cargandoSesion) {
        if (cargandoSesion || rutaActual == null) return@LaunchedEffect

        if (perfilActual == null && rutaActual != AppScreens.Login.route) {
            navController.navigate(AppScreens.Login.route) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
        }

        if (perfilActual != null && rutaActual == AppScreens.Login.route) {
            navController.navigate(AppScreens.Home.route) {
                popUpTo(AppScreens.Login.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    if (cargandoSesion) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    NavHost(
        navController = navController,
        startDestination = if (perfilActual == null) AppScreens.Login.route else AppScreens.Home.route
    ) {
        composable(AppScreens.Login.route) {
            LoginScreen(scope = scope) {
                scope.launch {
                    perfilActual = GestorAutenticacion.cargarPerfilActual().getOrNull()
                }
            }
        }

        composable(AppScreens.Home.route) {
            PantallaPrincipal(navController = navController)
        }

        composable(AppScreens.Account.route) {
            val perfil = perfilActual
            if (perfil != null) {
                CuentaScreen(
                    perfil = perfil,
                    navController = navController,
                    scope = scope
                ) {
                    perfilActual = null
                }
            }
        }

        composable(AppScreens.Compass.route) {
            BrujulaEventosScreen(navController = navController)
        }

        composable(
            route = "${AppScreens.EventDetail.route}/{eventoId}",
            deepLinks = listOf(navDeepLink { uriPattern = "qhacemos://evento/{eventoId}" })
        ) { backStackEntryInterna ->
            val eventoId = backStackEntryInterna.arguments?.getString("eventoId")?.toLongOrNull()

            EventDetailScreen(
                eventoId = eventoId ?: 0L,
                navController = navController
            )
        }

        composable(AppScreens.CrearEvento.route) {
            CrearEventoScreen(
                navController = navController,
                perfilActual = perfilActual
            )
        }

        composable("${AppScreens.EditarEvento.route}/{eventoId}") { backStackEntryInterna ->
            val eventoId = backStackEntryInterna.arguments?.getString("eventoId")?.toLongOrNull()
            CrearEventoScreen(
                navController = navController,
                perfilActual = perfilActual,
                eventoId = eventoId
            )
        }

        composable(AppScreens.MisEventos.route) {
            MisEventosScreen(
                navController = navController,
                perfilActual = perfilActual
            )
        }
    }

}
