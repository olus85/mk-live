package com.example.mklive.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PairingQrGeneratorTest {

    @Test
    fun testPayloadLengthAndStructure() {
        val seed = ByteArray(16) { it.toByte() }
        val ssid = "Ptest123"
        val channel = 6

        val payload = PairingQrGenerator.buildPayload(seed, ssid, channel)

        // 1. Must be exactly 62 bytes according to SwitchBrew LP2P spec
        assertEquals(62, payload.size)

        val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)

        // 2. First 16 bytes: Seed
        val extractedSeed = ByteArray(16)
        buffer.get(extractedSeed)
        for (i in 0 until 16) {
            assertEquals(i.toByte(), extractedSeed[i])
        }

        // 3. Next 32 bytes: SSID with zero padding
        val extractedSsidBytes = ByteArray(32)
        buffer.get(extractedSsidBytes)
        val extractedSsid = String(extractedSsidBytes).trimEnd('\u0000')
        assertEquals("Ptest123", extractedSsid)

        // 4. Next 2 bytes: Channel (uint16 LE)
        val extractedChannel = buffer.short.toInt() and 0xFFFF
        assertEquals(6, extractedChannel)

        // 5. Last 12 bytes: Zero padding
        val extractedZeroes = ByteArray(12)
        buffer.get(extractedZeroes)
        for (b in extractedZeroes) {
            assertEquals(0.toByte(), b)
        }
    }

    @Test
    fun testRandomSessionGeneration() {
        val session = PairingQrGenerator.createRandomSession(channel = 11, createBitmap = false)

        assertEquals(62, session.payload.size)
        assertEquals(16, session.seed.size)
        assertEquals(32, session.seedHex.length)
        assertTrue(session.ssid.startsWith("P"))
        assertEquals(11, session.channel)
        assertNotNull(session.qrMatrix)
        assertEquals(512, session.qrMatrix.width)
        assertEquals(512, session.qrMatrix.height)
    }

    @Test
    fun testChannel1and6and11() {
        listOf(1, 6, 11).forEach { ch ->
            val session = PairingQrGenerator.createRandomSession(channel = ch, createBitmap = false)
            val buffer = ByteBuffer.wrap(session.payload).order(ByteOrder.LITTLE_ENDIAN)
            buffer.position(48) // Offset to channel field
            val extractedCh = buffer.short.toInt() and 0xFFFF
            assertEquals(ch, extractedCh)
        }
    }

    @Test
    fun testDecodingRoundTrip() {
        val seed = ByteArray(16) { (it * 7).toByte() }
        val ssid = "openkart-pair_test123"
        val channel = 11

        val payload = PairingQrGenerator.buildPayload(seed, ssid, channel)
        val matrix = PairingQrGenerator.generateBitMatrix(payload, 256)

        val source = com.google.zxing.BinaryBitmap(com.google.zxing.common.HybridBinarizer(
            object : com.google.zxing.LuminanceSource(matrix.width, matrix.height) {
                override fun getRow(y: Int, row: ByteArray?): ByteArray {
                    val w = width
                    val r = row ?: ByteArray(w)
                    for (x in 0 until w) {
                        r[x] = if (matrix.get(x, y)) 0.toByte() else 255.toByte()
                    }
                    return r
                }
                override fun getMatrix(): ByteArray {
                    val w = width
                    val h = height
                    val r = ByteArray(w * h)
                    for (y in 0 until h) {
                        for (x in 0 until w) {
                            r[y * w + x] = if (matrix.get(x, y)) 0.toByte() else 255.toByte()
                        }
                    }
                    return r
                }
            }
        ))

        val result = com.google.zxing.qrcode.QRCodeReader().decode(source)
        assertNotNull(result)
        val rawBytes = result.rawBytes
        assertNotNull(rawBytes)
        // Check text/raw bytes
        val textBytes = result.text.toByteArray(Charsets.ISO_8859_1)
        assertEquals(62, textBytes.size)
        for (i in 0 until 62) {
            assertEquals("Byte $i must match", payload[i], textBytes[i])
        }
    }
}
