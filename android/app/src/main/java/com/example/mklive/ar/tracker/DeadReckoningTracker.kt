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

    var currentSpeedMps: Float = 0f
        private set

    private var lastUpdateTime = System.currentTimeMillis()

    fun reset(startX: Float = 0f, startY: Float = 0f, startHeadingDeg: Float = 0f) {
        x = startX
        y = startY
        headingRad = Math.toRadians(startHeadingDeg.toDouble()).toFloat()
        currentSpeedMps = 0f
        lastUpdateTime = System.currentTimeMillis()
    }

    /**
     * Updates dead reckoning pose using throttle, steering, physical inertia, and IMU gyro rate.
     */
    fun update(
        throttle: Int,
        steering: Int,
        brake: Boolean,
        isBoost: Boolean = false,
        gyroYawRateDegS: Float? = null,
        headingDeg: Float? = null,
        dtSec: Float? = null
    ): WorldPose {
        val now = System.currentTimeMillis()
        val dt = dtSec ?: ((now - lastUpdateTime).coerceIn(1, 100) / 1000f)
        lastUpdateTime = now

        // Target speed based on throttle and boost
        val maxSpeed = if (isBoost) 2.1f else 1.35f
        val targetSpeed = when {
            brake -> 0f
            throttle > 20 -> ((throttle - 20) / 107f) * maxSpeed
            throttle < -20 -> ((throttle + 20) / 108f) * 0.7f // Reverse
            else -> 0f
        }

        // Smooth physical acceleration / deceleration
        val accelRate = if (brake) 12.0f else 6.0f
        currentSpeedMps += (targetSpeed - currentSpeedMps) * (dt * accelRate).coerceAtMost(1.0f)
        if (kotlin.math.abs(currentSpeedMps) < 0.01f && targetSpeed == 0f) {
            currentSpeedMps = 0f
        }

        // Integrate Heading/Yaw:
        // Use gyro yaw rate if available (physical sensor), else kinematic steering model
        if (gyroYawRateDegS != null && !gyroYawRateDegS.isNaN() && kotlin.math.abs(gyroYawRateDegS) > 0.4f) {
            headingRad += Math.toRadians(gyroYawRateDegS.toDouble()).toFloat() * dt
        } else if (steering != 0 && kotlin.math.abs(currentSpeedMps) > 0.05f) {
            // Kinematic bicycle turning model: omega = (v / L) * tan(delta)
            val steerAngle = (steering / 127f) * 0.42f // ~24 deg max steer angle
            val steerTurnRate = (currentSpeedMps / 0.12f) * kotlin.math.sin(steerAngle)
            headingRad += steerTurnRate * dt
        }

        // Normalize heading to -PI .. +PI
        while (headingRad < -Math.PI) headingRad += (Math.PI * 2).toFloat()
        while (headingRad > Math.PI) headingRad -= (Math.PI * 2).toFloat()

        // Integrate Position in world space
        x += sin(headingRad) * currentSpeedMps * dt
        y += cos(headingRad) * currentSpeedMps * dt

        return WorldPose(x, y, headingRad)
    }

    /**
     * Transforms a world point (wx, wy) into kart-relative coordinates (xRel, yRel).
     * xRel > 0 is to the right of the kart, yRel > 0 is ahead of the kart.
     */
    fun toRelative(worldX: Float, worldY: Float): RelativeCoords {
        val dx = worldX - x
        val dy = worldY - y

        // Project onto kart's Right vector (cosH, -sinH) and Forward vector (sinH, cosH)
        val cosH = cos(headingRad)
        val sinH = sin(headingRad)

        val xRel = dx * cosH - dy * sinH
        val yRel = dx * sinH + dy * cosH
        val dist = sqrt(xRel * xRel + yRel * yRel)

        return RelativeCoords(xRel, yRel, dist)
    }

    /**
     * Transforms kart-relative coordinates (xRel, yRel) into world coordinates (worldX, worldY).
     */
    fun toWorld(xRel: Float, yRel: Float): Pair<Float, Float> {
        val cosH = cos(headingRad)
        val sinH = sin(headingRad)
        val worldX = x + (xRel * cosH) + (yRel * sinH)
        val worldY = y - (xRel * sinH) + (yRel * cosH)
        return Pair(worldX, worldY)
    }
}
