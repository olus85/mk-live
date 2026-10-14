package com.example.mklive.ui.hud

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mklive.R
import com.example.mklive.combat.WeaponType
import com.example.mklive.model.DriveCommand
import com.example.mklive.model.ImuSnapshot
import com.example.mklive.model.KartDeviceInfo
import com.example.mklive.model.OpenKartStateResponse
import kotlin.math.min

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
    var showQuickMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // TOP BAR ROW (Pinned components: Stats on Left, Status in Center, Controls on Right)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // 1. TOP-LEFT: Brand & Race Stats (Coins & Lap)
            Row(
                modifier = Modifier.align(Alignment.TopStart),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.hud_brand),
                    color = Color(0xFF00E5FF),
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(8.dp))

                // Coins Pill
                Surface(
                    color = Color(0xDD111111),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFD54F))
                ) {
                    Text(
                        text = "🪙 x${"%02d".format(coins)}",
                        color = Color(0xFFFFD54F),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(5.dp))

                // Lap Pill
                Surface(
                    color = Color(0xDD111111),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF))
                ) {
                    Text(
                        text = "🏁 LAP $currentLap/3",
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                    )
                }
            }

            // 2. TOP-CENTER: Compact Telemetry Status Pill
            Surface(
                modifier = Modifier.align(Alignment.TopCenter),
                color = Color(0xCC0A0E14),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0x4400E5FF))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text(
                        text = (kartInfo.character ?: "KART").uppercase(),
                        color = if (kartInfo.character != null) Color(0xFF00E676) else Color(0x99FFFFFF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text("•", color = Color(0x44FFFFFF), fontSize = 9.sp)
                    Text(
                        text = "$videoFps FPS",
                        color = if (videoFps >= 20) Color.White else Color(0xFFFF9100),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text("•", color = Color(0x44FFFFFF), fontSize = 9.sp)
                    BatteryChip(
                        batteryLevel = kartInfo.battery ?: 0,
                        isCableConnected = kartInfo.cable == true
                    )
                }
            }

            // 3. TOP-RIGHT: Controls Cluster (REC • Quick Actions • Settings ⚙️)
            Row(
                modifier = Modifier.align(Alignment.TopEnd),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Record Button
                Surface(
                    onClick = onRecordToggle,
                    color = if (isRecording) Color(0xFFD50000) else Color(0xCC1E272C),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, if (isRecording) Color(0xFFFF5252) else Color(0x66FF1744)),
                    modifier = Modifier.height(32.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val mins = recordingDurationSec / 60
                        val secs = recordingDurationSec % 60
                        val timeStr = String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
                        if (isRecording) {
                            Text("⏹ $timeStr", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        } else {
                            Text("⏺ REC", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF5252))
                        }
                    }
                }

                // Quick Actions Dropdown Menu Button
                Box {
                    Surface(
                        onClick = { showQuickMenu = !showQuickMenu },
                        color = Color(0xCC1E272C),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Color(0x6600E5FF)),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🏎️ ACTIONS ▾", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))
                        }
                    }

                    DropdownMenu(
                        expanded = showQuickMenu,
                        onDismissRequest = { showQuickMenu = false },
                        modifier = Modifier.background(Color(0xF510161C))
                    ) {
                        DropdownMenuItem(
                            text = { Text("🚩 Checkpoint-Tor platzieren", color = Color.White, fontSize = 12.sp) },
                            onClick = {
                                showQuickMenu = false
                                onDropGate()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("🔄 AR-Strecke vor Kart zentrieren", color = Color(0xFFFFD54F), fontSize = 12.sp) },
                            onClick = {
                                showQuickMenu = false
                                onResetTrack()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("🥽 VR-Modus (Quest / SBS)", color = Color(0xFFB388FF), fontSize = 12.sp) },
                            onClick = {
                                showQuickMenu = false
                                onToggleVrMode()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("🔗 Kart koppeln (QR-Code)", color = Color(0xFF80D8FF), fontSize = 12.sp) },
                            onClick = {
                                showQuickMenu = false
                                onPairClicked()
                            }
                        )
                    }
                }

                // Settings Button (Always pinned and fully visible!)
                Surface(
                    onClick = onSettingsClicked,
                    color = Color(0xCC263238),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0x8890A4AE)),
                    modifier = Modifier.height(32.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚙️", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.hud_settings), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // MARIO KART ITEM SLOT (Top-Left under Stats - tap to use weapon!)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 46.dp)
        ) {
            val itemBorderColor = when {
                isRouletteActive -> Color(0xFFFF9100)
                activeWeapon != WeaponType.NONE -> Color(0xFFFF1744)
                else -> Color(0x44FFFFFF)
            }

            Surface(
                onClick = onFireWeapon,
                color = Color(0xDD12161A),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.5.dp, itemBorderColor),
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isRouletteActive) {
                        Text(rouletteItem.icon, fontSize = 24.sp)
                    } else if (activeWeapon != WeaponType.NONE) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(activeWeapon.icon, fontSize = 20.sp)
                            Text(
                                text = "x$ammo",
                                color = Color(0xFFFFD54F),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        Text("🎲", fontSize = 18.sp, color = Color(0x44FFFFFF))
                    }
                }
            }
        }

        // COMPASS TAPE (TOP CENTER)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 44.dp)
                .clickable { onResetHeading() }
        ) {
            CompassTape(headingDeg = imuSnapshot.headingDeg)
        }

        // G-METER INSTRUMENT (TOP RIGHT)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 46.dp, end = 16.dp)
        ) {
            GMeterView(gLat = imuSnapshot.gLat, gLong = imuSnapshot.gLong)
        }

        // BOTTOM DASHBOARD (Steer, Throttle, Brake lamp, Input source)
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Steering Gauge
            AxisGauge(label = stringResource(R.string.hud_steer), value = (driveCommand.steering / 127f * 100).toInt())
            Spacer(modifier = Modifier.width(14.dp))

            // Throttle & Boost Gauge
            val throttlePercent = (driveCommand.throttle / 127f * 100).toInt()
            if (driveCommand.throttle >= 120) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFFFD600), RoundedCornerShape(2.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.hud_boost),
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                AxisGauge(label = stringResource(R.string.hud_throttle), value = throttlePercent)
            }
            Spacer(modifier = Modifier.width(14.dp))

            // Brake Lamp
            Box(
                modifier = Modifier
                    .background(if (driveCommand.brake) Color(0xFFFF1744) else Color(0x33263238), RoundedCornerShape(2.dp))
                    .border(1.dp, Color(0x66FF1744), RoundedCornerShape(2.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.hud_brake),
                    color = if (driveCommand.brake) Color.White else Color(0x66FFFFFF),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.width(14.dp))

            // Input source
            Text(
                text = stringResource(R.string.hud_input, inputSource),
                color = Color(0xAA00E5FF),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun HudChip(
    label: String,
    value: String,
    isOk: Boolean = false,
    isWarn: Boolean = false,
    isAlert: Boolean = false
) {
    val borderColor = when {
        isAlert -> Color(0xFFFF1744)
        isWarn -> Color(0xFFFFAB00)
        isOk -> Color(0xFF00E676)
        else -> Color(0x4490A4AE)
    }

    Box(
        modifier = Modifier
            .background(Color(0x77000000), RoundedCornerShape(3.dp))
            .border(1.dp, borderColor, RoundedCornerShape(3.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Color(0x99FFFFFF), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(4.dp))
            Text(value, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun BatteryChip(batteryLevel: Int, isCableConnected: Boolean = false) {
    Box(
        modifier = Modifier
            .background(Color(0x77000000), RoundedCornerShape(3.dp))
            .border(1.dp, if (isCableConnected) Color(0xFFFFB300) else Color(0x4490A4AE), RoundedCornerShape(3.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.hud_battery), color = Color(0x99FFFFFF), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(4) { i ->
                    val color = if (i < batteryLevel) {
                        if (batteryLevel <= 1) Color(0xFFFF5252) else Color(0xFF00E676)
                    } else {
                        Color(0x33FFFFFF)
                    }
                    Box(
                        modifier = Modifier
                            .size(width = 5.dp, height = 9.dp)
                            .background(color)
                    )
                }
            }
            if (isCableConnected) {
                Spacer(modifier = Modifier.width(4.dp))
                Text("🔌", fontSize = 9.sp)
            }
        }
    }
}

@Composable
fun CompassTape(headingDeg: Float) {
    val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    val currentHdg = ((headingDeg % 360f + 360f) % 360f).toInt()

    Box(
        modifier = Modifier
            .width(170.dp)
            .height(26.dp)
            .background(Color(0x88000000), RoundedCornerShape(4.dp))
            .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        val dirIdx = ((currentHdg + 22) / 45) % 8
        val dirLabel = directions[dirIdx]

        Text(
            text = "${"%03d".format(currentHdg)}°  $dirLabel",
            color = Color(0xFF00E5FF),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun GMeterView(gLat: Float, gLong: Float) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .background(Color(0x88000000), RoundedCornerShape(32.dp))
            .border(1.dp, Color(0x4400E5FF), RoundedCornerShape(32.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = min(cx, cy) - 5f

            // Circles
            drawCircle(color = Color(0x3300E5FF), radius = radius * 0.5f, center = Offset(cx, cy), style = Stroke(1f))
            drawCircle(color = Color(0x5500E5FF), radius = radius, center = Offset(cx, cy), style = Stroke(1f))
            // Crosshair
            drawLine(color = Color(0x3300E5FF), start = Offset(cx - radius, cy), end = Offset(cx + radius, cy))
            drawLine(color = Color(0x3300E5FF), start = Offset(cx, cy - radius), end = Offset(cx, cy + radius))

            // G-ball position (clamped)
            val bx = cx + (gLat.coerceIn(-1f, 1f) * radius)
            val by = cy - (gLong.coerceIn(-1f, 1f) * radius)

            drawCircle(color = Color(0x6600E5FF), radius = 5f, center = Offset(bx, by))
            drawCircle(color = Color(0xFF00E5FF), radius = 2.5f, center = Offset(bx, by))
        }
    }
}

@Composable
fun AxisGauge(label: String, value: Int) {
    Column {
        Text(
            text = "$label: $value%",
            color = Color(0x99FFFFFF),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .width(85.dp)
                .height(5.dp)
                .background(Color(0x44263238), RoundedCornerShape(3.dp))
        ) {
            val pct = (value.coerceIn(-100, 100) / 100f)
            val fillWidth = 42.dp * kotlin.math.abs(pct)
            val isNegative = pct < 0

            Box(
                modifier = Modifier
                    .width(fillWidth)
                    .height(5.dp)
                    .padding(start = if (isNegative) (42.dp - fillWidth) else 42.dp)
                    .background(if (isNegative) Color(0xFFFF9100) else Color(0xFF00E5FF), RoundedCornerShape(3.dp))
            )
        }
    }
}
