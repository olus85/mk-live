package com.example.mklive.model

import kotlinx.serialization.Serializable

@Serializable
data class DriveCommand(
    val throttle: Int = 0,    // -128 (full reverse) to +127 (boost)
    val steering: Int = 0,    // -128 (full left) to +127 (full right)
    val brake: Boolean = false,
    val source: String = "touch"
) {
    fun isNeutral(): Boolean = throttle == 0 && steering == 0 && !brake
}

@Serializable
data class OpenKartStateResponse(
    val ok: Boolean = false,
    val state: String = "DOWN",
    val pairing: Boolean = false,
    val error: String? = null
)

@Serializable
data class KartDeviceInfo(
    val serial: String? = null,
    val address: String? = null,
    val character: String? = "Mario",
    val firmware: String? = null,
    val battery: Int? = null,          // 0 to 4 bars
    val cable: Boolean? = null,        // true = USB-C connected
    val signal: Int? = null            // dBm RSSI, e.g. -60
)

@Serializable
data class VideoWorkerStatus(
    val ok: Boolean = false,
    val framesIn: Int = 0,
    val lastUdpAge: Float? = null,
    val pipelineLatency: Float? = null
)

@Serializable
data class ImuSnapshot(
    val ok: Boolean = false,
    val headingDeg: Float = 0f,
    val gLat: Float = 0f,
    val gLong: Float = 0f,
    val pitchDeg: Float = 0f,
    val rollDeg: Float = 0f,
    val yawRateDegS: Float = 0f,
    val calibrated: Boolean = false
)

enum class VideoStreamMode {
    H264_MEDIACODEC,
    MJPEG_STREAM
}

enum class ConnectionType {
    LOCAL_MK_LIVE_BRIDGE, // Connects to mk-live daemon on PC/Pi/Deck
    DIRECT_KART_UDP       // Direct UDP 5102 to Kart IP (169.254.98.97 or custom)
}
