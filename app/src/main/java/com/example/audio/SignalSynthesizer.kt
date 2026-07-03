package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.*
import java.util.Random
import kotlin.math.sin

class SignalSynthesizer {
    private val sampleRate = 22050
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false
    private var synthesizerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Synthesis parameters (thread-safe updates)
    @Volatile var droneVolume = 0.5f
    @Volatile var staticVolume = 0.1f
    @Volatile var signalFrequency = 100.0f
    @Volatile var pitchSweep = 0.0f

    init {
        initAudioTrack()
    }

    private fun initAudioTrack() {
        try {
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (e: Exception) {
            Log.e("SignalSynthesizer", "Failed to initialize AudioTrack", e)
        }
    }

    fun startAmbientDrone() {
        if (isPlaying) return
        isPlaying = true
        audioTrack?.play()

        synthesizerJob = scope.launch {
            val buffer = ShortArray(1024)
            var phase1 = 0.0
            var phase2 = 0.0
            val random = Random()

            while (isActive && isPlaying) {
                for (i in buffer.indices) {
                    // 1. Dread Drone (Binaural beating: 55Hz and 58Hz to cause low-frequency tension)
                    val freq1 = 55.0 + pitchSweep
                    val freq2 = 58.0 + pitchSweep
                    val angleIncrement1 = (2 * Math.PI * freq1) / sampleRate
                    val angleIncrement2 = (2 * Math.PI * freq2) / sampleRate

                    phase1 += angleIncrement1
                    phase2 += angleIncrement2

                    if (phase1 > 2 * Math.PI) phase1 -= 2 * Math.PI
                    if (phase2 > 2 * Math.PI) phase2 -= 2 * Math.PI

                    val droneSample1 = sin(phase1)
                    val droneSample2 = sin(phase2)
                    val droneSample = (droneSample1 + droneSample2) * 0.5 * droneVolume

                    // 2. Radio Static (White/Pink noise)
                    // High frequency static mixed in
                    val staticSample = (random.nextFloat() * 2.0 - 1.0) * staticVolume

                    // 3. Sentient signal whisper (strange modulation wave)
                    val modulation = sin(2 * Math.PI * 0.25 * i / sampleRate) // 0.25 Hz slow pulsing
                    val sentientSample = sin(2 * Math.PI * signalFrequency / sampleRate * i) * 0.2 * modulation * droneVolume

                    // Combine and clamp to Short range
                    val mixedSample = (droneSample + staticSample + sentientSample) * 32767.0
                    buffer[i] = mixedSample.coerceIn(-32768.0, 32767.0).toInt().toShort()
                }

                audioTrack?.write(buffer, 0, buffer.size)
                yield()
            }
        }
    }

    fun stop() {
        isPlaying = false
        synthesizerJob?.cancel()
        synthesizerJob = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
        } catch (e: Exception) {
            Log.e("SignalSynthesizer", "Error pausing AudioTrack", e)
        }
    }

    /**
     * Synthesizes a specific warning beep or chime
     */
    fun playChirp(highPitch: Boolean = false) {
        scope.launch {
            val durationMs = 150
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val buffer = ShortArray(numSamples)
            val freq = if (highPitch) 880.0 else 440.0
            val angleIncrement = (2 * Math.PI * freq) / sampleRate
            var phase = 0.0

            for (i in buffer.indices) {
                // Decay envelope so it sounds like a real beep
                val envelope = 1.0 - (i.toDouble() / numSamples)
                phase += angleIncrement
                val sample = sin(phase) * envelope * 0.5 * 32767.0
                buffer[i] = sample.toInt().toShort()
            }

            val tempTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            tempTrack.write(buffer, 0, buffer.size)
            tempTrack.play()
            delay(durationMs.toLong() + 50)
            tempTrack.release()
        }
    }

    /**
     * Generates a Morse code signal on a separate temporary audio track
     */
    fun playMorseCode(message: String) {
        scope.launch {
            val dotDurationMs = 100L
            val dashDurationMs = dotDurationMs * 3
            val charGapMs = dotDurationMs * 3
            val wordGapMs = dotDurationMs * 7

            val morseMap = mapOf(
                'E' to ".", 'T' to "-", 'H' to "....", 'A' to ".-", 'N' to "-.",
                'S' to "...", 'O' to "---"
            )

            for (char in message.uppercase()) {
                if (char == ' ') {
                    delay(wordGapMs)
                    continue
                }

                val code = morseMap[char] ?: continue
                for (symbol in code) {
                    val duration = if (symbol == '.') dotDurationMs else dashDurationMs
                    playTone(650.0, duration.toInt())
                    delay(duration + dotDurationMs) // space between elements
                }
                delay(charGapMs)
            }
        }
    }

    private suspend fun playTone(frequency: Double, durationMs: Int) {
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        val angleIncrement = (2 * Math.PI * frequency) / sampleRate
        var phase = 0.0

        for (i in buffer.indices) {
            // Smooth attack and release to avoid clicks
            val envelope = when {
                i < 200 -> i / 200.0
                i > numSamples - 200 -> (numSamples - i) / 200.0
                else -> 1.0
            }
            phase += angleIncrement
            val sample = sin(phase) * envelope * 0.4 * 32767.0
            buffer[i] = sample.toInt().toShort()
        }

        val toneTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .build()
            )
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        toneTrack.write(buffer, 0, buffer.size)
        toneTrack.play()
        delay(durationMs.toLong() + 20)
        toneTrack.release()
    }

    fun release() {
        stop()
        audioTrack?.release()
        audioTrack = null
        scope.cancel()
    }
}
