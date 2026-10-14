package com.example.mklive.ar.model

import android.graphics.Canvas
import com.example.mklive.ar.ArGeometry

enum class ArElementType {
    COIN,
    ITEM_BOX,
    DASH_PAD,
    GATE,
    BANANA,
    PROJECTILE_ROCKET,
    PROJECTILE_TRACER
}

abstract class ArElement(
    val id: String,
    var x: Float, // World coordinate X in meters
    var y: Float, // World coordinate Y in meters
    var z: Float = 0f, // Height in meters above floor
    var hitRadius: Float = 0.16f,
    val type: ArElementType
) {
    var isActive: Boolean = true
    var isCollected: Boolean = false

    abstract fun update(dt: Float)

    abstract fun render(
        canvas: Canvas,
        proj: ArGeometry.ProjectedPoint,
        viewWidth: Float,
        viewHeight: Float,
        isLeftEye: Boolean? = null
    )
}
