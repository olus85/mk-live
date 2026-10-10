package com.example.mklive.ui.hud

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    isRecording: Boolean = false,
    recordingDurationSec: Int = 0,
    onRecordToggle: () -> Unit,
    onPairClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    onResetHeading: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // TOP STATUS BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand & Chips
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "MK LIVE",
                    color = Color(0xFF00E5FF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(12.dp))

                // AP State Chip
                HudChip(
                    label = "AP",
                    value = openKartState.state,
                    isOk = openKartState.ok && openKartState.state == "RUNNING",
                    isWarn = openKartState.state == "PAIRING"
                )
                Spacer(modifier = Modifier.width(6.dp))

                // Kart Link Chip
                HudChip(
                    label = "LINK",
                    value = (kartInfo.character ?: "KART").uppercase(),
                    isOk = kartInfo.character != null
                )
                Spacer(modifier = Modifier.width(6.dp))

                // Video FPS Chip
                HudChip(
                    label = "VIDEO",
                    value = "$videoFps FPS",
                    isOk = videoFps >= 20,
                    isWarn = videoFps in 1..19
                )
                Spacer(modifier = Modifier.width(6.dp))

                // Battery Chip (with subtle cable icon if plugged in)
                BatteryChip(
                    batteryLevel = kartInfo.battery ?: 0,
                    isCableConnected = kartInfo.cable == true
                )
            }

            // Actions: Record, Pair Kart & Settings
            Row(verticalAlignment = Alignment.CenterVertically) {
                // RECORD BUTTON
                Button(
                    onClick = onRecordToggle,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) Color(0xFFD50000) else Color(0x33263238)
                    ),
                    border = if (isRecording) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252)) else androidx.compose.foundation.BorderStroke(1.dp, Color(0x66FF1744)),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    val mins = recordingDurationSec / 60
                    val secs = recordingDurationSec % 60
                    val timeStr = String.format(java.util.Locale.getDefault(), "%02d:%02d", mins, secs)
                    if (isRecording) {
                        Text("⏹ $timeStr", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    } else {
                        Text("🔴 REC", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF5252))
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onPairClicked,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF)),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("PAIR KART", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onSettingsClicked,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F)),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("⚙ SETTINGS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // COMPASS TAPE (TOP CENTER)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 46.dp)
                .clickable { onResetHeading() }
        ) {
            CompassTape(headingDeg = imuSnapshot.headingDeg)
        }

        // G-METER INSTRUMENT (TOP RIGHT)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 16.dp)
        ) {
            GMeterView(gLat = imuSnapshot.gLat, gLong = imuSnapshot.gLong)
        }

        // BOTTOM DASHBOARD (Steer, Throttle, Brake lamp, Input source)
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Steering Gauge
            AxisGauge(label = "STEER", value = (driveCommand.steering / 127f * 100).toInt())
            Spacer(modifier = Modifier.width(16.dp))

            // Throttle & Boost Gauge
            val throttlePercent = (driveCommand.throttle / 127f * 100).toInt()
            if (driveCommand.throttle >= 120) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFFFD600), RoundedCornerShape(2.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "🚀 BOOST 100%",
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                AxisGauge(label = "THROTTLE", value = throttlePercent)
            }
            Spacer(modifier = Modifier.width(16.dp))

            // Brake Lamp
            Box(
                modifier = Modifier
                    .background(if (driveCommand.brake) Color(0xFFFF1744) else Color(0x33263238), RoundedCornerShape(2.dp))
                    .border(1.dp, Color(0x66FF1744), RoundedCornerShape(2.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "BRAKE",
                    color = if (driveCommand.brake) Color.White else Color(0x66FFFFFF),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.width(16.dp))

            // Input source
            Text(
                text = "INPUT: $inputSource",
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
            Text(label, color = Color(0x99FFFFFF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(4.dp))
            Text(value, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
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
            Text("BAT", color = Color(0x99FFFFFF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                            .size(width = 5.dp, height = 10.dp)
                            .background(color)
                    )
                }
            }
            if (isCableConnected) {
                Spacer(modifier = Modifier.width(4.dp))
                Text("🔌", fontSize = 10.sp)
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
            .width(180.dp)
            .height(28.dp)
            .background(Color(0x88000000), RoundedCornerShape(4.dp))
            .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        val dirIdx = ((currentHdg + 22) / 45) % 8
        val dirLabel = directions[dirIdx]

        Text(
            text = "${"%03d".format(currentHdg)}°  $dirLabel",
            color = Color(0xFF00E5FF),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun GMeterView(gLat: Float, gLong: Float) {
    Box(
        modifier = Modifier
            .size(70.dp)
            .background(Color(0x88000000), RoundedCornerShape(35.dp))
            .border(1.dp, Color(0x4400E5FF), RoundedCornerShape(35.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = min(cx, cy) - 6f

            // Circles
            drawCircle(color = Color(0x3300E5FF), radius = radius * 0.5f, center = Offset(cx, cy), style = Stroke(1f))
            drawCircle(color = Color(0x5500E5FF), radius = radius, center = Offset(cx, cy), style = Stroke(1f))
            // Crosshair
            drawLine(color = Color(0x3300E5FF), start = Offset(cx - radius, cy), end = Offset(cx + radius, cy))
            drawLine(color = Color(0x3300E5FF), start = Offset(cx, cy - radius), end = Offset(cx, cy + radius))

            // G-ball position (clamped)
            val bx = cx + (gLat.coerceIn(-1f, 1f) * radius)
            val by = cy - (gLong.coerceIn(-1f, 1f) * radius)

            drawCircle(color = Color(0x6600E5FF), radius = 6f, center = Offset(bx, by))
            drawCircle(color = Color(0xFF00E5FF), radius = 3f, center = Offset(bx, by))
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
                .width(90.dp)
                .height(6.dp)
                .background(Color(0x44263238), RoundedCornerShape(3.dp))
        ) {
            val pct = (value.coerceIn(-100, 100) / 100f)
            val fillWidth = 45.dp * kotlin.math.abs(pct)
            val isNegative = pct < 0

            Box(
                modifier = Modifier
                    .width(fillWidth)
                    .height(6.dp)
                    .padding(start = if (isNegative) (45.dp - fillWidth) else 45.dp)
                    .background(if (isNegative) Color(0xFFFF9100) else Color(0xFF00E5FF), RoundedCornerShape(3.dp))
            )
        }
    }
}
