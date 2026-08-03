package com.example.amasamya.service

import com.example.amasamya.db.ElementIssue
import com.example.amasamya.settings.SettingsManager
import com.example.amasamya.utils.PersonaReportHelper
import org.junit.Assert.assertTrue
import org.junit.Test

class A11yPersonaReportHelperTest {

    @Test
    fun testDeveloperPersonaTargetSize() {
        val issue = ElementIssue(
            sessionId = 1L,
            screenName = "MainScreen",
            className = "android.widget.Button",
            bounds = "[0,0][100,50]",
            text = "Click me",
            contentDescription = "",
            issueType = "Target Size",
            severity = "Critical",
            description = "This button or interactive item is too small to tap easily (it measures only 20 by 10 screen units). It should be at least 48 by 48 units. Making it larger helps everyone.",
            wcagSc = "2.5.5"
        )
        val devDesc = PersonaReportHelper.getPersonaDescription(issue, SettingsManager.PERSONA_DEVELOPER)
        assertTrue(devDesc.contains("Touch target size 20x10dp is below WCAG recommendation"))
        assertTrue(devDesc.contains("Modifier.requiredSizeIn(minWidth = 48.dp, minHeight = 48.dp)"))
    }

    @Test
    fun testDeveloperPersonaMissingLabel() {
        val issue = ElementIssue(
            sessionId = 1L,
            screenName = "MainScreen",
            className = "android.widget.ImageView",
            bounds = "[0,0][100,100]",
            text = "",
            contentDescription = "",
            issueType = "Missing Label",
            severity = "Critical",
            description = "This button or interactive item does not have a text label or name.",
            wcagSc = "1.1.1"
        )
        val devDesc = PersonaReportHelper.getPersonaDescription(issue, SettingsManager.PERSONA_DEVELOPER)
        assertTrue(devDesc.contains("Interactive component has empty text and contentDescription"))
        assertTrue(devDesc.contains("android:contentDescription"))
    }

    @Test
    fun testTesterPersonaColorContrast() {
        val issue = ElementIssue(
            sessionId = 1L,
            screenName = "MainScreen",
            className = "android.widget.TextView",
            bounds = "[10,10][100,50]",
            text = "Submit",
            contentDescription = "",
            issueType = "Color Contrast",
            severity = "Critical",
            description = "The text is hard to read because the contrast between the text color and its background is too low. The contrast score is only 3.2 out of 10, but it should be at least 4.5 out of 10. To make it easy to read for everyone (especially those with low vision), we suggest changing the text color to #ffffff.",
            wcagSc = "1.4.3"
        )
        val testerDesc = PersonaReportHelper.getPersonaDescription(issue, SettingsManager.PERSONA_TESTER)
        assertTrue(testerDesc.contains("Low contrast text"))
        assertTrue(testerDesc.contains("simulated sunlight glare"))
    }

    @Test
    fun testDesignerPersonaFrictionWarning() {
        val issue = ElementIssue(
            sessionId = 1L,
            screenName = "MainScreen",
            className = "Screen Level Rule",
            bounds = "[0,0][0,0]",
            text = "",
            contentDescription = "",
            issueType = "Friction Warning",
            severity = "Warning",
            description = "This screen is very cluttered with 30 items that screen readers can focus on.",
            wcagSc = "2.4.3"
        )
        val designerDesc = PersonaReportHelper.getPersonaDescription(issue, SettingsManager.PERSONA_DESIGNER)
        assertTrue(designerDesc.contains("high information density (30 focus points)"))
        assertTrue(designerDesc.contains("group related texts or buttons"))
    }

    @Test
    fun testProductOwnerPersonaRedundantClickTarget() {
        val issue = ElementIssue(
            sessionId = 1L,
            screenName = "MainScreen",
            className = "Screen Level Rule",
            bounds = "[0,0][0,0]",
            text = "",
            contentDescription = "",
            issueType = "Redundant Click Target",
            severity = "Warning",
            description = "Two adjacent clickable items have the exact same label 'Edit Profile'.",
            wcagSc = "2.4.4"
        )
        val poDesc = PersonaReportHelper.getPersonaDescription(issue, SettingsManager.PERSONA_PRODUCT_OWNER)
        assertTrue(poDesc.contains("Redundant options double the time to read the screen"))
        assertTrue(poDesc.contains("Violates WCAG SC 2.4.4"))
    }
}
