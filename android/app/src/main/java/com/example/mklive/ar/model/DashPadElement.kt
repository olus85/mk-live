package com.example.mklive.ar.model

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.example.mklive.ar.ArAssets
import com.example.mklive.ar.ArGeometry
import kotlin.math.PI
import kotlin.math.sin

/**
 * Turbo Boost Dash Pad on the race track floor.
 * Features glowing perspective boost chevrons that ignite an instant 100% Nitro Boost.
 */
class DashPadElement(
    id: String,
    x: Float,
    y: Float
) : ArElement(id, x, y, z = 0.0f, hitRadius = 0.55f, type = ArElementType.DASH_PAD) {

    private var animPhase = 0f

    companion object {
        private val padGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(0x55, 0x00, 0xE5, 0xFF)
            style = Paint.Style.FILL
        }
    }

    override fun update(dt: Float) {
        animPhase = (animPhase + 6f * dt) % (PI * 2).toFloat()
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
        val w = 130f * proj.scale
        val h = 85f * proj.scale

        if (w < 8f) return

        // 1. Floor Glow Aura
        val glowAlpha = (60 + sin(animPhase) * 35).toInt().coerceIn(30, 110)
        padGlowPaint.color = Color.argb(glowAlpha, 0x00, 0xE5, 0xFF)
        canvas.drawOval(RectF(cx - w * 0.8f, cy - h * 0.6f, cx + w * 0.8f, cy + h * 0.6f), padGlowPaint)

        // 2. Render Dash Pad Bitmap
        val bmp = ArAssets.dashPadBitmap
        if (bmp != null) {
            val dstRect = RectF(cx - w * 0.65f, cy - h * 0.55f, cx + w * 0.65f, cy + h * 0.55f)
            val srcRect = Rect(0, 0, bmp.width, bmp.height)
            canvas.drawBitmap(bmp, srcRect, dstRect, null)
        }
    }
}
