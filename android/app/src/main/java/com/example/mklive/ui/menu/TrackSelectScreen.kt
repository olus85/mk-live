package com.example.mklive.ui.menu

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mklive.game.GameState
import com.example.mklive.game.SpeedClass
import com.example.mklive.game.TrackManager
import com.example.mklive.game.TrackPreset
import com.example.mklive.ui.common.GameAssetHelper
import com.example.mklive.ui.main.CockpitViewModel

@Composable
fun TrackSelectScreen(viewModel: CockpitViewModel) {
    val selectedTrack by viewModel.selectedTrack.collectAsState()
    val selectedSpeedClass by viewModel.selectedSpeedClass.collectAsState()
    val customTracks by viewModel.customTracks.collectAsState()

    val builtinPresets = remember { TrackManager.getBuiltinPresets() }
    val allTracks = remember(customTracks) { builtinPresets + customTracks }

    val infiniteTransition = rememberInfiniteTransition(label = "racePulse")
    val startPulse by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B14))
            .padding(horizontal = 28.dp, vertical = 18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. TOP BAR: Back, Title & Start Race Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back Button
                Surface(
                    onClick = { viewModel.navigateTo(GameState.GARAGE) },
                    color = Color(0xFF1E2838),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, Color(0x66FFFFFF))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("◀", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("FAHRER", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                }

                // Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "CUP",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "WÄHLEN",
                        color = Color(0xFF00E5FF),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                // Big Start Race Button
                Surface(
                    onClick = { viewModel.startRace() },
                    modifier = Modifier.scale(startPulse),
                    color = Color(0xFFFF2A4D),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(2.dp, Color(0xFFFFD54F)),
                    shadowElevation = 12.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🏁", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "STARTEN!",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // 2. CENTER: Horizontal Row of Huge Cup Cards (Full Height, Uncropped Trophies!)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(allTracks) { track ->
                    val isSelected = selectedTrack.id == track.id
                    val cardBorder = if (isSelected) Color(0xFF00E5FF) else Color(0x33FFFFFF)
                    val cardBg = if (isSelected) Color(0xFF111E30) else Color(0xFF0C131D)

                    Surface(
                        onClick = { viewModel.selectTrack(track) },
                        modifier = Modifier
                            .width(275.dp)
                            .fillMaxHeight(),
                        color = cardBg,
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(if (isSelected) 3.5.dp else 1.5.dp, cardBorder),
                        shadowElevation = if (isSelected) 14.dp else 4.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // 3D Cup Trophy Image (Fit mode, fully visible mushroom and flower!)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF06090F)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = GameAssetHelper.getDrawableResId(track.drawableResName)),
                                    contentDescription = track.name,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp)
                                )

                                if (isSelected) {
                                    Surface(
                                        color = Color(0xFF00E5FF),
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("✓", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Name & Elements Count
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = track.name,
                                    color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val summaryText = when (track.id) {
                                    "room_dynamic_arcade" -> "🚀 Dynamische Tore vor dem Kart • 0% Wandkollision"
                                    "track_builder_circuit" -> "🎨 Mit Kart abfahren • Tore frei im Raum platzieren"
                                    else -> {
                                        val gates = track.elements.count { it.type == "GATE" }
                                        val coins = track.elements.count { it.type == "COIN" }
                                        val pads = track.elements.count { it.type == "DASH_PAD" }
                                        "🚩 $gates Tore • 🪙 $coins Münzen • ⚡ $pads Turbo"
                                    }
                                }
                                Text(
                                    text = summaryText,
                                    color = if (isSelected) Color(0xFF80D8FF) else Color(0xBBFFFFFF),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // 4. BOTTOM BAR: Secondary Track Builder Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    onClick = {
                        viewModel.selectMode(com.example.mklive.game.GameMode.TRACK_BUILDER)
                        viewModel.startRace()
                    },
                    color = Color(0xFF131C28),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0x6600E5FF))
                ) {
                    Text(
                        text = "🛠️ EIGENE STRECKE IM AR-EDITOR BAUEN",
                        color = Color(0xFF80D8FF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp)
                    )
                }
            }
        }
    }
}
