package com.example.mklive.ui.vr

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mklive.R
import com.example.mklive.model.DriveCommand
import com.example.mklive.model.ImuSnapshot
import com.example.mklive.model.KartDeviceInfo
import com.example.mklive.model.OpenKartStateResponse
import com.example.mklive.model.VideoStreamMode
import com.example.mklive.ui.video.VideoSurfaceView
import com.example.mklive.video.H264MediaCodecPlayer
import kotlin.math.abs
import kotlin.math.min

/**
 * High-immersion Side-By-Side (SBS) Stereoscopic VR Cockpit for Meta Quest 3 & VR Headsets.
 *
 * Renders independent Left Eye and Right Eye viewports with:
 * - Ultra low latency FPV camera stream
 * - Subtle stereoscopic parallax IPD offset for true 3D depth perception
 * - Dynamic Artificial Gyroscopic Horizon responding in real-time to kart pitch & roll
 * - Futuristic jet-fighter style HUD instruments (Speedometer, Boost, G-Meter, Heading, Battery)
 */
@Composable
fun VrCockpitView(
    videoMode: VideoStreamMode,
    h264Player: H264MediaCodecPlayer,
    mjpegBitmap: Bitmap?,
    isStreamConnected: Boolean,
    openKartState: OpenKartStateResponse,
    kartInfo: KartDeviceInfo,
    driveCommand: DriveCommand,
    imuSnapshot: ImuSnapshot,
    videoFps: Int,
    isRecording: Boolean,
    recordingDurationSec: Int,
    inputSource: String,
    onToggleVr: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // LEFT EYE VIEWPORT (with +2dp parallax offset)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .border(1.dp, Color(0x3300E5FF))
        ) {
            EyeViewport(
                isLeftEye = true,
                videoMode = videoMode,
                h264Player = h264Player,
                mjpegBitmap = mjpegBitmap,
                isStreamConnected = isStreamConnected,
                openKartState = openKartState,
                kartInfo = kartInfo,
                driveCommand = driveCommand,
                imuSnapshot = imuSnapshot,
                videoFps = videoFps,
                isRecording = isRecording,
                recordingDurationSec = recordingDurationSec,
                inputSource = inputSource,
                onToggleVr = onToggleVr
            )
        }

        // RIGHT EYE VIEWPORT (with -2dp parallax offset)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .border(1.dp, Color(0x3300E5FF))
        ) {
            EyeViewport(
                isLeftEye = false,
                videoMode = videoMode,
                h264Player = h264Player,
                mjpegBitmap = mjpegBitmap,
                isStreamConnected = isStreamConnected,
                openKartState = openKartState,
                kartInfo = kartInfo,
                driveCommand = driveCommand,
                imuSnapshot = imuSnapshot,
                videoFps = videoFps,
                isRecording = isRecording,
                recordingDurationSec = recordingDurationSec,
                inputSource = inputSource,
                onToggleVr = onToggleVr
            )
        }
    }
}

