package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ui.components.MyraaCore
import com.example.ui.components.MyraaCoreState
import com.example.ui.navigation.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ChatMessage(val text: String, val isUser: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavController) {
    var textInput by remember { mutableStateOf("") }
    var messages by remember { 
        mutableStateOf(listOf(
            ChatMessage("Hello! I'm Myraa. How can I assist you today?", false)
        ))
    }
    var isThinking by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(32.dp)) {
                            MyraaCore(state = if (isThinking) MyraaCoreState.THINKING else MyraaCoreState.IDLE)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Myraa", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (isThinking) "Thinking..." else "Online", 
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isThinking) MaterialTheme.colorScheme.primary else Color.Green
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Voice.route) }) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice Mode")
                    }
                    IconButton(onClick = { /* Menu */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.navigationBarsPadding()
            ) {
                IconButton(onClick = { /* Attach */ }) {
                    Icon(Icons.Default.Add, contentDescription = "Attach")
                }
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Ask Myraa something...") },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    shape = CircleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    maxLines = 3
                )
                if (textInput.isNotBlank()) {
                    IconButton(
                        onClick = {
                            val userMsg = textInput
                            textInput = ""
                            messages = messages + ChatMessage(userMsg, true)
                            isThinking = true
                            coroutineScope.launch {
                                delay(1500)
                                messages = messages + ChatMessage("This is a simulated response to: \"$userMsg\". As an AI prototype, my real backend is not connected yet.", false)
                                isThinking = false
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send")
                    }
                } else {
                    IconButton(onClick = { navController.navigate(Screen.Voice.route) }) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(msg)
            }
            if (isThinking) {
                item {
                    ChatBubble(ChatMessage("...", false))
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val shape = if (message.isUser) {
        RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(shape)
                .background(backgroundColor)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = message.text,
                color = textColor,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
