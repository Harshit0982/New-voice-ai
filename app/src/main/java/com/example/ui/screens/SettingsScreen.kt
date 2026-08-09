package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.utils.Content
import com.example.utils.GenerateContentRequest
import com.example.utils.Part
import com.example.utils.RetrofitClient
import com.example.utils.SecretManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val secretManager = remember { SecretManager(context) }
    var savedApiKey by remember { mutableStateOf(secretManager.getApiKey() ?: "") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            GeminiConnectionSettings(
                initialKey = savedApiKey,
                onSave = { 
                    secretManager.saveApiKey(it)
                    savedApiKey = it
                },
                onRemove = {
                    secretManager.removeApiKey()
                    savedApiKey = ""
                }
            )
            
            SettingsCategory("Assistant")
            SettingsSwitch("Myraa Voice", true)
            SettingsItem("Language", "English (US)")
            SettingsItem("Response Style", "Concise")
            SettingsItem("Wake Word", "Hey Myraa")
            
            SettingsCategory("Appearance")
            SettingsItem("Theme", "System Default")
            SettingsSwitch("Animation Intensity", true)
            
            SettingsCategory("Chat")
            SettingsSwitch("Conversation History", true)
            SettingsItem("Clear Conversations", "")
            
            SettingsCategory("Permissions")
            
            val context = LocalContext.current
            SettingsItem(
                title = "App Permissions",
                subtitle = "Manage background and microphone access in Android settings",
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            )
            
            SettingsCategory("About")
            SettingsItem("About Myraa", "")
            SettingsItem("Version", "1.0.0 Phase 2")
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun GeminiConnectionSettings(initialKey: String, onSave: (String) -> Unit, onRemove: () -> Unit) {
    var apiKeyInput by remember(initialKey) { mutableStateOf(initialKey) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var testStatus by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var showRemoveDialog by remember { mutableStateOf(false) }

    SettingsCategory("Gemini Connection")
    
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = apiKeyInput,
            onValueChange = { apiKeyInput = it },
            label = { Text("Gemini API Key") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(image, "Toggle password visibility")
                }
            },
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onSave(apiKeyInput) },
                enabled = apiKeyInput.isNotBlank() && apiKeyInput != initialKey
            ) {
                Text(if (initialKey.isEmpty()) "SAVE KEY" else "UPDATE KEY")
            }
            
            if (initialKey.isNotEmpty()) {
                OutlinedButton(onClick = { showRemoveDialog = true }) {
                    Text("REMOVE KEY")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (initialKey.isNotEmpty()) {
            Button(
                onClick = {
                    isTesting = true
                    testStatus = "Connecting to Gemini..."
                    coroutineScope.launch {
                        try {
                            val request = GenerateContentRequest(listOf(Content(listOf(Part("Hello")))))
                            val response = RetrofitClient.service.testConnection(initialKey, request)
                            if (response.candidates?.isNotEmpty() == true) {
                                testStatus = "✓ Gemini Connected"
                            } else {
                                testStatus = "✕ Gemini API key is invalid or unauthorized."
                            }
                        } catch (e: retrofit2.HttpException) {
                            if (e.code() == 401 || e.code() == 403 || e.code() == 400) {
                                testStatus = "✕ Gemini API key is invalid or unauthorized."
                            } else if (e.code() == 429) {
                                testStatus = "⚠ Gemini usage limit reached."
                            } else {
                                testStatus = "⚠ Gemini is temporarily unavailable."
                            }
                        } catch (e: java.io.IOException) {
                            testStatus = "⚠ Unable to reach Gemini. Check your internet connection."
                        } catch (e: Exception) {
                            testStatus = "⚠ Something went wrong. Please try again."
                        }
                        isTesting = false
                    }
                },
                enabled = !isTesting
            ) {
                Text("TEST CONNECTION")
            }
        }
        
        if (testStatus != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = testStatus!!,
                color = when {
                    testStatus!!.startsWith("✓") -> MaterialTheme.colorScheme.primary
                    testStatus!!.startsWith("✕") -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.tertiary
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
    
    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Remove saved Gemini API key?") },
            text = { Text("This will disable Myraa's AI capabilities until a new key is added.") },
            confirmButton = {
                TextButton(onClick = { 
                    onRemove()
                    testStatus = null
                    showRemoveDialog = false 
                }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsCategory(title: String) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp, end = 16.dp)
    )
}

@Composable
fun SettingsItem(title: String, subtitle: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f, fill = false).padding(start = 16.dp)
            )
        }
    }
}

@Composable
fun SettingsSwitch(title: String, initialValue: Boolean) {
    var checked by remember { mutableStateOf(initialValue) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = { checked = it })
    }
}