@Composable
private fun EyeViewport(
    isLeftEye: Boolean,
    videoMode: VideoStreamMode,
    h264Player: H264MediaCodecPlayer,
    mjpegBitmap: Bitmap?,
    isStreamConnected: Boolean,
    openKartState: OpenKartStateResponse,
    kartInfo: KartDeviceInfo,
    driveCommand: DriveCommand,
    imuSnapshot: ImuSnapshot,
    videoFps: Int,
    isRecording: Boolean,
    recordingDurationSec: Int,
    inputSource: String,
    onToggleVr: () -> Unit
) {
    // Parallax IPD offset: gives a subtle stereoscopic depth to HUD elements relative to video
    val ipdOffset = if (isLeftEye) 4.dp else (-4).dp

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. VIDEO LAYER
        VideoSurfaceView(
            videoMode = videoMode,
            h264Player = h264Player,
            mjpegBitmap = mjpegBitmap,
            isStreamConnected = isStreamConnected,
            modifier = Modifier.fillMaxSize()
        )

        // 2. VR COCKPIT HUD LAYER
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = if (isLeftEye) ipdOffset else 0.dp, end = if (!isLeftEye) -ipdOffset else 0.dp)
        ) {
            // TOP BAR: REC Status + Compass + Battery
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Top Left: Character & FPS
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = (kartInfo.character ?: "MARIO").uppercase(),
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$videoFps FPS",
                        color = if (videoFps >= 20) Color(0xFF00E676) else Color(0xFFFFB300),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Top Center: Mini Compass
                val currentHdg = ((imuSnapshot.headingDeg % 360f + 360f) % 360f).toInt()
                val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
                val dirLabel = directions[((currentHdg + 22) / 45) % 8]
                Box(
                    modifier = Modifier
                        .background(Color(0x88000000), RoundedCornerShape(3.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${"%03d".format(currentHdg)}° $dirLabel",
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Top Right: Battery & REC indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isRecording) {
                        val mins = recordingDurationSec / 60
                        val secs = recordingDurationSec % 60
                        Text(
                            text = String.format(java.util.Locale.US, "🔴 %02d:%02d", mins, secs),
                            color = Color(0xFFFF1744),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = "⚡ ${kartInfo.battery ?: 0}/4",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // CENTER: ARTIFICIAL GYROSCOPIC HORIZON & RETICLE
            ArtificialHorizon(
                pitchG = imuSnapshot.gLong,
                rollG = imuSnapshot.gLat,
                modifier = Modifier
                    .size(140.dp)
                    .align(Alignment.Center)
            )

            // BOTTOM BAR: Cockpit Gauges (Steer, Throttle, Boost, Brake)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Steer Gauge
                val steerPct = (driveCommand.steering / 127f * 100).toInt()
                Text(
                    text = if (steerPct < -5) "< ${-steerPct}%" else if (steerPct > 5) "${steerPct}% >" else "◄ CTR ►",
                    color = Color(0xFF00E5FF),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                // Central Throttle & Boost
                if (driveCommand.throttle >= 120) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFFD600), RoundedCornerShape(2.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
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
                    val tPct = (driveCommand.throttle / 127f * 100).toInt()
                    Text(
                        text = "PWR: $tPct%",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Brake / Reverse lamp
                if (driveCommand.brake) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFF1744), RoundedCornerShape(2.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "BRAKE",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    Text(
                        text = inputSource.take(8),
                        color = Color(0x8800E5FF),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

/**
 * Real-time Artificial Gyro Horizon responding to kart inclination and drift G-forces.
 */
@Composable
fun ArtificialHorizon(pitchG: Float, rollG: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = min(cx, cy) - 10f

        // Calculate pitch offset and roll angle
        // Roll: gLat causes roll angle (-30 to +30 deg)
        val rollDeg = (rollG.coerceIn(-1.5f, 1.5f) * -25f)
        // Pitch: gLong causes pitch vertical shift (-20 to +20 px)
        val pitchDy = (pitchG.coerceIn(-1.5f, 1.5f) * 20f)

        // Outer Reticle Ring
        drawCircle(
            color = Color(0x3300E5FF),
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(1.5f)
        )

        // Center Aiming Pip
        drawCircle(
            color = Color(0x8800E5FF),
            radius = 3f,
            center = Offset(cx, cy)
        )

        // Dynamic Gyro Horizon Line
        rotate(degrees = rollDeg, pivot = Offset(cx, cy)) {
            // Horizon line
            val lineY = cy + pitchDy
            drawLine(
                color = Color(0xAA00E5FF),
                start = Offset(cx - radius * 0.7f, lineY),
                end = Offset(cx + radius * 0.7f, lineY),
                strokeWidth = 2f
            )

            // Pitch ladder ticks
            drawLine(
                color = Color(0x6600E5FF),
                start = Offset(cx - 15f, lineY - 16f),
                end = Offset(cx + 15f, lineY - 16f),
                strokeWidth = 1f
            )
            drawLine(
                color = Color(0x6600E5FF),
                start = Offset(cx - 15f, lineY + 16f),
                end = Offset(cx + 15f, lineY + 16f),
                strokeWidth = 1f
            )
        }
    }
}
