package com.example.luna

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class VoiceRecognitionManager(
    private val context: Context,
    private val onWakeWordDetected: () -> Unit,
    private val onCommandReceived: (String) -> Unit,
    private val onListeningStarted: () -> Unit,
    private val onError: (String) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListeningForWakeWord = true
    private var isActive = false
    var onDebugText: ((String) -> Unit)? = null

    private val wakeWords = listOf("luna", "hey luna", "ehi luna", "ei luna", "hei luna", "l'una", "unna", "duna")

    private fun muteBeepSoundOfRecorder() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        audioManager.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.ADJUST_MUTE, 0)
        audioManager.adjustStreamVolume(android.media.AudioManager.STREAM_SYSTEM, android.media.AudioManager.ADJUST_MUTE, 0)
    }

    private fun unmuteBeepSoundOfRecorder() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        audioManager.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.ADJUST_UNMUTE, 0)
        audioManager.adjustStreamVolume(android.media.AudioManager.STREAM_SYSTEM, android.media.AudioManager.ADJUST_UNMUTE, 0)
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition not available on this device")
            return
        }
        isActive = true
        isListeningForWakeWord = true
        createAndStartRecognizer()
    }

    fun startCommandListening() {
        isListeningForWakeWord = false
        destroyRecognizer()
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (isActive && !isListeningForWakeWord) {
                createAndStartRecognizer()
            }
        }, 300)
    }

    fun stopListening() {
        isActive = false
        destroyRecognizer()
    }

    private fun destroyRecognizer() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w("VoiceRecognition", "Error destroying recognizer: ${e.message}")
        }
        speechRecognizer = null
    }

    private fun createAndStartRecognizer() {
        destroyRecognizer()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("VoiceRecognition", "Ready for speech (wakeWord=$isListeningForWakeWord)")
                if (!isListeningForWakeWord) {
                    onListeningStarted()
                }
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d("VoiceRecognition", "End of speech")
            }

            override fun onError(error: Int) {
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "no_match"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "timeout"
                    SpeechRecognizer.ERROR_AUDIO -> "audio_error"
                    SpeechRecognizer.ERROR_CLIENT -> "client_error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "no_permission"
                    SpeechRecognizer.ERROR_NETWORK -> "network_error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "network_timeout"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "recognizer_busy"
                    SpeechRecognizer.ERROR_SERVER -> "server_error"
                    11 -> "server_disconnected"
                    else -> "unknown_error_$error"
                }
                
                // Do not clutter debug text for common background timeouts/disconnects
                if (error != 11 && error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    Log.d("VoiceRecognition", "Error: $errorMsg (wakeWord=$isListeningForWakeWord)")
                    onDebugText?.invoke("Error: $errorMsg")
                }

                if (isListeningForWakeWord && isActive) {
                    // If the recognizer is busy or client error, wait longer before restarting to avoid loops
                    val delay = if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_CLIENT || error == 11) 1500L else 500L
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        if (isActive && isListeningForWakeWord) {
                            createAndStartRecognizer()
                        }
                    }, delay)
                } else if (!isListeningForWakeWord) {
                    if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        onError("Non ho capito, riprova")
                    } else {
                        onError("Errore ascolto: $errorMsg")
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.lowercase() ?: ""
                if (isListeningForWakeWord) {
                    // Check if any wake word is present
                    val containsWakeWord = wakeWords.any { text.contains(it) }
                    if (containsWakeWord) {
                        Log.d("VoiceRecognition", "Wake word detected in: $text")
                        // Extract the command part after the wake word (if any)
                        var command = text
                        for (ww in wakeWords.sortedByDescending { it.length }) {
                            val idx = command.indexOf(ww)
                            if (idx >= 0) {
                                command = command.substring(idx + ww.length).trim()
                                break
                            }
                        }
                        if (command.isNotBlank()) {
                            // The user already said a command after the wake word. Process it directly!
                            onDebugText?.invoke(command)
                            onCommandReceived(command)
                        } else {
                            // Just the wake word, now listen for the actual command
                            onWakeWordDetected()
                        }
                    } else {
                        // No wake word detected, restart listening
                        if (isActive) {
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                if (isActive && isListeningForWakeWord) {
                                    createAndStartRecognizer()
                                }
                            }, 300)
                        }
                    }
                } else {
                    // Command mode - deliver the full text
                    if (text.isNotBlank()) {
                        onDebugText?.invoke(text)
                        onCommandReceived(text)
                    } else {
                        onError("Non ho capito, riprova")
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                if (!isListeningForWakeWord && text.isNotBlank()) {
                    onDebugText?.invoke(text)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "it-IT")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        if (isListeningForWakeWord) {
            muteBeepSoundOfRecorder()
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("VoiceRecognition", "Failed to start listening: ${e.message}")
            if (isActive) {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    if (isActive) createAndStartRecognizer()
                }, 1000)
            }
        }

        if (isListeningForWakeWord) {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                unmuteBeepSoundOfRecorder()
            }, 500)
        }
    }

    fun resetToWakeWordMode() {
        isListeningForWakeWord = true
        if (isActive) {
            createAndStartRecognizer()
        }
    }
}
