package com.example.mklive.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Ultra-low-latency SoundPool Audio Engine for Mario Kart Live.
 * Preloads all synthesized arcade sound effects into memory for instant (<10 ms) playback.
 */
class SoundManager(private val context: Context) {

    private val TAG = "SoundManager"

    var isSfxEnabled: Boolean = true
    var isEngineSoundEnabled: Boolean = true

    private val soundPool: SoundPool
    private val soundIdMap = ConcurrentHashMap<SoundId, Int>()
    private val loadedSounds = ConcurrentHashMap.newKeySet<Int>()

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(16)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedSounds.add(sampleId)
            }
        }

        // Synthesize and load sounds in background
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val assetMap = ProceduralAudioGenerator.ensureSfxAssets(context)
                assetMap.forEach { (soundId, file) ->
                    val poolId = soundPool.load(file.absolutePath, 1)
                    soundIdMap[soundId] = poolId
                }
                Log.i(TAG, "Audio engine initialized: ${soundIdMap.size} sounds preloaded")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize sound engine: ${e.message}", e)
            }
        }
    }

    fun play(soundId: SoundId, volume: Float = 1.0f, rate: Float = 1.0f, loop: Int = 0): Int {
        if (!isSfxEnabled && soundId != SoundId.ENGINE_LOOP) return 0
        if (!isEngineSoundEnabled && soundId == SoundId.ENGINE_LOOP) return 0

        val poolId = soundIdMap[soundId] ?: return 0
        if (!loadedSounds.contains(poolId)) return 0

        return soundPool.play(poolId, volume, volume, 1, loop, rate.coerceIn(0.5f, 2.5f))
    }

    fun stop(streamId: Int) {
        if (streamId > 0) {
            soundPool.stop(streamId)
        }
    }

    fun setPitch(streamId: Int, rate: Float) {
        if (streamId > 0) {
            soundPool.setRate(streamId, rate.coerceIn(0.5f, 2.5f))
        }
    }

    fun setVolume(streamId: Int, volume: Float) {
        if (streamId > 0) {
            soundPool.setVolume(streamId, volume, volume)
        }
    }

    // High-level Sound Triggers
    fun playCoin() = play(SoundId.COIN, 0.95f)
    fun playBoxHit() = play(SoundId.BOX_HIT, 1.0f)
    fun playRouletteTick() = play(SoundId.ROULETTE_TICK, 0.7f)
    fun playItemAwarded() = play(SoundId.ITEM_AWARDED, 1.0f)
    fun playMinigunShot() = play(SoundId.MINIGUN_SHOT, 0.85f, (0.95f + (Math.random().toFloat() * 0.1f)))
    fun playRocketLaunch() = play(SoundId.ROCKET_LAUNCH, 1.0f)
    fun playExplosion() = play(SoundId.EXPLOSION, 1.0f)
    fun playBananaDrop() = play(SoundId.BANANA_DROP, 0.9f)
    fun playSlip() = play(SoundId.SLIP_SPIN, 1.0f)
    fun playBoostTurbo() = play(SoundId.BOOST_TURBO, 1.0f)
    fun playHorn() = play(SoundId.HORN, 0.95f)
    fun playLockOn() = play(SoundId.LOCK_ON, 0.9f)
    fun playCountdownBeep() = play(SoundId.COUNTDOWN_BEEP, 0.9f)
    fun playCountdownGo() = play(SoundId.COUNTDOWN_GO, 1.0f)

    fun release() {
        soundPool.release()
    }
}
