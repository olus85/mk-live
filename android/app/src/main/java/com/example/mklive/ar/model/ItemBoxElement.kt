package com.example.mklive.ar.model

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import com.example.mklive.ar.ArGeometry
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Holographic ?-Item Box.
 * Rotates slowly in 3D space, pulsates with a translucent neon aura,
 * and shatters into crystalline fragments on impact.
 */
class ItemBoxElement(
    id: String,
    x: Float,
    y: Float
) : ArElement(id, x, y, z = 0.08f, hitRadius = 0.20f, type = ArElementType.ITEM_BOX) {

    private var rotationAngle = (Math.random() * 360).toFloat()
    private var pulsePhase = (Math.random() * PI * 2).toFloat()

    companion object {
        private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(0x44, 0xFF, 0xEA, 0x00)
            style = Paint.Style.FILL
        }

        private val boxFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(0xBB, 0xFF, 0x91, 0x00)
            style = Paint.Style.FILL
        }

        private val boxBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFFF00")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }

        private val questionMarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(6f, 0f, 0f, Color.YELLOW)
        }
    }

    override fun update(dt: Float) {
        rotationAngle = (rotationAngle + 90f * dt) % 360f
        pulsePhase = (pulsePhase + 4.0f * dt) % (PI * 2).toFloat()
        z = 0.08f + (sin(pulsePhase) * 0.01f)
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
        val size = 26f * proj.scale

        if (size < 4f) return

        val pulseAlpha = (180 + (sin(pulsePhase) * 50)).toInt().coerceIn(120, 240)

        // 1. Outer Hologram Glow
        val glowR = size * 1.35f
        glowPaint.color = Color.argb(pulseAlpha / 3, 0xFF, 0xEA, 0x00)
        canvas.drawCircle(cx, cy, glowR, glowPaint)

        // 2. 3D Isometric/Perspective Box Outline
        val halfW = size * 0.85f
        val halfH = size * 0.85f
        val rotRad = Math.toRadians(rotationAngle.toDouble())
        val skewX = (sin(rotRad) * size * 0.25f).toFloat()

        val boxRect = RectF(cx - halfW, cy - halfH, cx + halfW, cy + halfH)
        boxFillPaint.color = Color.argb(pulseAlpha, 0xFF, 0x8F, 0x00)
        canvas.drawRoundRect(boxRect, size * 0.2f, size * 0.2f, boxFillPaint)

        // 3. Neon Box Frame
        boxBorderPaint.strokeWidth = (2.5f * proj.scale).coerceIn(1.5f, 6f)
        canvas.drawRoundRect(boxRect, size * 0.2f, size * 0.2f, boxBorderPaint)

        // Corner rivets
        val cornerOffset = size * 0.65f
        val rSize = (2.5f * proj.scale).coerceIn(1f, 5f)
        canvas.drawCircle(cx - cornerOffset, cy - cornerOffset, rSize, boxBorderPaint)
        canvas.drawCircle(cx + cornerOffset, cy - cornerOffset, rSize, boxBorderPaint)
        canvas.drawCircle(cx - cornerOffset, cy + cornerOffset, rSize, boxBorderPaint)
        canvas.drawCircle(cx + cornerOffset, cy + cornerOffset, rSize, boxBorderPaint)

        // 4. Center '?' Question Mark
        questionMarkPaint.textSize = size * 1.15f
        val textY = cy + (questionMarkPaint.textSize * 0.35f)
        canvas.drawText("?", cx + skewX * 0.4f, textY, questionMarkPaint)
    }
}
