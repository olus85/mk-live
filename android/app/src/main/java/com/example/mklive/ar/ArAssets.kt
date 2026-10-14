package com.example.mklive.ar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.example.mklive.R

/**
 * High-quality 3D rendered bitmap assets and shadow caching for Mario Kart Live AR elements.
 */
object ArAssets {

    var coinBitmap: Bitmap? = null
        private set
    var itemBoxBitmap: Bitmap? = null
        private set
    var dashPadBitmap: Bitmap? = null
        private set
    var bananaBitmap: Bitmap? = null
        private set
    var gateArchBitmap: Bitmap? = null
        private set
    var rocketBitmap: Bitmap? = null
        private set
    var bombBitmap: Bitmap? = null
        private set
    var mushroomBitmap: Bitmap? = null
        private set
    var explosionBitmap: Bitmap? = null
        private set
    var sparklesBitmap: Bitmap? = null
        private set

    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.FILL
        alpha = 110
    }

    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        val res = context.resources

        try {
            coinBitmap = BitmapFactory.decodeResource(res, R.drawable.ar_coin)
            itemBoxBitmap = BitmapFactory.decodeResource(res, R.drawable.ar_item_box)
            dashPadBitmap = BitmapFactory.decodeResource(res, R.drawable.ar_dash_pad)
            bananaBitmap = BitmapFactory.decodeResource(res, R.drawable.ar_banana)
            gateArchBitmap = BitmapFactory.decodeResource(res, R.drawable.ar_gate_arch)
            rocketBitmap = BitmapFactory.decodeResource(res, R.drawable.ar_rocket)
            bombBitmap = BitmapFactory.decodeResource(res, R.drawable.ar_bomb)
            mushroomBitmap = BitmapFactory.decodeResource(res, R.drawable.ar_mushroom)
            explosionBitmap = BitmapFactory.decodeResource(res, R.drawable.ar_explosion)
            sparklesBitmap = BitmapFactory.decodeResource(res, R.drawable.ar_sparkles)
            isInitialized = true
        } catch (_: Exception) {}
    }

    /**
     * Draws a soft, realistic drop shadow on the carpet / floor beneath a hovering or resting 3D element.
     */
    fun drawGroundShadow(
        canvas: Canvas,
        cx: Float,
        groundY: Float,
        rx: Float,
        ry: Float,
        alpha: Int = 110
    ) {
        shadowPaint.alpha = alpha.coerceIn(0, 255)
        val shadowRect = RectF(cx - rx, groundY - ry, cx + rx, groundY + ry)
        canvas.drawOval(shadowRect, shadowPaint)
    }
}
