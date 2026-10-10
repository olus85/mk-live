package com.example.mklive.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.mklive.model.VideoStreamMode
import com.example.mklive.ui.controller.VirtualTouchOverlay
import com.example.mklive.ui.hud.CockpitOverlay
import com.example.mklive.ui.pairing.PairingDialog
import com.example.mklive.ui.settings.SettingsDialog
import com.example.mklive.ui.video.VideoSurfaceView

@Composable
fun CockpitScreen(
    viewModel: CockpitViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val openKartState by viewModel.openKartState.collectAsState()
    val kartInfo by viewModel.kartInfo.collectAsState()
    val imuSnapshot by viewModel.imuSnapshot.collectAsState()
    val currentCommand by viewModel.currentCommand.collectAsState()
    val videoMode by viewModel.videoMode.collectAsState()
    val hostIp by viewModel.hostIp.collectAsState()
    val inputSource by viewModel.inputSource.collectAsState()
    val isGamepadActive by viewModel.isGamepadActive.collectAsState()
    val serverQrBitmap by viewModel.serverQrBitmap.collectAsState()
    val serverPairingSsid by viewModel.serverPairingSsid.collectAsState()
    val serverPairingChannel by viewModel.serverPairingChannel.collectAsState()
    val showSettingsDialog by viewModel.isSettingsOpen.collectAsState()
    val showPairingDialog by viewModel.isPairingOpen.collectAsState()

    val isRecording by viewModel.isRecording.collectAsState()
    val recordingDurationSec by viewModel.recordingDurationSec.collectAsState()
    val recordOverlayEnabled by viewModel.recordOverlayEnabled.collectAsState()
    val recordAudioEnabled by viewModel.recordAudioEnabled.collectAsState()

    val h264Fps by viewModel.h264Player.fps.collectAsState()
    val isH264Connected by viewModel.h264Player.isConnected.collectAsState()

    val mjpegBitmap by viewModel.mjpegPlayer.latestBitmap.collectAsState()
    val mjpegFps by viewModel.mjpegPlayer.fps.collectAsState()
    val isMjpegConnected by viewModel.mjpegPlayer.isConnected.collectAsState()

    val effectiveFps = if (videoMode == VideoStreamMode.H264_MEDIACODEC) h264Fps else mjpegFps
    val isStreamConnected = if (videoMode == VideoStreamMode.H264_MEDIACODEC) isH264Connected else isMjpegConnected

    // Intercept hardware and gesture back navigation so app never closes unexpectedly
    BackHandler(enabled = true) {
        if (showSettingsDialog) {
            viewModel.closeSettings()
        } else if (showPairingDialog) {
            viewModel.closePairing()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. VIDEO LAYER (SurfaceView or MJPEG Bitmap)
        VideoSurfaceView(
            videoMode = videoMode,
            h264Player = viewModel.h264Player,
            mjpegBitmap = mjpegBitmap,
            isStreamConnected = isStreamConnected,
            modifier = Modifier.fillMaxSize()
        )

        // 2. VIRTUAL TOUCH CONTROLLER (Auto-hides with delay or on external gamepad)
        VirtualTouchOverlay(
            isGamepadActive = isGamepadActive,
            onDriveCommand = { t, s, b, _ ->
                viewModel.handleTouchInput(t, s, b)
            },
            modifier = Modifier.fillMaxSize()
        )

        // 3. COCKPIT INSTRUMENTS, HUD OVERLAY & TOP BAR
        CockpitOverlay(
            openKartState = openKartState,
            kartInfo = kartInfo,
            videoFps = effectiveFps,
            driveCommand = currentCommand,
            imuSnapshot = imuSnapshot,
            inputSource = inputSource,
            isRecording = isRecording,
            recordingDurationSec = recordingDurationSec,
            onRecordToggle = { viewModel.toggleRecording(context) },
            onPairClicked = { viewModel.openPairing() },
            onSettingsClicked = { viewModel.openSettings() },
            onResetHeading = { viewModel.resetHeading() },
            modifier = Modifier.fillMaxSize()
        )

        // 4. PAIRING DIALOG
        if (showPairingDialog) {
            PairingDialog(
                serverQrBitmap = serverQrBitmap,
                serverPairingSsid = serverPairingSsid,
                serverPairingChannel = serverPairingChannel,
                onChannelSelected = { ch -> viewModel.triggerPairing(channel = ch, forceNew = true) },
                onTriggerNewCode = { ch -> viewModel.triggerPairing(channel = ch, forceNew = true) },
                onDismiss = { viewModel.closePairing() }
            )
        }

        // 5. SETTINGS DIALOG
        if (showSettingsDialog) {
            SettingsDialog(
                currentHostIp = hostIp,
                currentVideoMode = videoMode,
                isRecordingOverlay = recordOverlayEnabled,
                isRecordingAudio = recordAudioEnabled,
                onSetRecordingOverlay = { viewModel.setRecordingOverlay(it) },
                onSetRecordingAudio = { viewModel.setRecordingAudio(it) },
                onSave = { newIp, newMode ->
                    viewModel.saveSettings(newIp, newMode)
                },
                onDismiss = { viewModel.closeSettings() }
            )
        }
    }
}
