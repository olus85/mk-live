package com.example.mklive.ui.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
    onSetRecordingOverlay: (Boolean) -> Unit,
    onSetRecordingAudio: (Boolean) -> Unit,
    onSetRecordingColorSwap: (Boolean) -> Unit,
    onSetArEnabled: (Boolean) -> Unit,
    onSetSfxEnabled: (Boolean) -> Unit,
    onSetEngineSoundEnabled: (Boolean) -> Unit,
    onSetVrMode: (Boolean) -> Unit,
    onSetLanguage: (String) -> Unit,
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E262C),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF37474F)),
            modifier = Modifier
                .width(720.dp)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(14.dp)
                    .fillMaxWidth()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settings_title),
                        color = Color(0xFF00E5FF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            color = Color(0x3300E5FF),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.settings_tag_pc),
                                color = Color(0xFF00E5FF),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Surface(
                            color = Color(0x337C4DFF),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.settings_tag_quest),
                                color = Color(0xFFB388FF),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2-Column Compact Layout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // LEFT COLUMN: Base IP, Video Mode, AR & VR
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Section: Base Station IP
                        Column {
                            Text(
                                text = stringResource(R.string.settings_base_ip),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            OutlinedTextField(
                                value = hostIp,
                                onValueChange = { hostIp = it },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF00E5FF)
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00E5FF),
                                    unfocusedBorderColor = Color(0xFF37474F),
                                    focusedContainerColor = Color(0xFF141A1E),
                                    unfocusedContainerColor = Color(0xFF141A1E)
                                ),
                                modifier = Modifier.fillMaxWidth().height(44.dp)
                            )
                        }

                        // Section: Video Mode
                        Column {
                            Text(
                                text = stringResource(R.string.settings_video_mode),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = { videoMode = VideoStreamMode.H264_MEDIACODEC },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (videoMode == VideoStreamMode.H264_MEDIACODEC) Color(0xFF00B0FF) else Color(0x3337474F)
                                    ),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.weight(1f).height(30.dp)
                                ) {
                                    Text(
                                        stringResource(R.string.settings_h264_label),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (videoMode == VideoStreamMode.H264_MEDIACODEC) Color.White else Color(0xFFCFD8DC)
                                    )
                                }

                                Button(
                                    onClick = { videoMode = VideoStreamMode.MJPEG_STREAM },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (videoMode == VideoStreamMode.MJPEG_STREAM) Color(0xFF00B0FF) else Color(0x3337474F)
                                    ),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.weight(1f).height(30.dp)
                                ) {
                                    Text(
                                        stringResource(R.string.settings_mjpeg_label),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (videoMode == VideoStreamMode.MJPEG_STREAM) Color.White else Color(0xFFCFD8DC)
                                    )
                                }
                            }
                        }

                        // Section: AR Toggle
                        Surface(
                            color = Color(0xFF141A1E),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (arEnabled) Color(0xFF00E5FF) else Color(0x2237474F)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(stringResource(R.string.settings_ar_toggle), color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                    Text(stringResource(R.string.settings_ar_desc), color = Color(0xFF00E5FF), fontSize = 7.5.sp)
                                }
                                Switch(
                                    checked = arEnabled,
                                    onCheckedChange = {
                                        arEnabled = it
                                        onSetArEnabled(it)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF00E5FF),
                                        checkedTrackColor = Color(0x6600E5FF)
                                    )
                                )
                            }
                        }

                        // Section: VR & Meta Quest 3 Mode
                        Surface(
                            color = Color(0xFF141A1E),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (vrMode) Color(0xFF7C4DFF) else Color(0x2237474F)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(stringResource(R.string.settings_vr_toggle), color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                    Text(stringResource(R.string.settings_vr_desc), color = Color(0xFFB388FF), fontSize = 7.5.sp)
                                }
                                Switch(
                                    checked = vrMode,
                                    onCheckedChange = {
                                        vrMode = it
                                        onSetVrMode(it)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFFB388FF),
                                        checkedTrackColor = Color(0x667C4DFF)
                                    )
                                )
                            }
                        }
                    }

                    // RIGHT COLUMN: Recording Options, Audio Engine & Language
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_rec_title),
                            color = Color.White,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        // Option 1: Cockpit HUD in Video
                        Surface(
                            color = Color(0xFF141A1E),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(stringResource(R.string.settings_rec_hud_title), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text(stringResource(R.string.settings_rec_hud_desc), color = Color(0xFF78909C), fontSize = 7.5.sp)
                                }
                                Switch(
                                    checked = recordOverlay,
                                    onCheckedChange = {
                                        recordOverlay = it
                                        onSetRecordingOverlay(it)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF00E5FF),
                                        checkedTrackColor = Color(0x6600E5FF)
                                    )
                                )
                            }
                        }

                        // Option 2: Color Channel Swap (RGB/BGR)
                        Surface(
                            color = Color(0xFF141A1E),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(stringResource(R.string.settings_rec_color_swap_title), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text(stringResource(R.string.settings_rec_color_swap_desc), color = Color(0xFFFFB74D), fontSize = 7.5.sp)
                                }
                                Switch(
                                    checked = recordColorSwap,
                                    onCheckedChange = {
                                        recordColorSwap = it
                                        onSetRecordingColorSwap(it)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFFFFB74D),
                                        checkedTrackColor = Color(0x66FFB74D)
                                    )
                                )
                            }
                        }

                        // Option 3: Arcade SFX
                        Surface(
                            color = Color(0xFF141A1E),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(stringResource(R.string.settings_audio_sfx_toggle), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text(stringResource(R.string.settings_audio_sfx_desc), color = Color(0xFF81C784), fontSize = 7.5.sp)
                                }
                                Switch(
                                    checked = sfxEnabled,
                                    onCheckedChange = {
                                        sfxEnabled = it
                                        onSetSfxEnabled(it)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF81C784),
                                        checkedTrackColor = Color(0x6681C784)
                                    )
                                )
                            }
                        }

                        // Option 4: Dynamic Engine RPM Sound
                        Surface(
                            color = Color(0xFF141A1E),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(stringResource(R.string.settings_audio_engine_toggle), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text(stringResource(R.string.settings_audio_engine_desc), color = Color(0xFF81C784), fontSize = 7.5.sp)
                                }
                                Switch(
                                    checked = engineSoundEnabled,
                                    onCheckedChange = {
                                        engineSoundEnabled = it
                                        onSetEngineSoundEnabled(it)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF81C784),
                                        checkedTrackColor = Color(0x6681C784)
                                    )
                                )
                            }
                        }

                        // Section: Language Selection
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val languages = listOf("system" to "SYSTEM", "de" to "DEUTSCH", "en" to "ENGLISH")
                            languages.forEach { (code, label) ->
                                val isSelected = selectedLang == code
                                Button(
                                    onClick = {
                                        selectedLang = code
                                        onSetLanguage(code)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) Color(0xFF00B0FF) else Color(0x3337474F)
                                    ),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.weight(1f).height(28.dp)
                                ) {
                                    Text(
                                        label,
                                        fontSize = 8.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else Color(0xFFCFD8DC)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x3337474F)),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(stringResource(R.string.settings_cancel), fontSize = 10.sp, color = Color(0xFFCFD8DC))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(hostIp, videoMode)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(stringResource(R.string.settings_save), color = Color(0xFF00222A), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
