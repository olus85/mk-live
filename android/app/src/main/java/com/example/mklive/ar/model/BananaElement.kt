package com.example.mklive.ar.model

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.example.mklive.ar.ArGeometry

/**
 * Banana Peel obstacle deployed onto the race track.
 * Causes karts driving over it to spin out and temporarily lose engine power.
 */
class BananaElement(
    id: String,
    x: Float,
    y: Float
) : ArElement(id, x, y, z = 0f, hitRadius = 0.16f, type = ArElementType.BANANA) {

    companion object {
        private val peelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFEB3B") // Bright banana yellow
            style = Paint.Style.FILL
        }

        private val stemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#5D4037") // Brown stem
            style = Paint.Style.FILL
        }

        private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F57F17") // Deep amber outline
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
    }

    override fun update(dt: Float) {
        // Stationary on floor
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
        val r = 16f * scale

        if (r < 3f) return

        // 3-Lobed opened banana peel
        val path = Path().apply {
            moveTo(cx, cy - r * 0.4f)
            // Left peel
            quadTo(cx - r * 0.9f, cy - r * 0.2f, cx - r * 1.1f, cy + r * 0.6f)
            quadTo(cx - r * 0.4f, cy + r * 0.3f, cx, cy)
            // Right peel
            quadTo(cx + r * 0.4f, cy + r * 0.3f, cx + r * 1.1f, cy + r * 0.6f)
            quadTo(cx + r * 0.9f, cy - r * 0.2f, cx, cy - r * 0.4f)
            // Center peel
            quadTo(cx - r * 0.3f, cy + r * 0.8f, cx, cy + r * 1.2f)
            quadTo(cx + r * 0.3f, cy + r * 0.8f, cx, cy - r * 0.4f)
            close()
        }

        canvas.drawPath(path, peelPaint)
        outlinePaint.strokeWidth = (1.5f * scale).coerceIn(1f, 4f)
        canvas.drawPath(path, outlinePaint)

        // Brown top stem
        canvas.drawCircle(cx, cy - r * 0.4f, (2.5f * scale).coerceIn(1.5f, 6f), stemPaint)
    }
}
