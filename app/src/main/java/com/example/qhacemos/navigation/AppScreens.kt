package com.example.qhacemos.navigation

sealed class AppScreens(val route: String) {
    object Home : AppScreens("home")
    object EventDetail : AppScreens("event_detail")
}