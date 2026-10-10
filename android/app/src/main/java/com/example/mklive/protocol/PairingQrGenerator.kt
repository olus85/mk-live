package com.example.mklive.protocol

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.SecureRandom

/**
 * Nintendo Switch LP2P Pairing QR Code Generator.
 *
 * The Mario Kart Live kart camera scans a 62-byte binary payload:
 * - 16 bytes: Seed (random entropy for pairing crypto)
 * - 32 bytes: SSID (ASCII string, null-padded to 32 bytes)
 * - 2 bytes: Channel (uint16 LE, e.g. 1, 6, 11)
 * - 12 bytes: Zero bytes (padding)
 *
 * Rendered as QR Code Version 4 (33x33), Error Correction Level M, Byte Encoding.
 */
object PairingQrGenerator {

    private val random = SecureRandom()

    /**
     * Builds the exact 62-byte binary payload according to SwitchBrew LP2P specs.
     */
    fun buildPayload(seed: ByteArray, ssid: String, channel: Int): ByteArray {
        require(seed.size == 16) { "Seed must be exactly 16 bytes, got ${seed.size}" }
        val ssidBytes = ssid.toByteArray(Charsets.US_ASCII)
        require(ssidBytes.size <= 32) { "SSID must be at most 32 bytes, got ${ssidBytes.size}" }

        val buffer = ByteBuffer.allocate(62).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(seed)
        buffer.put(ssidBytes)
        // Null-padding for remaining SSID space up to 32 bytes
        val remainingSsid = 32 - ssidBytes.size
        if (remainingSsid > 0) {
            buffer.put(ByteArray(remainingSsid))
        }
        buffer.putShort(channel.toShort())
        // 12 bytes of zeros
        buffer.put(ByteArray(12))

        return buffer.array()
    }

    /**
     * Helper to build a random pairing payload.
     * Generates a random 16-byte seed and random pairing SSID starting with 'P'.
     */
    fun createRandomSession(channel: Int = 6, createBitmap: Boolean = true): PairingSession {
        val seed = ByteArray(16)
        random.nextBytes(seed)
        val randomHex = ByteArray(8)
        random.nextBytes(randomHex)
        val hexString = randomHex.joinToString("") { "%02x".format(it) }
        val ssid = "P$hexString"

        val payload = buildPayload(seed, ssid, channel)
        val matrix = generateBitMatrix(payload, 512)
        val bitmap = if (createBitmap) renderQrBitmapFromMatrix(matrix) else null

        return PairingSession(
            seed = seed,
            seedHex = seed.joinToString("") { "%02x".format(it) },
            ssid = ssid,
            channel = channel,
            payload = payload,
            qrMatrix = matrix,
            qrBitmap = bitmap
        )
    }

    /**
     * Builds payload from hex seed and SSID string, and generates the QR code bitmap.
     */
    fun createSession(seedHex: String, ssid: String, channel: Int, createBitmap: Boolean = true): PairingSession {
        val cleanHex = seedHex.trim().replace(":", "").replace(" ", "")
        val seed = cleanHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val payload = buildPayload(seed, ssid, channel)
        val matrix = generateBitMatrix(payload, 512)
        val bitmap = if (createBitmap) renderQrBitmapFromMatrix(matrix) else null

        return PairingSession(
            seed = seed,
            seedHex = cleanHex,
            ssid = ssid,
            channel = channel,
            payload = payload,
            qrMatrix = matrix,
            qrBitmap = bitmap
        )
    }

    /**
     * Generates the pure ZXing BitMatrix without requiring Android graphics.
     */
    fun generateBitMatrix(payload: ByteArray, sizePx: Int = 512): com.google.zxing.common.BitMatrix {
        val hints = HashMap<EncodeHintType, Any>().apply {
            put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
            // DO NOT set CHARACTER_SET here! Setting CHARACTER_SET causes ZXing to inject
            // an ECI header (Mode.ECI 0111), which breaks Nintendo Mario Kart's hardware QR scanner.
            // When omitted, ZXing defaults to ISO-8859-1 byte mode with NO ECI header.
            put(EncodeHintType.MARGIN, 4)
        }

        val byteString = String(payload, Charsets.ISO_8859_1)
        return MultiFormatWriter().encode(
            byteString,
            BarcodeFormat.QR_CODE,
            sizePx,
            sizePx,
            hints
        )
    }

    /**
     * Encodes arbitrary raw bytes into a QR Code bitmap using ISO-8859-1 byte mapping.
     */
    fun renderQrBitmap(payload: ByteArray, sizePx: Int = 512): Bitmap {
        val matrix = generateBitMatrix(payload, sizePx)
        return renderQrBitmapFromMatrix(matrix)
    }

    fun renderQrBitmapFromMatrix(matrix: com.google.zxing.common.BitMatrix): Bitmap {
        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height)

        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (matrix.get(x, y)) Color.BLACK else Color.WHITE
            }
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    data class PairingSession(
        val seed: ByteArray,
        val seedHex: String,
        val ssid: String,
        val channel: Int,
        val payload: ByteArray,
        val qrMatrix: com.google.zxing.common.BitMatrix,
        val qrBitmap: Bitmap?
    )
}
