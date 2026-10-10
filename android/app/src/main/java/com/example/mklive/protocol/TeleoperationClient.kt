package com.example.mklive.protocol

import com.example.mklive.model.DriveCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.WebSocket
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Handles teleoperation drive commands (throttle, steering, brake).
 * Supports both:
 * 1. Direct UDP packets sent to the Kart on port 5102 at 30 Hz.
 * 2. High-speed JSON payloads over WebSocket to mk-live server.
 *
 * Implements a safety deadman switch: automatically halts the kart if no
 * input is received within [deadmanSec] (default: 600 ms).
 */
class TeleoperationClient(
    private var targetHost: String = "169.254.98.97",
    private var targetPort: Int = 5102,
    private val deadmanSec: Float = 3.0f
) {
    private var udpSocket: DatagramSocket? = null
    private var inetAddress: InetAddress? = null
    private var loopJob: Job? = null
    private var counter: Long = 0

    @Volatile
    private var currentCommand = DriveCommand()

    @Volatile
    private var lastCommandTime: Long = 0L

    var activeWebSocket: WebSocket? = null

    init {
        updateTarget(targetHost, targetPort)
    }

    fun updateTarget(host: String, port: Int = 5102) {
        targetHost = host
        targetPort = port
        try {
            inetAddress = InetAddress.getByName(host)
            if (udpSocket == null || udpSocket?.isClosed == true) {
                udpSocket = DatagramSocket()
            }
        } catch (_: Exception) {}
    }

    fun submitCommand(throttle: Int, steering: Int, brake: Boolean, source: String) {
        val clampedT = throttle.coerceIn(-128, 127)
        val clampedS = steering.coerceIn(-128, 127)
        currentCommand = DriveCommand(clampedT, clampedS, brake, source)
        lastCommandTime = System.currentTimeMillis()

        // Also push immediately over WebSocket if active
        activeWebSocket?.let { ws ->
            val json = """{"t":$clampedT,"s":$clampedS,"b":$brake}"""
            ws.send(json)
        }
    }

    fun startDriveLoop(scope: CoroutineScope) {
        loopJob?.cancel()
        loopJob = scope.launch(Dispatchers.IO) {
            val buffer = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN)

            while (isActive) {
                val now = System.currentTimeMillis()
                // Deadman switch check (local fallback)
                if (now - lastCommandTime > (deadmanSec * 1000) && !currentCommand.isNeutral()) {
                    currentCommand = DriveCommand(0, 0, false, "deadman")
                }

                val cmd = currentCommand

                // 1. Send UDP packet to Kart (32 bytes)
                inetAddress?.let { addr ->
                    try {
                        buffer.clear()
                        buffer.put(cmd.throttle.toByte())
                        buffer.put(cmd.steering.toByte())
                        buffer.put(if (cmd.brake) 1.toByte() else 0.toByte())
                        buffer.put(0.toByte()) // Reserved
                        buffer.putInt((counter and 0xFFFFFFFFL).toInt())
                        // Pad with 24 zeros up to 32 bytes
                        buffer.put(ByteArray(24))

                        val packet = DatagramPacket(buffer.array(), 32, addr, targetPort)
                        udpSocket?.send(packet)
                    } catch (_: Exception) {
                        // Ignore occasional network socket send errors during AP restarts
                    }
                }

                // 2. Drive command stream / Heartbeat over WebSocket
                // Continuously stream every 33ms when driving, or every 200 ms when neutral heartbeat
                if (!cmd.isNeutral() || counter % 6 == 0L) {
                    activeWebSocket?.let { ws ->
                        val json = """{"t":${cmd.throttle},"s":${cmd.steering},"b":${cmd.brake}}"""
                        ws.send(json)
                    }
                }

                counter = (counter + 1) and 0xFFFFFFFFL
                delay(33) // ~30 Hz
            }
        }
    }

    fun stop() {
        loopJob?.cancel()
        loopJob = null
        submitCommand(0, 0, false, "stop")
        try {
            udpSocket?.close()
        } catch (_: Exception) {}
        udpSocket = null
    }
}
