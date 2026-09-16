package com.example.amasamya.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

object AudioHapticRadar {

    private const val TAG = "AudioHapticRadar"

    enum class TargetType {
        CRITICAL_UNLABELLED, // Low pitch (220 Hz) + Heavy pulse
        WARNING_SMALL_TARGET, // Medium pitch (440 Hz) + Double pulse
        COMPLIANT_TARGET      // High pitch (880 Hz) + Light buzz
    }

    fun playFeedback(context: Context, type: TargetType) {
        val (frequencyHz, durationMs) = when (type) {
            TargetType.CRITICAL_UNLABELLED -> Pair(220, 150)
            TargetType.WARNING_SMALL_TARGET -> Pair(440, 100)
            TargetType.COMPLIANT_TARGET -> Pair(880, 60)
        }

        playAudioTone(frequencyHz, durationMs)
        playHapticPulse(context, type)
    }

    private fun playAudioTone(frequencyHz: Int, durationMs: Int) {
        try {
            val sampleRate = 44100
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val sample = DoubleArray(numSamples)
            val generatedSnd = ByteArray(2 * numSamples)

            for (i in 0 until numSamples) {
                sample[i] = Math.sin(2.0 * Math.PI * i.toDouble() / (sampleRate / frequencyHz))
            }

            var idx = 0
            for (dVal in sample) {
                val valInt = (dVal * 32767).toInt().toShort()
                generatedSnd[idx++] = (valInt.toInt() and 0x00ff).toByte()
                generatedSnd[idx++] = ((valInt.toInt() and 0xff00) shr 8).toByte()
            }

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(generatedSnd.size)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(generatedSnd, 0, generatedSnd.size)
            audioTrack.play()
            
            // Release audio track resources after playback
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    audioTrack.stop()
                    audioTrack.release()
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping AudioTrack", e)
                }
            }, (durationMs + 50).toLong())
        } catch (e: Exception) {
            Log.e(TAG, "Audio tone playback error", e)
        }
    }

    private fun playHapticPulse(context: Context, type: TargetType) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = when (type) {
                    TargetType.CRITICAL_UNLABELLED -> VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                    TargetType.WARNING_SMALL_TARGET -> VibrationEffect.createWaveform(longArrayOf(0, 40, 40, 40), -1)
                    TargetType.COMPLIANT_TARGET -> VibrationEffect.createOneShot(30, 80)
                }
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                when (type) {
                    TargetType.CRITICAL_UNLABELLED -> vibrator.vibrate(100)
                    TargetType.WARNING_SMALL_TARGET -> vibrator.vibrate(longArrayOf(0, 40, 40, 40), -1)
                    TargetType.COMPLIANT_TARGET -> vibrator.vibrate(30)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Haptic pulse playback error", e)
        }
    }
}
