package com.example.mklive.ar.model

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import com.example.mklive.ar.ArAssets
import com.example.mklive.ar.ArGeometry

/**
 * Banana Peel obstacle deployed onto the race track.
 * Causes karts driving over it to spin out and temporarily lose engine power.
 */
class BananaElement(
    id: String,
    x: Float,
    y: Float
) : ArElement(id, x, y, z = 0f, hitRadius = 0.45f, type = ArElementType.BANANA) {

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
        val r = 44f * scale

        if (r < 3f) return

        // 1. Soft Floor Shadow
        val shadowRx = (r * 0.8f).coerceIn(4f, 50f)
        val shadowRy = (shadowRx * 0.45f).coerceIn(2f, 25f)
        ArAssets.drawGroundShadow(canvas, cx, proj.groundY, shadowRx, shadowRy, alpha = 130)

        // 2. Render 3D Banana Bitmap
        val bmp = ArAssets.bananaBitmap
        if (bmp != null) {
            val dstRect = RectF(cx - r, cy - r, cx + r, cy + r)
            val srcRect = Rect(0, 0, bmp.width, bmp.height)
            canvas.drawBitmap(bmp, srcRect, dstRect, null)
        }
    }
}
