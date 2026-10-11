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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.mklive.model.VideoStreamMode

@Composable
fun SettingsDialog(
    currentHostIp: String,
    currentVideoMode: VideoStreamMode,
    isRecordingOverlay: Boolean,
    isRecordingAudio: Boolean,
    onSetRecordingOverlay: (Boolean) -> Unit,
    onSetRecordingAudio: (Boolean) -> Unit,
    onSave: (hostIp: String, videoMode: VideoStreamMode) -> Unit,
    onDismiss: () -> Unit
) {
    var hostIp by remember { mutableStateOf(currentHostIp) }
    var videoMode by remember { mutableStateOf(currentVideoMode) }
    var recordOverlay by remember { mutableStateOf(isRecordingOverlay) }
    var recordAudio by remember { mutableStateOf(isRecordingAudio) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E262C),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF37474F)),
            modifier = Modifier
                .width(660.dp)
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
                        text = "COCKPIT & BASE STATION SETTINGS",
                        color = Color(0xFF00E5FF),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Surface(
                        color = Color(0x3300E5FF),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "PC BASE STATION",
                            color = Color(0xFF00E5FF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2-Column Compact Landscape Layout (Fits perfectly on screen)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // LEFT COLUMN: PC Base Station IP & Video Mode
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Section: Base Station IP
                        Column {
                            Text(
                                text = "BASE STATION IP ADDRESS:",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = hostIp,
                                onValueChange = { hostIp = it },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF00E5FF)
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00E5FF),
                                    unfocusedBorderColor = Color(0xFF37474F),
                                    focusedContainerColor = Color(0xFF141A1E),
                                    unfocusedContainerColor = Color(0xFF141A1E)
                                ),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            )
                        }

                        // Section: Video Mode
                        Column {
                            Text(
                                text = "VIDEO STREAMING MODE:",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
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
                                    modifier = Modifier.weight(1f).height(34.dp)
                                ) {
                                    Text(
                                        "H.264 (Low Latency)",
                                        fontSize = 10.sp,
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
                                    modifier = Modifier.weight(1f).height(34.dp)
                                ) {
                                    Text(
                                        "MJPEG (Universal)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (videoMode == VideoStreamMode.MJPEG_STREAM) Color.White else Color(0xFFCFD8DC)
                                    )
                                }
                            }
                        }
                    }

                    // RIGHT COLUMN: Video Recording Options
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "RECORDING SETTINGS:",
                            color = Color.White,
                            fontSize = 11.sp,
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
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Cockpit HUD in Video", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("Overlay speed, throttle & telemetry", color = Color(0xFF78909C), fontSize = 9.sp)
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

                        // Option 2: Audio Recording
                        Surface(
                            color = Color(0xFF141A1E),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Audio / Microphone", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("Record phone microphone audio", color = Color(0xFF78909C), fontSize = 9.sp)
                                }
                                Switch(
                                    checked = recordAudio,
                                    onCheckedChange = {
                                        recordAudio = it
                                        onSetRecordingAudio(it)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF00E5FF),
                                        checkedTrackColor = Color(0x6600E5FF)
                                    )
                                )
                            }
                        }

                        // Storage location info
                        Text(
                            text = "Storage: Movies/MK_Live (visible in gallery)",
                            color = Color(0xFF90A4AE),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x3337474F)),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Cancel", fontSize = 11.sp, color = Color(0xFFCFD8DC))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(hostIp, videoMode)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Save & Connect", color = Color(0xFF00222A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
