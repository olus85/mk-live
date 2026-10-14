package com.example.mklive.ar.model

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
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
    private val speed = if (isRocket) 5.5f else 22.0f // m/s
    private var lifetimeSec = if (isRocket) 2.2f else 0.45f
    var isDetonated = false

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
            setShadowLayer(8f, 0f, 0f, Color.RED)
        }

        private val tracerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFD600")
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            setShadowLayer(6f, 0f, 0f, Color.parseColor("#FF6D00"))
        }
    }

    override fun update(dt: Float) {
        lifetimeSec -= dt
        if (lifetimeSec <= 0f) {
            isActive = false
            return
        }

        // Homing Missile Guidance: Adjust heading towards target if locked
        val tx = targetX
        val ty = targetY
        if (isRocket && tx != null && ty != null) {
            val dx = tx - x
            val dy = ty - y
            val targetAngle = atan2(dx.toDouble(), dy.toDouble()).toFloat()
            var diff = targetAngle - headingRad
            while (diff < -Math.PI) diff += (Math.PI * 2).toFloat()
            while (diff > Math.PI) diff -= (Math.PI * 2).toFloat()
            headingRad += (diff * 4.0f * dt).coerceIn(-1.5f * dt, 1.5f * dt)
        }

        // Move forward
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
            // Render 3D Rocket
            val rLen = 28f * scale
            val rW = 8f * scale
            if (rLen < 3f) return

            // Cylindrical Body
            val bodyRect = RectF(cx - rW / 2f, cy - rLen / 2f, cx + rW / 2f, cy + rLen / 2f)
            canvas.drawRoundRect(bodyRect, 2f, 2f, rocketBodyPaint)

            // Red Cone Warhead
            val nosePath = Path().apply {
                moveTo(cx - rW / 2f, cy - rLen / 2f)
                lineTo(cx, cy - rLen * 0.9f)
                lineTo(cx + rW / 2f, cy - rLen / 2f)
                close()
            }
            canvas.drawPath(nosePath, rocketNosePaint)

            // Exhaust Thruster Flame
            val flamePath = Path().apply {
                moveTo(cx - rW * 0.4f, cy + rLen / 2f)
                lineTo(cx, cy + rLen * 0.9f + (Math.random().toFloat() * 4f * scale))
                lineTo(cx + rW * 0.4f, cy + rLen / 2f)
                close()
            }
            canvas.drawPath(flamePath, rocketFlamePaint)
        } else {
            // Render High-speed Minigun Tracer Line
            val tracerLen = 22f * scale
            tracerPaint.strokeWidth = (2.5f * scale).coerceIn(1.5f, 6f)
            canvas.drawLine(cx, cy + tracerLen, cx, cy - tracerLen, tracerPaint)
        }
    }
}
