package com.example.mklive.ui.main

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

    private var pollJob: Job? = null
    private var wsDriveSocket: WebSocket? = null
    private var lastGamepadInputTime = 0L

    init {
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
                    headingDeg = imu.headingDeg
                )
                videoRecorder.recordFrame(bitmap, overlay)
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
                // Also run mjpeg in background if recording is enabled
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
        _currentCommand.value = DriveCommand(throttle, steering, brake, padName)
        teleoperation.submitCommand(throttle, steering, brake, padName)
    }

    fun handleTouchInput(throttle: Int, steering: Int, brake: Boolean) {
        if (!_isGamepadActive.value) {
            _inputSource.value = "Touch"
            _currentCommand.value = DriveCommand(throttle, steering, brake, "touch")
            teleoperation.submitCommand(throttle, steering, brake, "touch")
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
                Toast.makeText(context, "Recording saved: $fileName", Toast.LENGTH_LONG).show()
            }
        } else {
            // Ensure mjpeg stream is running to supply frames for recorder
            val ip = _hostIp.value
            if (!mjpegPlayer.isConnected.value) {
                mjpegPlayer.start("http://$ip:9001/video.mjpg", viewModelScope)
            }
            val ok = videoRecorder.startRecording(context, recordOverlay = _recordOverlayEnabled.value)
            if (ok) {
                Toast.makeText(context, "Recording started (HUD Overlay active)", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Recording error!", Toast.LENGTH_SHORT).show()
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
        teleoperation.stop()
        telemetry.stop()
        h264Player.stop()
        mjpegPlayer.stop()
        try {
            wsDriveSocket?.close(1000, "Destroyed")
        } catch (_: Exception) {}
    }
}
