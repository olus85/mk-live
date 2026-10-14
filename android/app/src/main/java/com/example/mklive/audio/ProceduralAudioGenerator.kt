package com.example.mklive.audio

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Procedural synthesizer for arcade-quality 16-bit PCM WAV audio effects.
 * Synthesizes crisp 44.1 kHz sound effects locally on first run so no external
 * media assets are needed.
 */
object ProceduralAudioGenerator {

    private const val SAMPLE_RATE = 44100

    fun ensureSfxAssets(context: Context): Map<SoundId, File> {
        val sfxDir = File(context.cacheDir, "sfx").apply { if (!exists()) mkdirs() }
        val soundMap = mutableMapOf<SoundId, File>()

        SoundId.values().forEach { soundId ->
            val file = File(sfxDir, "${soundId.name.lowercase()}.wav")
            if (!file.exists() || file.length() < 100) {
                val samples = generateSamples(soundId)
                writeWavFile(file, samples, SAMPLE_RATE)
            }
            soundMap[soundId] = file
        }

        return soundMap
    }

    private fun generateSamples(soundId: SoundId): ShortArray {
        return when (soundId) {
            SoundId.COIN -> generateCoin()
            SoundId.COUNTDOWN_BEEP -> generateCountdownBeep()
            SoundId.COUNTDOWN_GO -> generateCountdownGo()
            SoundId.BOX_HIT -> generateBoxHit()
            SoundId.ROULETTE_TICK -> generateRouletteTick()
            SoundId.ITEM_AWARDED -> generateItemAwarded()
            SoundId.MINIGUN_SHOT -> generateMinigunShot()
            SoundId.ROCKET_LAUNCH -> generateRocketLaunch()
            SoundId.EXPLOSION -> generateExplosion()
            SoundId.BANANA_DROP -> generateBananaDrop()
            SoundId.SLIP_SPIN -> generateSlipSpin()
            SoundId.BOOST_TURBO -> generateBoostTurbo()
            SoundId.HORN -> generateHorn()
            SoundId.LOCK_ON -> generateLockOn()
            SoundId.ENGINE_LOOP -> generateEngineLoop()
        }
    }

