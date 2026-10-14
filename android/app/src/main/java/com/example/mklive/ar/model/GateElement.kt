package com.example.mklive.ar.model

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.example.mklive.ar.ArGeometry
import kotlin.math.PI
import kotlin.math.sin

/**
 * Augmented Reality Race Track Gate / Checkpoint.
 * Renders an archway with support pillars, overhead banner (Gate 1..4 / FINISH),
 * and dynamic directional guide arrows.
 */
class GateElement(
    id: String,
    x: Float,
    y: Float,
    val gateNumber: Int, // 1, 2, 3, 4
    val isFinishLine: Boolean = false
) : ArElement(id, x, y, z = 0f, hitRadius = 0.45f, type = ArElementType.GATE) {

    var isPassedThisLap = false
    private var arrowPulse = 0f

    companion object {
        private val pillarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#455A64")
            style = Paint.Style.FILL
        }

        private val bannerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(0xEE, 0x1A, 0x23, 0x7E)
            style = Paint.Style.FILL
        }

        private val finishBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(0xEE, 0xB7, 0x1C, 0x1C)
            style = Paint.Style.FILL
        }

        private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E5FF")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }

        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E676")
            style = Paint.Style.FILL
        }
    }

    override fun update(dt: Float) {
        arrowPulse = (arrowPulse + 5f * dt) % (PI * 2).toFloat()
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

        val gateW = 90f * scale
        val gateH = 70f * scale
        val pillarW = 8f * scale

        if (gateW < 10f) return

        // 1. Left and Right Support Pillars (from ground up)
        val leftPillar = RectF(cx - gateW / 2f, cy - gateH, cx - gateW / 2f + pillarW, cy)
        val rightPillar = RectF(cx + gateW / 2f - pillarW, cy - gateH, cx + gateW / 2f, cy)
        canvas.drawRoundRect(leftPillar, 2f, 2f, pillarPaint)
        canvas.drawRoundRect(rightPillar, 2f, 2f, pillarPaint)

        // 2. Overhead Banner
        val bannerH = 26f * scale
        val bannerRect = RectF(cx - gateW / 2f, cy - gateH, cx + gateW / 2f, cy - gateH + bannerH)
        val bgPaint = if (isFinishLine) finishBgPaint else bannerBgPaint
        canvas.drawRoundRect(bannerRect, 4f, 4f, bgPaint)

        borderPaint.strokeWidth = (2f * scale).coerceIn(1.5f, 5f)
        canvas.drawRoundRect(bannerRect, 4f, 4f, borderPaint)

        // 3. Banner Text
        textPaint.textSize = (13f * scale).coerceIn(8f, 32f)
        val label = if (isFinishLine) "🏁 FINISH / LAP" else "🚩 GATE $gateNumber"
        val textY = cy - gateH + (bannerH * 0.65f)
        canvas.drawText(label, cx, textY, textPaint)

        // 4. Center Directional Guide Arrow (pointing down through gate)
        val arrowY = cy - (gateH * 0.45f) + (sin(arrowPulse) * 4f * scale)
        val arrowSize = 6f * scale
        if (arrowSize > 2f) {
            val arrowPath = android.graphics.Path().apply {
                moveTo(cx, arrowY + arrowSize)
                lineTo(cx - arrowSize * 1.2f, arrowY - arrowSize)
                lineTo(cx + arrowSize * 1.2f, arrowY - arrowSize)
                close()
            }
            canvas.drawPath(arrowPath, arrowPaint)
        }
    }
}
