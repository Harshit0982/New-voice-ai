package com.example.utils

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class AudioController(private val geminiClient: GeminiLiveClient) {

    private val inputSampleRate = 16000
    private val outputSampleRate = 24000
    private val channelConfigIn = AudioFormat.CHANNEL_IN_MONO
    private val channelConfigOut = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null

    private var recordingJob: Job? = null
    private var playbackJob: Job? = null

    private val scope = CoroutineScope(Dispatchers.IO)
    
    private val _volumeFlow = MutableSharedFlow<Float>(extraBufferCapacity = 1)
    val volumeFlow = _volumeFlow.asSharedFlow()

    @SuppressLint("MissingPermission")
    fun startRecording() {
        if (recordingJob?.isActive == true) return
        
        val minBufferSize = AudioRecord.getMinBufferSize(inputSampleRate, channelConfigIn, audioFormat)
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            inputSampleRate,
            channelConfigIn,
            audioFormat,
            minBufferSize * 2
        )

        audioRecord?.startRecording()

        recordingJob = scope.launch {
            val buffer = ByteArray(minBufferSize)
            while (isActive) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    geminiClient.sendAudio(buffer, read)
                    
                    // Calculate volume for UI
                    var sum = 0f
                    for (i in 0 until read step 2) {
                        val sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
                        val normalized = sample / 32768f
                        sum += normalized * normalized
                    }
                    val rms = Math.sqrt((sum / (read / 2)).toDouble()).toFloat()
                    _volumeFlow.tryEmit(rms)
                }
            }
        }
    }

    fun stopRecording() {
        recordingJob?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    fun startPlayback() {
        if (playbackJob?.isActive == true) return

        val minBufferSize = AudioTrack.getMinBufferSize(outputSampleRate, channelConfigOut, audioFormat)
        audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            outputSampleRate,
            channelConfigOut,
            audioFormat,
            minBufferSize * 4,
            AudioTrack.MODE_STREAM
        )

        audioTrack?.play()

        playbackJob = scope.launch {
            geminiClient.audioFlow.collect { audioData ->
                if (isActive) {
                    audioTrack?.write(audioData, 0, audioData.size)
                }
            }
        }
    }
    
    fun flushPlayback() {
        audioTrack?.pause()
        audioTrack?.flush()
        audioTrack?.play()
    }

    fun stopPlayback() {
        playbackJob?.cancel()
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }
}
