package com.example.mklive.audio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Dynamic Kart Engine Synthesizer.
 * Continuously scales the pitch and volume of the engine audio loop
 * based on live throttle telemetry and boost state.
 */
class EngineSoundSynthesizer(private val soundManager: SoundManager) {

    private var engineStreamId: Int = 0
    private var isRunning: Boolean = false
    private var updateJob: Job? = null

    private var targetPitch: Float = 0.8f
    private var currentPitch: Float = 0.8f
    private var targetVolume: Float = 0.25f
    private var currentVolume: Float = 0.25f

    fun start(scope: CoroutineScope) {
        if (isRunning) return
        isRunning = true

        updateJob = scope.launch(Dispatchers.Default) {
            // Wait briefly for sound pool to finish preloading
            delay(400)
            engineStreamId = soundManager.play(SoundId.ENGINE_LOOP, volume = 0.25f, rate = 0.8f, loop = -1)

            while (isActive && isRunning) {
                delay(33) // ~30 Hz smooth interpolation
                if (engineStreamId == 0) {
                    engineStreamId = soundManager.play(SoundId.ENGINE_LOOP, volume = currentVolume, rate = currentPitch, loop = -1)
                }

                // Smoothly interpolate pitch and volume towards targets
                currentPitch += (targetPitch - currentPitch) * 0.25f
                currentVolume += (targetVolume - currentVolume) * 0.25f

                if (engineStreamId > 0) {
                    soundManager.setPitch(engineStreamId, currentPitch)
                    soundManager.setVolume(engineStreamId, currentVolume)
                }
            }
        }
    }

    fun updateTelemetry(throttle: Int, isBraking: Boolean, isBoost: Boolean) {
        if (!soundManager.isEngineSoundEnabled) {
            targetVolume = 0.0f
            return
        }

        val forwardThrottle = throttle.coerceAtLeast(0)
        val normalized = (forwardThrottle / 127f).coerceIn(0f, 1f)

        targetPitch = when {
            isBoost -> 2.2f
            isBraking -> 0.7f
            forwardThrottle > 0 -> 0.8f + (normalized * 1.2f)
            else -> 0.8f // Idle
        }

        targetVolume = when {
            isBoost -> 0.85f
            forwardThrottle > 0 -> 0.35f + (normalized * 0.45f)
            else -> 0.22f // Quiet idle
        }
    }

    fun stop() {
        isRunning = false
        updateJob?.cancel()
        updateJob = null
        if (engineStreamId > 0) {
            soundManager.stop(engineStreamId)
            engineStreamId = 0
        }
    }
}
