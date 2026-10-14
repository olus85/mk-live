package com.example.mklive.ui.main

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mklive.R
import com.example.mklive.ar.ArEngine
import com.example.mklive.audio.EngineSoundSynthesizer
import com.example.mklive.audio.SoundManager
import com.example.mklive.combat.WeaponType
import com.example.mklive.model.DriveCommand
import com.example.mklive.model.ImuSnapshot
import com.example.mklive.model.KartDeviceInfo
import com.example.mklive.model.OpenKartStateResponse
import com.example.mklive.model.VideoStreamMode
import com.example.mklive.network.MkLiveRepository
import com.example.mklive.protocol.TeleoperationClient
import com.example.mklive.protocol.TelemetryClient
import com.example.mklive.video.H264MediaCodecPlayer
import com.example.mklive.video.MjpegPlayer
import com.example.mklive.video.RecordingOverlayInfo
import com.example.mklive.video.VideoRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class CockpitViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "CockpitVM"
    private val prefs = application.getSharedPreferences("mklive_prefs", Context.MODE_PRIVATE)

    val repository = MkLiveRepository()
    val teleoperation = TeleoperationClient()
    val telemetry = TelemetryClient()
    val h264Player = H264MediaCodecPlayer(repository.client)
    val mjpegPlayer = MjpegPlayer(repository.client)
    val videoRecorder = VideoRecorder()

    // 🌟 Sound & Audio Engine
    val soundManager = SoundManager(application)
    val engineSynthesizer = EngineSoundSynthesizer(soundManager)

    // 🌟 Augmented Reality & Combat Engine
    val arEngine = ArEngine(soundManager, viewModelScope, application)

    // Host IP: default to PC Base Station (192.168.0.105)
    private val _hostIp = MutableStateFlow(prefs.getString("base_station_ip", "192.168.0.105") ?: "192.168.0.105")
    val hostIp: StateFlow<String> = _hostIp.asStateFlow()

    private val _videoMode = MutableStateFlow(
        VideoStreamMode.valueOf(prefs.getString("video_mode", VideoStreamMode.MJPEG_STREAM.name) ?: VideoStreamMode.MJPEG_STREAM.name)
    )
    val videoMode: StateFlow<VideoStreamMode> = _videoMode.asStateFlow()

    // Recording configuration
    private val _recordOverlayEnabled = MutableStateFlow(prefs.getBoolean("rec_overlay", true))
    val recordOverlayEnabled: StateFlow<Boolean> = _recordOverlayEnabled.asStateFlow()

    private val _recordAudioEnabled = MutableStateFlow(prefs.getBoolean("rec_audio", false))
    val recordAudioEnabled: StateFlow<Boolean> = _recordAudioEnabled.asStateFlow()

    private val _recordColorSwapEnabled = MutableStateFlow(prefs.getBoolean("rec_color_swap", true))
    val recordColorSwapEnabled: StateFlow<Boolean> = _recordColorSwapEnabled.asStateFlow()

    // AR & Audio configuration
    private val _arEnabled = MutableStateFlow(prefs.getBoolean("ar_enabled", true))
    val arEnabled: StateFlow<Boolean> = _arEnabled.asStateFlow()

    private val _sfxEnabled = MutableStateFlow(prefs.getBoolean("sfx_enabled", true))
    val sfxEnabled: StateFlow<Boolean> = _sfxEnabled.asStateFlow()

    private val _engineSoundEnabled = MutableStateFlow(prefs.getBoolean("engine_sound_enabled", true))
    val engineSoundEnabled: StateFlow<Boolean> = _engineSoundEnabled.asStateFlow()

    val isRecording: StateFlow<Boolean> = videoRecorder.isRecording
    val recordingDurationSec: StateFlow<Int> = videoRecorder.durationSec
    val lastSavedVideoPath: StateFlow<String?> = videoRecorder.lastSavedPath

    private val _currentCommand = MutableStateFlow(DriveCommand())
    val currentCommand: StateFlow<DriveCommand> = _currentCommand.asStateFlow()

    private val _inputSource = MutableStateFlow("Touch")
    val inputSource: StateFlow<String> = _inputSource.asStateFlow()

    private val _isGamepadActive = MutableStateFlow(false)
    val isGamepadActive: StateFlow<Boolean> = _isGamepadActive.asStateFlow()

    private val _serverQrBitmap = MutableStateFlow<Bitmap?>(null)
    val serverQrBitmap: StateFlow<Bitmap?> = _serverQrBitmap.asStateFlow()

    private val _serverPairingSsid = MutableStateFlow<String?>(null)
    val serverPairingSsid: StateFlow<String?> = _serverPairingSsid.asStateFlow()

    private val _serverPairingChannel = MutableStateFlow<Int?>(6)
    val serverPairingChannel: StateFlow<Int?> = _serverPairingChannel.asStateFlow()

    val openKartState: StateFlow<OpenKartStateResponse> = repository.openKartState
    val kartInfo: StateFlow<KartDeviceInfo> = repository.kartInfo
    val imuSnapshot: StateFlow<ImuSnapshot> = telemetry.imuState

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _isPairingOpen = MutableStateFlow(false)
    val isPairingOpen: StateFlow<Boolean> = _isPairingOpen.asStateFlow()

    private val _isVrMode = MutableStateFlow(prefs.getBoolean("vr_mode", false))
    val isVrMode: StateFlow<Boolean> = _isVrMode.asStateFlow()

    private val _currentLanguage = MutableStateFlow(prefs.getString("app_language", "system") ?: "system")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private var pollJob: Job? = null
    private var arUpdateJob: Job? = null
    private var wsDriveSocket: WebSocket? = null
    private var lastGamepadInputTime = 0L

    init {
        // Initialize sound settings
        soundManager.isSfxEnabled = _sfxEnabled.value
        soundManager.isEngineSoundEnabled = _engineSoundEnabled.value
        arEngine.isArEnabled = _arEnabled.value
        videoRecorder.isColorSwapEnabled = _recordColorSwapEnabled.value

        // Start dynamic engine sound loop
        engineSynthesizer.start(viewModelScope)

        // Connect frame callback from MJPEG player to video recorder
        mjpegPlayer.onFrameAvailable = { bitmap ->
            if (videoRecorder.isRecording.value) {
                val cmd = _currentCommand.value
                val info = kartInfo.value
                val imu = imuSnapshot.value
                val overlay = RecordingOverlayInfo(
                    throttle = cmd.throttle,
                    steering = cmd.steering,
                    isBraking = cmd.brake,
                    batteryPct = info.battery ?: 100,
                    character = info.character ?: "Mario",
                    speedKmh = (cmd.throttle.coerceAtLeast(0) * 15 / 127),
                    headingDeg = imu.headingDeg,
                    coins = arEngine.coinsCollected.value,
                    lap = arEngine.currentLap.value,
                    activeWeapon = arEngine.combatManager.activeWeapon.value.displayName
                )
                videoRecorder.recordFrame(bitmap, overlay) { canvas ->
                    if (arEngine.isArEnabled) {
                        arEngine.render(canvas, 1280f, 720f, imu.pitchDeg, imu.rollDeg)
                    }
                }
            }
        }

        // Continuous ~30 Hz AR simulation loop
        arUpdateJob = viewModelScope.launch(Dispatchers.Default) {
            var lastTick = System.currentTimeMillis()
            while (isActive) {
                delay(33) // ~30 Hz
                val now = System.currentTimeMillis()
                val dt = ((now - lastTick).coerceIn(1, 100) / 1000f)
                lastTick = now

                val cmd = _currentCommand.value
                val imu = imuSnapshot.value

                arEngine.update(
                    dt = dt,
                    throttle = cmd.throttle,
                    steering = cmd.steering,
                    brake = cmd.brake,
                    pitchDeg = imu.pitchDeg,
                    rollDeg = imu.rollDeg,
                    yawRateDegS = imu.yawRateDegS,
                    headingDeg = imu.headingDeg
                )
            }
        }

        // Start drive loop and connect to base station
        teleoperation.startDriveLoop(viewModelScope)
        connectAll()

        // Gamepad idle timer: revert to touch after 5000 ms of no gamepad input
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(1000)
                if (_isGamepadActive.value && System.currentTimeMillis() - lastGamepadInputTime > 5000) {
                    _isGamepadActive.value = false
                    _inputSource.value = "Touch"
                }
            }
        }
    }

    fun openSettings() {
        _isSettingsOpen.value = true
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
    }

    fun openPairing() {
        _isPairingOpen.value = true
        triggerPairing()
    }

    fun closePairing() {
        _isPairingOpen.value = false
    }

    fun setVrMode(enabled: Boolean) {
        _isVrMode.value = enabled
        prefs.edit().putBoolean("vr_mode", enabled).apply()
    }

    fun toggleVrMode() {
        setVrMode(!_isVrMode.value)
    }

    fun setLanguage(context: Context, langCode: String) {
        _currentLanguage.value = langCode
        com.example.mklive.util.LocaleHelper.setLanguage(context, langCode)
    }

    fun connectAll() {
        val ip = _hostIp.value

        // 1. Update target for direct UDP driving and start listeners
        teleoperation.updateTarget(ip, 5102)
        telemetry.startUdpListener(19002, viewModelScope)

        // 2. Connect WebSocket to mk-live server on port 9000 (/ws)
        connectDriveWebSocket(ip)

        // 3. Connect Video Stream
        startVideoStream(ip, _videoMode.value)

        // 4. Start periodic status poll
        startStatusPolling(ip)
    }

    private fun startStatusPolling(ip: String) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                repository.fetchStatus(ip)
                delay(700)
            }
        }
    }

    private fun connectDriveWebSocket(ip: String) {
        try {
            wsDriveSocket?.close(1000, "Reconnect")
        } catch (_: Exception) {}

        val wsUrl = "ws://$ip:9000/ws"
        val request = Request.Builder().url(wsUrl).build()
        wsDriveSocket = repository.client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Drive WebSocket opened: $wsUrl")
                teleoperation.activeWebSocket = webSocket
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                telemetry.feedJsonTelemetry(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.d(TAG, "Drive WebSocket failure: ${t.message}")
                teleoperation.activeWebSocket = null
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                teleoperation.activeWebSocket = null
            }
        })
    }

    fun startVideoStream(ip: String, mode: VideoStreamMode) {
        h264Player.stop()
        mjpegPlayer.stop()

        when (mode) {
            VideoStreamMode.H264_MEDIACODEC -> {
                val wsUrl = "ws://$ip:9001/video.ws"
                h264Player.start(wsUrl, viewModelScope)
                val mjpegUrl = "http://$ip:9001/video.mjpg"
                mjpegPlayer.start(mjpegUrl, viewModelScope)
            }
            VideoStreamMode.MJPEG_STREAM -> {
                val mjpegUrl = "http://$ip:9001/video.mjpg"
                mjpegPlayer.start(mjpegUrl, viewModelScope)
            }
        }
    }

    fun handleGamepadInput(throttle: Int, steering: Int, brake: Boolean, padName: String) {
        lastGamepadInputTime = System.currentTimeMillis()
        _isGamepadActive.value = true
        _inputSource.value = padName

        // Apply AR buffs (Dash Pad 100% boost, Coin speed multiplier, Banana spin cut)
        var effThrottle = throttle
        if (arEngine.isSpinningOut.value) {
            effThrottle = 0 // Spin-out engine cut
        } else if (arEngine.isBoostActive.value) {
            effThrottle = 127 // 100% Nitro Boost
        } else if (effThrottle > 0 && arEngine.coinsCollected.value > 0) {
            // +1.5% max speed per coin
            val buff = 1.0f + (arEngine.coinsCollected.value * 0.015f)
            effThrottle = (effThrottle * buff).toInt().coerceIn(0, 127)
        }

        _currentCommand.value = DriveCommand(effThrottle, steering, brake, padName)
        teleoperation.submitCommand(effThrottle, steering, brake, padName)
        engineSynthesizer.updateTelemetry(effThrottle, brake, arEngine.isBoostActive.value)
    }

    fun handleTouchInput(throttle: Int, steering: Int, brake: Boolean) {
        if (!_isGamepadActive.value) {
            _inputSource.value = "Touch"
            var effThrottle = throttle
            if (arEngine.isSpinningOut.value) {
                effThrottle = 0
            } else if (arEngine.isBoostActive.value) {
                effThrottle = 127
            } else if (effThrottle > 0 && arEngine.coinsCollected.value > 0) {
                val buff = 1.0f + (arEngine.coinsCollected.value * 0.015f)
                effThrottle = (effThrottle * buff).toInt().coerceIn(0, 127)
            }

            _currentCommand.value = DriveCommand(effThrottle, steering, brake, "touch")
            teleoperation.submitCommand(effThrottle, steering, brake, "touch")
            engineSynthesizer.updateTelemetry(effThrottle, brake, arEngine.isBoostActive.value)
        }
    }

    // 🌟 Combat & Weapon Methods
    fun startMinigun() = arEngine.combatManager.startMinigun(viewModelScope)
    fun stopMinigun() = arEngine.combatManager.stopMinigun()
    fun fireRocket() = arEngine.combatManager.fireRocket()
    fun dropBanana() = arEngine.combatManager.dropBanana()
    fun throwBobomb() = arEngine.combatManager.throwBobomb()
    fun useBoost() = arEngine.combatManager.useBoost()
    fun cycleWeapon() = arEngine.combatManager.cycleWeapon()
    fun addVirtualGate() = arEngine.addVirtualGate()

    fun useActiveItem() {
        when (arEngine.combatManager.activeWeapon.value) {
            WeaponType.ROCKET -> fireRocket()
            WeaponType.BANANA -> dropBanana()
            WeaponType.BOBOMB -> throwBobomb()
            WeaponType.MUSHROOM_BOOST -> useBoost()
            WeaponType.GREEN_SHELL -> fireRocket()
            WeaponType.MINIGUN -> {
                if (arEngine.combatManager.isMinigunFiring.value) stopMinigun() else startMinigun()
            }
            WeaponType.NONE -> cycleWeapon()
        }
    }

    fun triggerPairing(channel: Int = 6, forceNew: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val ip = _hostIp.value
            _serverQrBitmap.value = null
            _serverPairingChannel.value = channel
            val ok = repository.setOpenKartState(ip, "PAIRING", channel = channel, force = forceNew)
            Log.d(TAG, "Trigger pairing: state request sent (ok=$ok, ch=$channel, force=$forceNew)")
            for (i in 0 until 12) {
                val qrResult = repository.fetchPairingQr(ip)
                if (qrResult != null) {
                    _serverQrBitmap.value = qrResult.bitmap
                    _serverPairingSsid.value = qrResult.ssid
                    _serverPairingChannel.value = qrResult.channel ?: channel
                    Log.i(TAG, "Pairing QR acquired: ssid=${qrResult.ssid}, ch=${qrResult.channel}")
                    break
                }
                delay(350)
            }
        }
    }

    fun toggleRecording(context: Context) {
        if (videoRecorder.isRecording.value) {
            val savedPath = videoRecorder.stopRecording(context)
            if (savedPath != null) {
                val fileName = savedPath.substringAfterLast("/")
                val msg = context.getString(R.string.toast_rec_saved, fileName)
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        } else {
            val ip = _hostIp.value
            if (!mjpegPlayer.isConnected.value) {
                mjpegPlayer.start("http://$ip:9001/video.mjpg", viewModelScope)
            }
            val ok = videoRecorder.startRecording(
                context,
                recordOverlay = _recordOverlayEnabled.value,
                colorSwap = _recordColorSwapEnabled.value
            )
            if (ok) {
                val msg = context.getString(R.string.toast_rec_started)
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            } else {
                val msg = context.getString(R.string.toast_rec_error)
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun setRecordingOverlay(enabled: Boolean) {
        _recordOverlayEnabled.value = enabled
        prefs.edit().putBoolean("rec_overlay", enabled).apply()
    }

    fun setRecordingAudio(enabled: Boolean) {
        _recordAudioEnabled.value = enabled
        prefs.edit().putBoolean("rec_audio", enabled).apply()
    }

    fun setRecordingColorSwap(enabled: Boolean) {
        _recordColorSwapEnabled.value = enabled
        videoRecorder.isColorSwapEnabled = enabled
        prefs.edit().putBoolean("rec_color_swap", enabled).apply()
    }

    fun setArEnabled(enabled: Boolean) {
        _arEnabled.value = enabled
        arEngine.isArEnabled = enabled
        prefs.edit().putBoolean("ar_enabled", enabled).apply()
    }

    fun setSfxEnabled(enabled: Boolean) {
        _sfxEnabled.value = enabled
        soundManager.isSfxEnabled = enabled
        prefs.edit().putBoolean("sfx_enabled", enabled).apply()
    }

    fun setEngineSoundEnabled(enabled: Boolean) {
        _engineSoundEnabled.value = enabled
        soundManager.isEngineSoundEnabled = enabled
        prefs.edit().putBoolean("engine_sound_enabled", enabled).apply()
    }

    fun saveSettings(newIp: String, newMode: VideoStreamMode) {
        val cleanIp = newIp.trim().removePrefix("http://").removePrefix("https://").substringBefore(":")
        _hostIp.value = cleanIp
        _videoMode.value = newMode

        prefs.edit()
            .putString("base_station_ip", cleanIp)
            .putString("video_mode", newMode.name)
            .apply()

        connectAll()
    }

    fun resetHeading() {
        telemetry.resetHeading()
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
        arUpdateJob?.cancel()
        engineSynthesizer.stop()
        soundManager.release()
        teleoperation.stop()
        telemetry.stop()
        h264Player.stop()
        mjpegPlayer.stop()
        try {
            wsDriveSocket?.close(1000, "Destroyed")
        } catch (_: Exception) {}
    }
}
