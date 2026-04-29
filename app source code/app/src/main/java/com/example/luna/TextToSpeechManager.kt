package com.example.luna

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class TextToSpeechManager(context: Context) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var onDoneCallback: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.ITALIAN)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // Fallback to default locale
                    tts?.setLanguage(Locale.getDefault())
                    Log.w("TTS", "Italian not available, using default locale")
                } else {
                    try {
                        val voices = tts?.voices
                        if (voices != null) {
                            // Try to find a high-quality network voice or high-quality local voice
                            val bestVoice = voices.firstOrNull { it.locale.language == "it" && it.name.contains("network", ignoreCase = true) }
                                ?: voices.firstOrNull { it.locale.language == "it" && it.quality >= android.speech.tts.Voice.QUALITY_HIGH }
                                ?: voices.firstOrNull { it.locale.language == "it" }
                            if (bestVoice != null) {
                                tts?.voice = bestVoice
                                Log.d("TTS", "Selected premium voice: ${bestVoice.name}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("TTS", "Failed to get premium voices: ${e.message}")
                    }
                }
                isInitialized = true
                Log.d("TTS", "TextToSpeech initialized successfully")

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        Log.d("TTS", "Started speaking: $utteranceId")
                    }

                    override fun onDone(utteranceId: String?) {
                        Log.d("TTS", "Done speaking: $utteranceId")
                        onDoneCallback?.invoke()
                        onDoneCallback = null
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        Log.e("TTS", "Error speaking: $utteranceId")
                        onDoneCallback?.invoke()
                        onDoneCallback = null
                    }
                })
            } else {
                Log.e("TTS", "TextToSpeech initialization failed with status: $status")
            }
        }
    }

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (!isInitialized) {
            Log.w("TTS", "TTS not initialized yet, skipping speak")
            onDone?.invoke()
            return
        }
        onDoneCallback = onDone
        val params = android.os.Bundle()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "luna_utterance_${System.currentTimeMillis()}")
    }

    fun stop() {
        tts?.stop()
        onDoneCallback = null
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
