package com.example.amasamya.rules

import com.example.amasamya.service.A11yAuditService
import com.example.amasamya.service.A11yAuditService.Companion.A11yNodeData
import com.example.amasamya.settings.SettingsManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ComplianceStandardTest {

    @Test
    fun testGigw30StandardInfo() {
        val info = ComplianceStandard.getStandardInfo(SettingsManager.STANDARD_GIGW_3_0)
        assertEquals("GIGW 3.0 (India Govt)", info.name)
        assertEquals("GIGW3", info.shortCode)
        assertEquals(48, info.defaultMinTouchTargetDp)
        assertEquals(4.5f, info.minNormalContrast, 0.01f)
        assertEquals(3.0f, info.minLargeContrast, 0.01f)
    }

    @Test
    fun testIs17802StandardInfo() {
        val info = ComplianceStandard.getStandardInfo(SettingsManager.STANDARD_IS_17802)
        assertEquals("IS 17802 (BIS ICT Standard)", info.name)
        assertEquals("IS17802", info.shortCode)
        assertEquals(48, info.defaultMinTouchTargetDp)
    }

    @Test
    fun testIndiaNationalBaselineStandardInfo() {
        val info = ComplianceStandard.getStandardInfo(SettingsManager.STANDARD_INDIA_NATIONAL)
        assertEquals("India National Baseline (RPwD Act)", info.name)
        assertEquals("IN-BASE", info.shortCode)
        assertEquals(48, info.defaultMinTouchTargetDp)
    }

    @Test
    fun testTouchTargetMinimumsForIndianStandards() {
        val gigwDp = ComplianceStandard.getMinTouchTargetDp(SettingsManager.STANDARD_GIGW_3_0, SettingsManager.LEVEL_AA, 48)
        val isDp = ComplianceStandard.getMinTouchTargetDp(SettingsManager.STANDARD_IS_17802, SettingsManager.LEVEL_AA, 48)
        val inDp = ComplianceStandard.getMinTouchTargetDp(SettingsManager.STANDARD_INDIA_NATIONAL, SettingsManager.LEVEL_A, 48)

        assertEquals(48, gigwDp)
        assertEquals(48, isDp)
        assertEquals(48, inDp)
    }

    @Test
    fun testContrastRatioForIndianStandards() {
        val normalContrast = ComplianceStandard.getMinContrastRatio(SettingsManager.STANDARD_GIGW_3_0, SettingsManager.LEVEL_AA, false)
        val largeContrast = ComplianceStandard.getMinContrastRatio(SettingsManager.STANDARD_IS_17802, SettingsManager.LEVEL_AA, true)

        assertEquals(4.5f, normalContrast, 0.01f)
        assertEquals(3.0f, largeContrast, 0.01f)
    }

    @Test
    fun testEvaluateNodeDataWithGigwRules() {
        val smallNode = A11yNodeData(
            className = "android.widget.Button",
            text = "Submit",
            contentDescription = "",
            bounds = null,
            isClickable = true,
            isFocusable = true,
            isHeading = false,
            isVisibleToUser = true,
            hasTextInSubtree = true,
            width = 30,
            height = 30,
            left = 0,
            top = 0
        )

        val violations = A11yAuditService.evaluateNodeData(
            node = smallNode,
            density = 1.0f,
            wcagLevel = SettingsManager.LEVEL_AA,
            screenBitmap = null,
            context = null,
            complianceStandard = SettingsManager.STANDARD_GIGW_3_0
        )

        assertTrue(violations.any { it.type.contains("Target Size") && it.description.contains("GIGW 3.0") })
    }
}
