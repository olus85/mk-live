package com.example.mklive.ui.hud

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mklive.R
import com.example.mklive.combat.WeaponType
import com.example.mklive.game.GameMode
import com.example.mklive.model.DriveCommand
import com.example.mklive.model.ImuSnapshot
import com.example.mklive.model.KartDeviceInfo
import com.example.mklive.model.OpenKartStateResponse

@Composable
fun CockpitOverlay(
    openKartState: OpenKartStateResponse,
    kartInfo: KartDeviceInfo,
    videoFps: Int,
    driveCommand: DriveCommand,
    imuSnapshot: ImuSnapshot,
    inputSource: String,
    coins: Int = 0,
    currentLap: Int = 1,
    activeWeapon: WeaponType = WeaponType.MINIGUN,
    ammo: Int = 99,
    isRouletteActive: Boolean = false,
    rouletteItem: WeaponType = WeaponType.NONE,
    isRecording: Boolean = false,
    recordingDurationSec: Int = 0,
    gameMode: GameMode = GameMode.GRAND_PRIX,
    balloonsRemaining: Int = 3,
    nextGateAngleDeg: Float? = null,
    onMenuClicked: () -> Unit = {},
    onAddBuilderElement: (String) -> Unit = {},
    onSaveTrackClicked: () -> Unit = {},
    onFireWeapon: () -> Unit = {},
    onCycleWeapon: () -> Unit = {},
    onDropGate: () -> Unit = {},
    onResetTrack: () -> Unit = {},
    onRecordToggle: () -> Unit,
    onPairClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    onToggleVrMode: () -> Unit,
    onResetHeading: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // 1. TOP BAR ROW (Stats on Left, Simple Turn Hint in Center, System & Controls on Right)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // TOP-LEFT: Brand & Race Stats (Coins & Lap / Balloons)
            Row(
                modifier = Modifier.align(Alignment.TopStart),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Coins Pill (Large, glowing gold)
                Surface(
                    color = Color(0xDD0D131F),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, Color(0xFFFFD54F)),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🪙", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "%02d".format(coins),
                            color = Color(0xFFFFD54F),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                if (gameMode == GameMode.BALLOON_BATTLE) {
                    // Balloon Pill
                    Surface(
                        color = Color(0xDD0D131F),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, Color(0xFFFF1744)),
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🎈", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "x$balloonsRemaining",
                                color = Color(0xFFFF1744),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                } else {
                    // Lap Pill
                    Surface(
                        color = Color(0xDD0D131F),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, Color(0xFF00E5FF)),
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🏁", fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "RUNDE $currentLap/3",
                                color = Color(0xFF00E5FF),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // TOP-CENTER: Friendly Directional Gate Arrow (Child-friendly navigation without technical numbers)
            if (nextGateAngleDeg != null) {
                val deg = nextGateAngleDeg.toInt()
                val arrow = when {
                    deg in -20..20 -> "▲ NÄCHSTES TOR"
                    deg in 21..70 -> "TOR RECHTS ▶"
                    deg > 70 -> "SCHARF RECHTS ▶▶"
                    deg in -70..-21 -> "◀ TOR LINKS"
                    else -> "◀◀ SCHARF LINKS"
                }
                Surface(
                    color = Color(0xEE0B1424),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, Color(0xFF00E5FF)),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    Text(
                        text = arrow,
                        color = Color(0xFF00E5FF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            // TOP-RIGHT: Controls Cluster (REC • Battery • Settings ⚙️ • Menu ⏸️)
            Row(
                modifier = Modifier.align(Alignment.TopEnd),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Record Indicator / Button
                if (isRecording) {
                    Surface(
                        onClick = onRecordToggle,
                        color = Color(0xDDE52521),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("● REC", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "%02d:%02d".format(recordingDurationSec / 60, recordingDurationSec % 60),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Battery Chip
                BatteryChip(
                    batteryLevel = kartInfo.battery ?: 0,
                    isCableConnected = kartInfo.cable == true
                )

                // Settings Button
                Surface(
                    onClick = onSettingsClicked,
                    color = Color(0xCC1A2433),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0x66FFFFFF)),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("⚙️", fontSize = 16.sp)
                    }
                }

                // Pause / Menu Button
                Surface(
                    onClick = onMenuClicked,
                    color = Color(0xCC0091EA),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, Color(0xFF80D8FF)),
                    modifier = Modifier.height(36.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⏸️", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("MENÜ", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
            }
        }

        // 2. MARIO KART ITEM SLOT (Top-Left under Stats - tap to use weapon!)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 58.dp)
        ) {
            val itemBorderColor = when {
                isRouletteActive -> Color(0xFFFFD600)
                activeWeapon != WeaponType.NONE -> Color(0xFFFF2A4D)
                else -> Color(0x55FFFFFF)
            }

            Surface(
                onClick = onFireWeapon,
                color = Color(0xEE0B121E),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, itemBorderColor),
                shadowElevation = 8.dp,
                modifier = Modifier.size(62.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isRouletteActive) {
                        Text(rouletteItem.icon, fontSize = 28.sp)
                    } else if (activeWeapon != WeaponType.NONE) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(activeWeapon.icon, fontSize = 24.sp)
                            Text(
                                text = "x$ammo",
                                color = Color(0xFFFFD54F),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        Text("❓", fontSize = 24.sp, color = Color(0x88FFFFFF))
                    }
                }
            }
        }

        // 3. BOTTOM-LEFT: Input & Brake Indicator
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 18.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (driveCommand.brake) {
                Surface(
                    color = Color(0xFFFF1744),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.5.dp, Color.White),
                    shadowElevation = 6.dp
                ) {
                    Text(
                        text = "🛑 BREMSE",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Surface(
                color = Color(0x99000000),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0x33FFFFFF))
            ) {
                Text(
                    text = if (inputSource.contains("Gamepad", ignoreCase = true)) "🎮 Gamepad" else "📱 Touch",
                    color = Color(0xCCFFFFFF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // 4. ARCADE SPEEDOMETER (Bottom-Right Corner)
        val isBoosting = driveCommand.throttle >= 120
        val throttlePct = when {
            isBoosting -> 100
            driveCommand.throttle > 0 -> ((driveCommand.throttle / 90f) * 70f).toInt().coerceIn(0, 70)
            else -> 0
        }
        ArcadeSpeedometerView(
            speedPercent = throttlePct,
            isBoosting = isBoosting,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 16.dp)
        )
    }
}

/**
 * Vibrant, arcade Nintendo Speedometer with dial tick marks, dynamic speed glow, and boost styling.
 */
@Composable
fun ArcadeSpeedometerView(
    speedPercent: Int,
    isBoosting: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(120.dp)
            .background(Color(0xE6080E1C), CircleShape)
            .border(
                width = if (isBoosting) 3.5.dp else 2.5.dp,
                color = if (isBoosting) Color(0xFFFFD54F) else Color(0x9900E5FF),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(11.dp)) {
            val strokeW = 10.dp.toPx()
            val centerPt = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
            val radius = (size.width - strokeW) / 2f

            // 1. Dial Tick Marks (9 notches from 150° to 390°)
            for (i in 0..8) {
                val tickAngleDeg = 150f + i * (240f / 8f)
                val tickRad = Math.toRadians(tickAngleDeg.toDouble())
                val cosT = kotlin.math.cos(tickRad).toFloat()
                val sinT = kotlin.math.sin(tickRad).toFloat()

                val innerR = radius - 8.dp.toPx()
                val outerR = radius - 2.dp.toPx()

                val p1 = androidx.compose.ui.geometry.Offset(centerPt.x + innerR * cosT, centerPt.y + innerR * sinT)
                val p2 = androidx.compose.ui.geometry.Offset(centerPt.x + outerR * cosT, centerPt.y + outerR * sinT)

                val tickCol = when {
                    i >= 6 -> Color(0xFFFF5252)
                    i >= 3 -> Color(0xFFFFD600)
                    else -> Color(0x8800E5FF)
                }
                drawLine(tickCol, p1, p2, strokeWidth = if (i % 2 == 0) 2.5f else 1.5f)
            }

            // 2. Background Track Arc
            drawArc(
                color = Color(0x3300E5FF),
                startAngle = 150f,
                sweepAngle = 240f,
                useCenter = false,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )

            // 3. Dynamic Colorful Speed Arc
            val sweep = (speedPercent.coerceIn(0, 100) / 100f) * 240f
            val arcColor = when {
                isBoosting -> Color(0xFFFF1744)
                speedPercent > 70 -> Color(0xFFFF5252)
                speedPercent > 40 -> Color(0xFFFFD600)
                speedPercent > 10 -> Color(0xFF00E676)
                else -> Color(0xFF00E5FF)
            }

            // Zero pip
            drawArc(
                color = if (speedPercent > 0) arcColor else Color(0x6600E5FF),
                startAngle = 150f,
                sweepAngle = sweep.coerceAtLeast(4f),
                useCenter = false,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
        }

        // Speed Number & KM/H in center
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val displaySpeed = speedPercent
            Text(
                text = "$displaySpeed",
                color = if (isBoosting) Color(0xFFFFD54F) else Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = if (isBoosting) "⚡ TURBO" else "KM/H",
                color = if (isBoosting) Color(0xFFFFD54F) else Color(0xFF00E5FF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun BatteryChip(batteryLevel: Int, isCableConnected: Boolean = false) {
    Surface(
        color = Color(0xDD0D131F),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (isCableConnected) Color(0xFFFFB300) else Color(0x55FFFFFF))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🔋", fontSize = 12.sp)
            Spacer(modifier = Modifier.width(5.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(4) { i ->
                    val color = if (i < batteryLevel) {
                        if (batteryLevel <= 1) Color(0xFFFF5252) else Color(0xFF00E676)
                    } else {
                        Color(0x33FFFFFF)
                    }
                    Box(
                        modifier = Modifier
                            .size(width = 5.dp, height = 10.dp)
                            .background(color, RoundedCornerShape(1.dp))
                    )
                }
            }
            if (isCableConnected) {
                Spacer(modifier = Modifier.width(4.dp))
                Text("⚡", fontSize = 10.sp)
            }
        }
    }
}
