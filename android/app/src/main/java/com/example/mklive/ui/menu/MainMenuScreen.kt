package com.example.mklive.ui.menu

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mklive.game.GameMode
import com.example.mklive.game.GameState
import com.example.mklive.ui.main.CockpitViewModel

@Composable
fun MainMenuScreen(
    viewModel: CockpitViewModel,
    modifier: Modifier = Modifier
) {
    val selectedMode by viewModel.selectedMode.collectAsState()
    val selectedCharacter by viewModel.selectedCharacter.collectAsState()
    val selectedSpeedClass by viewModel.selectedSpeedClass.collectAsState()
    val selectedTrack by viewModel.selectedTrack.collectAsState()
    val kartInfo by viewModel.kartInfo.collectAsState()
    val openKartState by viewModel.openKartState.collectAsState()
    val isStreamConnected by viewModel.mjpegPlayer.isConnected.collectAsState()
    val videoFps by viewModel.mjpegPlayer.fps.collectAsState()
    val hostIp by viewModel.hostIp.collectAsState()

    // Pulse animation for Start Button
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseColor by infiniteTransition.animateColor(
        initialValue = Color(0xFF00E5FF),
        targetValue = Color(0xFFFF1744),
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseColor"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0E17),
                        Color(0xFF101826),
                        Color(0xFF05080F)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 🌟 1. HEADER: Arcade Title Banner + Live Kart Telemetry Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title & Subtitle
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "MARIO KART LIVE",
                            color = Color(0xFFFFD54F),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            fontStyle = FontStyle.Italic,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Surface(
                            color = Color(0xFFE52521),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "AR PRO",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "HOME CIRCUIT • NEXT-GEN MIXED REALITY EDITION",
                        color = Color(0xFF80D8FF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp
                    )
                }

                // Live Kart Telemetry Pill
                Surface(
                    color = Color(0xDD151E28),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, if (kartInfo.character != null) Color(0xFF00E676) else Color(0x66FFFFFF))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Connection Dot
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isStreamConnected) Color(0xFF00E676) else Color(0xFFFF5252))
                        )
                        Text(
                            text = if (isStreamConnected) "KART ONLINE (${hostIp})" else "CONNECTING... (${hostIp})",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        if (kartInfo.battery != null) {
                            Text(
                                text = "🔋 ${kartInfo.battery}%",
                                color = if ((kartInfo.battery ?: 0) > 20) Color(0xFF00E676) else Color(0xFFFF5252),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "⚡ ${videoFps} FPS",
                            color = Color(0xFF00E5FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // 🌟 2. GAME MODE SELECTION (Interactive Cards)
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "SELECT GAME MODE / SPIELMODUS WÄHLEN",
                    color = Color(0x99FFFFFF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(GameMode.values()) { mode ->
                        val isSelected = mode == selectedMode
                        val cardBorder = if (isSelected) Color(0xFF00E5FF) else Color(0x33FFFFFF)
                        val cardBg = if (isSelected) Color(0xEE1A2938) else Color(0xCC111822)

                        Surface(
                            modifier = Modifier
                                .width(220.dp)
                                .height(150.dp)
                                .clickable { viewModel.selectMode(mode) },
                            shape = RoundedCornerShape(12.dp),
                            color = cardBg,
                            border = BorderStroke(if (isSelected) 2.dp else 1.dp, cardBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = mode.icon, fontSize = 28.sp)
                                    if (isSelected) {
                                        Surface(
                                            color = Color(0xFF00E5FF),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "ACTIVE",
                                                color = Color.Black,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Column {
                                    Text(
                                        text = mode.titleDe,
                                        color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.SansSerif
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = mode.descDe,
                                        color = Color(0xBBFFFFFF),
                                        fontSize = 10.sp,
                                        lineHeight = 13.sp,
                                        maxLines = 3
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 🌟 3. ACTIVE SETUP SUMMARY (Character • Speed Class • Selected Track)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x880C141F), RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0x4400E5FF), RoundedCornerShape(10.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Character pill
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = selectedCharacter.avatarIcon, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(text = "FAHRER", color = Color(0x88FFFFFF), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(text = selectedCharacter.characterName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Text("•", color = Color(0x44FFFFFF))

                // Engine Class
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "⚙️", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(text = "KLASSE", color = Color(0x88FFFFFF), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(text = selectedSpeedClass.label, color = Color(0xFFFFD54F), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Text("•", color = Color(0x44FFFFFF))

                // Track
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🏁", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(text = "STRECKE", color = Color(0x88FFFFFF), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(text = selectedTrack.name, color = Color(0xFF80D8FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 🌟 4. BOTTOM ACTION BAR (Garage, Tracks, Settings, Pairing & Big Launch Button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Quick Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Garage Button
                    Surface(
                        onClick = { viewModel.navigateTo(GameState.GARAGE) },
                        color = Color(0xCC182230),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0x6600E5FF)),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🏎️", fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("GARAGE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Track Select Button
                    Surface(
                        onClick = { viewModel.navigateTo(GameState.TRACK_SELECT) },
                        color = Color(0xCC182230),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0x6600E5FF)),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🗺️", fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("STRECKEN", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Settings Button
                    Surface(
                        onClick = { viewModel.openSettings() },
                        color = Color(0xCC182230),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0x66FFFFFF)),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⚙️", fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SETTINGS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Pairing QR Button
                    Surface(
                        onClick = { viewModel.openPairing() },
                        color = Color(0xCC182230),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0x66FFD54F)),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔗", fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("KOPPELN", color = Color(0xFFFFD54F), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Big Glowing "START RACE" Button
                Surface(
                    onClick = { viewModel.startRace() },
                    color = pulseColor,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(46.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🏁", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedMode == GameMode.TRACK_BUILDER) "EDITOR STARTEN" else "RENNEN STARTEN",
                            color = Color.Black,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
