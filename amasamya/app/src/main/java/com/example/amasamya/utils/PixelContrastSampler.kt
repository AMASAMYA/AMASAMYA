package com.example.amasamya.utils

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import kotlin.math.max
import kotlin.math.min

object PixelContrastSampler {

    data class ContrastResult(
        val contrastRatio: Double,
        val passesAaNormalText: Boolean,
        val passesAaLargeText: Boolean,
        val passesAaaNormalText: Boolean,
        val textColorHex: String,
        val backgroundColorHex: String
    )

    // Calculates WCAG 2.2 Relative Luminance: L = 0.2126 * R + 0.7152 * G + 0.0722 * B
    fun calculateLuminance(color: Int): Double {
        val r = sRgbToLinear(((color shr 16) and 0xFF) / 255.0)
        val g = sRgbToLinear(((color shr 8) and 0xFF) / 255.0)
        val b = sRgbToLinear((color and 0xFF) / 255.0)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun sRgbToLinear(c: Double): Double {
        return if (c <= 0.04045) {
            c / 12.92
        } else {
            Math.pow((c + 0.055) / 1.055, 2.4)
        }
    }

    // Calculates WCAG Contrast Ratio: (L1 + 0.05) / (L2 + 0.05)
    fun calculateContrastRatio(color1: Int, color2: Int): Double {
        val l1 = calculateLuminance(color1)
        val l2 = calculateLuminance(color2)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    // Samples average background color around element bounds from Bitmap
    fun sampleElementContrast(bitmap: Bitmap, bounds: Rect, textColor: Int = Color.BLACK): ContrastResult {
        val width = bitmap.width
        val height = bitmap.height

        // Ensure bounds are inside bitmap dimensions
        val safeLeft = bounds.left.coerceIn(0, width - 1)
        val safeTop = bounds.top.coerceIn(0, height - 1)
        val safeRight = bounds.right.coerceIn(safeLeft + 1, width)
        val safeBottom = bounds.bottom.coerceIn(safeTop + 1, height)

        var totalR = 0L
        var totalG = 0L
        var totalB = 0L
        var count = 0

        // Sample pixels along element border to detect background
        val step = max(1, (safeRight - safeLeft) / 10)
        for (x in safeLeft until safeRight step step) {
            val pixel = bitmap.getPixel(x, safeTop)
            totalR += Color.red(pixel)
            totalG += Color.green(pixel)
            totalB += Color.blue(pixel)
            count++
        }

        val avgR = (totalR / max(1, count)).toInt()
        val avgG = (totalG / max(1, count)).toInt()
        val avgB = (totalB / max(1, count)).toInt()
        val bgColor = Color.rgb(avgR, avgG, avgB)

        val ratio = calculateContrastRatio(textColor, bgColor)
        val roundedRatio = Math.round(ratio * 100.0) / 100.0

        return ContrastResult(
            contrastRatio = roundedRatio,
            passesAaNormalText = roundedRatio >= 4.5,
            passesAaLargeText = roundedRatio >= 3.0,
            passesAaaNormalText = roundedRatio >= 7.0,
            textColorHex = String.format("#%06X", 0xFFFFFF and textColor),
            backgroundColorHex = String.format("#%06X", 0xFFFFFF and bgColor)
        )
    }
}
