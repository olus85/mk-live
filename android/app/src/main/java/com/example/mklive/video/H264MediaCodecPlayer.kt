package com.example.mklive.video

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

/**
 * Ultra-low-latency H.264 Hardware Video Player for Mario Kart Live.
 * Connects to /video.ws over WebSocket, reassembles Annex-B access units,
 * and feeds Android's MediaCodec directly in low-latency mode to a Surface.
 */
class H264MediaCodecPlayer(
    private val client: OkHttpClient
) {
    private val TAG = "H264Player"

    private var codec: MediaCodec? = null
    private var surface: Surface? = null
    private var webSocket: WebSocket? = null
    private var isRunning = false
    private var needKeyframe = true

    private val _fps = MutableStateFlow(0)
    val fps: StateFlow<Int> = _fps.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private var frameCount = 0
    private var fpsJob: Job? = null

    @Synchronized
    fun attachSurface(surface: Surface) {
        this.surface = surface
        Log.d(TAG, "attachSurface called: valid=${surface.isValid}, isRunning=$isRunning")
        if (isRunning && surface.isValid) {
            initCodec()
        }
    }

    @Synchronized
    fun detachSurface() {
        Log.d(TAG, "detachSurface called")
        this.surface = null
        releaseCodec()
    }

    fun start(wsUrl: String, scope: CoroutineScope) {
        stop()
        isRunning = true

        fpsJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(1000)
                _fps.value = frameCount
                frameCount = 0
            }
        }

        scope.launch(Dispatchers.IO) {
            initCodec()
            connectWebSocket(wsUrl)
        }
    }

    @Synchronized
    private fun initCodec() {
        val surf = surface ?: run {
            Log.d(TAG, "initCodec skipped: surface is null")
            return
        }
        if (!surf.isValid) {
            Log.w(TAG, "initCodec skipped: surface is invalid")
            return
        }

        releaseCodec()

        try {
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 1280, 720).apply {
                setInteger(MediaFormat.KEY_LOW_LATENCY, 1)
                setInteger(MediaFormat.KEY_PRIORITY, 0) // Realtime priority
            }

            val dec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            dec.configure(format, surf, null, 0)
            dec.start()
            codec = dec
            needKeyframe = true
            Log.d(TAG, "MediaCodec initialized successfully for 1280x720 in low-latency mode")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaCodec: ${e.message}", e)
            codec = null
        }
    }

    @Synchronized
    private fun releaseCodec() {
        try {
            codec?.stop()
            codec?.release()
        } catch (_: Exception) {}
        codec = null
        needKeyframe = true
    }

    private fun connectWebSocket(url: String) {
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket video connection opened: $url")
                _isConnected.value = true
                needKeyframe = true
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                if (!isRunning) return
                val data = bytes.toByteArray()
                if (data.isEmpty()) return

                // Byte 0 is keyframe indicator (1 = keyframe, 0 = non-keyframe)
                val isKeyframe = data[0].toInt() == 1
                val au = if (data.size > 1) data.copyOfRange(1, data.size) else return

                feedAccessUnit(au, isKeyframe)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "WebSocket video error: ${t.message}")
                _isConnected.value = false
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _isConnected.value = false
            }
        })
    }

    @Synchronized
    private fun feedAccessUnit(data: ByteArray, isKey: Boolean) {
        if (needKeyframe && !isKey) {
            // Wait for keyframe (SPS/PPS/IDR) before feeding delta frames to decoder
            return
        }
        needKeyframe = false

        val dec = codec ?: run {
            // If surface is now valid but codec wasn't initialized, try once
            if (surface?.isValid == true) {
                initCodec()
            }
            codec ?: return
        }
        val surf = surface ?: return
        if (!surf.isValid) return

        try {
            val inIndex = dec.dequeueInputBuffer(2000) // 2ms timeout
            if (inIndex >= 0) {
                val inBuffer = dec.getInputBuffer(inIndex)
                if (inBuffer != null) {
                    inBuffer.clear()
                    inBuffer.put(data)
                    dec.queueInputBuffer(
                        inIndex,
                        0,
                        data.size,
                        System.nanoTime() / 1000,
                        if (isKey) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                    )
                }
            }

            // Drain output buffers immediately to surface
            val info = MediaCodec.BufferInfo()
            var outIndex = dec.dequeueOutputBuffer(info, 1000)
            while (outIndex >= 0) {
                dec.releaseOutputBuffer(outIndex, true)
                frameCount++
                outIndex = dec.dequeueOutputBuffer(info, 0)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Decode glitch or surface released: ${e.message}")
            needKeyframe = true
        }
    }

    @Synchronized
    fun stop() {
        isRunning = false
        fpsJob?.cancel()
        fpsJob = null
        try {
            webSocket?.close(1000, "Closed")
        } catch (_: Exception) {}
        webSocket = null

        releaseCodec()
        _isConnected.value = false
        _fps.value = 0
    }
}
