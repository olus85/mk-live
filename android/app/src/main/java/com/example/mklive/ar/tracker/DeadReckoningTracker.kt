package com.example.mklive.ar.tracker

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class WorldPose(
    val x: Float, // Meters east / right
    val y: Float, // Meters north / forward
    val headingRad: Float // Yaw in radians (0 = North/+Y)
)

data class RelativeCoords(
    val xRel: Float, // Meters to the right of kart
    val yRel: Float, // Meters ahead of kart
    val distance: Float
)

/**
 * Odometry & Dead Reckoning Tracker.
 * Integrates kart speed, steering, and IMU gyro yaw rate to continuously
 * track the kart's position and orientation in real-world space.
 */
class DeadReckoningTracker {

    var x: Float = 0f
        private set
    var y: Float = 0f
        private set
    var headingRad: Float = 0f
        private set

    private var lastUpdateTime = System.currentTimeMillis()

    fun reset(startX: Float = 0f, startY: Float = 0f, startHeadingDeg: Float = 0f) {
        x = startX
        y = startY
        headingRad = Math.toRadians(startHeadingDeg.toDouble()).toFloat()
        lastUpdateTime = System.currentTimeMillis()
    }

    /**
     * Updates dead reckoning pose using throttle, steering, and IMU gyro rate.
     */
    fun update(
        throttle: Int,
        steering: Int,
        brake: Boolean,
        gyroYawRateDegS: Float? = null,
        headingDeg: Float? = null,
        dtSec: Float? = null
    ): WorldPose {
        val now = System.currentTimeMillis()
        val dt = dtSec ?: ((now - lastUpdateTime).coerceIn(1, 100) / 1000f)
        lastUpdateTime = now

        // Estimate speed from throttle (approx 1.5 m/s at 100% throttle, 0 at brake)
        val speedMps = when {
            brake -> 0f
            throttle > 0 -> (throttle / 127f) * 1.5f
            throttle < 0 -> (throttle / 128f) * 0.75f // Reverse
            else -> 0f
        }

        // Integrate Heading/Yaw: prefer absolute IMU heading if available, else integrate yaw rate
        if (headingDeg != null && !headingDeg.isNaN()) {
            headingRad = Math.toRadians(headingDeg.toDouble()).toFloat()
        } else if (gyroYawRateDegS != null && !gyroYawRateDegS.isNaN()) {
            headingRad += Math.toRadians(gyroYawRateDegS.toDouble()).toFloat() * dt
        } else {
            // Fallback: estimate turning rate from steering angle
            val steerTurnRate = (steering / 127f) * 1.8f // ~100 deg/s at full lock
            headingRad += steerTurnRate * dt
        }

        // Normalize heading to -PI .. +PI
        while (headingRad < -Math.PI) headingRad += (Math.PI * 2).toFloat()
        while (headingRad > Math.PI) headingRad -= (Math.PI * 2).toFloat()

        // Integrate Position
        x += sin(headingRad) * speedMps * dt
        y += cos(headingRad) * speedMps * dt

        return WorldPose(x, y, headingRad)
    }

    /**
     * Transforms a world point (wx, wy) into kart-relative coordinates (xRel, yRel).
     * xRel > 0 is to the right of the kart, yRel > 0 is ahead of the kart.
     */
    fun toRelative(worldX: Float, worldY: Float): RelativeCoords {
        val dx = worldX - x
        val dy = worldY - y

        // Rotate by -headingRad
        val cosH = cos(-headingRad)
        val sinH = sin(-headingRad)

        val xRel = dx * cosH - dy * sinH
        val yRel = dx * sinH + dy * cosH
        val dist = sqrt(xRel * xRel + yRel * yRel)

        return RelativeCoords(xRel, yRel, dist)
    }
}
