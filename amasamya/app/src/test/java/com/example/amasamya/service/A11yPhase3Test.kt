package com.example.amasamya.service

import com.example.amasamya.service.A11yAuditService.Companion.A11yNodeData
import com.example.amasamya.service.A11yAuditService.Companion.evaluateScreenLevelRules
import com.example.amasamya.settings.SettingsManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class A11yPhase3Test {

    private val density = 2.0f // 1dp = 2px

    @Test
    fun testFrictionWarning_AA_Limit() {
        // AA flags friction warning if visible focusable elements count > 25
        val nodeList = mutableListOf<A11yNodeData>()
        
        // Add 26 focusable visible elements
        for (i in 1..26) {
            nodeList.add(
                A11yNodeData(
                    className = "android.widget.TextView",
                    text = "Item $i",
                    contentDescription = "",
                    bounds = null,
                    isClickable = false,
                    isFocusable = true,
                    isHeading = false,
                    isVisibleToUser = true,
                    hasTextInSubtree = false,
                    width = 100,
                    height = 50,
                    left = 10,
                    top = i * 60
                )
            )
        }

        val violations = evaluateScreenLevelRules(nodeList, density, SettingsManager.LEVEL_AA)
        val frictionWarning = violations.firstOrNull { it.type == "Friction Warning" }
        assertTrue(frictionWarning != null)
        assertEquals("Warning", frictionWarning?.severity)
        assertEquals("2.4.3", frictionWarning?.wcagSc)
        assertTrue(frictionWarning?.description?.contains("cluttered with 26 items") == true)
        
        // Under AAA, it should also fail (limit is 15)
        val aaaViolations = evaluateScreenLevelRules(nodeList, density, SettingsManager.LEVEL_AAA)
        assertTrue(aaaViolations.any { it.type == "Friction Warning" })
    }

    @Test
    fun testFrictionWarning_BelowLimit() {
        // 10 focusable visible elements (should pass AA and AAA)
        val nodeList = mutableListOf<A11yNodeData>()
        for (i in 1..10) {
            nodeList.add(
                A11yNodeData(
                    className = "android.widget.TextView",
                    text = "Item $i",
                    contentDescription = "",
                    bounds = null,
                    isClickable = false,
                    isFocusable = true,
                    isHeading = false,
                    isVisibleToUser = true,
                    hasTextInSubtree = false,
                    width = 100,
                    height = 50,
                    left = 10,
                    top = i * 60
                )
            )
        }

        val aaViolations = evaluateScreenLevelRules(nodeList, density, SettingsManager.LEVEL_AA)
        assertTrue(aaViolations.none { it.type == "Friction Warning" })

        val aaaViolations = evaluateScreenLevelRules(nodeList, density, SettingsManager.LEVEL_AAA)
        assertTrue(aaaViolations.none { it.type == "Friction Warning" })
    }

    @Test
    fun testRedundantClickTargets_IdenticalLabels() {
        // Two adjacent clickable elements with identical text
        val nodeA = A11yNodeData(
            className = "android.widget.Button",
            text = "Edit Profile",
            contentDescription = "",
            bounds = null,
            isClickable = true,
            isFocusable = true,
            isHeading = false,
            isVisibleToUser = true,
            hasTextInSubtree = true,
            width = 100,
            height = 50,
            left = 10,
            top = 10
        )
        val nodeB = A11yNodeData(
            className = "android.widget.Button",
            text = "Edit Profile",
            contentDescription = "",
            bounds = null,
            isClickable = true,
            isFocusable = true,
            isHeading = false,
            isVisibleToUser = true,
            hasTextInSubtree = true,
            width = 100,
            height = 50,
            left = 10,
            top = 70 // Distance between A and B is: 70 - (10 + 50) = 10px (5dp), which is < 24dp threshold (48px)
        )

        val nodeList = listOf(nodeA, nodeB)
        val violations = evaluateScreenLevelRules(nodeList, density, SettingsManager.LEVEL_AA)
        val redundancyViolation = violations.firstOrNull { it.type == "Redundant Click Target" }
        
        assertTrue(redundancyViolation != null)
        assertEquals("Warning", redundancyViolation?.severity)
        assertEquals("2.4.4", redundancyViolation?.wcagSc)
        assertTrue(redundancyViolation?.description?.contains("exact same label 'Edit Profile'") == true)
    }

    @Test
    fun testRedundantClickTargets_IconAndText() {
        // One clickable element has label, adjacent one is empty (e.g. icon button)
        val textButton = A11yNodeData(
            className = "android.widget.TextView",
            text = "Add Item",
            contentDescription = "",
            bounds = null,
            isClickable = true,
            isFocusable = true,
            isHeading = false,
            isVisibleToUser = true,
            hasTextInSubtree = true,
            width = 120,
            height = 50,
            left = 10,
            top = 10
        )
        val iconButton = A11yNodeData(
            className = "android.widget.ImageView",
            text = "",
            contentDescription = "",
            bounds = null,
            isClickable = true,
            isFocusable = true,
            isHeading = false,
            isVisibleToUser = true,
            hasTextInSubtree = false,
            width = 40,
            height = 40,
            left = 140, // Distance: 140 - (10 + 120) = 10px (5dp) < 48px threshold
            top = 15
        )

        val nodeList = listOf(textButton, iconButton)
        val violations = evaluateScreenLevelRules(nodeList, density, SettingsManager.LEVEL_AA)
        val redundancyViolation = violations.firstOrNull { it.type == "Redundant Click Target" }

        assertTrue(redundancyViolation != null)
        assertTrue(redundancyViolation?.description?.contains("one labeled 'Add Item' and the other empty") == true)
    }

    @Test
    fun testRedundantClickTargets_TooFarIgnored() {
        // Identical labels but too far apart (e.g. distance > 24dp / 48px)
        val nodeA = A11yNodeData(
            className = "android.widget.Button",
            text = "Delete",
            contentDescription = "",
            bounds = null,
            isClickable = true,
            isFocusable = true,
            isHeading = false,
            isVisibleToUser = true,
            hasTextInSubtree = true,
            width = 100,
            height = 50,
            left = 10,
            top = 10
        )
        val nodeB = A11yNodeData(
            className = "android.widget.Button",
            text = "Delete",
            contentDescription = "",
            bounds = null,
            isClickable = true,
            isFocusable = true,
            isHeading = false,
            isVisibleToUser = true,
            hasTextInSubtree = true,
            width = 100,
            height = 50,
            left = 10,
            top = 150 // Distance: 150 - (10 + 50) = 90px (45dp) > 24dp
        )

        val nodeList = listOf(nodeA, nodeB)
        val violations = evaluateScreenLevelRules(nodeList, density, SettingsManager.LEVEL_AA)
        assertTrue(violations.none { it.type == "Redundant Click Target" })
    }
}
