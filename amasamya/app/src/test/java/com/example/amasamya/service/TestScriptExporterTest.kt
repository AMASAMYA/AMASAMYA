package com.example.amasamya.service

import com.example.amasamya.db.ElementIssue
import com.example.amasamya.utils.TestScriptExporter
import org.junit.Assert.*
import org.junit.Test

class TestScriptExporterTest {

    @Test
    fun testGenerateComposeTestClass() {
        val sampleIssues = listOf(
            ElementIssue(
                id = 1,
                sessionId = 100,
                screenName = "HomeScreen",
                className = "android.widget.ImageButton",
                bounds = "[10,10][100,100]",
                text = "",
                contentDescription = "Submit Button",
                issueType = "Target Size",
                severity = "Critical",
                description = "Touch target size 36dp is below 48dp",
                wcagSc = "2.5.5"
            ),
            ElementIssue(
                id = 2,
                sessionId = 100,
                screenName = "HomeScreen",
                className = "android.widget.ImageView",
                bounds = "[10,120][100,200]",
                text = "",
                contentDescription = "",
                issueType = "Missing Label",
                severity = "Critical",
                description = "Graphic icon missing content description",
                wcagSc = "1.1.1"
            )
        )

        val composeCode = TestScriptExporter.generateFullTestClass(
            sessionName = "Home Audit",
            packageName = "org.amasamya.accessibility",
            issues = sampleIssues,
            framework = TestScriptExporter.TestFramework.JETPACK_COMPOSE
        )

        assertNotNull(composeCode)
        assertTrue(composeCode.contains("class HomeAuditAccessibilityTest"))
        assertTrue(composeCode.contains("val composeTestRule = createComposeRule()"))
        assertTrue(composeCode.contains("assertTouchTargetMinSize(48.dp)"))
        assertTrue(composeCode.contains("onNodeWithContentDescription(\"Submit Button\")"))
    }

    @Test
    fun testGenerateEspressoTestClass() {
        val sampleIssues = listOf(
            ElementIssue(
                id = 1,
                sessionId = 100,
                screenName = "SettingsScreen",
                className = "android.widget.Button",
                bounds = "[10,10][100,100]",
                text = "Save Settings",
                contentDescription = "Save Settings",
                issueType = "Target Size",
                severity = "Warning",
                description = "Touch target size 40dp is below 48dp",
                wcagSc = "2.5.5"
            )
        )

        val espressoCode = TestScriptExporter.generateFullTestClass(
            sessionName = "Settings Audit",
            packageName = "org.amasamya.accessibility",
            issues = sampleIssues,
            framework = TestScriptExporter.TestFramework.ESPRESSO
        )

        assertNotNull(espressoCode)
        assertTrue(espressoCode.contains("class SettingsAuditAccessibilityEspressoTest"))
        assertTrue(espressoCode.contains("onView(withContentDescription(\"Save Settings\"))"))
        assertTrue(espressoCode.contains("check(matches(isDisplayed()))"))
    }
}
