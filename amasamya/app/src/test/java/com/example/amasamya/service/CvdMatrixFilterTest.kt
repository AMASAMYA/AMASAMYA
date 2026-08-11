package com.example.amasamya.service

import com.example.amasamya.settings.SettingsManager
import com.example.amasamya.utils.CvdMatrixFilter
import org.junit.Assert.*
import org.junit.Test

class CvdMatrixFilterTest {

    @Test
    fun testColorMatrixModes() {
        assertNull(CvdMatrixFilter.getColorMatrix(SettingsManager.CVD_NONE))
        assertNotNull(CvdMatrixFilter.getColorMatrix(SettingsManager.CVD_PROTANOPIA))
        assertNotNull(CvdMatrixFilter.getColorMatrix(SettingsManager.CVD_DEUTERANOPIA))
        assertNotNull(CvdMatrixFilter.getColorMatrix(SettingsManager.CVD_TRITANOPIA))
        assertNotNull(CvdMatrixFilter.getColorMatrix(SettingsManager.CVD_MONOCHROMACY))
    }

    @Test
    fun testModeDescriptions() {
        assertTrue(CvdMatrixFilter.getModeDescription(SettingsManager.CVD_PROTANOPIA).contains("Red-Blindness"))
        assertTrue(CvdMatrixFilter.getModeDescription(SettingsManager.CVD_DEUTERANOPIA).contains("Green-Blindness"))
        assertTrue(CvdMatrixFilter.getModeDescription(SettingsManager.CVD_TRITANOPIA).contains("Blue-Blindness"))
        assertTrue(CvdMatrixFilter.getModeDescription(SettingsManager.CVD_MONOCHROMACY).contains("Achromatopsia"))
        assertTrue(CvdMatrixFilter.getModeDescription(SettingsManager.CVD_NONE).contains("Normal Vision"))
    }
}
