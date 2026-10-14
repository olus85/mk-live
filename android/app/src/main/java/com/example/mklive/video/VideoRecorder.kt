package com.example.mklive.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaScannerConnection
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Metadata passed to the video recorder for rendering dynamic cockpit HUD overlay.
 */
data class RecordingOverlayInfo(
    val throttle: Int = 0,
    val steering: Int = 0,
    val isBraking: Boolean = false,
    val batteryPct: Int = 100,
    val character: String = "Mario",
    val speedKmh: Int = 0,
    val headingDeg: Float = 0f,
    val coins: Int = 0,
    val lap: Int = 1,
    val activeWeapon: String? = null
)

/**
 * High-performance hardware video recorder for Mario Kart Live.
 * Captures stream video frames, draws the stylized Cockpit HUD on top,
 * and encodes an MP4 video directly into Movies/MK_Live storage using MediaCodec and MediaMuxer.
 */
class VideoRecorder {

    private val TAG = "VideoRecorder"

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _durationSec = MutableStateFlow(0)
    val durationSec: StateFlow<Int> = _durationSec.asStateFlow()

    private val _lastSavedPath = MutableStateFlow<String?>(null)
    val lastSavedPath: StateFlow<String?> = _lastSavedPath.asStateFlow()

    private var mediaCodec: MediaCodec? = null
    private var inputSurface: Surface? = null
    private var mediaMuxer: MediaMuxer? = null
    private var videoTrackIndex: Int = -1
    private var isMuxerStarted = false

    private var drainThread: HandlerThread? = null
    private var drainHandler: Handler? = null

    private var outputFile: File? = null
    private var recordingStartTime = 0L
    private var lastFramePresentationTimeUs = 0L
    private var frameIndex = 0L

    private val isRunning = AtomicBoolean(false)
    private var includeOverlay = true
    var isColorSwapEnabled: Boolean = true

