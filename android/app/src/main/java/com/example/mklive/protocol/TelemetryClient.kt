package com.example.mklive.protocol

import com.example.mklive.model.ImuSnapshot
import com.example.mklive.model.KartDeviceInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/**
 * Decodes Mario Kart telemetry:
 * - Type 1: Battery (0..4) and cable status
 * - Type 2: 6DoF IMU (Gyro heading integration, G-Force calculation, pitch/roll)
 *
 * Can receive either raw UDP packets (port 19002/5116) or JSON pushed from mk-live server /ws.
 */
class TelemetryClient {

    private val _imuState = MutableStateFlow(ImuSnapshot())
    val imuState: StateFlow<ImuSnapshot> = _imuState.asStateFlow()

    private val _deviceInfo = MutableStateFlow(KartDeviceInfo())
    val deviceInfo: StateFlow<KartDeviceInfo> = _deviceInfo.asStateFlow()

    private var udpJob: Job? = null
    private var udpSocket: DatagramSocket? = null

    // Gyro yaw integration
    private var integratedHeadingDeg = 0f
    private var lastSampleTimeMs = 0L

    // Gyro scale factor from mk-live reverse-engineering: 229.8 LSB per degree/second
    private val GYRO_SCALE = 229.8f

    fun resetHeading() {
        integratedHeadingDeg = 0f
        _imuState.value = _imuState.value.copy(headingDeg = 0f)
    }

    /**
     * Feeds JSON string received from WebSocket (/ws with {"telem": {...}})
     */
    fun feedJsonTelemetry(jsonString: String) {
        try {
            val root = JSONObject(jsonString)
            if (root.has("telem")) {
                val t = root.getJSONObject("telem")
                val heading = t.optDouble("heading", 0.0).toFloat()
                val gLat = t.optDouble("g_lat", 0.0).toFloat()
                val gLong = t.optDouble("g_long", 0.0).toFloat()
                val pitch = t.optDouble("pitch", 0.0).toFloat()
                val roll = t.optDouble("roll", 0.0).toFloat()
                val yawRate = t.optDouble("yaw_rate", 0.0).toFloat()
                val calibrated = t.optBoolean("calibrated", false)

                _imuState.value = ImuSnapshot(
                    ok = true,
                    headingDeg = (heading % 360f + 360f) % 360f,
                    gLat = gLat,
                    gLong = gLong,
                    pitchDeg = pitch,
                    rollDeg = roll,
                    yawRateDegS = yawRate,
                    calibrated = calibrated
                )
            }
        } catch (_: Exception) {}
    }

    /**
     * Decodes raw binary UDP packet directly from Kart or forwarder.
     */
    fun feedRawPacket(packet: ByteArray) {
        if (packet.isEmpty()) return
        val type = packet[0].toInt() and 0xFF

        when (type) {
            1 -> {
                // Type 1: Status / Battery
                if (packet.size >= 5) {
                    val battery = packet[4].toInt() and 0xFF
                    val cableConnected = (packet.size > 5 && (packet[5].toInt() and 0x01) != 0)
                    _deviceInfo.value = _deviceInfo.value.copy(
                        battery = battery.coerceIn(0, 4),
                        cable = cableConnected
                    )
                }
            }
            2 -> {
                // Type 2: 6DoF IMU
                decodeRawImu(packet)
            }
        }
    }

    private fun decodeRawImu(packet: ByteArray) {
        if (packet.size < 85) return
        val buffer = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN)

        // Read samples starting at byte offset 73
        // Each sample is 12 bytes: 3x i16 accel (col 0,1,2), 3x i16 gyro (col 3,4,5)
        val numSamples = (packet.size - 73) / 12
        if (numSamples <= 0) return

        var sumGyroYaw = 0f
        var sumAccelLat = 0f
        var sumAccelLong = 0f

        for (i in 0 until numSamples) {
            val offset = 73 + (i * 12)
            buffer.position(offset)
            val accelX = buffer.short.toFloat() // col 0 (roll / lateral)
            val accelY = buffer.short.toFloat() // col 1 (longitudinal / throttle)
            val accelZ = buffer.short.toFloat() // col 2 (vertical)
            val gyroPitch = buffer.short.toFloat() // col 3
            val gyroRoll = buffer.short.toFloat()  // col 4
            val gyroYaw = buffer.short.toFloat()   // col 5 (vertical axis)

            sumGyroYaw += gyroYaw
            sumAccelLat += accelX
            sumAccelLong += accelY
        }

        val avgGyroYaw = sumGyroYaw / numSamples
        val yawRateDegS = avgGyroYaw / GYRO_SCALE

        // Integrate heading
        val now = System.currentTimeMillis()
        if (lastSampleTimeMs > 0) {
            val dt = (now - lastSampleTimeMs) / 1000f
            integratedHeadingDeg = (integratedHeadingDeg + (yawRateDegS * dt)) % 360f
            if (integratedHeadingDeg < 0) integratedHeadingDeg += 360f
        }
        lastSampleTimeMs = now

        // Calculate G-Forces
        val gLat = (sumAccelLat / numSamples) / 8192f
        val gLong = (sumAccelLong / numSamples) / 8192f

        _imuState.value = ImuSnapshot(
            ok = true,
            headingDeg = integratedHeadingDeg,
            gLat = gLat,
            gLong = gLong,
            yawRateDegS = yawRateDegS,
            calibrated = true
        )
    }

    fun startUdpListener(port: Int = 19002, scope: CoroutineScope) {
        udpJob?.cancel()
        udpJob = scope.launch(Dispatchers.IO) {
            try {
                udpSocket = DatagramSocket(port)
                val buffer = ByteArray(2048)
                val packet = DatagramPacket(buffer, buffer.size)

                while (isActive) {
                    udpSocket?.receive(packet)
                    val data = packet.data.copyOf(packet.length)
                    feedRawPacket(data)
                }
            } catch (_: Exception) {
            } finally {
                udpSocket?.close()
                udpSocket = null
            }
        }
    }

    fun stop() {
        udpJob?.cancel()
        udpJob = null
        udpSocket?.close()
        udpSocket = null
    }
}