    // Classic Mario Two-tone Coin: B5 (987.77 Hz) -> E6 (1318.51 Hz)
    private fun generateCoin(): ShortArray {
        val durationMs = 380
        val totalSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(totalSamples)
        val noteSplit = (SAMPLE_RATE * 0.080).toInt()

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val freq = if (i < noteSplit) 987.77 else 1318.51
            val decay = if (i < noteSplit) 1.0 else exp(-8.0 * (t - 0.080))
            val wave = (sin(2.0 * PI * freq * t) + 0.3 * sin(2.0 * PI * freq * 2.0 * t)) * decay
            samples[i] = (wave.coerceIn(-1.0, 1.0) * 28000).toInt().toShort()
        }
        return samples
    }

    // Countdown 440 Hz low beep
    private fun generateCountdownBeep(): ShortArray {
        val durationMs = 200
        val total = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(total)

        for (i in 0 until total) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = when {
                i < 441 -> i / 441.0
                i > total - 441 -> (total - i) / 441.0
                else -> 1.0
            }
            val wave = sin(2.0 * PI * 440.0 * t) * env
            samples[i] = (wave * 26000).toInt().toShort()
        }
        return samples
    }

    // Countdown GO Fanfare (A-Major chord: 880 + 1108 + 1320 Hz)
    private fun generateCountdownGo(): ShortArray {
        val durationMs = 650
        val total = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(total)

        for (i in 0 until total) {
            val t = i.toDouble() / SAMPLE_RATE
            val decay = exp(-4.5 * t)
            val wave = (0.5 * sin(2.0 * PI * 880.0 * t) +
                    0.35 * sin(2.0 * PI * 1108.0 * t) +
                    0.35 * sin(2.0 * PI * 1320.0 * t)) * decay
            samples[i] = (wave.coerceIn(-1.0, 1.0) * 29000).toInt().toShort()
        }
        return samples
    }

    // Holographic Box Shatter (crystal cluster + noise)
    private fun generateBoxHit(): ShortArray {
        val durationMs = 260
        val total = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(total)
        val rnd = Random(42)

        for (i in 0 until total) {
            val t = i.toDouble() / SAMPLE_RATE
            val decay = exp(-12.0 * t)
            val tone = 0.3 * sin(2.0 * PI * 1760.0 * t) +
                    0.3 * sin(2.0 * PI * 2489.0 * t) +
                    0.2 * sin(2.0 * PI * 3520.0 * t)
            val noise = (rnd.nextDouble() * 2.0 - 1.0) * exp(-20.0 * t) * 0.4
            val wave = (tone + noise) * decay
            samples[i] = (wave.coerceIn(-1.0, 1.0) * 27000).toInt().toShort()
        }
        return samples
    }

    // Roulette Tick (woodblock impulse click)
    private fun generateRouletteTick(): ShortArray {
        val durationMs = 25
        val total = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(total)

        for (i in 0 until total) {
            val t = i.toDouble() / SAMPLE_RATE
            val wave = sin(2.0 * PI * 1400.0 * t) * exp(-120.0 * t)
            samples[i] = (wave.coerceIn(-1.0, 1.0) * 26000).toInt().toShort()
        }
        return samples
    }

    // Item Awarded Fanfare (Arpeggio: C5, E5, G5, C6)
    private fun generateItemAwarded(): ShortArray {
        val durationMs = 450
        val total = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(total)
        val noteDuration = (SAMPLE_RATE * 0.08).toInt()
        val freqs = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)

        for (i in 0 until total) {
            val noteIdx = minOf(i / noteDuration, freqs.size - 1)
            val t = i.toDouble() / SAMPLE_RATE
            val localT = (i % noteDuration).toDouble() / SAMPLE_RATE
            val decay = if (noteIdx == freqs.size - 1) exp(-5.0 * (t - 0.24)) else exp(-3.0 * localT)
            val wave = (sin(2.0 * PI * freqs[noteIdx] * t) + 0.25 * sin(4.0 * PI * freqs[noteIdx] * t)) * decay
            samples[i] = (wave.coerceIn(-1.0, 1.0) * 28000).toInt().toShort()
        }
        return samples
    }

    // Minigun Rapid Fire Shot (Punchy punch + metal clack)
    private fun generateMinigunShot(): ShortArray {
        val durationMs = 55
        val total = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(total)
        val rnd = Random(1337)

        for (i in 0 until total) {
            val t = i.toDouble() / SAMPLE_RATE
            val decay = exp(-75.0 * t)
            val bass = sin(2.0 * PI * (90.0 - 500.0 * t).coerceAtLeast(40.0) * t) * 0.6
            val noise = (rnd.nextDouble() * 2.0 - 1.0) * 0.5 * exp(-90.0 * t)
            val clack = sin(2.0 * PI * 650.0 * t) * 0.3 * exp(-120.0 * t)
            val wave = (bass + noise + clack) * decay
            samples[i] = (wave.coerceIn(-1.0, 1.0) * 30000).toInt().toShort()
        }
        return samples
    }

    // Rocket Launch Whoosh
    private fun generateRocketLaunch(): ShortArray {
        val durationMs = 450
        val total = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(total)
        val rnd = Random(99)

        for (i in 0 until total) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = t / (durationMs / 1000.0)
            val freq = 180.0 + 550.0 * progress
            val env = if (progress < 0.2) progress / 0.2 else (1.0 - progress)
            val tone = sin(2.0 * PI * freq * t) * 0.4
            val noise = (rnd.nextDouble() * 2.0 - 1.0) * 0.6
            val wave = (tone + noise) * env
            samples[i] = (wave.coerceIn(-1.0, 1.0) * 28000).toInt().toShort()
        }
        return samples
    }

    // Rocket / Bob-omb comic explosion
    private fun generateExplosion(): ShortArray {
        val durationMs = 850
        val total = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(total)
        val rnd = Random(555)

        for (i in 0 until total) {
            val t = i.toDouble() / SAMPLE_RATE
            val decay = exp(-3.8 * t)
            val bass = sin(2.0 * PI * (65.0 - 35.0 * t).coerceAtLeast(20.0) * t) * 0.7
            val noise = (rnd.nextDouble() * 2.0 - 1.0) * 0.7 * exp(-4.2 * t)
            val wave = (bass + noise) * decay
            samples[i] = (wave.coerceIn(-1.0, 1.0) * 31000).toInt().toShort()
        }
        return samples
    }

    // Banana Drop ("Fwip" slide down)
    private fun generateBananaDrop(): ShortArray {
        val durationMs = 220
        val total = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(total)

        for (i in 0 until total) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = t / (durationMs / 1000.0)
            val freq = 900.0 - 650.0 * (progress * progress)
            val env = exp(-7.0 * t)
            val wave = sin(2.0 * PI * freq * t) * env
            samples[i] = (wave.coerceIn(-1.0, 1.0) * 27000).toInt().toShort()
        }
        return samples
    }

    // Banana Slip & Spin (Tire squeak vibrato)
    private fun generateSlipSpin(): ShortArray {
        val durationMs = 550
        val total = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(total)

        for (i in 0 until total) {
            val t = i.toDouble() / SAMPLE_RATE
            val vibrato = sin(2.0 * PI * 35.0 * t) * 300.0
            val baseFreq = 1800.0 + vibrato
            val env = exp(-3.5 * t)
            val wave = sin(2.0 * PI * baseFreq * t) * env
            samples[i] = (wave.coerceIn(-1.0, 1.0) * 28000).toInt().toShort()
        }
        return samples
    }

    // Turbo Boost whoosh
    private fun generateBoostTurbo(): ShortArray {
        val durationMs = 600
        val total = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(total)
        val rnd = Random(777)

        for (i in 0 until total) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = t / (durationMs / 1000.0)
            val env = sin(PI * progress)
            val jetFreq = 300.0 + 800.0 * progress
            val tone = sin(2.0 * PI * jetFreq * t) * 0.4
            val noise = (rnd.nextDouble() * 2.0 - 1.0) * 0.6
            val wave = (tone + noise) * env
            samples[i] = (wave.coerceIn(-1.0, 1.0) * 29000).toInt().toShort()
        }
        return samples
    }

    // Horn (Dual tone 370 + 440 Hz)
    private fun generateHorn(): ShortArray {
        val durationMs = 320
        val total = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(total)

        for (i in 0 until total) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = when {
                i < 441 -> i / 441.0
                i > total - 882 -> (total - i) / 882.0
                else -> 1.0
            }
            val wave = (0.5 * sin(2.0 * PI * 370.0 * t) + 0.5 * sin(2.0 * PI * 440.0 * t)) * env
            samples[i] = (wave.coerceIn(-1.0, 1.0) * 26000).toInt().toShort()
        }
        return samples
    }

    // Rocket Lock-On high beep pulse
    private fun generateLockOn(): ShortArray {
        val durationMs = 180
        val total = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(total)
        val pulse1 = (SAMPLE_RATE * 0.06).toInt()
        val gap = (SAMPLE_RATE * 0.03).toInt()
        val pulse2 = pulse1 + gap + (SAMPLE_RATE * 0.08).toInt()

        for (i in 0 until total) {
            val active = (i < pulse1) || (i in (pulse1 + gap)..pulse2)
            if (active) {
                val t = i.toDouble() / SAMPLE_RATE
                val wave = sin(2.0 * PI * 1760.0 * t)
                samples[i] = (wave * 26000).toInt().toShort()
            } else {
                samples[i] = 0
            }
        }
        return samples
    }

    // Continuous Kart Engine Harmonic Loop (500 ms seamless loop)
    private fun generateEngineLoop(): ShortArray {
        val durationMs = 500
        val total = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(total)
        val baseFreq = 62.0 // Deep 2-stroke kart engine idle

        for (i in 0 until total) {
            val t = i.toDouble() / SAMPLE_RATE
            // Sum harmonics for rich combustion engine texture
            val wave = 0.50 * sin(2.0 * PI * baseFreq * t) +
                    0.25 * sin(2.0 * PI * (baseFreq * 2.0) * t) +
                    0.15 * sin(2.0 * PI * (baseFreq * 3.0) * t) +
                    0.10 * sin(2.0 * PI * (baseFreq * 4.0) * t)
            samples[i] = (wave.coerceIn(-1.0, 1.0) * 22000).toInt().toShort()
        }
        return samples
    }

    private fun writeWavFile(file: File, samples: ShortArray, sampleRate: Int) {
        val dataSize = samples.size * 2
        val totalSize = 36 + dataSize
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray())
            putInt(totalSize)
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16) // Subchunk1Size (16 for PCM)
            putShort(1) // AudioFormat (1 = PCM)
            putShort(1) // NumChannels (1 = Mono)
            putInt(sampleRate)
            putInt(sampleRate * 2) // ByteRate
            putShort(2) // BlockAlign
            putShort(16) // BitsPerSample
            put("data".toByteArray())
            putInt(dataSize)
        }

        FileOutputStream(file).use { out ->
            out.write(header.array())
            val buffer = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            samples.forEach { buffer.putShort(it) }
            out.write(buffer.array())
        }
    }
}
