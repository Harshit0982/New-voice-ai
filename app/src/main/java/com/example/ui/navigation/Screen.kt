package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Voice : Screen("voice")
    object Chat : Screen("chat")
    object History : Screen("history")
    object Profile : Screen("profile")
    object Settings : Screen("settings")
}
