package com.example.mklive.ar.model

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.example.mklive.ar.ArGeometry
import kotlin.math.PI
import kotlin.math.sin

/**
 * Turbo Boost Dash Pad on the race track floor.
 * Features animated flowing neon chevrons that trigger a 100% Nitro Boost.
 */
class DashPadElement(
    id: String,
    x: Float,
    y: Float
) : ArElement(id, x, y, z = 0.0f, hitRadius = 0.28f, type = ArElementType.DASH_PAD) {

    private var animPhase = 0f

    companion object {
        private val padBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(0x88, 0x00, 0x60, 0x64)
            style = Paint.Style.FILL
        }

        private val chevronPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E5FF")
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF1744")
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
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
        val w = 48f * proj.scale
        val h = 32f * proj.scale

        if (w < 6f) return

        // 1. Floor Trapezoid/Rectangle base
        val padPath = Path().apply {
            moveTo(cx - w * 0.45f, cy - h * 0.5f)
            lineTo(cx + w * 0.45f, cy - h * 0.5f)
            lineTo(cx + w * 0.55f, cy + h * 0.5f)
            lineTo(cx - w * 0.55f, cy + h * 0.5f)
            close()
        }
        canvas.drawPath(padPath, padBgPaint)

        // 2. Animated Flowing Chevrons (>> >)
        chevronPaint.strokeWidth = (3.5f * proj.scale).coerceIn(2f, 10f)
        accentPaint.strokeWidth = chevronPaint.strokeWidth

        val numChevrons = 3
        for (i in 0 until numChevrons) {
            val yOffset = ((i - 1) * h * 0.32f) + (sin(animPhase + i) * h * 0.08f)
            val chevPath = Path().apply {
                moveTo(cx - w * 0.35f, cy + yOffset + h * 0.15f)
                lineTo(cx, cy + yOffset - h * 0.15f)
                lineTo(cx + w * 0.35f, cy + yOffset + h * 0.15f)
            }
            val paint = if (i == 1) accentPaint else chevronPaint
            canvas.drawPath(chevPath, paint)
        }
    }
}
