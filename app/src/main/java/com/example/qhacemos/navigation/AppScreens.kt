package com.example.qhacemos.navigation

sealed class AppScreens(val route: String) {
    object Login : AppScreens("login")
    object Home : AppScreens("home")
    object Account : AppScreens("account")
    object EventDetail : AppScreens("event_detail")
}
