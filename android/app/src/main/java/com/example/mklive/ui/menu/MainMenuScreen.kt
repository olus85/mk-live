package com.example.mklive.ui.menu

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mklive.R
import com.example.mklive.game.GameState
import com.example.mklive.ui.common.GameAssetHelper
import com.example.mklive.ui.main.CockpitViewModel
import com.example.mklive.ui.pairing.PairingDialog
import com.example.mklive.ui.settings.SettingsDialog

@Composable
fun MainMenuScreen(viewModel: CockpitViewModel) {
    val selectedCharacter by viewModel.selectedCharacter.collectAsState()
    val selectedSpeedClass by viewModel.selectedSpeedClass.collectAsState()
    val selectedTrack by viewModel.selectedTrack.collectAsState()
    val kartInfo by viewModel.kartInfo.collectAsState()
    val isKartConnected = kartInfo.character != null
    val hostIp by viewModel.hostIp.collectAsState()
    val videoMode by viewModel.videoMode.collectAsState()
    val recordOverlayEnabled by viewModel.recordOverlayEnabled.collectAsState()
    val recordAudioEnabled by viewModel.recordAudioEnabled.collectAsState()
    val recordColorSwapEnabled by viewModel.recordColorSwapEnabled.collectAsState()
    val arEnabled by viewModel.arEnabled.collectAsState()
    val sfxEnabled by viewModel.sfxEnabled.collectAsState()
    val engineSoundEnabled by viewModel.engineSoundEnabled.collectAsState()
    val isVrMode by viewModel.isVrMode.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val touchControlsEnabled by viewModel.touchControlsEnabled.collectAsState()
    val serverQrBitmap by viewModel.serverQrBitmap.collectAsState()
    val serverPairingSsid by viewModel.serverPairingSsid.collectAsState()
    val serverPairingChannel by viewModel.serverPairingChannel.collectAsState()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
    val isPairingOpen by viewModel.isPairingOpen.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Breathing pulse animation for the "SPIELEN" button
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Fullscreen 3D Living Room Racing Background
        Image(
            painter = painterResource(id = R.drawable.bg_title_art),
            contentDescription = "Title Art",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Cinematic Gradient Overlay (Darkens top & bottom for high contrast)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xCC050A14),
                            Color(0x22050A14),
                            Color(0x33050A14),
                            Color(0xF0050A14)
                        )
                    )
                )
        )

        // 3. Foreground UI Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 36.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP BAR: Title & Kart Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Game Title Banner
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFFE52521),
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 8.dp
                    ) {
                        Text(
                            text = "MARIO KART",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = 2.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "LIVE",
                        color = Color(0xFFFFD54F),
                        fontWeight = FontWeight.Black,
                        fontSize = 26.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = Color(0xCC00E5FF),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "HOME CIRCUIT",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Status & Pairing Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Kart Status Pill
                    Surface(
                        color = Color(0xDD0A101C),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, if (isKartConnected) Color(0xFF00E676) else Color(0x55FFFFFF))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isKartConnected) Color(0xFF00E676) else Color(0xFFFFA000))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isKartConnected) "KART BEREIT ($hostIp)" else "DEMO MODUS (Offline)",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Quick Kart Pairing Button
                    Surface(
                        onClick = { viewModel.openPairing() },
                        color = Color(0xDD121B2B),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFF00E5FF))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔗", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "KOPPELN",
                                color = Color(0xFF00E5FF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // BOTTOM: The 3 Big Primary Nintendo Action Buttons (Clean & Uncluttered)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(94.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. SPIELEN (Main Hero Button - Large, Pulsing, Vibrant)
                Surface(
                    onClick = { viewModel.navigateTo(GameState.GARAGE) },
                    modifier = Modifier
                        .weight(1.5f)
                        .fillMaxHeight()
                        .scale(pulseScale),
                    color = Color(0xFFE52521),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(2.5.dp, Color(0xFFFFD54F)),
                    shadowElevation = 14.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(text = "🏁", fontSize = 38.sp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "SPIELEN",
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                            Text(
                                text = "🏎️ ${selectedCharacter.characterName}  •  ${selectedTrack.name}",
                                color = Color(0xFFFFD54F),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // 2. SPIELMODI (Grand Prix, Ballonschlacht, Zeitfahren, Editor)
                Surface(
                    onClick = { viewModel.navigateTo(GameState.MODE_SELECT) },
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight(),
                    color = Color(0xFF0277BD),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(2.dp, Color(0xFF40C4FF)),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(text = "🏆", fontSize = 32.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "SPIELMODI",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "GP, Ballons, Editor",
                                color = Color(0xFF80D8FF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 3. EINSTELLUNGEN (Options, Controls, Audio)
                Surface(
                    onClick = { viewModel.openSettings() },
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxHeight(),
                    color = Color(0xFF1E2838),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.5.dp, Color(0x8890A4AE)),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(text = "⚙️", fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "OPTIONEN",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Audio, VR, Steuerung",
                                color = Color(0x99FFFFFF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Dialogs
    if (isSettingsOpen) {
        SettingsDialog(
            currentHostIp = hostIp,
            currentVideoMode = videoMode,
            isRecordingOverlay = recordOverlayEnabled,
            isRecordingAudio = recordAudioEnabled,
            isRecordingColorSwap = recordColorSwapEnabled,
            isArEnabled = arEnabled,
            isSfxEnabled = sfxEnabled,
            isEngineSoundEnabled = engineSoundEnabled,
            isVrMode = isVrMode,
            currentLanguage = currentLanguage,
            isTouchControlsEnabled = touchControlsEnabled,
            onSetRecordingOverlay = { viewModel.setRecordingOverlay(it) },
            onSetRecordingAudio = { viewModel.setRecordingAudio(it) },
            onSetRecordingColorSwap = { viewModel.setRecordingColorSwap(it) },
            onSetArEnabled = { viewModel.setArEnabled(it) },
            onSetSfxEnabled = { viewModel.setSfxEnabled(it) },
            onSetEngineSoundEnabled = { viewModel.setEngineSoundEnabled(it) },
            onSetVrMode = { viewModel.setVrMode(it) },
            onSetLanguage = { viewModel.setLanguage(context, it) },
            onSetTouchControlsEnabled = { viewModel.setTouchControlsEnabled(it) },
            onSave = { newIp, newMode -> viewModel.saveSettings(newIp, newMode) },
            onDismiss = { viewModel.closeSettings() }
        )
    }

    if (isPairingOpen) {
        PairingDialog(
            serverQrBitmap = serverQrBitmap,
            serverPairingSsid = serverPairingSsid,
            serverPairingChannel = serverPairingChannel,
            onChannelSelected = { ch -> viewModel.triggerPairing(channel = ch, forceNew = true) },
            onTriggerNewCode = { ch -> viewModel.triggerPairing(channel = ch, forceNew = true) },
            onDismiss = { viewModel.closePairing() }
        )
    }
}
