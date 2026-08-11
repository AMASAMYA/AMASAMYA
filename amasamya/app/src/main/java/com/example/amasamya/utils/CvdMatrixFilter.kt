package com.example.amasamya.utils

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import com.example.amasamya.settings.SettingsManager

object CvdMatrixFilter {

    /**
     * Standard ColorMatrix 4x5 float array representations for Color Vision Deficiencies.
     * Based on Viénot, Brettel, and Mollon LMS color space matrices.
     */
    private val PROTANOPIA_MATRIX = floatArrayOf(
        0.56667f, 0.43333f, 0.00000f, 0f, 0f,
        0.55833f, 0.44167f, 0.00000f, 0f, 0f,
        0.00000f, 0.24167f, 0.75833f, 0f, 0f,
        0.00000f, 0.00000f, 0.00000f, 1f, 0f
    )

    private val DEUTERANOPIA_MATRIX = floatArrayOf(
        0.62500f, 0.37500f, 0.00000f, 0f, 0f,
        0.70000f, 0.30000f, 0.00000f, 0f, 0f,
        0.00000f, 0.30000f, 0.70000f, 0f, 0f,
        0.00000f, 0.00000f, 0.00000f, 1f, 0f
    )

    private val TRITANOPIA_MATRIX = floatArrayOf(
        0.95000f, 0.05000f, 0.00000f, 0f, 0f,
        0.00000f, 0.43333f, 0.56667f, 0f, 0f,
        0.00000f, 0.47500f, 0.52500f, 0f, 0f,
        0.00000f, 0.00000f, 0.00000f, 1f, 0f
    )

    private val MONOCHROMACY_MATRIX = floatArrayOf(
        0.299f, 0.587f, 0.114f, 0f, 0f,
        0.299f, 0.587f, 0.114f, 0f, 0f,
        0.299f, 0.587f, 0.114f, 0f, 0f,
        0.000f, 0.000f, 0.000f, 1f, 0f
    )

    fun getColorMatrix(mode: String): ColorMatrix? {
        val array = when (mode) {
            SettingsManager.CVD_PROTANOPIA -> PROTANOPIA_MATRIX
            SettingsManager.CVD_DEUTERANOPIA -> DEUTERANOPIA_MATRIX
            SettingsManager.CVD_TRITANOPIA -> TRITANOPIA_MATRIX
            SettingsManager.CVD_MONOCHROMACY -> MONOCHROMACY_MATRIX
            else -> return null
        }
        return ColorMatrix(array)
    }

    fun getColorFilter(mode: String): ColorMatrixColorFilter? {
        val matrix = getColorMatrix(mode) ?: return null
        return ColorMatrixColorFilter(matrix)
    }

    fun getModeDescription(mode: String): String {
        return when (mode) {
            SettingsManager.CVD_PROTANOPIA -> "Red-Blindness (L-cone deficiency)"
            SettingsManager.CVD_DEUTERANOPIA -> "Green-Blindness (M-cone deficiency)"
            SettingsManager.CVD_TRITANOPIA -> "Blue-Blindness (S-cone deficiency)"
            SettingsManager.CVD_MONOCHROMACY -> "Achromatopsia (Total Grayscale)"
            else -> "Normal Vision (No Simulation)"
        }
    }
}
