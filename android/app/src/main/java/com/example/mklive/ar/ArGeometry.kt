package com.example.mklive.ar

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * 3D Perspective & Homography Projection for Mario Kart Live AR Engine.
 * Transforms 3D ground/world coordinates into 2D camera viewport coordinates
 * taking into account physical camera height, fixed tilt, live IMU pitch & roll,
 * and stereoscopic IPD offsets for VR (Meta Quest 3).
 */
object ArGeometry {

    // Physical Camera Calibration for Mario Kart Live
    const val CAMERA_HEIGHT_METERS = 0.045f // 4.5 cm from floor
    const val CAMERA_TILT_DEG = 8.0f        // 8° downward tilt
    const val FOV_HORIZONTAL_DEG = 82.0f    // 82° H-FOV
    const val FOV_VERTICAL_DEG = 52.0f      // 52° V-FOV
    const val IPD_METERS = 0.064f           // 64 mm Inter-pupillary distance for VR

    data class ProjectedPoint(
        val screenX: Float,
        val screenY: Float,
        val scale: Float,
        val distance: Float,
        val isVisible: Boolean
    )

    /**
     * Projects a 3D point in kart-relative coordinates (X: right+, Y: forward+, Z: up+)
     * onto 2D screen coordinates.
     */
    fun project(
        xRel: Float,
        yRel: Float,
        zRel: Float = 0f,
        viewWidth: Float,
        viewHeight: Float,
        pitchDeg: Float = 0f,
        rollDeg: Float = 0f,
        isLeftEye: Boolean? = null
    ): ProjectedPoint {
        // Apply stereoscopic eye offset for Quest 3 VR
        val eyeShift = when (isLeftEye) {
            true -> (IPD_METERS / 2f)
            false -> -(IPD_METERS / 2f)
            null -> 0f
        }
        val effX = xRel + eyeShift

        // Behind camera or too close to render
        if (yRel <= 0.06f) {
            return ProjectedPoint(0f, 0f, 0f, yRel, false)
        }

        val fx = (viewWidth / 2f) / tan(Math.toRadians(FOV_HORIZONTAL_DEG / 2.0)).toFloat()
        val fy = (viewHeight / 2f) / tan(Math.toRadians(FOV_VERTICAL_DEG / 2.0)).toFloat()

        // Vertical pitch angle
        val deltaZ = CAMERA_HEIGHT_METERS - zRel
        val effectivePitchRad = Math.toRadians((CAMERA_TILT_DEG + pitchDeg).toDouble()).toFloat()
        val gammaV = atan2(deltaZ, yRel) - effectivePitchRad
        val gammaH = atan2(effX, yRel)

        var u = (viewWidth / 2f) + fx * tan(gammaH)
        var v = (viewHeight / 2f) + fy * tan(gammaV)

        // Gyro Roll Compensation
        if (rollDeg != 0f) {
            val rollRad = Math.toRadians(rollDeg.toDouble()).toFloat()
            val cosR = cos(rollRad)
            val sinR = sin(rollRad)
            val cx = u - (viewWidth / 2f)
            val cy = v - (viewHeight / 2f)
            u = (viewWidth / 2f) + (cx * cosR - cy * sinR)
            v = (viewHeight / 2f) + (cx * sinR + cy * cosR)
        }

        val distance = kotlin.math.sqrt(effX * effX + yRel * yRel + deltaZ * deltaZ)
        val scale = (fy * 0.15f / yRel).coerceIn(0.08f, 20f)
        val isVisible = u in -150f..(viewWidth + 150f) && v in -150f..(viewHeight + 150f)

        return ProjectedPoint(u, v, scale, distance, isVisible)
    }
}
