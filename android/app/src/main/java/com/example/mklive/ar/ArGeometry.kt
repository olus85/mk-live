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
    const val CAMERA_HEIGHT_METERS = 0.09f // ~9 cm from floor (head/rollbar mount)
    const val HORIZON_RATIO = 0.38f        // Horizon line at 38% height
    const val FOV_HORIZONTAL_DEG = 82.0f   // 82° H-FOV
    const val FOV_VERTICAL_DEG = 52.0f     // 52° V-FOV
    const val IPD_METERS = 0.064f          // 64 mm Inter-pupillary distance for VR

    data class ProjectedPoint(
        val screenX: Float,
        val screenY: Float,
        val groundY: Float,
        val scale: Float,
        val distance: Float,
        val isVisible: Boolean
    )

    /**
     * Projects a 3D point in kart-relative coordinates (X: right+, Y: forward+, Z: up+)
     * onto 2D screen coordinates using robust ground plane homography.
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
        if (yRel <= 0.12f) {
            return ProjectedPoint(0f, 0f, 0f, 0f, yRel, false)
        }

        val cx = viewWidth / 2f
        val fx = (viewWidth / 2f) / tan(Math.toRadians(FOV_HORIZONTAL_DEG / 2.0)).toFloat()
        val fy = (viewHeight / 2f) / tan(Math.toRadians(FOV_VERTICAL_DEG / 2.0)).toFloat()

        // Pitch shifts horizon vertically
        val pitchShift = fy * tan(Math.toRadians(pitchDeg.toDouble())).toFloat()
        val vHorizon = (viewHeight * HORIZON_RATIO) + pitchShift

        // Ground plane projection constant (K_ground = fy * H_camera)
        val kGround = viewHeight * 0.28f
        val groundV = vHorizon + (kGround / yRel)

        // Elevation offset for zRel > 0 (elevation decreases screen Y)
        val elevationV = (fy * 0.85f * zRel) / yRel
        var u = cx + (fx * effX) / yRel
        var v = groundV - elevationV

        // Gyro Roll Compensation: rotate (u, v) and groundV around (cx, vHorizon)
        var finalGroundV = groundV
        if (rollDeg != 0f) {
            val rollRad = Math.toRadians(-rollDeg.toDouble()).toFloat()
            val cosR = cos(rollRad)
            val sinR = sin(rollRad)

            val dx = u - cx
            val dy = v - vHorizon
            u = cx + (dx * cosR - dy * sinR)
            v = vHorizon + (dx * sinR + dy * cosR)

            val dyG = groundV - vHorizon
            finalGroundV = vHorizon + (dyG * cosR)
        }

        val distance = kotlin.math.sqrt(effX * effX + yRel * yRel + zRel * zRel)
        val scale = ((1.0f / yRel) * (viewHeight / 720f)).coerceIn(0.08f, 6.0f)
        val isVisible = yRel in 0.12f..14.0f && u in -180f..(viewWidth + 180f) && v in -180f..(viewHeight + 180f)

        return ProjectedPoint(u, v, finalGroundV, scale, distance, isVisible)
    }
}
