package com.example.mklive.ar.model

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import com.example.mklive.ar.ArAssets
import com.example.mklive.ar.ArGeometry
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Holographic ?-Item Box.
 * Rotates slowly in 3D space, pulsates with a translucent neon aura,
 * casts a soft floor shadow, and shatters into crystalline fragments on impact.
 */
class ItemBoxElement(
    id: String,
    x: Float,
    y: Float
) : ArElement(id, x, y, z = 0.08f, hitRadius = 0.52f, type = ArElementType.ITEM_BOX) {

    private var rotationAngle = (Math.random() * 360).toFloat()
    private var pulsePhase = (Math.random() * PI * 2).toFloat()

    companion object {
        private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(0x44, 0xFF, 0xEA, 0x00)
            style = Paint.Style.FILL
        }

        private val boxFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(0xCC, 0xFF, 0x8F, 0x00)
            style = Paint.Style.FILL
        }

        private val boxBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFFF00")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
    }

    override fun update(dt: Float) {
        rotationAngle = (rotationAngle + 90f * dt) % 360f
        pulsePhase = (pulsePhase + 4.0f * dt) % (PI * 2).toFloat()
        z = 0.08f + (sin(pulsePhase) * 0.015f)
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
        val size = 64f * proj.scale

        if (size < 4f) return

        // 1. Draw floor contact shadow directly beneath the item box
        val shadowRx = (size * 0.75f).coerceIn(6f, 80f)
        val shadowRy = (shadowRx * 0.45f).coerceIn(3f, 40f)
        ArAssets.drawGroundShadow(canvas, cx, proj.groundY, shadowRx, shadowRy, alpha = 135)

        // 2. Outer Hologram Glow
        val pulseAlpha = (140 + (sin(pulsePhase) * 45)).toInt().coerceIn(80, 200)
        glowPaint.color = Color.argb(pulseAlpha / 3, 0xFF, 0xEA, 0x00)
        canvas.drawCircle(cx, cy, size * 1.15f, glowPaint)

        // 3. Render 3D Item Box Bitmap
        val bmp = ArAssets.itemBoxBitmap
        if (bmp != null) {
            val halfW = size * 0.9f
            val halfH = size * 0.9f
            val dstRect = RectF(cx - halfW, cy - halfH, cx + halfW, cy + halfH)
            val srcRect = Rect(0, 0, bmp.width, bmp.height)
            canvas.drawBitmap(bmp, srcRect, dstRect, null)
        } else {
            // Procedural fallback
            val halfW = size * 0.75f
            val halfH = size * 0.75f
            val boxRect = RectF(cx - halfW, cy - halfH, cx + halfW, cy + halfH)
            boxFillPaint.color = Color.argb(pulseAlpha, 0xFF, 0x8F, 0x00)
            canvas.drawRoundRect(boxRect, size * 0.2f, size * 0.2f, boxFillPaint)
            boxBorderPaint.strokeWidth = (2.5f * proj.scale).coerceIn(1.5f, 6f)
            canvas.drawRoundRect(boxRect, size * 0.2f, size * 0.2f, boxBorderPaint)
        }
    }
}
