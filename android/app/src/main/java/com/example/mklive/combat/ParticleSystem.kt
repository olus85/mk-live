package com.example.mklive.combat

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class ParticleType {
    GOLD_STAR,
    SHARD,
    EXPLOSION_SPARK,
    SMOKE,
    MUZZLE_FLASH,
    SPEED_LINE
}

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var size: Float,
    var color: Int,
    var alpha: Float,
    var lifeSec: Float,
    var maxLifeSec: Float,
    val type: ParticleType
)

/**
 * High-performance 2D/3D Particle System and Screen Shake Controller.
 */
class ParticleSystem {

    private val particles = CopyOnWriteArrayList<Particle>()
    private val random = Random(12345)

    var screenShakeAmount: Float = 0f
    var screenShakeOffsetX: Float = 0f
    var screenShakeOffsetY: Float = 0f

    companion object {
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
    }

    fun update(dt: Float) {
        // Update screen shake decay
        if (screenShakeAmount > 0f) {
            screenShakeOffsetX = (random.nextFloat() * 2f - 1f) * screenShakeAmount
            screenShakeOffsetY = (random.nextFloat() * 2f - 1f) * screenShakeAmount
            screenShakeAmount = (screenShakeAmount - 25f * dt).coerceAtLeast(0f)
        } else {
            screenShakeOffsetX = 0f
            screenShakeOffsetY = 0f
        }

        // Update particles
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.lifeSec -= dt
            if (p.lifeSec <= 0f) {
                particles.remove(p)
                continue
            }

            p.x += p.vx * dt
            p.y += p.vy * dt
            p.alpha = (p.lifeSec / p.maxLifeSec).coerceIn(0f, 1f)

            // Apply gravity/drag depending on type
            when (p.type) {
                ParticleType.GOLD_STAR -> {
                    p.vy += 80f * dt // Slight gravity
                }
                ParticleType.SHARD, ParticleType.EXPLOSION_SPARK -> {
                    p.vx *= (1f - 1.5f * dt)
                    p.vy += 120f * dt // Gravity
                }
                ParticleType.SMOKE -> {
                    p.vy -= 15f * dt // Upward rise
                    p.size += 20f * dt // Expanding smoke
                }
                ParticleType.SPEED_LINE -> {
                    p.size += 300f * dt
                }
                else -> {}
            }
        }
    }

    fun triggerScreenShake(amount: Float) {
        screenShakeAmount = maxOf(screenShakeAmount, amount)
    }

    fun emitCoinStars(screenX: Float, screenY: Float) {
        val count = 10
        val goldColors = intArrayOf(
            Color.parseColor("#FFD54F"),
            Color.parseColor("#FFE082"),
            Color.parseColor("#FFB300"),
            Color.WHITE
        )

        for (i in 0 until count) {
            val angle = (random.nextFloat() * PI * 2).toFloat()
            val speed = 120f + (random.nextFloat() * 180f)
            val life = 0.5f + (random.nextFloat() * 0.3f)
            val color = goldColors[random.nextInt(goldColors.size)]

            particles.add(
                Particle(
                    x = screenX,
                    y = screenY,
                    vx = cos(angle) * speed,
                    vy = (sin(angle) * speed) - 100f, // Upward bias
                    size = 10f + (random.nextFloat() * 8f),
                    color = color,
                    alpha = 1.0f,
                    lifeSec = life,
                    maxLifeSec = life,
                    type = ParticleType.GOLD_STAR
                )
            )
        }
    }

    fun emitBoxShards(screenX: Float, screenY: Float) {
        val count = 16
        val colors = intArrayOf(
            Color.parseColor("#FF9100"),
            Color.parseColor("#FFEA00"),
            Color.parseColor("#FF5722"),
            Color.WHITE
        )

        for (i in 0 until count) {
            val angle = (random.nextFloat() * PI * 2).toFloat()
            val speed = 180f + (random.nextFloat() * 260f)
            val life = 0.65f + (random.nextFloat() * 0.4f)
            val color = colors[random.nextInt(colors.size)]

            particles.add(
                Particle(
                    x = screenX,
                    y = screenY,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed - 80f,
                    size = 12f + (random.nextFloat() * 10f),
                    color = color,
                    alpha = 1.0f,
                    lifeSec = life,
                    maxLifeSec = life,
                    type = ParticleType.SHARD
                )
            )
        }
    }

    fun emitExplosion(screenX: Float, screenY: Float) {
        triggerScreenShake(20f)

        // Fire sparks
        val sparkCount = 28
        val colors = intArrayOf(
            Color.parseColor("#FF1744"),
            Color.parseColor("#FF6D00"),
            Color.parseColor("#FFD600"),
            Color.WHITE
        )

        for (i in 0 until sparkCount) {
            val angle = (random.nextFloat() * PI * 2).toFloat()
            val speed = 220f + (random.nextFloat() * 380f)
            val life = 0.6f + (random.nextFloat() * 0.5f)
            val color = colors[random.nextInt(colors.size)]

            particles.add(
                Particle(
                    x = screenX,
                    y = screenY,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    size = 8f + (random.nextFloat() * 12f),
                    color = color,
                    alpha = 1.0f,
                    lifeSec = life,
                    maxLifeSec = life,
                    type = ParticleType.EXPLOSION_SPARK
                )
            )
        }

        // Smoke clouds
        val smokeCount = 12
        for (i in 0 until smokeCount) {
            val angle = (random.nextFloat() * PI * 2).toFloat()
            val speed = 40f + (random.nextFloat() * 90f)
            val life = 0.8f + (random.nextFloat() * 0.6f)
            val shade = 80 + random.nextInt(60)

            particles.add(
                Particle(
                    x = screenX,
                    y = screenY,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed - 40f,
                    size = 30f + (random.nextFloat() * 30f),
                    color = Color.argb(160, shade, shade, shade),
                    alpha = 0.8f,
                    lifeSec = life,
                    maxLifeSec = life,
                    type = ParticleType.SMOKE
                )
            )
        }
    }

    fun emitMuzzleFlash(screenX: Float, screenY: Float) {
        triggerScreenShake(3.5f)
        particles.add(
            Particle(
                x = screenX,
                y = screenY,
                vx = 0f,
                vy = 0f,
                size = 32f,
                color = Color.parseColor("#FFEA00"),
                alpha = 1.0f,
                lifeSec = 0.06f,
                maxLifeSec = 0.06f,
                type = ParticleType.MUZZLE_FLASH
            )
        )
    }

    fun emitSpeedLines(viewWidth: Float, viewHeight: Float) {
        val count = 4
        val cx = viewWidth / 2f
        val cy = viewHeight / 2f

        for (i in 0 until count) {
            val angle = (random.nextFloat() * PI * 2).toFloat()
            val dist = 180f + (random.nextFloat() * 150f)
            val px = cx + cos(angle) * dist
            val py = cy + sin(angle) * dist
            val speed = 600f

            particles.add(
                Particle(
                    x = px,
                    y = py,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    size = 40f + (random.nextFloat() * 60f),
                    color = Color.argb(180, 0, 229, 255),
                    alpha = 0.8f,
                    lifeSec = 0.25f,
                    maxLifeSec = 0.25f,
                    type = ParticleType.SPEED_LINE
                )
            )
        }
    }

    fun render(canvas: Canvas) {
        particles.forEach { p ->
            val alphaInt = (p.alpha * 255).toInt().coerceIn(0, 255)
            if (alphaInt <= 0) return@forEach

            when (p.type) {
                ParticleType.GOLD_STAR -> {
                    fillPaint.color = Color.argb(
                        alphaInt,
                        Color.red(p.color),
                        Color.green(p.color),
                        Color.blue(p.color)
                    )
                    // Draw 4-point star
                    val s = p.size
                    val path = Path().apply {
                        moveTo(p.x, p.y - s)
                        lineTo(p.x + s * 0.3f, p.y - s * 0.3f)
                        lineTo(p.x + s, p.y)
                        lineTo(p.x + s * 0.3f, p.y + s * 0.3f)
                        lineTo(p.x, p.y + s)
                        lineTo(p.x - s * 0.3f, p.y + s * 0.3f)
                        lineTo(p.x - s, p.y)
                        lineTo(p.x - s * 0.3f, p.y - s * 0.3f)
                        close()
                    }
                    canvas.drawPath(path, fillPaint)
                }

                ParticleType.SHARD, ParticleType.EXPLOSION_SPARK -> {
                    fillPaint.color = Color.argb(
                        alphaInt,
                        Color.red(p.color),
                        Color.green(p.color),
                        Color.blue(p.color)
                    )
                    canvas.drawCircle(p.x, p.y, p.size, fillPaint)
                }

                ParticleType.SMOKE -> {
                    fillPaint.color = Color.argb(
                        (alphaInt * 0.45f).toInt(),
                        Color.red(p.color),
                        Color.green(p.color),
                        Color.blue(p.color)
                    )
                    canvas.drawCircle(p.x, p.y, p.size, fillPaint)
                }

                ParticleType.MUZZLE_FLASH -> {
                    fillPaint.color = Color.argb(alphaInt, 255, 234, 0)
                    canvas.drawCircle(p.x, p.y, p.size, fillPaint)
                    fillPaint.color = Color.argb(alphaInt, 255, 255, 255)
                    canvas.drawCircle(p.x, p.y, p.size * 0.45f, fillPaint)
                }

                ParticleType.SPEED_LINE -> {
                    strokePaint.color = Color.argb(alphaInt, 0, 229, 255)
                    strokePaint.strokeWidth = 3f
                    val len = p.size
                    val dirX = if (p.vx != 0f) p.vx / Math.abs(p.vx) else 1f
                    val dirY = if (p.vy != 0f) p.vy / Math.abs(p.vy) else 1f
                    canvas.drawLine(p.x, p.y, p.x + dirX * len, p.y + dirY * len, strokePaint)
                }
            }
        }
    }
}
