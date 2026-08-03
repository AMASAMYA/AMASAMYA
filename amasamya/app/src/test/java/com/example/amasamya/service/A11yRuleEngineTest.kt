package com.example.amasamya.service

import com.example.amasamya.service.A11yAuditService.Companion.A11yNodeData
import com.example.amasamya.service.A11yAuditService.Companion.NodeViolation
import com.example.amasamya.service.A11yAuditService.Companion.evaluateNodeData
import com.example.amasamya.settings.SettingsManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class A11yRuleEngineTest {

    private val density = 2.0f // 1dp = 2px

    @Test
    fun testTargetSizeAACompliance() {
        // AA requires >= 24dp (which is 48px at density 2.0)
        
        // Compliant: 24x24dp (48x48px)
        val compliantNode = A11yNodeData(
            className = "android.widget.Button",
            text = "Click me",
            contentDescription = "",
            bounds = null,
            isClickable = true,
            isFocusable = true,
            isHeading = false,
            isVisibleToUser = true,
            hasTextInSubtree = true,
            width = 48,
            height = 48,
            left = 10,
            top = 10
        )
        val compliantViolations = evaluateNodeData(compliantNode, density, SettingsManager.LEVEL_AA)
        assertTrue(compliantViolations.none { it.type == "Target Size" })

        // Non-compliant: 20x20dp (40x40px)
        val failingNode = A11yNodeData(
            className = "android.widget.Button",
            text = "Click me",
            contentDescription = "",
            bounds = null,
            isClickable = true,
            isFocusable = true,
            isHeading = false,
            isVisibleToUser = true,
            hasTextInSubtree = true,
            width = 40,
            height = 40,
            left = 10,
            top = 10
        )
        val failingViolations = evaluateNodeData(failingNode, density, SettingsManager.LEVEL_AA)
        val targetSizeViolation = failingViolations.firstOrNull { it.type == "Target Size" }
        assertTrue(targetSizeViolation != null)
        assertEquals("Warning", targetSizeViolation?.severity)
        assertEquals("2.5.8", targetSizeViolation?.wcagSc)
    }

    @Test
    fun testTargetSizeAAACompliance() {
        // AAA requires >= 48dp (which is 96px at density 2.0)

        // Compliant: 48x48dp (96x96px)
        val compliantNode = A11yNodeData(
            className = "android.widget.Button",
            text = "Submit",
            contentDescription = "",
            bounds = null,
            isClickable = true,
            isFocusable = true,
            isHeading = false,
            isVisibleToUser = true,
            hasTextInSubtree = true,
            width = 96,
            height = 96,
            left = 10,
            top = 10
        )
        val compliantViolations = evaluateNodeData(compliantNode, density, SettingsManager.LEVEL_AAA)
        assertTrue(compliantViolations.none { it.type == "Target Size" })

        // Non-compliant for AAA but compliant for AA: 30x30dp (60x60px)
        val failingNode = A11yNodeData(
            className = "android.widget.Button",
            text = "Submit",
            contentDescription = "",
            bounds = null,
            isClickable = true,
            isFocusable = true,
            isHeading = false,
            isVisibleToUser = true,
            hasTextInSubtree = true,
            width = 60,
            height = 60,
            left = 10,
            top = 10
        )
        
        // Under AA, it should pass
        val aaViolations = evaluateNodeData(failingNode, density, SettingsManager.LEVEL_AA)
        assertTrue(aaViolations.none { it.type == "Target Size" })
        
        // Under AAA, it should fail
        val aaaViolations = evaluateNodeData(failingNode, density, SettingsManager.LEVEL_AAA)
        val targetSizeViolation = aaaViolations.firstOrNull { it.type == "Target Size" }
        assertTrue(targetSizeViolation != null)
        assertEquals("Critical", targetSizeViolation?.severity)
        assertEquals("2.5.5", targetSizeViolation?.wcagSc)
    }

    @Test
    fun testMissingLabelValidation() {
        // Interactive element with no text or content description
        val missingLabelNode = A11yNodeData(
            className = "android.widget.ImageView",
            text = "",
            contentDescription = "",
            bounds = null,
            isClickable = true,
            isFocusable = true,
            isHeading = false,
            isVisibleToUser = true,
            hasTextInSubtree = false,
            width = 80,
            height = 80,
            left = 10,
            top = 10
        )
        val violations = evaluateNodeData(missingLabelNode, density, SettingsManager.LEVEL_AA)
        val violation = violations.firstOrNull { it.type == "Missing Label" }
        assertTrue(violation != null)
        assertEquals("Critical", violation?.severity)
        assertEquals("1.1.1", violation?.wcagSc)

        // Subtree has text (should NOT flag container as missing label)
        val containerWithTextInSubtree = A11yNodeData(
            className = "android.widget.LinearLayout",
            text = "",
            contentDescription = "",
            bounds = null,
            isClickable = true,
            isFocusable = true,
            isHeading = false,
            isVisibleToUser = true,
            hasTextInSubtree = true,
            width = 200,
            height = 100,
            left = 10,
            top = 10
        )
        val containerViolations = evaluateNodeData(containerWithTextInSubtree, density, SettingsManager.LEVEL_AA)
        assertTrue(containerViolations.none { it.type == "Missing Label" })
    }

    @Test
    fun testRedundantLabelValidation() {
        // Redundant role word in description
        val redundantDescNode = A11yNodeData(
            className = "android.widget.Button",
            text = "",
            contentDescription = "Submit Button",
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
        val violations = evaluateNodeData(redundantDescNode, density, SettingsManager.LEVEL_AA)
        val violation = violations.firstOrNull { it.type == "Redundant Label" }
        assertTrue(violation != null)
        assertEquals("Info", violation?.severity)
        assertTrue(violation?.description?.contains("contains the word") == true)

        // Content description duplicates text
        val duplicateTextNode = A11yNodeData(
            className = "android.widget.Button",
            text = "Back",
            contentDescription = "Back",
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
        val dupViolations = evaluateNodeData(duplicateTextNode, density, SettingsManager.LEVEL_AA)
        val dupViolation = dupViolations.firstOrNull { it.type == "Redundant Label" }
        assertTrue(dupViolation != null)
        assertTrue(dupViolation?.description?.contains("is exactly the same as the visible text") == true)
    }

    @Test
    fun testFocusNoiseValidation() {
        // Focusable but empty node (no text/desc and no subtree text) -> Focus Noise
        val focusNoiseNode = A11yNodeData(
            className = "android.view.View",
            text = "",
            contentDescription = "",
            bounds = null,
            isClickable = false,
            isFocusable = true,
            isHeading = false,
            isVisibleToUser = true,
            hasTextInSubtree = false,
            width = 50,
            height = 50,
            left = 10,
            top = 10
        )
        val violations = evaluateNodeData(focusNoiseNode, density, SettingsManager.LEVEL_AA)
        val violation = violations.firstOrNull { it.type == "Focus Noise" }
        assertTrue(violation != null)
        assertEquals("Warning", violation?.severity)

        // Focusable but contains text in subtree -> NOT focus noise (it will read subtree contents)
        val focusWithSubtreeNode = A11yNodeData(
            className = "android.view.View",
            text = "",
            contentDescription = "",
            bounds = null,
            isClickable = false,
            isFocusable = true,
            isHeading = false,
            isVisibleToUser = true,
            hasTextInSubtree = true,
            width = 50,
            height = 50,
            left = 10,
            top = 10
        )
        val cleanViolations = evaluateNodeData(focusWithSubtreeNode, density, SettingsManager.LEVEL_AA)
        assertTrue(cleanViolations.none { it.type == "Focus Noise" })
    }

    @Test
    fun testOffscreenNodesIgnored() {
        // Node is not visible to user
        val invisibleNode = A11yNodeData(
            className = "android.widget.Button",
            text = "",
            contentDescription = "",
            bounds = null,
            isClickable = true,
            isFocusable = true,
            isHeading = false,
            isVisibleToUser = false,
            hasTextInSubtree = false,
            width = 80,
            height = 80,
            left = 10,
            top = 10
        )
        val invisibleViolations = evaluateNodeData(invisibleNode, density, SettingsManager.LEVEL_AA)
        assertTrue(invisibleViolations.isEmpty())

        // Node has 0 width or height
        val zeroSizeNode = A11yNodeData(
            className = "android.widget.Button",
            text = "",
            contentDescription = "",
            bounds = null,
            isClickable = true,
            isFocusable = true,
            isHeading = false,
            isVisibleToUser = true,
            hasTextInSubtree = false,
            width = 0,
            height = 50,
            left = 10,
            top = 10
        )
        val zeroSizeViolations = evaluateNodeData(zeroSizeNode, density, SettingsManager.LEVEL_AA)
        assertTrue(zeroSizeViolations.isEmpty())
        
        // Node is partially scrolled off screen but visible to user (e.g. left is -10, but right is 40)
        // This node should be evaluated!
        val partiallyOffscreenNode = A11yNodeData(
            className = "android.widget.Button",
            text = "",
            contentDescription = "",
            bounds = null, // left=-10, right=40 (right > 0)
            isClickable = true,
            isFocusable = true,
            isHeading = false,
            isVisibleToUser = true,
            hasTextInSubtree = false,
            width = 50,
            height = 50,
            left = -10,
            top = 10
        )
        val partViolations = evaluateNodeData(partiallyOffscreenNode, density, SettingsManager.LEVEL_AA)
        // Should trigger Missing Label violation since it is evaluated!
        assertTrue(partViolations.any { it.type == "Missing Label" })
    }
}
