package com.example.amasamya.utils

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.util.Log
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object ContrastAnalyzer {

    private const val TAG = "ContrastAnalyzer"

    data class ContrastResult(
        val ratio: Float,
        val foregroundColorHex: String,
        val backgroundColorHex: String,
        val isCompliant: Boolean,
        val suggestedColorHex: String? = null
    )

    // Calculate relative luminance of a color
    // WCAG formula: L = 0.2126 * R + 0.7152 * G + 0.0722 * B
    fun calculateLuminance(color: Int): Double {
        val r = Color.red(color) / 255.0
        val g = Color.green(color) / 255.0
        val b = Color.blue(color) / 255.0

        val rLinear = if (r <= 0.03928) r / 12.92 else ((r + 0.055) / 1.055).pow(2.4)
        val gLinear = if (g <= 0.03928) g / 12.92 else ((g + 0.055) / 1.055).pow(2.4)
        val bLinear = if (b <= 0.03928) b / 12.92 else ((b + 0.055) / 1.055).pow(2.4)

        return 0.2126 * rLinear + 0.7152 * gLinear + 0.0722 * bLinear
    }

    // Calculate contrast ratio between two colors
    fun calculateContrastRatio(color1: Int, color2: Int): Float {
        val lum1 = calculateLuminance(color1)
        val lum2 = calculateLuminance(color2)

        val lighter = max(lum1, lum2)
        val darker = min(lum1, lum2)

        return ((lighter + 0.05) / (darker + 0.05)).toFloat()
    }

    // Simple K-Means (K=2) clustering to find dominant foreground and background colors in a cropped bitmap
    fun extractDominantColors(croppedBitmap: Bitmap): Pair<Int, Int> {
        val width = croppedBitmap.width
        val height = croppedBitmap.height
        val totalPixels = width * height

        if (totalPixels <= 0) {
            return Pair(Color.BLACK, Color.WHITE)
        }

        val pixels = IntArray(totalPixels)
        croppedBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Choose initial centroids
        // Centroid 1: Top-Left pixel (usually representing background)
        var c1 = pixels[0]
        // Centroid 2: Let's find the pixel that has the maximum color distance from Centroid 1
        var c2 = pixels[totalPixels / 2]
        var maxDistance = -1.0
        for (i in 0 until totalPixels step max(1, totalPixels / 100)) {
            val dist = colorDistance(c1, pixels[i])
            if (dist > maxDistance) {
                maxDistance = dist
                c2 = pixels[i]
            }
        }

        // Run K-Means for 4 iterations (which is extremely fast and sufficient for 2 clear clusters)
        for (iter in 0 until 4) {
            var sumR1 = 0.0; var sumG1 = 0.0; var sumB1 = 0.0; var count1 = 0
            var sumR2 = 0.0; var sumG2 = 0.0; var sumB2 = 0.0; var count2 = 0

            for (pixel in pixels) {
                val dist1 = colorDistance(pixel, c1)
                val dist2 = colorDistance(pixel, c2)

                if (dist1 < dist2) {
                    sumR1 += Color.red(pixel)
                    sumG1 += Color.green(pixel)
                    sumB1 += Color.blue(pixel)
                    count1++
                } else {
                    sumR2 += Color.red(pixel)
                    sumG2 += Color.green(pixel)
                    sumB2 += Color.blue(pixel)
                    count2++
                }
            }

            if (count1 > 0) {
                c1 = Color.rgb((sumR1 / count1).toInt(), (sumG1 / count1).toInt(), (sumB1 / count1).toInt())
            }
            if (count2 > 0) {
                c2 = Color.rgb((sumR2 / count2).toInt(), (sumG2 / count2).toInt(), (sumB2 / count2).toInt())
            }
        }

        // Determine which is foreground (text) and which is background
        // Usually, background takes up more area (count1 vs count2)
        val count1 = pixels.count { colorDistance(it, c1) < colorDistance(it, c2) }
        val count2 = totalPixels - count1

        val background = if (count1 >= count2) c1 else c2
        val foreground = if (count1 >= count2) c2 else c1

        return Pair(foreground, background)
    }

    private fun colorDistance(color1: Int, color2: Int): Double {
        val dr = Color.red(color1) - Color.red(color2)
        val dg = Color.green(color1) - Color.green(color2)
        val db = Color.blue(color1) - Color.blue(color2)
        return (dr * dr + dg * dg + db * db).toDouble()
    }

    fun toHex(color: Int): String {
        return String.format("#%06X", 0xFFFFFF and color)
    }

    // Incremental color adjuster to suggest a compliant text color
    fun suggestCompliantColor(foreground: Int, background: Int, targetRatio: Float): String {
        val lumBg = calculateLuminance(background)
        val isBgLighter = lumBg > 0.5

        var r = Color.red(foreground)
        var g = Color.green(foreground)
        var b = Color.blue(foreground)

        // Iterate by moving the color components closer to black or white
        for (step in 1..25) {
            val factor = step / 25.0
            val newFg = if (isBgLighter) {
                // Background is light, make text darker (closer to black)
                Color.rgb(
                    (r * (1.0 - factor)).toInt(),
                    (g * (1.0 - factor)).toInt(),
                    (b * (1.0 - factor)).toInt()
                )
            } else {
                // Background is dark, make text lighter (closer to white)
                Color.rgb(
                    (r + (255 - r) * factor).toInt(),
                    (g + (255 - g) * factor).toInt(),
                    (b + (255 - b) * factor).toInt()
                )
            }

            if (calculateContrastRatio(newFg, background) >= targetRatio) {
                return toHex(newFg)
            }
        }

        // Fallback: pure black or pure white
        return if (isBgLighter) "#000000" else "#FFFFFF"
    }

    // Main API to analyze contrast
    fun analyzeContrast(
        screenBitmap: Bitmap,
        nodeBounds: Rect,
        isLargeText: Boolean,
        wcagLevel: String
    ): ContrastResult? {
        try {
            // Validate bounds relative to screen dimensions
            val left = max(0, nodeBounds.left)
            val top = max(0, nodeBounds.top)
            val right = min(screenBitmap.width, nodeBounds.right)
            val bottom = min(screenBitmap.height, nodeBounds.bottom)

            val width = right - left
            val height = bottom - top

            if (width <= 0 || height <= 0) return null

            // Crop text view area
            val cropped = Bitmap.createBitmap(screenBitmap, left, top, width, height)
            val (fg, bg) = extractDominantColors(cropped)
            cropped.recycle()

            val ratio = calculateContrastRatio(fg, bg)

            // WCAG Thresholds
            val targetRatio = when (wcagLevel) {
                "AAA" -> if (isLargeText) 4.5f else 7.0f
                "AA" -> if (isLargeText) 3.0f else 4.5f
                else -> 1.0f // Level A has no strict contrast minimum
            }

            val isCompliant = ratio >= targetRatio
            var suggestedHex: String? = null
            if (!isCompliant) {
                suggestedHex = suggestCompliantColor(fg, bg, targetRatio)
            }

            return ContrastResult(
                ratio = ratio,
                foregroundColorHex = toHex(fg),
                backgroundColorHex = toHex(bg),
                isCompliant = isCompliant,
                suggestedColorHex = suggestedHex
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error performing contrast analysis", e)
            return null
        }
    }
}
