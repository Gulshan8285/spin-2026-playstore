package com.spinwin.rewards.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * Premium Sound & Haptic Manager
 * Provides synthetic acoustic waveforms for:
 * - Spin tick (wooden click)
 * - Win (coin drop + chime)
 * - Big win (celebration fanfare)
 * - Button tap (soft pop)
 * - Error (soft buzz)
 */
class SoundManager(private val context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    var isSoundEnabled: Boolean = true
    var isHapticEnabled: Boolean = true

    private val scope = CoroutineScope(Dispatchers.Default)

    fun playButtonTap() {
        if (isHapticEnabled) {
            triggerHaptic(HapticType.LIGHT)
        }
        if (isSoundEnabled) {
            scope.launch {
                // Soft pop: quick frequency sweep from 600Hz down to 200Hz over 35ms
                playToneSweep(600f, 200f, 35)
            }
        }
    }

    fun playSpinTick() {
        if (isHapticEnabled) {
            triggerHaptic(HapticType.LIGHT)
        }
        if (isSoundEnabled) {
            scope.launch {
                // Wooden click: sharp percussive transient at 1200Hz dropping to 300Hz in 15ms
                playToneSweep(1200f, 300f, 15, amplitude = 0.6f)
            }
        }
    }

    fun playWin() {
        if (isHapticEnabled) {
            triggerHaptic(HapticType.MEDIUM)
        }
        if (isSoundEnabled) {
            scope.launch {
                // Coin drop chime: two ascending harmonic notes (1318.5Hz - E6, 1760Hz - A6)
                playTone(1318.5f, 90, 0.5f)
                playTone(1760.0f, 160, 0.7f)
            }
        }
    }

    fun playBigWin() {
        if (isHapticEnabled) {
            triggerHaptic(HapticType.HEAVY)
        }
        if (isSoundEnabled) {
            scope.launch {
                // Celebration fanfare: C-E-G-C ascending arpeggio with sustain
                val notes = listOf(523.25f, 659.25f, 783.99f, 1046.50f)
                for (note in notes) {
                    playTone(note, 80, 0.7f)
                }
                playTone(1318.51f, 300, 0.9f)
            }
        }
    }

    fun playError() {
        if (isHapticEnabled) {
            triggerHaptic(HapticType.MEDIUM)
        }
        if (isSoundEnabled) {
            scope.launch {
                // Soft buzz: low 130Hz square-ish wave for 120ms
                playBuzz(130f, 120)
            }
        }
    }

    enum class HapticType {
        LIGHT, MEDIUM, HEAVY, CONTINUOUS
    }

    fun triggerHaptic(type: HapticType) {
        if (!isHapticEnabled || vibrator == null || !vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = when (type) {
                    HapticType.LIGHT -> VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE)
                    HapticType.MEDIUM -> VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE)
                    HapticType.HEAVY -> VibrationEffect.createWaveform(longArrayOf(0, 50, 40, 60), intArrayOf(0, 180, 0, 255), -1)
                    HapticType.CONTINUOUS -> VibrationEffect.createOneShot(15, 80)
                }
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                when (type) {
                    HapticType.LIGHT -> vibrator.vibrate(10)
                    HapticType.MEDIUM -> vibrator.vibrate(30)
                    HapticType.HEAVY -> vibrator.vibrate(70)
                    HapticType.CONTINUOUS -> vibrator.vibrate(15)
                }
            }
        } catch (_: Exception) {
            // Ignore system haptic errors
        }
    }

    private fun playTone(frequency: Float, durationMs: Int, amplitude: Float = 0.5f) {
        try {
            val sampleRate = 44100
            val numSamples = (durationMs * sampleRate) / 1000
            val samples = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val time = i.toDouble() / sampleRate
                // Sine wave with exponential decay envelope
                val envelope = 1.0 - (i.toDouble() / numSamples)
                val sampleValue = (sin(2.0 * Math.PI * frequency * time) * amplitude * envelope * Short.MAX_VALUE).toInt()
                samples[i] = sampleValue.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
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
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(samples, 0, samples.size)
            audioTrack.play()
            Thread.sleep(durationMs.toLong())
            audioTrack.release()
        } catch (_: Exception) {}
    }

    private fun playToneSweep(startFreq: Float, endFreq: Float, durationMs: Int, amplitude: Float = 0.5f) {
        try {
            val sampleRate = 44100
            val numSamples = (durationMs * sampleRate) / 1000
            val samples = ShortArray(numSamples)

            var phase = 0.0
            for (i in 0 until numSamples) {
                val progress = i.toDouble() / numSamples
                val currentFreq = startFreq + (endFreq - startFreq) * progress
                phase += 2.0 * Math.PI * currentFreq / sampleRate
                val envelope = 1.0 - progress
                val sampleValue = (sin(phase) * amplitude * envelope * Short.MAX_VALUE).toInt()
                samples[i] = sampleValue.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
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
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(samples, 0, samples.size)
            audioTrack.play()
            Thread.sleep(durationMs.toLong())
            audioTrack.release()
        } catch (_: Exception) {}
    }

    private fun playBuzz(frequency: Float, durationMs: Int) {
        try {
            val sampleRate = 44100
            val numSamples = (durationMs * sampleRate) / 1000
            val samples = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val time = i.toDouble() / sampleRate
                val sine = sin(2.0 * Math.PI * frequency * time)
                // Soft clip / square wave for buzzy dissonance
                val clipped = if (sine > 0.3) 0.5 else if (sine < -0.3) -0.5 else sine
                samples[i] = (clipped * Short.MAX_VALUE * 0.4).toInt().toShort()
            }

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
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
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(samples, 0, samples.size)
            audioTrack.play()
            Thread.sleep(durationMs.toLong())
            audioTrack.release()
        } catch (_: Exception) {}
    }

    companion object {
        @Volatile
        private var instance: SoundManager? = null

        fun getInstance(context: Context): SoundManager {
            return instance ?: synchronized(this) {
                instance ?: SoundManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
