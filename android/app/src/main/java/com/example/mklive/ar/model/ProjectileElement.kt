package com.example.mklive.ar.model

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import com.example.mklive.ar.ArAssets
import com.example.mklive.ar.ArGeometry
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Combat Projectile (Missile / Rocket or Minigun Tracer Bullet).
 * Advances through world space towards target or straight ahead,
 * leaving smoke/fire trails and detonating on impact.
 */
class ProjectileElement(
    id: String,
    x: Float,
    y: Float,
    z: Float = 0.05f,
    var headingRad: Float,
    val isRocket: Boolean,
    var targetX: Float? = null,
    var targetY: Float? = null
) : ArElement(
    id = id,
    x = x,
    y = y,
    z = z,
    hitRadius = if (isRocket) 0.22f else 0.12f,
    type = if (isRocket) ArElementType.PROJECTILE_ROCKET else ArElementType.PROJECTILE_TRACER
) {
    private val speed = if (isRocket) 3.4f else 16.0f // m/s (visible arcade speed!)
    private var lifetimeSec = if (isRocket) 2.2f else 0.65f
    var isDetonated = false
    var onEmitTrail: ((screenX: Float, screenY: Float) -> Unit)? = null

    val tailX: Float get() = x - sin(headingRad) * 0.45f
    val tailY: Float get() = y - cos(headingRad) * 0.45f
    var tailScreenX: Float = 0f
    var tailScreenY: Float = 0f

    companion object {
        private val rocketBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E0E0E0")
            style = Paint.Style.FILL
        }

        private val rocketNosePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF1744") // Red warhead
            style = Paint.Style.FILL
        }

        private val rocketFlamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFEA00")
            style = Paint.Style.FILL
            setShadowLayer(14f, 0f, 0f, Color.RED)
        }

        private val tracerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFD600")
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            setShadowLayer(14f, 0f, 0f, Color.parseColor("#FF6D00"))
        }

        private val tracerCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        private val tracerTipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
    }

    override fun update(dt: Float) {
        lifetimeSec -= dt
        if (lifetimeSec <= 0f) {
            isActive = false
            return
        }

        // Homing Missile Guidance: Adjust heading towards target ONLY if locked in front
        val tx = targetX
        val ty = targetY
        if (isRocket && tx != null && ty != null) {
            val dx = tx - x
            val dy = ty - y
            val targetAngle = atan2(dx.toDouble(), dy.toDouble()).toFloat()
            var diff = targetAngle - headingRad
            while (diff < -Math.PI) diff += (Math.PI * 2).toFloat()
            while (diff > Math.PI) diff -= (Math.PI * 2).toFloat()
            // Only home in if target is within a tight 25° front cone
            if (kotlin.math.abs(diff) < 0.45f) {
                headingRad += (diff * 3.5f * dt).coerceIn(-1.8f * dt, 1.8f * dt)
            }
        }

        // Move forward straight along heading
        x += sin(headingRad) * speed * dt
        y += cos(headingRad) * speed * dt
    }

    override fun render(
        canvas: Canvas,
        proj: ArGeometry.ProjectedPoint,
        viewWidth: Float,
        viewHeight: Float,
        isLeftEye: Boolean?
    ) {
        if (!proj.isVisible || !isActive) return

        val cx = proj.screenX
        val cy = proj.screenY
        val scale = proj.scale

        if (isRocket) {
            val bmp = ArAssets.rocketBitmap
            // Bold AAA scale: clearly visible Red Shell rocket!
            val rSize = (115f * scale).coerceIn(24f, 320f)

            // Floor Shadow
            val shadowRx = (rSize * 0.45f).coerceIn(4f, 60f)
            val shadowRy = (shadowRx * 0.45f).coerceIn(2f, 25f)
            ArAssets.drawGroundShadow(canvas, cx, proj.groundY, shadowRx, shadowRy, alpha = 110)

            // Emit live particle smoke & fire trail
            onEmitTrail?.invoke(cx, cy + rSize * 0.35f)

            if (bmp != null) {
                val dstRect = RectF(cx - rSize / 2f, cy - rSize / 2f, cx + rSize / 2f, cy + rSize / 2f)
                val srcRect = Rect(0, 0, bmp.width, bmp.height)
                canvas.drawBitmap(bmp, srcRect, dstRect, null)
            } else {
                // Cylindrical Body fallback
                val rW = 20f * scale
                val bodyRect = RectF(cx - rW / 2f, cy - rSize / 2f, cx + rW / 2f, cy + rSize / 2f)
                canvas.drawRoundRect(bodyRect, 4f, 4f, rocketBodyPaint)
                val nosePath = Path().apply {
                    moveTo(cx - rW / 2f, cy - rSize / 2f)
                    lineTo(cx, cy - rSize * 0.9f)
                    lineTo(cx + rW / 2f, cy - rSize / 2f)
                    close()
                }
                canvas.drawPath(nosePath, rocketNosePaint)
            }

            // Exhaust Thruster Flame
            val flamePath = Path().apply {
                val fW = 12f * scale
                moveTo(cx - fW, cy + rSize * 0.42f)
                lineTo(cx, cy + rSize * 0.85f + (Math.random().toFloat() * 12f * scale))
                lineTo(cx + fW, cy + rSize * 0.42f)
                close()
            }
            canvas.drawPath(flamePath, rocketFlamePaint)
        } else {
            // Render High-speed Minigun Tracer Line (Perspective 3D Laser Beam streaking forward!)
            val startX = if (tailScreenX != 0f) tailScreenX else cx
            val startY = if (tailScreenY != 0f) tailScreenY else (cy + 25f * scale)
            val endX = cx
            val endY = cy

            // Outer glowing neon beam
            tracerGlowPaint.strokeWidth = (9.0f * scale).coerceIn(4.0f, 22f)
            canvas.drawLine(startX, startY, endX, endY, tracerGlowPaint)

            // Inner bright white laser core
            tracerCorePaint.strokeWidth = (3.5f * scale).coerceIn(1.8f, 9f)
            canvas.drawLine(startX, startY, endX, endY, tracerCorePaint)

            // Glowing tip spark
            canvas.drawCircle(endX, endY, (3.0f * scale).coerceIn(2f, 7f), tracerTipPaint)
        }
    }
}
