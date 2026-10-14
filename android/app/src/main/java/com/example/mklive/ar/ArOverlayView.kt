package com.example.mklive.ar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import com.example.mklive.ar.model.ArElement
import com.example.mklive.combat.WeaponType

/**
 * Jetpack Compose Canvas overlay for rendering AR elements,
 * Gatling crosshairs, and Rocket Lock-On indicators.
 */
@Composable
fun ArOverlayView(
    arEngine: ArEngine,
    pitchDeg: Float,
    rollDeg: Float,
    isLeftEye: Boolean? = null,
    activeWeapon: WeaponType = WeaponType.MINIGUN,
    lockTarget: ArElement? = null,
    isMinigunFiring: Boolean = false,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // 1. Render all 3D AR elements, projectiles and particle effects
        arEngine.render(
            canvas = drawContext.canvas.nativeCanvas,
            viewWidth = w,
            viewHeight = h,
            pitchDeg = pitchDeg,
            rollDeg = rollDeg,
            isLeftEye = isLeftEye
        )

        val cx = w / 2f
        val cy = h / 2f

        // 2. Render Combat Crosshairs / Reticles
        when (activeWeapon) {
            WeaponType.MINIGUN -> {
                // Minigun Circular Gatling Reticle
                val r = 24f
                val strokeCol = if (isMinigunFiring) Color(0xFFFFD600) else Color(0x8800E5FF)

                drawCircle(
                    color = strokeCol,
                    radius = r,
                    center = Offset(cx, cy),
                    style = Stroke(width = if (isMinigunFiring) 3f else 1.5f)
                )

                // 4 Cross ticks
                val tickLen = 8f
                drawLine(strokeCol, Offset(cx - r - tickLen, cy), Offset(cx - r + 3f, cy), strokeWidth = 2f)
                drawLine(strokeCol, Offset(cx + r - 3f, cy), Offset(cx + r + tickLen, cy), strokeWidth = 2f)
                drawLine(strokeCol, Offset(cx, cy - r - tickLen), Offset(cx, cy - r + 3f), strokeWidth = 2f)
                drawLine(strokeCol, Offset(cx, cy + r - 3f), Offset(cx, cy + r + tickLen), strokeWidth = 2f)

                // Center pip
                drawCircle(color = strokeCol, radius = 2.5f, center = Offset(cx, cy))
            }

            WeaponType.ROCKET -> {
                // Rocket Lock-On Radar Reticle
                val isLocked = lockTarget != null
                val reticleColor = if (isLocked) Color(0xFFFF1744) else Color(0xFF00E676)
                val boxR = if (isLocked) 36f else 28f

                // Outer square corner brackets
                val cornerLen = 10f
                // Top-Left
                drawLine(reticleColor, Offset(cx - boxR, cy - boxR), Offset(cx - boxR + cornerLen, cy - boxR), strokeWidth = 2.5f)
                drawLine(reticleColor, Offset(cx - boxR, cy - boxR), Offset(cx - boxR, cy - boxR + cornerLen), strokeWidth = 2.5f)
                // Top-Right
                drawLine(reticleColor, Offset(cx + boxR, cy - boxR), Offset(cx + boxR - cornerLen, cy - boxR), strokeWidth = 2.5f)
                drawLine(reticleColor, Offset(cx + boxR, cy - boxR), Offset(cx + boxR, cy - boxR + cornerLen), strokeWidth = 2.5f)
                // Bottom-Left
                drawLine(reticleColor, Offset(cx - boxR, cy + boxR), Offset(cx - boxR + cornerLen, cy + boxR), strokeWidth = 2.5f)
                drawLine(reticleColor, Offset(cx - boxR, cy + boxR), Offset(cx - boxR, cy + boxR - cornerLen), strokeWidth = 2.5f)
                // Bottom-Right
                drawLine(reticleColor, Offset(cx + boxR, cy + boxR), Offset(cx + boxR - cornerLen, cy + boxR), strokeWidth = 2.5f)
                drawLine(reticleColor, Offset(cx + boxR, cy + boxR), Offset(cx + boxR, cy + boxR - cornerLen), strokeWidth = 2.5f)

                // Center diamond or pip
                drawCircle(color = reticleColor, radius = 3f, center = Offset(cx, cy))
            }

            else -> {
                // Subtle central targeting pip
                drawCircle(color = Color(0x6600E5FF), radius = 3f, center = Offset(cx, cy))
            }
        }
    }
}
