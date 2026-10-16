package com.example.game.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import kotlin.concurrent.thread
import kotlin.math.sin
import kotlin.random.Random

/**
 * Real-time Procedural Audio & Haptic engine for 3D racing simulator.
 * Generates dynamic engine RPM frequencies, tire drift screech, nitro whoosh and crash impacts.
 */
class GameAudio(private val context: Context, private val prefs: GamePreferences) {

    private var vibrator: Vibrator? = try {
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    } catch (e: Exception) {
        null
    }

    @Volatile
    private var isRunning = false
    private var audioTrack: AudioTrack? = null
    private var audioThread: Thread? = null

    // Target parameters updated each frame from physics
    @Volatile var targetRpm: Float = 1000f // 800 - 8000 RPM
    @Volatile var throttle: Float = 0f
    @Volatile var driftIntensity: Float = 0f // 0f to 1f
    @Volatile var nitroActive: Boolean = false
    @Volatile var triggerCrash: Boolean = false
    @Volatile var triggerGunshot: Boolean = false
    @Volatile var isPlasmaGun: Boolean = false
    @Volatile var triggerExplosion: Boolean = false

    private val sampleRate = 22050
    private val bufferSize = 1024

    fun start() {
        if (!prefs.soundEnabled) return
        if (isRunning) return
        isRunning = true

        try {
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val actualBuf = maxOf(minBuf, bufferSize * 2)

            audioTrack = AudioTrack.Builder()
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
                .setBufferSizeInBytes(actualBuf)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()

            audioThread = thread(name = "GameAudioThread", priority = Thread.MAX_PRIORITY) {
                runAudioLoop()
            }
        } catch (e: Exception) {
            isRunning = false
        }
    }

    private fun runAudioLoop() {
        val buffer = ShortArray(bufferSize)
        var phase1 = 0.0
        var phase2 = 0.0
        var phase3 = 0.0
        var currentRpm = 1000f
        var crashFramesRemaining = 0
        var gunshotFramesRemaining = 0
        var explosionFramesRemaining = 0
        var plasmaTonePhase = 0.0

        while (isRunning) {
            if (!prefs.soundEnabled) {
                try { Thread.sleep(50) } catch (e: Exception) {}
                continue
            }

            // Smooth RPM transition
            currentRpm += (targetRpm - currentRpm) * 0.15f
            val baseFreq = (currentRpm / 60.0) * 2.5 // V8 firing pulses: approx 40Hz to 350Hz

            val isDrifting = driftIntensity > 0.2f
            val isNitro = nitroActive

            if (triggerCrash) {
                crashFramesRemaining = 12 // ~0.5s of impact
                triggerCrash = false
                vibrate(120)
            }

            if (triggerGunshot) {
                gunshotFramesRemaining = if (isPlasmaGun) 4 else 3
                triggerGunshot = false
                vibrate(if (isPlasmaGun) 25 else 45)
            }

            if (triggerExplosion) {
                explosionFramesRemaining = 9
                triggerExplosion = false
                vibrate(85)
            }

            for (i in 0 until bufferSize) {
                // Engine tone: Harmonics of firing cycle
                val dt = 1.0 / sampleRate
                phase1 += 2.0 * Math.PI * baseFreq * dt
                phase2 += 2.0 * Math.PI * (baseFreq * 2.0) * dt
                phase3 += 2.0 * Math.PI * (baseFreq * 0.5) * dt

                if (phase1 > 2.0 * Math.PI) phase1 -= 2.0 * Math.PI
                if (phase2 > 2.0 * Math.PI) phase2 -= 2.0 * Math.PI
                if (phase3 > 2.0 * Math.PI) phase3 -= 2.0 * Math.PI

                // Rich engine timbre combining triangle-like harmonics
                val saw1 = (phase1 / Math.PI - 1.0).toFloat()
                val sub = sin(phase3).toFloat()
                val high = (phase2 / Math.PI - 1.0).toFloat() * 0.3f
                var engineSample = (saw1 * 0.6f + sub * 0.3f + high * 0.2f) * (0.35f + throttle * 0.45f)

                // Nitro roar (rushing high-frequency noise & low rumble)
                if (isNitro) {
                    val nitroNoise = (Random.nextFloat() * 2f - 1f) * 0.4f
                    val nitroSub = sin(phase1 * 1.5).toFloat() * 0.3f
                    engineSample = engineSample * 0.7f + nitroNoise + nitroSub
                }

                // Tire screech (modulated bandpass noise)
                if (isDrifting) {
                    val driftNoise = (Random.nextFloat() * 2f - 1f) * (driftIntensity * 0.5f)
                    val squeal = sin(i * 0.28).toFloat() * (driftIntensity * 0.3f)
                    engineSample += driftNoise + squeal
                }

                // Crash noise burst
                if (crashFramesRemaining > 0) {
                    val crashNoise = (Random.nextFloat() * 2f - 1f) * 0.9f
                    engineSample = crashNoise
                }

                // Gunshot synthesis: Sharp transient impulse + sub boom / plasma laser sweep
                if (gunshotFramesRemaining > 0) {
                    if (isPlasmaGun) {
                        plasmaTonePhase += 2.0 * Math.PI * (880.0 - gunshotFramesRemaining * 120.0) * dt
                        val laserZap = sin(plasmaTonePhase).toFloat() * 0.75f
                        val laserHiss = (Random.nextFloat() * 2f - 1f) * 0.25f
                        engineSample += laserZap + laserHiss
                    } else {
                        val gunCrack = (Random.nextFloat() * 2f - 1f) * 0.85f
                        val gunPunch = sin(phase1 * 3.0).toFloat() * 0.45f
                        engineSample += gunCrack + gunPunch
                    }
                }

                // Explosion synthesis: Rumbling low frequency blast + debris noise
                if (explosionFramesRemaining > 0) {
                    val boomSub = sin(i * 0.045).toFloat() * 0.7f
                    val boomRumble = (Random.nextFloat() * 2f - 1f) * 0.55f
                    engineSample += boomSub + boomRumble
                }

                // Clamp to Short
                val clamped = engineSample.coerceIn(-1f, 1f)
                buffer[i] = (clamped * 30000).toInt().toShort()
            }

            if (crashFramesRemaining > 0) crashFramesRemaining--
            if (gunshotFramesRemaining > 0) gunshotFramesRemaining--
            if (explosionFramesRemaining > 0) explosionFramesRemaining--

            audioTrack?.write(buffer, 0, bufferSize)
        }
    }

    fun vibrate(durationMs: Long) {
        if (!prefs.hapticsEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (e: Exception) {}
    }

    fun onDrift(intensity: Float) {
        driftIntensity = intensity.coerceIn(0f, 1f)
        if (intensity > 0.4f && Random.nextFloat() < 0.15f) {
            vibrate(30)
        }
    }

    fun stop() {
        isRunning = false
        try {
            audioThread?.join(300)
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {}
        audioTrack = null
    }
}
