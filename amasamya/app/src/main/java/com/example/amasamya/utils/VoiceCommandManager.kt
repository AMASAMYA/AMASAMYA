package com.example.amasamya.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.example.amasamya.service.A11yAuditService
import java.util.Locale

class VoiceCommandManager(
    private val context: Context,
    private val onCommandRecognized: (String) -> Unit
) : RecognitionListener {

    companion object {
        private const val TAG = "VoiceCommandManager"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    fun startListening() {
        if (isListening) return
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(this@VoiceCommandManager)
                }
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }
                speechRecognizer?.startListening(intent)
                isListening = true
                Log.d(TAG, "SpeechRecognizer started listening...")
            } else {
                Log.w(TAG, "Speech Recognition is not available on this device.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing SpeechRecognizer", e)
        }
    }

    fun stopListening() {
        if (!isListening) return
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
            isListening = false
            Log.d(TAG, "SpeechRecognizer stopped.")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping SpeechRecognizer", e)
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {
        isListening = false
    }

    override fun onError(error: Int) {
        isListening = false
        Log.d(TAG, "Speech recognition error code: $error")
    }

    override fun onResults(results: Bundle?) {
        isListening = false
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
        val commandText = matches.joinToString(" ").lowercase(Locale.getDefault())
        Log.d(TAG, "Voice command received: $commandText")
        
        processCommand(commandText)
    }

    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}

    private fun processCommand(text: String) {
        val service = A11yAuditService.instance
        when {
            text.contains("scan") -> {
                onCommandRecognized("Executing voice command: Scan Screen")
                service?.performFullScreenScan()
            }
            text.contains("start session") || text.contains("start audit") || text.contains("start recording") -> {
                onCommandRecognized("Executing voice command: Start Session")
                service?.startAuditSession("Voice Session Audit")
            }
            text.contains("stop session") || text.contains("stop audit") || text.contains("stop recording") -> {
                onCommandRecognized("Executing voice command: Stop Session")
                service?.stopAuditSession()
            }
            text.contains("summary") || text.contains("status") || text.contains("read report") -> {
                onCommandRecognized("Executing voice command: Announcing Summary")
                service?.speak("AMASAMYA Service Active. Say scan screen or stop session to control audits.")
            }
        }
    }
}