    // Color swap paint for hardware encoders expecting BGR (Tensor, Mali, Exynos)
    private val colorSwapPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
        val rbSwapMatrix = ColorMatrix(floatArrayOf(
            0f, 0f, 1f, 0f, 0f, // New R from B
            0f, 1f, 0f, 0f, 0f, // G stays G
            1f, 0f, 0f, 0f, 0f, // New B from R
            0f, 0f, 0f, 1f, 0f  // A stays A
        ))
        colorFilter = ColorMatrixColorFilter(rbSwapMatrix)
    }

    // Reusable Paints for fast HUD rendering
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        typeface = Typeface.MONOSPACE
        setShadowLayer(4f, 2f, 2f, Color.BLACK)
    }

    private val redDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        style = Paint.Style.FILL
    }

    private val hudBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(0xAA, 0x10, 0x16, 0x1A)
        style = Paint.Style.FILL
    }

    private val cyanPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF")
        textSize = 22f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        setShadowLayer(4f, 1f, 1f, Color.BLACK)
    }

    private val yellowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD600")
        textSize = 22f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        setShadowLayer(4f, 1f, 1f, Color.BLACK)
    }

    private val gaugePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val destRect = Rect(0, 0, 1280, 720)

    @Synchronized
    fun startRecording(context: Context, recordOverlay: Boolean = true, colorSwap: Boolean = true): Boolean {
        if (isRunning.get()) return false
        includeOverlay = recordOverlay
        isColorSwapEnabled = colorSwap

        try {
            val moviesDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                "MK_Live"
            )
            if (!moviesDir.exists()) moviesDir.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            outputFile = File(moviesDir, "MKLIVE_$timestamp.mp4")

            val width = 1280
            val height = 720
            val bitRate = 4_000_000
            val frameRate = 30

            val format = MediaFormat.createVideoFormat("video/avc", width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            mediaCodec = MediaCodec.createEncoderByType("video/avc").apply {
                configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                inputSurface = createInputSurface()
                start()
            }

            mediaMuxer = MediaMuxer(outputFile!!.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            isMuxerStarted = false
            videoTrackIndex = -1
            frameIndex = 0L
            lastFramePresentationTimeUs = 0L
            recordingStartTime = System.currentTimeMillis()

            isRunning.set(true)
            _isRecording.value = true
            _durationSec.value = 0

            // Start async drain thread
            drainThread = HandlerThread("VideoRecorderDrain").apply {
                start()
                drainHandler = Handler(looper)
                drainHandler?.post { drainEncoderLoop() }
            }

            Log.i(TAG, "Recording started -> ${outputFile?.absolutePath}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording: ${e.message}", e)
            stopRecording(context)
            return false
        }
    }

    fun recordFrame(
        frameBitmap: Bitmap,
        overlay: RecordingOverlayInfo,
        arDrawCallback: ((Canvas) -> Unit)? = null
    ) {
        if (!isRunning.get() || inputSurface == null) return

        try {
            val surface = inputSurface ?: return
            val canvas: Canvas = surface.lockHardwareCanvas() ?: return

            try {
                // 1. Draw raw video stream frame scaled to 1280x720 with ColorMatrix R/B swap if enabled
                val framePaint = if (isColorSwapEnabled) colorSwapPaint else null
                canvas.drawBitmap(frameBitmap, null, destRect, framePaint)

                // 2. Render AR overlay elements if callback supplied
                arDrawCallback?.invoke(canvas)

                // 3. Draw Cockpit HUD overlay if enabled
                if (includeOverlay) {
                    drawCockpitHud(canvas, overlay)
                }
            } finally {
                surface.unlockCanvasAndPost(canvas)
            }

            // Update duration ticker
            val elapsedSec = ((System.currentTimeMillis() - recordingStartTime) / 1000).toInt()
            if (_durationSec.value != elapsedSec) {
                _durationSec.value = elapsedSec
            }

            frameIndex++
        } catch (e: Exception) {
            Log.w(TAG, "Error rendering recording frame: ${e.message}")
        }
    }

    private fun drawCockpitHud(canvas: Canvas, overlay: RecordingOverlayInfo) {
        // TOP HUD BAR: MK Live logo + Red REC indicator + Timer
        canvas.drawRoundRect(RectF(24f, 20f, 320f, 76f), 12f, 12f, hudBgPaint)

        // Blinking REC dot (500ms on / off)
        val isBlinkOn = (System.currentTimeMillis() / 500) % 2 == 0L
        if (isBlinkOn) {
            canvas.drawCircle(46f, 48f, 9f, redDotPaint)
        }

        val elapsed = _durationSec.value
        val mins = elapsed / 60
        val secs = elapsed % 60
        val timeStr = String.format(Locale.getDefault(), "REC %02d:%02d", mins, secs)

        textPaint.color = Color.WHITE
        textPaint.textSize = 24f
        canvas.drawText(timeStr, 68f, 56f, textPaint)

        // TOP CENTER: Coins & Lap Info
        canvas.drawRoundRect(RectF(480f, 20f, 800f, 76f), 12f, 12f, hudBgPaint)
        canvas.drawText("🪙 x${"%02d".format(overlay.coins)}", 500f, 54f, yellowPaint)
        canvas.drawText("LAP ${overlay.lap}/3", 640f, 54f, cyanPaint)

        // TOP RIGHT: Character & Battery
        canvas.drawRoundRect(RectF(1020f, 20f, 1256f, 76f), 12f, 12f, hudBgPaint)
        cyanPaint.textSize = 20f
        canvas.drawText("${overlay.character.uppercase()} | ${overlay.batteryPct}% BAT", 1038f, 54f, cyanPaint)

        // BOTTOM LEFT: Gas / Throttle HUD
        canvas.drawRoundRect(RectF(24f, 620f, 340f, 696f), 12f, 12f, hudBgPaint)
        val isBoost = overlay.throttle >= 120
        cyanPaint.textSize = 18f
        val throttleLabel = if (isBoost) "THROTTLE: 100% [BOOST!]" else "THROTTLE: ${((overlay.throttle / 127f) * 100).toInt().coerceAtLeast(0)}%"
        canvas.drawText(throttleLabel, 38f, 648f, cyanPaint)

        // Throttle progress bar
        val barBg = RectF(38f, 660f, 326f, 680f)
        gaugePaint.color = Color.parseColor("#37474F")
        canvas.drawRoundRect(barBg, 6f, 6f, gaugePaint)

        val fillPct = (overlay.throttle.coerceAtLeast(0) / 127f).coerceIn(0f, 1f)
        if (fillPct > 0f) {
            gaugePaint.color = if (isBoost) Color.parseColor("#FF5252") else Color.parseColor("#00E5FF")
            val fillRect = RectF(38f, 660f, 38f + (288f * fillPct), 680f)
            canvas.drawRoundRect(fillRect, 6f, 6f, gaugePaint)
        }

        // BOTTOM RIGHT: Steering & Brake HUD
        canvas.drawRoundRect(RectF(960f, 620f, 1256f, 696f), 12f, 12f, hudBgPaint)
        val steerPct = ((overlay.steering / 127f) * 100).toInt()
        val steerLabel = if (steerPct < -5) "STEER: < ${-steerPct}% L" else if (steerPct > 5) "STEER: > ${steerPct}% R" else "STEER: CENTER"
        canvas.drawText(steerLabel, 976f, 648f, cyanPaint)

        if (overlay.isBraking) {
            textPaint.color = Color.parseColor("#FF1744")
            textPaint.textSize = 20f
            canvas.drawText("● BRAKE ACTIVE", 976f, 680f, textPaint)
        } else {
            textPaint.color = Color.parseColor("#B0BEC5")
            textPaint.textSize = 18f
            canvas.drawText("DIR: ${overlay.headingDeg.toInt()}°", 976f, 680f, textPaint)
        }
    }

    private fun drainEncoderLoop() {
        val bufferInfo = MediaCodec.BufferInfo()
        val timeoutUs = 10_000L

        while (isRunning.get() || mediaCodec != null) {
            val codec = mediaCodec ?: break
            try {
                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
                if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (isMuxerStarted) {
                        Log.e(TAG, "Format changed twice, ignoring")
                    } else {
                        val newFormat = codec.outputFormat
                        videoTrackIndex = mediaMuxer?.addTrack(newFormat) ?: -1
                        mediaMuxer?.start()
                        isMuxerStarted = true
                        Log.i(TAG, "Muxer started with track $videoTrackIndex")
                    }
                } else if (outputIndex >= 0) {
                    val encodedBuffer = codec.getOutputBuffer(outputIndex)
                    if (encodedBuffer != null && bufferInfo.size > 0 && isMuxerStarted) {
                        encodedBuffer.position(bufferInfo.offset)
                        encodedBuffer.limit(bufferInfo.offset + bufferInfo.size)

                        // Ensure timestamps strictly increase
                        if (bufferInfo.presentationTimeUs <= lastFramePresentationTimeUs) {
                            bufferInfo.presentationTimeUs = lastFramePresentationTimeUs + 33_333L
                        }
                        lastFramePresentationTimeUs = bufferInfo.presentationTimeUs

                        mediaMuxer?.writeSampleData(videoTrackIndex, encodedBuffer, bufferInfo)
                    }

                    codec.releaseOutputBuffer(outputIndex, false)

                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break
                    }
                }
            } catch (e: Exception) {
                if (isRunning.get()) {
                    Log.w(TAG, "Drain error: ${e.message}")
                }
                break
            }
        }
    }

    @Synchronized
    fun stopRecording(context: Context): String? {
        if (!isRunning.get()) return null
        isRunning.set(false)
        _isRecording.value = false

        try {
            // Signal End of Stream
            mediaCodec?.signalEndOfInputStream()
            Thread.sleep(150) // allow last buffer to drain
        } catch (_: Exception) {}

        try {
            drainThread?.quitSafely()
            drainThread?.join(1000)
            drainThread = null
            drainHandler = null
        } catch (_: Exception) {}

        try {
            mediaCodec?.stop()
            mediaCodec?.release()
            mediaCodec = null
        } catch (_: Exception) {}

        try {
            inputSurface?.release()
            inputSurface = null
        } catch (_: Exception) {}

        try {
            if (isMuxerStarted) {
                mediaMuxer?.stop()
            }
            mediaMuxer?.release()
            mediaMuxer = null
            isMuxerStarted = false
        } catch (_: Exception) {}

        val savedPath = outputFile?.absolutePath
        if (savedPath != null) {
            _lastSavedPath.value = savedPath
            // Notify MediaStore so it immediately appears in Google Photos / Gallery
            MediaScannerConnection.scanFile(
                context,
                arrayOf(savedPath),
                arrayOf("video/mp4"),
                null
            )
            Log.i(TAG, "Recording saved successfully: $savedPath")
        }

        return savedPath
    }
}
