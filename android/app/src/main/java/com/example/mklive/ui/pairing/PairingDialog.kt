package com.example.mklive.ui.pairing

import android.app.Activity
import android.graphics.Bitmap
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Nintendo Switch LP2P Pairing QR Dialog:
 * - Dynamic Channel (1, 6, 11) and Fresh Code generation directly synchronized with hostapd.
 * - Automatic 100% display brightness boost.
 * - Fullscreen mode (edge-to-edge pure white, max contrast).
 * - Exact 62-byte binary payload for the Mario Kart camera.
 */
@Composable
fun PairingDialog(
    serverQrBitmap: Bitmap?,
    serverPairingSsid: String? = null,
    serverPairingChannel: Int? = null,
    onChannelSelected: (Int) -> Unit,
    onTriggerNewCode: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // Temporarily boost screen brightness to 100% while QR dialog is open
    DisposableEffect(Unit) {
        val originalBrightness = activity?.window?.attributes?.screenBrightness
        activity?.window?.let { window ->
            val lp = window.attributes
            lp.screenBrightness = 1.0f
            window.attributes = lp
        }
        onDispose {
            activity?.window?.let { window ->
                val lp = window.attributes
                lp.screenBrightness = originalBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = lp
            }
        }
    }

    var selectedChannel by remember(serverPairingChannel) {
        mutableIntStateOf(serverPairingChannel ?: 6)
    }
    var isFullscreenQr by remember { mutableStateOf(false) }

    val activeSsid = serverPairingSsid ?: "Loading..."
    val displayChannel = serverPairingChannel ?: selectedChannel

    if (isFullscreenQr) {
        // FULLSCREEN QR MODE: Pure white background, max size, high contrast
        Dialog(
            onDismissRequest = { isFullscreenQr = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .clickable { isFullscreenQr = false }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "HOLD MARIO KART 15–20 CM IN FRONT OF QR CODE",
                        color = Color.Black,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (serverQrBitmap != null) {
                        Image(
                            bitmap = serverQrBitmap.asImageBitmap(),
                            contentDescription = "Nintendo LP2P Pairing QR Fullscreen",
                            modifier = Modifier.size(290.dp)
                        )
                    } else {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(48.dp))
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "SSID: $activeSsid · CH: $displayChannel · (Tap to close)",
                        color = Color(0xFF333333),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        return
    }

    // REGULAR DIALOG MODE (Landscape 2-column)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E262C),
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(20.dp)
                    .width(580.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT: QR Code Card
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                        .clickable { if (serverQrBitmap != null) isFullscreenQr = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (serverQrBitmap != null) {
                        Image(
                            bitmap = serverQrBitmap.asImageBitmap(),
                            contentDescription = "Nintendo LP2P Pairing QR",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF00E5FF),
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Starting AP...",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                // RIGHT: Information & Controls
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "MARIO KART PAIRING",
                        color = Color(0xFF00E5FF),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    // Connection Status Badge
                    Surface(
                        color = if (serverQrBitmap != null) Color(0x3300E676) else Color(0x33FFB300),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (serverQrBitmap != null) "● AP READY: $activeSsid" else "○ INITIALIZING AP (CH $selectedChannel)...",
                            color = if (serverQrBitmap != null) Color(0xFF00E676) else Color(0xFFFFB300),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = "1. Turn on Mario Kart (unplug USB cable)\n2. Hold camera 15–20 cm in front of QR code\n3. Blue LED stays solid on success",
                        color = Color(0xCCFFFFFF),
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )

                    Text(
                        text = "AP SSID: $activeSsid\nChannel: $displayChannel",
                        color = Color(0xFF00E5FF),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    // Channel selector
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(1, 6, 11).forEach { ch ->
                            OutlinedButton(
                                onClick = {
                                    if (selectedChannel != ch) {
                                        selectedChannel = ch
                                        onChannelSelected(ch)
                                    }
                                },
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.height(28.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selectedChannel == ch) Color(0x3300E5FF) else Color.Transparent
                                )
                            ) {
                                Text(
                                    "Ch $ch",
                                    fontSize = 10.sp,
                                    color = if (selectedChannel == ch) Color(0xFF00E5FF) else Color.White
                                )
                            }
                        }

                        Button(
                            onClick = { if (serverQrBitmap != null) isFullscreenQr = true },
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.height(28.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x4400E5FF))
                        ) {
                            Text("FULLSCREEN", fontSize = 10.sp, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Close", color = Color.White, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onTriggerNewCode(selectedChannel)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF)),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("New Code", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
