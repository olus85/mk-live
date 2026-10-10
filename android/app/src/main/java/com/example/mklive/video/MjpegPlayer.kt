package com.example.mklive.video

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
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
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream

/**
 * Universal MJPEG stream decoder for Mario Kart Live.
 * Connects to /video.mjpg and decodes JPEG frames into Bitmaps.
 */
class MjpegPlayer(
    private val client: OkHttpClient
) {
    private val TAG = "MjpegPlayer"

    private val _latestBitmap = MutableStateFlow<Bitmap?>(null)
    val latestBitmap: StateFlow<Bitmap?> = _latestBitmap.asStateFlow()

    private val _fps = MutableStateFlow(0)
    val fps: StateFlow<Int> = _fps.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    var onFrameAvailable: ((Bitmap) -> Unit)? = null

    private var streamJob: Job? = null
    private var fpsJob: Job? = null
    private var frameCount = 0

    fun start(url: String, scope: CoroutineScope) {
        stop()

        fpsJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(1000)
                _fps.value = frameCount
                frameCount = 0
            }
        }

        streamJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val request = Request.Builder().url(url).build()
                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) {
                        _isConnected.value = false
                        response.close()
                        delay(2000)
                        continue
                    }

                    _isConnected.value = true
                    val stream = BufferedInputStream(response.body!!.byteStream())
                    val jpegBuffer = ByteArrayOutputStream(128 * 1024)
                    val chunk = ByteArray(16384)

                    var prev = 0
                    var inFrame = false

                    while (isActive) {
                        val count = stream.read(chunk)
                        if (count == -1) break

                        for (i in 0 until count) {
                            val b = chunk[i].toInt() and 0xFF
                            if (!inFrame) {
                                if (prev == 0xFF && b == 0xD8) {
                                    // Start of Image (SOI)
                                    inFrame = true
                                    jpegBuffer.reset()
                                    jpegBuffer.write(0xFF)
                                    jpegBuffer.write(0xD8)
                                }
                            } else {
                                jpegBuffer.write(b)
                                if (prev == 0xFF && b == 0xD9) {
                                    // End of Image (EOI)
                                    inFrame = false
                                    val bytes = jpegBuffer.toByteArray()
                                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                    if (bitmap != null) {
                                        _latestBitmap.value = bitmap
                                        frameCount++
                                        onFrameAvailable?.invoke(bitmap)
                                    }
                                }
                            }
                            prev = b
                        }
                    }
                    response.close()
                } catch (e: Exception) {
                    _isConnected.value = false
                    Log.w(TAG, "MJPEG stream disconnected: ${e.message}")
                    delay(1500)
                }
            }
        }
    }

    fun stop() {
        streamJob?.cancel()
        streamJob = null
        fpsJob?.cancel()
        fpsJob = null
        _isConnected.value = false
        _fps.value = 0
        _latestBitmap.value = null
    }
}
