package com.example.mklive.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.mklive.R
import com.example.mklive.model.VideoStreamMode

@Composable
fun SettingsDialog(
    currentHostIp: String,
    currentVideoMode: VideoStreamMode,
    isRecordingOverlay: Boolean,
    isRecordingAudio: Boolean,
    isRecordingColorSwap: Boolean,
    isArEnabled: Boolean,
    isSfxEnabled: Boolean,
    isEngineSoundEnabled: Boolean,
    isVrMode: Boolean,
    currentLanguage: String,
    isTouchControlsEnabled: Boolean = false,
    onSetRecordingOverlay: (Boolean) -> Unit,
    onSetRecordingAudio: (Boolean) -> Unit,
    onSetRecordingColorSwap: (Boolean) -> Unit,
    onSetArEnabled: (Boolean) -> Unit,
    onSetSfxEnabled: (Boolean) -> Unit,
    onSetEngineSoundEnabled: (Boolean) -> Unit,
    onSetVrMode: (Boolean) -> Unit,
    onSetLanguage: (String) -> Unit,
    onSetTouchControlsEnabled: (Boolean) -> Unit = {},
    onSave: (hostIp: String, videoMode: VideoStreamMode) -> Unit,
    onDismiss: () -> Unit
) {
    var hostIp by remember { mutableStateOf(currentHostIp) }
    var videoMode by remember { mutableStateOf(currentVideoMode) }
    var recordOverlay by remember { mutableStateOf(isRecordingOverlay) }
    var recordAudio by remember { mutableStateOf(isRecordingAudio) }
    var recordColorSwap by remember { mutableStateOf(isRecordingColorSwap) }
    var arEnabled by remember { mutableStateOf(isArEnabled) }
    var sfxEnabled by remember { mutableStateOf(isSfxEnabled) }
    var engineSoundEnabled by remember { mutableStateOf(isEngineSoundEnabled) }
    var vrMode by remember { mutableStateOf(isVrMode) }
    var selectedLang by remember { mutableStateOf(currentLanguage) }
    var touchControls by remember { mutableStateOf(isTouchControlsEnabled) }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("🎮 GAMEPLAY", "🕶️ AR & GRAFIK", "🔊 AUDIO", "🕹️ STEUERUNG", "🌐 NETZWERK")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xF0131A24),
            border = BorderStroke(1.5.dp, Color(0xFF00E5FF)),
            modifier = Modifier
                .width(680.dp)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚙️ SYSTEM EINSTELLUNGEN",
                        color = Color(0xFF00E5FF),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "v2.3.2 • AAA EDITION",
                        color = Color(0x88FFFFFF),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Navigation Tabs
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tabs.indices.toList()) { index ->
                        val isSelected = selectedTab == index
                        Surface(
                            onClick = { selectedTab = index },
                            color = if (isSelected) Color(0xFF00E5FF) else Color(0xCC1A2433),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF00E5FF) else Color(0x33FFFFFF)),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Text(
                                    text = tabs[index],
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Content Body
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color(0x880B1017), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                        .padding(14.dp)
                ) {
                    when (selectedTab) {
                        0 -> { // 🎮 GAMEPLAY
                            Column(
                                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Language
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("App-Sprache / Language", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("Wähle System, Deutsch oder Englisch", color = Color(0x88FFFFFF), fontSize = 10.sp)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        val langs = listOf("system" to "SYSTEM", "de" to "DEUTSCH", "en" to "ENGLISH")
                                        langs.forEach { (code, label) ->
                                            val isSel = selectedLang == code
                                            Surface(
                                                onClick = {
                                                    selectedLang = code
                                                    onSetLanguage(code)
                                                },
                                                color = if (isSel) Color(0xFF00E5FF) else Color(0x33FFFFFF),
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                                                    Text(label, color = if (isSel) Color.Black else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }

                                // VR Mode
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Stereoscopic SBS VR-Modus", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("Für Meta Quest 3 & VR Headsets (Side-by-Side 3D)", color = Color(0x88FFFFFF), fontSize = 10.sp)
                                    }
                                    Switch(
                                        checked = vrMode,
                                        onCheckedChange = {
                                            vrMode = it
                                            onSetVrMode(it)
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF), checkedTrackColor = Color(0x6600E5FF))
                                    )
                                }
                            }
                        }

                        1 -> { // 🕶️ AR & GRAFIK
                            Column(
                                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // AR Toggle
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Augmented Reality Hologramme", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("Münzen, ?-Boxen, Turbostreifen & Raketen einblenden", color = Color(0x88FFFFFF), fontSize = 10.sp)
                                    }
                                    Switch(
                                        checked = arEnabled,
                                        onCheckedChange = {
                                            arEnabled = it
                                            onSetArEnabled(it)
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF), checkedTrackColor = Color(0x6600E5FF))
                                    )
                                }

                                // Red/Blue Color Correction (BGR Swap)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Rot/Blau Farbkorrektur (BGR/RGB Swap)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("Korrigiert blaue Mario-Mützen in Videoaufnahmen", color = Color(0xFF00E676), fontSize = 10.sp)
                                    }
                                    Switch(
                                        checked = recordColorSwap,
                                        onCheckedChange = {
                                            recordColorSwap = it
                                            onSetRecordingColorSwap(it)
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E676), checkedTrackColor = Color(0x6600E676))
                                    )
                                }

                                // Stream Mode (H.264 vs MJPEG)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Video Streaming Decoder", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("MJPEG (Sehr stabil) oder H.264 MediaCodec (Ultrageringe Latenz)", color = Color(0x88FFFFFF), fontSize = 10.sp)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        VideoStreamMode.values().forEach { mode ->
                                            val isSel = videoMode == mode
                                            Surface(
                                                onClick = { videoMode = mode },
                                                color = if (isSel) Color(0xFF00E5FF) else Color(0x33FFFFFF),
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                                                    Text(mode.name.substringBefore("_"), color = if (isSel) Color.Black else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        2 -> { // 🔊 AUDIO
                            Column(
                                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // SFX Toggle
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Arcade Soundeffekte (SFX)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("Münz-Klingeln, Roulette, Raketenstart & Fanfaren", color = Color(0x88FFFFFF), fontSize = 10.sp)
                                    }
                                    Switch(
                                        checked = sfxEnabled,
                                        onCheckedChange = {
                                            sfxEnabled = it
                                            onSetSfxEnabled(it)
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD54F), checkedTrackColor = Color(0x66FFD54F))
                                    )
                                }

                                // Engine Sound Synthesizer
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Prozeduraler Motorsound Synthesizer", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("Dynamischer V8 / Elektromotor-Sound synchron zum Gaspedal", color = Color(0xFF81C784), fontSize = 10.sp)
                                    }
                                    Switch(
                                        checked = engineSoundEnabled,
                                        onCheckedChange = {
                                            engineSoundEnabled = it
                                            onSetEngineSoundEnabled(it)
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF81C784), checkedTrackColor = Color(0x6681C784))
                                    )
                                }
                            }
                        }

                        3 -> { // 🕹️ CONTROLS
                            Column(
                                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Touch Controller Toggle
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Touch-Steuerung (On-Screen)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("Virtuelles Lenkrad & Pedale einblenden. Für GameSir / physische Gamepads standardmäßig DEAKTIVIERT für freie Sicht!", color = Color(0xFF80D8FF), fontSize = 10.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Switch(
                                        checked = touchControls,
                                        onCheckedChange = {
                                            touchControls = it
                                            onSetTouchControlsEnabled(it)
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF), checkedTrackColor = Color(0x6600E5FF))
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text("GAMEPAD & CONTROLLER BELEGUNG:", color = Color(0xFFFFD54F), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("• R2 / Gas-Trigger: Beschleunigen (bis zu 100% Nitro)", color = Color.White, fontSize = 10.sp)
                                Text("• L2 / Bremse: Bremsen & Rückwärtsgang", color = Color.White, fontSize = 10.sp)
                                Text("• Linker Stick / D-Pad: Präzise Lenkung", color = Color.White, fontSize = 10.sp)
                                Text("• Taste A: Aktives Item einsetzen & Rocket Start Gas", color = Color(0xFF00E5FF), fontSize = 10.sp)
                                Text("• Taste X: Zielsuchende Raketen abfeuern", color = Color(0xFFFF5252), fontSize = 10.sp)
                                Text("• Taste Y: Bob-Omb / EMP-Schockwelle werfen", color = Color(0xFFFF9100), fontSize = 10.sp)
                                Text("• Taste L1: Minigun Schnellfeuer gedrückt halten", color = Color(0xFFFFD54F), fontSize = 10.sp)
                                Text("• D-Pad Oben: Checkpoint-Tor an aktueller Position setzen", color = Color(0xFF00E676), fontSize = 10.sp)
                                Text("• D-Pad Unten: AR-Strecke vor dem Kart neu ausrichten", color = Color(0xFF80D8FF), fontSize = 10.sp)
                            }
                        }

                        4 -> { // 🌐 NETWORK
                            Column(
                                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("KART BASE STATION VERBINDUNG:", color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = hostIp,
                                    onValueChange = { hostIp = it },
                                    label = { Text("IP-Adresse des PC Servers / Hotspots", fontSize = 10.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF00E5FF),
                                        unfocusedBorderColor = Color(0x66FFFFFF)
                                    )
                                )
                                Text("Ports: WebSocket 9000 • Video 9001 • Teleoperation 5102 UDP • Telemetrie 19002 UDP", color = Color(0x88FFFFFF), fontSize = 9.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("ABBRECHEN", fontSize = 10.sp, color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            onSave(hostIp, videoMode)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("SPEICHERN & ANWENDEN", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
