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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Interactive 3D Gold Coin.
 * Hovers ~5 cm above the ground, rotates continuously in 3D space,
 * casts a realistic floor shadow, bobs rhythmically, and glitters.
 */
class CoinElement(
    id: String,
    x: Float,
    y: Float
) : ArElement(id, x, y, z = 0.05f, hitRadius = 0.48f, type = ArElementType.COIN) {

    private var rotationAngle = (Math.random() * 360).toFloat()
    private var bobPhase = (Math.random() * PI * 2).toFloat()

    companion object {
        private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF8F00")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }

        private val goldFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFD54F")
            style = Paint.Style.FILL
        }

        private val sparklePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            setShadowLayer(8f, 0f, 0f, Color.YELLOW)
        }
    }

    override fun update(dt: Float) {
        rotationAngle = (rotationAngle + 180f * dt) % 360f
        bobPhase = (bobPhase + 3.0f * dt) % (PI * 2).toFloat()
        // Bob up and down ~2 cm
        z = 0.05f + (sin(bobPhase) * 0.02f)
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
        val baseR = 46f * proj.scale

        if (baseR < 4f) return

        // 1. Draw floor contact shadow directly beneath the coin
        val shadowRx = (baseR * 0.75f).coerceIn(4f, 70f)
        val shadowRy = (shadowRx * 0.45f).coerceIn(2f, 35f)
        ArAssets.drawGroundShadow(canvas, cx, proj.groundY, shadowRx, shadowRy, alpha = 130)

        // 2. 3D rotation aspect factor: cos(angle) creates the illusion of 3D rotation
        val rotRad = Math.toRadians(rotationAngle.toDouble())
        val widthFactor = abs(cos(rotRad)).toFloat().coerceAtLeast(0.12f)
        val rx = baseR * widthFactor
        val ry = baseR

        val dstRect = RectF(cx - rx, cy - ry, cx + rx, cy + ry)
        val bmp = ArAssets.coinBitmap

        if (bmp != null) {
            val srcRect = Rect(0, 0, bmp.width, bmp.height)
            canvas.drawBitmap(bmp, srcRect, dstRect, null)
        } else {
            // Fallback: procedural shiny oval
            canvas.drawOval(dstRect, goldFillPaint)
            rimPaint.strokeWidth = (2.5f * proj.scale).coerceIn(1.5f, 6f)
            canvas.drawOval(dstRect, rimPaint)
        }

        // 3. Glint Sparkle when facing viewer
        val glintPhase = abs(cos(rotRad))
        if (glintPhase > 0.85f) {
            val sparkSize = (6f * proj.scale).coerceIn(3f, 16f)
            val sx = cx + rx * 0.4f
            val sy = cy - ry * 0.4f
            canvas.drawCircle(sx, sy, sparkSize * 0.5f, sparklePaint)
            canvas.drawLine(sx - sparkSize, sy, sx + sparkSize, sy, sparklePaint)
            canvas.drawLine(sx, sy - sparkSize, sx, sy + sparkSize, sparklePaint)
        }
    }
}
