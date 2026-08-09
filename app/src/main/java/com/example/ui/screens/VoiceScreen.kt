package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ui.components.MyraaCore
import com.example.ui.components.MyraaCoreState
import com.example.ui.components.VoicePermissionHandler
import com.example.ui.navigation.Screen
import com.example.utils.LiveConnectionState
import com.example.utils.SecretManager
import com.example.utils.VoiceService
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun VoiceScreen(navController: NavController) {
    val context = LocalContext.current
    val secretManager = remember { SecretManager(context) }
    val apiKey = secretManager.getApiKey()
    
    val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
    }
    
    val micPermissionState = rememberMultiplePermissionsState(permissions)

    val connectionState by VoiceService.connectionState.collectAsState()
    val errorMessage by VoiceService.errorMessage.collectAsState()
    val isTurnComplete by VoiceService.isTurnComplete.collectAsState(initial = null)
    val isSpeaking by VoiceService.speakingFlow.collectAsState(initial = null)
    
    var currentState by remember { mutableStateOf(MyraaCoreState.READY) }
    var statusText by remember { mutableStateOf("Myraa is ready") }
    
    // Automatically manage UI based on connection state
    LaunchedEffect(connectionState) {
        when (connectionState) {
            LiveConnectionState.CONNECTED -> {
                currentState = MyraaCoreState.LISTENING
                statusText = "Listening (Background Active)..."
            }
            LiveConnectionState.DISCONNECTED, LiveConnectionState.ERROR -> {
                currentState = MyraaCoreState.READY
                statusText = if (connectionState == LiveConnectionState.ERROR) "Connection Error" else "Myraa is ready"
            }
            LiveConnectionState.CONNECTING -> {
                currentState = MyraaCoreState.THINKING
                statusText = "Connecting to Gemini..."
            }
        }
    }
    
    LaunchedEffect(isSpeaking) {
        if (connectionState == LiveConnectionState.CONNECTED && isSpeaking != null) {
            currentState = MyraaCoreState.SPEAKING
            statusText = "Myraa is speaking..."
        }
    }

    LaunchedEffect(isTurnComplete) {
        if (connectionState == LiveConnectionState.CONNECTED && isTurnComplete != null) {
            currentState = MyraaCoreState.LISTENING
            statusText = "Listening (Background Active)..."
        }
    }
    
    // Notice: We removed the DisposableEffect that disconnects on leaving the screen!
    // This allows background execution to continue.

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Myraa Voice", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color(0xFF0F0F13) // Dark futuristic background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))
            
            if (apiKey.isNullOrEmpty()) {
                Text(
                    text = "Add your Gemini API key in Settings.",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
                Button(onClick = { navController.navigate(Screen.Settings.route) }) {
                    Text("Go to Settings")
                }
            } else if (!micPermissionState.allPermissionsGranted) {
                VoicePermissionHandler(
                    permissionState = micPermissionState,
                    onCancel = { navController.popBackStack() }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center
                ) {
                    MyraaCore(state = currentState, modifier = Modifier.fillMaxSize(0.8f))
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Bottom Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { 
                        val stopIntent = Intent(context, VoiceService::class.java).apply {
                            action = VoiceService.ACTION_STOP
                        }
                        context.startService(stopIntent)
                        navController.popBackStack() 
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                }
                
                FloatingActionButton(
                    onClick = {
                        if (apiKey.isNullOrEmpty() || !micPermissionState.allPermissionsGranted) return@FloatingActionButton
                        
                        if (connectionState == LiveConnectionState.CONNECTED) {
                            // Interrupt / barge-in manually if needed
                            VoiceService.instance?.flushPlayback()
                            VoiceService.instance?.sendUserMessage("Stop")
                            currentState = MyraaCoreState.LISTENING
                            statusText = "Listening (Background Active)..."
                        } else {
                            val startIntent = Intent(context, VoiceService::class.java)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(startIntent)
                            } else {
                                context.startService(startIntent)
                            }
                        }
                    },
                    modifier = Modifier.size(80.dp),
                    containerColor = if (connectionState == LiveConnectionState.CONNECTED) 
                        MaterialTheme.colorScheme.error 
                    else MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Microphone", modifier = Modifier.size(36.dp), tint = Color.White)
                }
                
                IconButton(
                    onClick = { 
                        navController.navigate(Screen.Chat.route) {
                            popUpTo(Screen.Home.route)
                        }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Icon(Icons.Default.Chat, contentDescription = "Chat", tint = Color.White)
                }
            }
        }
    }
}
