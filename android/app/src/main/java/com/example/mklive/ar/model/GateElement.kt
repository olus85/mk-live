package com.example.mklive.ar.model

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import com.example.mklive.ar.ArAssets
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
) : ArElement(id, x, y, z = 0f, hitRadius = 0.60f, type = ArElementType.GATE) {

    var isPassedThisLap = false
    private var arrowPulse = 0f

    companion object {
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(4f, 0f, 0f, Color.BLACK)
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

        val gateW = 100f * scale
        val gateH = 80f * scale

        if (gateW < 10f) return

        // 1. Pillar Shadows on floor
        val shadowRx = (10f * scale).coerceIn(3f, 30f)
        val shadowRy = (shadowRx * 0.45f).coerceIn(2f, 15f)
        ArAssets.drawGroundShadow(canvas, cx - gateW * 0.42f, proj.groundY, shadowRx, shadowRy, alpha = 130)
        ArAssets.drawGroundShadow(canvas, cx + gateW * 0.42f, proj.groundY, shadowRx, shadowRy, alpha = 130)

        // 2. Render Gate Arch Bitmap
        val bmp = ArAssets.gateArchBitmap
        if (bmp != null) {
            val dstRect = RectF(cx - gateW / 2f, cy - gateH, cx + gateW / 2f, cy)
            val srcRect = Rect(0, 0, bmp.width, bmp.height)
            canvas.drawBitmap(bmp, srcRect, dstRect, null)
        }

        // 3. Banner Text
        textPaint.textSize = (13f * scale).coerceIn(8f, 30f)
        val label = if (isFinishLine) "🏁 FINISH / LAP" else "🚩 GATE $gateNumber"
        val textY = cy - gateH * 0.72f
        canvas.drawText(label, cx, textY, textPaint)

        // 4. Center Directional Guide Arrow (pointing down through gate)
        val arrowY = cy - (gateH * 0.35f) + (sin(arrowPulse) * 4f * scale)
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
