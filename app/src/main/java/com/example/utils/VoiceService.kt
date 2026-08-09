package com.example.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class VoiceService : Service() {

    companion object {
        const val ACTION_STOP = "com.example.action.STOP"
        const val CHANNEL_ID = "VoiceServiceChannel"
        const val NOTIFICATION_ID = 1

        val connectionState = MutableStateFlow(LiveConnectionState.DISCONNECTED)
        val errorMessage = MutableStateFlow<String?>(null)
        val isTurnComplete = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val speakingFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

        var instance: VoiceService? = null
            private set
    }

    private var geminiClient: GeminiLiveClient? = null
    private var audioController: AudioController? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val secretManager = SecretManager(this)
        val apiKey = secretManager.getApiKey()
        
        if (apiKey.isNullOrEmpty()) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (geminiClient == null) {
            geminiClient = GeminiLiveClient(apiKey)
            audioController = AudioController(geminiClient!!)

            scope.launch {
                geminiClient!!.connectionState.collect { connectionState.value = it }
            }
            scope.launch {
                geminiClient!!.errorFlow.collect { errorMessage.value = it }
            }
            scope.launch {
                geminiClient!!.turnCompleteFlow.collect { isTurnComplete.tryEmit(it) }
            }
            scope.launch {
                geminiClient!!.speakingFlow.collect { speakingFlow.tryEmit(it) }
            }
        }

        startForeground(NOTIFICATION_ID, createNotification())

        if (connectionState.value == LiveConnectionState.DISCONNECTED || connectionState.value == LiveConnectionState.ERROR) {
            geminiClient?.connect()
        }

        scope.launch {
            connectionState.collect { state ->
                when (state) {
                    LiveConnectionState.CONNECTED -> {
                        audioController?.startRecording()
                        audioController?.startPlayback()
                    }
                    LiveConnectionState.DISCONNECTED, LiveConnectionState.ERROR -> {
                        audioController?.stopRecording()
                        audioController?.stopPlayback()
                        if (state == LiveConnectionState.ERROR) {
                            stopSelf()
                        }
                    }
                    else -> {}
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        geminiClient?.disconnect()
        audioController?.stopRecording()
        audioController?.stopPlayback()
        scope.cancel()
        instance = null
        connectionState.value = LiveConnectionState.DISCONNECTED
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Voice Assistant",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): android.app.Notification {
        val stopIntent = Intent(this, VoiceService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val mainIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Myraa Voice Mode")
            .setContentText("Listening in background...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    fun sendUserMessage(msg: String) {
        geminiClient?.sendUserMessage(msg)
    }

    fun flushPlayback() {
        audioController?.flushPlayback()
    }
}
