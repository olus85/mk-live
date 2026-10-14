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
import com.example.mklive.game.CharacterProfile
import com.example.mklive.game.GameMode
import com.example.mklive.game.GameState
import com.example.mklive.game.HudTheme
import com.example.mklive.game.RaceResult
import com.example.mklive.game.RaceState
import com.example.mklive.game.SpeedClass
import com.example.mklive.game.TrackManager
import com.example.mklive.game.TrackPreset
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

    // 🌟 AAA Game State & Customization
    private val _gameState = MutableStateFlow(GameState.MAIN_MENU)
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _selectedMode = MutableStateFlow(GameMode.GRAND_PRIX)
    val selectedMode: StateFlow<GameMode> = _selectedMode.asStateFlow()

    private val _selectedCharacter = MutableStateFlow(
        try {
            CharacterProfile.valueOf(prefs.getString("character_profile", CharacterProfile.MARIO.name) ?: CharacterProfile.MARIO.name)
        } catch (_: Exception) { CharacterProfile.MARIO }
    )
    val selectedCharacter: StateFlow<CharacterProfile> = _selectedCharacter.asStateFlow()

    private val _selectedSpeedClass = MutableStateFlow(
        try {
            SpeedClass.valueOf(prefs.getString("speed_class", SpeedClass.CC_100.name) ?: SpeedClass.CC_100.name)
        } catch (_: Exception) { SpeedClass.CC_100 }
    )
    val selectedSpeedClass: StateFlow<SpeedClass> = _selectedSpeedClass.asStateFlow()

    private val _selectedHudTheme = MutableStateFlow(
        try {
            HudTheme.valueOf(prefs.getString("hud_theme", HudTheme.CYBERPUNK.name) ?: HudTheme.CYBERPUNK.name)
        } catch (_: Exception) { HudTheme.CYBERPUNK }
    )
    val selectedHudTheme: StateFlow<HudTheme> = _selectedHudTheme.asStateFlow()

    private val _selectedTrack = MutableStateFlow(TrackManager.getBuiltinPresets()[0])
    val selectedTrack: StateFlow<TrackPreset> = _selectedTrack.asStateFlow()

    private val _customTracks = MutableStateFlow<List<TrackPreset>>(emptyList())
    val customTracks: StateFlow<List<TrackPreset>> = _customTracks.asStateFlow()

    private val _raceState = MutableStateFlow(RaceState.READY)
    val raceState: StateFlow<RaceState> = _raceState.asStateFlow()

    private val _countdownNumber = MutableStateFlow(-1) // 3, 2, 1, 0 (GO), -1 (off)
    val countdownNumber: StateFlow<Int> = _countdownNumber.asStateFlow()

    private val _rocketStartActive = MutableStateFlow(false)
    val rocketStartActive: StateFlow<Boolean> = _rocketStartActive.asStateFlow()

    private val _rocketStartStall = MutableStateFlow(false)
    val rocketStartStall: StateFlow<Boolean> = _rocketStartStall.asStateFlow()

    private val _balloonsRemaining = MutableStateFlow(3)
    val balloonsRemaining: StateFlow<Int> = _balloonsRemaining.asStateFlow()

    private val _lastRaceResult = MutableStateFlow<RaceResult?>(null)
    val lastRaceResult: StateFlow<RaceResult?> = _lastRaceResult.asStateFlow()

    private val _lapTimes = MutableStateFlow<List<Float>>(emptyList())
    val lapTimes: StateFlow<List<Float>> = _lapTimes.asStateFlow()

    private val _nextGateAngleDeg = MutableStateFlow<Float?>(null)
    val nextGateAngleDeg: StateFlow<Float?> = _nextGateAngleDeg.asStateFlow()

    private var countdownJob: Job? = null
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

        // Load saved custom tracks
        reloadCustomTracks()

        // 🌟 Wire AR Engine Game & Lap Events
        arEngine.onLapCompleted = { lap, lapTime ->
            _lapTimes.value = _lapTimes.value + lapTime
        }

        arEngine.onRaceFinished = { totalTime, laps ->
            val result = RaceResult(
                gameMode = _selectedMode.value,
                speedClass = _selectedSpeedClass.value,
                totalTimeSec = totalTime,
                lapTimesSec = laps,
                bestLapTimeSec = laps.minOrNull() ?: totalTime,
                coinsEarned = arEngine.coinsCollected.value,
                isNewRecord = true,
                balloonsRemaining = _balloonsRemaining.value,
                rank = 1
            )
            _lastRaceResult.value = result
            _raceState.value = RaceState.FINISHED
            _gameState.value = GameState.VICTORY
        }

        arEngine.onHazardHit = {
            if (_selectedMode.value == GameMode.BALLOON_BATTLE) {
                val rem = (_balloonsRemaining.value - 1).coerceAtLeast(0)
                _balloonsRemaining.value = rem
                soundManager.playBalloonPop()
                if (rem == 0) {
                    val result = RaceResult(
                        gameMode = GameMode.BALLOON_BATTLE,
                        speedClass = _selectedSpeedClass.value,
                        totalTimeSec = arEngine.totalRaceTimer,
                        lapTimesSec = emptyList(),
                        bestLapTimeSec = arEngine.totalRaceTimer,
                        coinsEarned = arEngine.coinsCollected.value,
                        isNewRecord = false,
                        balloonsRemaining = 0,
                        rank = 3
                    )
                    _lastRaceResult.value = result
                    _raceState.value = RaceState.FINISHED
                    _gameState.value = GameState.VICTORY
                }
            }
        }

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

                _nextGateAngleDeg.value = arEngine.getNextGateAngleDeg()
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

        // In countdown, allow checking throttle for rocket start, but don't drive kart yet
        val isLocked = _raceState.value == RaceState.COUNTDOWN
        val speedFactor = _selectedSpeedClass.value.maxSpeedMps / SpeedClass.CC_100.maxSpeedMps

        var effThrottle = (throttle * speedFactor).toInt().coerceIn(-127, 127)
        if (isLocked) {
            effThrottle = 0
        } else if (_rocketStartStall.value || arEngine.isSpinningOut.value) {
            effThrottle = 0 // Spin-out or stall cut
        } else if (arEngine.isBoostActive.value) {
            effThrottle = 127 // 100% Nitro Boost
        } else if (effThrottle > 0 && arEngine.coinsCollected.value > 0) {
            val buff = 1.0f + (arEngine.coinsCollected.value * 0.015f)
            effThrottle = (effThrottle * buff).toInt().coerceIn(0, 127)
        }

        _currentCommand.value = DriveCommand(if (isLocked) throttle else effThrottle, steering, brake, padName)
        teleoperation.submitCommand(effThrottle, steering, brake, padName)
        engineSynthesizer.updateTelemetry(effThrottle, brake, arEngine.isBoostActive.value)
    }

    fun handleTouchInput(throttle: Int, steering: Int, brake: Boolean) {
        if (!_isGamepadActive.value) {
            _inputSource.value = "Touch"
            val isLocked = _raceState.value == RaceState.COUNTDOWN
            val speedFactor = _selectedSpeedClass.value.maxSpeedMps / SpeedClass.CC_100.maxSpeedMps

            var effThrottle = (throttle * speedFactor).toInt().coerceIn(-127, 127)
            if (isLocked) {
                effThrottle = 0
            } else if (_rocketStartStall.value || arEngine.isSpinningOut.value) {
                effThrottle = 0
            } else if (arEngine.isBoostActive.value) {
                effThrottle = 127
            } else if (effThrottle > 0 && arEngine.coinsCollected.value > 0) {
                val buff = 1.0f + (arEngine.coinsCollected.value * 0.015f)
                effThrottle = (effThrottle * buff).toInt().coerceIn(0, 127)
            }

            _currentCommand.value = DriveCommand(if (isLocked) throttle else effThrottle, steering, brake, "touch")
            teleoperation.submitCommand(effThrottle, steering, brake, "touch")
            engineSynthesizer.updateTelemetry(effThrottle, brake, arEngine.isBoostActive.value)
        }
    }

    // 🌟 Navigation & Game Lifecycle Methods
    fun navigateTo(state: GameState) {
        soundManager.playMenuSelect()
        _gameState.value = state
    }

    fun selectMode(mode: GameMode) {
        soundManager.playMenuSelect()
        _selectedMode.value = mode
    }

    fun selectCharacter(char: CharacterProfile) {
        soundManager.playMenuSelect()
        _selectedCharacter.value = char
        prefs.edit().putString("character_profile", char.name).apply()
    }

    fun selectSpeedClass(sc: SpeedClass) {
        soundManager.playMenuSelect()
        _selectedSpeedClass.value = sc
        prefs.edit().putString("speed_class", sc.name).apply()
    }

    fun selectHudTheme(theme: HudTheme) {
        soundManager.playMenuSelect()
        _selectedHudTheme.value = theme
        prefs.edit().putString("hud_theme", theme.name).apply()
    }

    fun selectTrack(preset: TrackPreset) {
        soundManager.playMenuSelect()
        _selectedTrack.value = preset
    }

    fun reloadCustomTracks() {
        _customTracks.value = TrackManager.loadCustomTracks(getApplication())
    }

    fun saveCurrentTrack(name: String): Boolean {
        val elements = arEngine.getPlacedElements()
        val ok = TrackManager.saveCustomTrack(getApplication(), name, elements)
        if (ok) {
            reloadCustomTracks()
            soundManager.playMenuConfirm()
        }
        return ok
    }

    fun startRace() {
        soundManager.playMenuConfirm()
        _balloonsRemaining.value = 3
        _lapTimes.value = emptyList()
        _rocketStartActive.value = false
        _rocketStartStall.value = false
        arEngine.loadTrack(_selectedTrack.value)
        _gameState.value = GameState.IN_GAME

        // Start Lakitu Countdown (3, 2, 1, GO)
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch(Dispatchers.Default) {
            _raceState.value = RaceState.COUNTDOWN

            // 3...
            _countdownNumber.value = 3
            soundManager.playCountdownBeep()
            delay(1000)

            // 2... (Rocket start trigger window)
            var heldGasOnTwo = false
            _countdownNumber.value = 2
            soundManager.playCountdownBeep()
            val startTwo = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTwo < 1000) {
                if (_currentCommand.value.throttle >= 60) {
                    heldGasOnTwo = true
                }
                delay(40)
            }

            // 1...
            _countdownNumber.value = 1
            soundManager.playCountdownBeep()
            delay(1000)

            // GO!
            _countdownNumber.value = 0
            if (heldGasOnTwo && _currentCommand.value.throttle >= 60) {
                // 🚀 ROCKET START SUCCESS!
                _rocketStartActive.value = true
                soundManager.playRocketStart()
                arEngine.triggerBoost(2.5f)
            } else {
                soundManager.playCountdownGo()
            }

            _raceState.value = RaceState.RACING
            delay(800)
            _countdownNumber.value = -1
            _rocketStartActive.value = false
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
