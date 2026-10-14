package com.example.mklive.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.mklive.ar.ArOverlayView
import com.example.mklive.model.VideoStreamMode
import com.example.mklive.ui.controller.VirtualTouchOverlay
import com.example.mklive.ui.hud.CockpitOverlay
import com.example.mklive.ui.pairing.PairingDialog
import com.example.mklive.ui.settings.SettingsDialog
import com.example.mklive.ui.video.VideoSurfaceView
import com.example.mklive.ui.vr.VrCockpitView

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

    val isVrMode by viewModel.isVrMode.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()

    val isRecording by viewModel.isRecording.collectAsState()
    val recordingDurationSec by viewModel.recordingDurationSec.collectAsState()
    val recordOverlayEnabled by viewModel.recordOverlayEnabled.collectAsState()
    val recordAudioEnabled by viewModel.recordAudioEnabled.collectAsState()
    val recordColorSwapEnabled by viewModel.recordColorSwapEnabled.collectAsState()

    // AR & Combat states
    val arEnabled by viewModel.arEnabled.collectAsState()
    val sfxEnabled by viewModel.sfxEnabled.collectAsState()
    val engineSoundEnabled by viewModel.engineSoundEnabled.collectAsState()

    val coins by viewModel.arEngine.coinsCollected.collectAsState()
    val currentLap by viewModel.arEngine.currentLap.collectAsState()
    val activeWeapon by viewModel.arEngine.combatManager.activeWeapon.collectAsState()
    val ammo by viewModel.arEngine.combatManager.ammo.collectAsState()
    val isRouletteActive by viewModel.arEngine.combatManager.isRouletteActive.collectAsState()
    val rouletteItem by viewModel.arEngine.combatManager.rouletteItem.collectAsState()
    val lockTarget by viewModel.arEngine.combatManager.lockOnTarget.collectAsState()
    val isMinigunFiring by viewModel.arEngine.combatManager.isMinigunFiring.collectAsState()

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
        } else if (isVrMode) {
            viewModel.setVrMode(false)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (isVrMode) {
            // META QUEST 3 & VR HEADSET SBS 3D COCKPIT WITH STEREOSCOPIC AR
            VrCockpitView(
                videoMode = videoMode,
                h264Player = viewModel.h264Player,
                mjpegBitmap = mjpegBitmap,
                isStreamConnected = isStreamConnected,
                openKartState = openKartState,
                kartInfo = kartInfo,
                driveCommand = currentCommand,
                imuSnapshot = imuSnapshot,
                videoFps = effectiveFps,
                isRecording = isRecording,
                recordingDurationSec = recordingDurationSec,
                inputSource = inputSource,
                arEngine = viewModel.arEngine,
                coins = coins,
                currentLap = currentLap,
                activeWeapon = activeWeapon,
                lockTarget = lockTarget,
                isMinigunFiring = isMinigunFiring,
                onToggleVr = { viewModel.toggleVrMode() },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // STANDARD 2D COCKPIT VIEW
            // 1. VIDEO LAYER (SurfaceView or MJPEG Bitmap)
            VideoSurfaceView(
                videoMode = videoMode,
                h264Player = viewModel.h264Player,
                mjpegBitmap = mjpegBitmap,
                isStreamConnected = isStreamConnected,
                modifier = Modifier.fillMaxSize()
            )

            // 2. 🌟 AUGMENTED REALITY LAYER (Coins, ?-Boxes, Dash-Pads, Projectiles & Crosshairs)
            if (arEnabled) {
                ArOverlayView(
                    arEngine = viewModel.arEngine,
                    pitchDeg = imuSnapshot.pitchDeg,
                    rollDeg = imuSnapshot.rollDeg,
                    isLeftEye = null,
                    activeWeapon = activeWeapon,
                    lockTarget = lockTarget,
                    isMinigunFiring = isMinigunFiring,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 3. VIRTUAL TOUCH CONTROLLER (Auto-hides on external gamepad)
            VirtualTouchOverlay(
                isGamepadActive = isGamepadActive,
                onDriveCommand = { t, s, b, _ ->
                    viewModel.handleTouchInput(t, s, b)
                },
                modifier = Modifier.fillMaxSize()
            )

            // 4. COCKPIT INSTRUMENTS, HUD OVERLAY, AR STATS & ACTIONS
            CockpitOverlay(
                openKartState = openKartState,
                kartInfo = kartInfo,
                videoFps = effectiveFps,
                driveCommand = currentCommand,
                imuSnapshot = imuSnapshot,
                inputSource = inputSource,
                coins = coins,
                currentLap = currentLap,
                activeWeapon = activeWeapon,
                ammo = ammo,
                isRouletteActive = isRouletteActive,
                rouletteItem = rouletteItem,
                isRecording = isRecording,
                recordingDurationSec = recordingDurationSec,
                onFireWeapon = { viewModel.useActiveItem() },
                onCycleWeapon = { viewModel.cycleWeapon() },
                onDropGate = { viewModel.addVirtualGate() },
                onResetTrack = { viewModel.arEngine.resetTrack() },
                onRecordToggle = { viewModel.toggleRecording(context) },
                onPairClicked = { viewModel.openPairing() },
                onSettingsClicked = { viewModel.openSettings() },
                onToggleVrMode = { viewModel.toggleVrMode() },
                onResetHeading = { viewModel.resetHeading() },
                modifier = Modifier.fillMaxSize()
            )
        }

        // 5. PAIRING DIALOG
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

        // 6. SETTINGS DIALOG
        if (showSettingsDialog) {
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
                onSetRecordingOverlay = { viewModel.setRecordingOverlay(it) },
                onSetRecordingAudio = { viewModel.setRecordingAudio(it) },
                onSetRecordingColorSwap = { viewModel.setRecordingColorSwap(it) },
                onSetArEnabled = { viewModel.setArEnabled(it) },
                onSetSfxEnabled = { viewModel.setSfxEnabled(it) },
                onSetEngineSoundEnabled = { viewModel.setEngineSoundEnabled(it) },
                onSetVrMode = { viewModel.setVrMode(it) },
                onSetLanguage = { viewModel.setLanguage(context, it) },
                onSave = { newIp, newMode ->
                    viewModel.saveSettings(newIp, newMode)
                },
                onDismiss = { viewModel.closeSettings() }
            )
        }
    }
}
