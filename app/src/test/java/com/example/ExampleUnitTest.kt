package com.example

import com.example.util.QrCodeGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testQrCodeGenerator() {
        val testUrl = "https://draftkings.com/r/refreels"
        val matrix = QrCodeGenerator.encode(testUrl)
        assertNotNull(matrix)
        assertTrue(matrix.isNotEmpty())
        assertEquals(matrix.size, matrix[0].size)
        // Check top-left finder pattern corner is black
        assertTrue(matrix[0][0])
    }

    @Test
    fun testQrCodeVariableLength() {
        val shortUrl = "https://x.co/1"
        val longUrl = "https://pointsbet.com/ref/REFREELS150?campaign=mobile&source=refreels_app"
        val matrixShort = QrCodeGenerator.encode(shortUrl)
        val matrixLong = QrCodeGenerator.encode(longUrl)
        assertTrue(matrixShort.isNotEmpty())
        assertTrue(matrixLong.isNotEmpty())
        assertTrue(matrixLong.size >= matrixShort.size)
    }
}
