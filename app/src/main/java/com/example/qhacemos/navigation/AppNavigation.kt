package com.example.qhacemos.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.*
import androidx.navigation.compose.NavHost
import com.example.qhacemos.pantallas.*

@Composable
fun AppNavigation() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppScreens.Home.route
    ) {

        composable(AppScreens.Home.route) {
            PantallaPrincipal(navController)
        }

        composable("${AppScreens.EventDetail.route}/{eventoId}") { backStackEntry ->

            val eventoId = backStackEntry.arguments?.getString("eventoId")?.toInt()

            EventDetailScreen(
                eventoId = eventoId ?: 0,
                navController = navController
            )
        }
    }
}