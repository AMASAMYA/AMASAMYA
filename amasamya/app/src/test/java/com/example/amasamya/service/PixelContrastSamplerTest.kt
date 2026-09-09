package com.example.amasamya.service

import android.graphics.Color
import com.example.amasamya.utils.PixelContrastSampler
import org.junit.Assert.*
import org.junit.Test

class PixelContrastSamplerTest {

    @Test
    fun testBlackAndWhiteContrastRatio() {
        val black = 0xFF000000.toInt()
        val white = 0xFFFFFFFF.toInt()
        val ratio = PixelContrastSampler.calculateContrastRatio(black, white)
        assertEquals(21.0, ratio, 0.1)
    }

    @Test
    fun testSameColorContrastRatio() {
        val white = 0xFFFFFFFF.toInt()
        val ratio = PixelContrastSampler.calculateContrastRatio(white, white)
        assertEquals(1.0, ratio, 0.01)
    }

    @Test
    fun testWcag22AaThresholds() {
        val black = 0xFF000000.toInt()
        val white = 0xFFFFFFFF.toInt()
        val ratio = PixelContrastSampler.calculateContrastRatio(black, white)
        assertTrue(ratio >= 4.5) // Passes AA Normal Text
        assertTrue(ratio >= 3.0) // Passes AA Large Text
        assertTrue(ratio >= 7.0) // Passes AAA Normal Text
    }
}
