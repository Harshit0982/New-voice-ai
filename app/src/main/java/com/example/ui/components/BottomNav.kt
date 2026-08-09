package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.ui.navigation.Screen

@Composable
fun BottomNav(navController: NavController, modifier: Modifier = Modifier) {
    val items = listOf(
        NavigationItem("Home", Icons.Default.Home, Screen.Home.route),
        NavigationItem("Voice", Icons.Default.Mic, Screen.Voice.route),
        NavigationItem("Chat", Icons.Default.Chat, Screen.Chat.route),
        NavigationItem("History", Icons.Default.History, Screen.History.route),
        NavigationItem("Profile", Icons.Default.Person, Screen.Profile.route)
    )
    
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    NavigationBar(modifier = modifier) {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}

data class NavigationItem(val title: String, val icon: ImageVector, val route: String)
