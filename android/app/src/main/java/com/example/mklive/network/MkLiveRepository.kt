package com.example.mklive.network

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.mklive.model.KartDeviceInfo
import com.example.mklive.model.OpenKartStateResponse
import com.example.mklive.model.VideoWorkerStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class MkLiveRepository {

    private val TAG = "MkLiveRepo"

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    private val _openKartState = MutableStateFlow(OpenKartStateResponse())
    val openKartState: StateFlow<OpenKartStateResponse> = _openKartState.asStateFlow()

    private val _kartInfo = MutableStateFlow(KartDeviceInfo())
    val kartInfo: StateFlow<KartDeviceInfo> = _kartInfo.asStateFlow()

    private val _videoWorkerStatus = MutableStateFlow(VideoWorkerStatus())
    val videoWorkerStatus: StateFlow<VideoWorkerStatus> = _videoWorkerStatus.asStateFlow()

    private val _isRooted = MutableStateFlow(checkRootStatus())
    val isRooted: StateFlow<Boolean> = _isRooted.asStateFlow()

    suspend fun fetchStatus(serverIp: String): Boolean = withContext(Dispatchers.IO) {
        val url = "http://$serverIp:9000/api/status"
        try {
            val req = Request.Builder().url(url).build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext false

            val body = resp.body?.string() ?: return@withContext false
            val json = JSONObject(body)

            // 1. OpenKart AP State
            if (json.has("openkart")) {
                val okJson = json.getJSONObject("openkart")
                val isOk = okJson.optBoolean("ok", false)
                val state = okJson.optString("state", "DOWN")
                val isPairing = okJson.optBoolean("pairing", false)
                _openKartState.value = OpenKartStateResponse(ok = isOk, state = state, pairing = isPairing)
            }

            // 2. Kart details
            if (json.has("kart") && !json.isNull("kart")) {
                val k = json.getJSONObject("kart")
                _kartInfo.value = KartDeviceInfo(
                    serial = if (k.has("serial") && !k.isNull("serial")) k.getString("serial") else null,
                    address = if (k.has("address") && !k.isNull("address")) k.getString("address") else null,
                    character = k.optString("character", "Mario"),
                    firmware = if (k.has("firmware") && !k.isNull("firmware")) k.getString("firmware") else null,
                    battery = if (k.has("battery") && !k.isNull("battery")) k.getInt("battery") else null,
                    cable = if (k.has("cable") && !k.isNull("cable")) k.getBoolean("cable") else null,
                    signal = if (k.has("signal") && !k.isNull("signal")) k.getInt("signal") else null
                )
            } else {
                _kartInfo.value = KartDeviceInfo()
            }

            // 3. Video Worker
            if (json.has("video")) {
                val v = json.getJSONObject("video")
                val latObj = v.optJSONObject("pipeline_latency")
                val latencyVal = when {
                    latObj != null -> latObj.optDouble("median_ms", 0.0).toFloat()
                    v.has("pipeline_latency") && !v.isNull("pipeline_latency") -> v.optDouble("pipeline_latency", 0.0).toFloat()
                    else -> null
                }
                _videoWorkerStatus.value = VideoWorkerStatus(
                    ok = v.optBoolean("ok", false),
                    framesIn = v.optInt("frames_in", 0),
                    lastUdpAge = if (v.has("last_udp_age") && !v.isNull("last_udp_age")) v.optDouble("last_udp_age", 0.0).toFloat() else null,
                    pipelineLatency = latencyVal
                )
            }

            true
        } catch (e: Exception) {
            Log.d(TAG, "Status fetch error from $url: ${e.message}")
            false
        }
    }

    suspend fun setOpenKartState(
        serverIp: String,
        state: String,
        channel: Int? = null,
        force: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        val url = "http://$serverIp:9000/api/openkart/state"
        try {
            val jsonBody = JSONObject().apply {
                put("state", state.uppercase())
                if (channel != null) put("channel", channel)
                if (force) put("force", true)
            }.toString()
            val mediaType = "application/json".toMediaType()
            val req = Request.Builder()
                .url(url)
                .post(jsonBody.toRequestBody(mediaType))
                .build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext false

            val body = resp.body?.string() ?: return@withContext true
            val json = JSONObject(body)
            if (json.has("pairing") && !json.isNull("pairing")) {
                val p = json.getJSONObject("pairing")
                val seed = p.optString("seed", "")
                val ssid = p.optString("ssid", "")
                val ch = if (p.has("channel") && !p.isNull("channel")) p.getInt("channel") else json.optInt("channel", 6)
                if (seed.isNotEmpty() && ssid.isNotEmpty()) {
                    val session = com.example.mklive.protocol.PairingQrGenerator.createSession(seed, ssid, ch)
                    session.qrBitmap?.let { bmp ->
                        val result = PairingQrResult(bmp, ssid, ch)
                        _serverPairingInfo.value = result
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to change OpenKart state: ${e.message}")
            false
        }
    }

    data class PairingQrResult(
        val bitmap: Bitmap,
        val ssid: String?,
        val channel: Int?
    )

    private val _serverPairingInfo = MutableStateFlow<PairingQrResult?>(null)
    val serverPairingInfo: StateFlow<PairingQrResult?> = _serverPairingInfo.asStateFlow()

    suspend fun fetchPairingQr(serverIp: String): PairingQrResult? = withContext(Dispatchers.IO) {
        // 1. Primary: Fetch verified server-generated PNG from python-qrcode (identical to Base Station)
        val url = "http://$serverIp:9000/api/pairing/qr.png"
        try {
            val req = Request.Builder().url(url).build()
            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val bytes = resp.body?.bytes()
                if (bytes != null) {
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bitmap != null) {
                        val ssid = resp.header("X-Pairing-SSID")
                        val channel = resp.header("X-Pairing-Channel")?.toIntOrNull()
                        val result = PairingQrResult(bitmap, ssid, channel)
                        _serverPairingInfo.value = result
                        Log.i(TAG, "Pairing QR loaded from server PNG: ssid=$ssid, ch=$channel")
                        return@withContext result
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Server PNG fetch error: ${e.message}")
        }

        // 2. Fallback attempt: Query pairing metadata and generate QR code
        val infoUrl = "http://$serverIp:9000/api/pairing/info"
        try {
            val req = Request.Builder().url(infoUrl).build()
            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val body = resp.body?.string()
                if (body != null) {
                    val json = JSONObject(body)
                    if (json.has("pairing") && !json.isNull("pairing")) {
                        val p = json.getJSONObject("pairing")
                        val seed = p.optString("seed", "")
                        val ssid = p.optString("ssid", "")
                        val ch = if (p.has("channel") && !p.isNull("channel")) p.getInt("channel") else json.optInt("channel", 6)
                        if (seed.isNotEmpty() && ssid.isNotEmpty()) {
                            val session = com.example.mklive.protocol.PairingQrGenerator.createSession(seed, ssid, ch)
                            session.qrBitmap?.let { bmp ->
                                val result = PairingQrResult(bmp, ssid, ch)
                                _serverPairingInfo.value = result
                                Log.i(TAG, "Pairing QR generated from server metadata fallback: ssid=$ssid, channel=$ch")
                                return@withContext result
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Failed to fetch /api/pairing/info: ${e.message}")
        }

        null
    }

    private fun checkRootStatus(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (_: Exception) {
            false
        }
    }
}
