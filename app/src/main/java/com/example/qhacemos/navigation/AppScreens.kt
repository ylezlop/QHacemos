package com.example.qhacemos.navigation

sealed class AppScreens(val route: String) {
    object Login : AppScreens("login")
    object Home : AppScreens("home")
    object Account : AppScreens("account")
    object Compass : AppScreens("compass")
    object EventDetail : AppScreens("event_detail")

    object CrearEvento : AppScreens("crear_evento")
    object EditarEvento : AppScreens("editar_evento")
    object MisEventos : AppScreens("mis_eventos")
}
