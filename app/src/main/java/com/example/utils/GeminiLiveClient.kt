package com.example.utils

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString.Companion.toByteString
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import android.util.Base64

enum class LiveConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

class GeminiLiveClient(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
        
    private var webSocket: WebSocket? = null

    private val _connectionState = MutableSharedFlow<LiveConnectionState>(replay = 1)
    val connectionState = _connectionState.asSharedFlow()

    private val _audioFlow = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    val audioFlow = _audioFlow.asSharedFlow()
    
    private val _turnCompleteFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val turnCompleteFlow = _turnCompleteFlow.asSharedFlow()

    private val _speakingFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val speakingFlow = _speakingFlow.asSharedFlow()

    private val _errorFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errorFlow = _errorFlow.asSharedFlow()
    
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        scope.launch { _connectionState.emit(LiveConnectionState.DISCONNECTED) }
    }

    fun connect() {
        if (webSocket != null) return
        scope.launch { _connectionState.emit(LiveConnectionState.CONNECTING) }
        
        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key=$apiKey"
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                scope.launch { _connectionState.emit(LiveConnectionState.CONNECTED) }
                sendSetupMessage()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleServerMessage(text)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                scope.launch { _connectionState.emit(LiveConnectionState.DISCONNECTED) }
                this@GeminiLiveClient.webSocket = null
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                scope.launch { 
                    _errorFlow.emit(t.message ?: "Unknown network error")
                    _connectionState.emit(LiveConnectionState.ERROR) 
                }
                this@GeminiLiveClient.webSocket = null
            }
        })
    }

    private fun sendSetupMessage() {
        val setup = JSONObject().apply {
            put("setup", JSONObject().apply {
                put("model", "models/gemini-2.0-flash-exp")
                put("generationConfig", JSONObject().apply {
                    put("responseModalities", JSONArray().put("AUDIO"))
                    put("speechConfig", JSONObject().apply {
                        put("voiceConfig", JSONObject().apply {
                            put("prebuiltVoiceConfig", JSONObject().apply {
                                put("voiceName", "Aoede")
                            })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply {
                        put("text", "Your name is Myraa. You are an intelligent, calm, helpful, natural, friendly, futuristic, and concise AI voice assistant. Always reply natively in the language the user speaks. Keep responses short and suitable for voice.")
                    }))
                })
            })
        }
        webSocket?.send(setup.toString())
        
        // Also send initial client content to trigger conversation immediately if we wanted to, 
        // but typically we wait for the user to speak.
    }

    fun sendAudio(pcmData: ByteArray, bytesRead: Int) {
        val base64Audio = Base64.encodeToString(pcmData, 0, bytesRead, Base64.NO_WRAP)
        val clientContent = JSONObject().apply {
            put("realtimeInput", JSONObject().apply {
                put("mediaChunks", JSONArray().put(JSONObject().apply {
                    put("mimeType", "audio/pcm;rate=16000")
                    put("data", base64Audio)
                }))
            })
        }
        webSocket?.send(clientContent.toString())
    }
    
    fun sendUserMessage(text: String) {
        val clientContent = JSONObject().apply {
            put("clientContent", JSONObject().apply {
                put("turns", JSONArray().put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().apply {
                        put("text", text)
                    }))
                }))
                put("turnComplete", true)
            })
        }
        webSocket?.send(clientContent.toString())
    }

    private fun handleServerMessage(text: String) {
        try {
            val json = JSONObject(text)
            if (json.has("serverContent")) {
                val serverContent = json.getJSONObject("serverContent")
                if (serverContent.has("modelTurn")) {
                    val modelTurn = serverContent.getJSONObject("modelTurn")
                    val parts = modelTurn.getJSONArray("parts")
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        if (part.has("inlineData")) {
                            val inlineData = part.getJSONObject("inlineData")
                            val mimeType = inlineData.getString("mimeType")
                            if (mimeType.startsWith("audio/pcm")) {
                                val base64Data = inlineData.getString("data")
                                val audioBytes = Base64.decode(base64Data, Base64.DEFAULT)
                                scope.launch { 
                                    _speakingFlow.tryEmit(Unit)
                                    _audioFlow.emit(audioBytes) 
                                }
                            }
                        }
                    }
                }
                if (serverContent.has("turnComplete") && serverContent.getBoolean("turnComplete")) {
                    scope.launch { _turnCompleteFlow.emit(Unit) }
                }
                if (serverContent.has("interrupted") && serverContent.getBoolean("interrupted")) {
                    scope.launch { _turnCompleteFlow.emit(Unit) } // End of turn
                }
            } else if (json.has("setupComplete")) {
                // Setup is successful
            }
        } catch (e: Exception) {
            Log.e("GeminiLiveClient", "Error parsing message", e)
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "Normal closure")
        webSocket = null
        scope.launch { _connectionState.emit(LiveConnectionState.DISCONNECTED) }
    }
}
