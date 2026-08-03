package com.example.amasamya.rules

import com.example.amasamya.settings.SettingsManager

object ComplianceStandard {

    data class StandardInfo(
        val name: String,
        val shortCode: String,
        val description: String,
        val defaultMinTouchTargetDp: Int,
        val minNormalContrast: Float,
        val minLargeContrast: Float
    )

    fun getStandardInfo(standardName: String): StandardInfo {
        return when (standardName) {
            SettingsManager.STANDARD_SECTION_508 -> StandardInfo(
                name = "Section 508 (US Federal)",
                shortCode = "508",
                description = "US Federal Government accessibility standard based on WCAG 2.0 Level AA with strict hardware & software mandates.",
                defaultMinTouchTargetDp = 48,
                minNormalContrast = 4.5f,
                minLargeContrast = 3.0f
            )
            SettingsManager.STANDARD_EN_301_549 -> StandardInfo(
                name = "EN 301 549 (EU Standard)",
                shortCode = "EN",
                description = "European Union Accessibility Standard for ICT products and services, mandating WCAG 2.1 AA & EAA compliance.",
                defaultMinTouchTargetDp = 44,
                minNormalContrast = 4.5f,
                minLargeContrast = 3.0f
            )
            else -> StandardInfo(
                name = "WCAG 2.2 (W3C Standard)",
                shortCode = "WCAG",
                description = "Latest W3C Web Content Accessibility Guidelines including target size minimums (24dp level AA, 48dp level AAA).",
                defaultMinTouchTargetDp = 48,
                minNormalContrast = 4.5f,
                minLargeContrast = 3.0f
            )
        }
    }

    fun getMinTouchTargetDp(standardName: String, wcagLevel: String, customThresholdDp: Int): Int {
        if (customThresholdDp != 48) return customThresholdDp
        return when (standardName) {
            SettingsManager.STANDARD_EN_301_549 -> 44
            SettingsManager.STANDARD_SECTION_508 -> 48
            else -> if (wcagLevel == SettingsManager.LEVEL_A) 0 else if (wcagLevel == SettingsManager.LEVEL_AA) 24 else 48
        }
    }

    fun getMinContrastRatio(standardName: String, wcagLevel: String, isLargeText: Boolean): Float {
        return when (wcagLevel) {
            SettingsManager.LEVEL_AAA -> if (isLargeText) 4.5f else 7.0f
            SettingsManager.LEVEL_AA -> if (isLargeText) 3.0f else 4.5f
            else -> 1.0f
        }
    }
}
