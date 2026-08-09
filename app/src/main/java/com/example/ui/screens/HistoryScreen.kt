package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ui.components.MyraaCore
import com.example.ui.components.MyraaCoreState
import com.example.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavController) {
    val dummyHistory = listOf(
        "Explain electrical engineering" to "Just now",
        "Help me plan my day" to "2 hours ago",
        "Voice conversation" to "Yesterday",
        "AI discussion" to "2 days ago",
        "Project discussion" to "Last week"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recent Conversations") },
                actions = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Default.History, contentDescription = "History Settings")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(dummyHistory.size) { index ->
                HistoryItem(
                    title = dummyHistory[index].first,
                    time = dummyHistory[index].second,
                    onClick = { navController.navigate(Screen.Chat.route) }
                )
            }
        }
    }
}

@Composable
fun HistoryItem(title: String, time: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(40.dp)) {
                MyraaCore(state = MyraaCoreState.IDLE)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = time,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "View")
        }
    }
}
