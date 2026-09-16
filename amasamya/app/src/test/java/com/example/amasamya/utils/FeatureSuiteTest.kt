package com.example.amasamya.utils

import com.example.amasamya.db.ElementIssue
import com.example.amasamya.db.FocusPathNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureSuiteTest {

    @Test
    fun testAccessibilityScorecardCalculation() {
        val issues = listOf(
            ElementIssue(
                sessionId = 1,
                screenName = "TestScreen",
                className = "android.widget.Button",
                bounds = "0,0,30,30",
                text = "",
                contentDescription = "",
                issueType = "Target Size",
                severity = "Critical",
                description = "Target size too small",
                wcagSc = "2.5.5"
            ),
            ElementIssue(
                sessionId = 1,
                screenName = "TestScreen",
                className = "android.widget.ImageView",
                bounds = "0,0,50,50",
                text = "",
                contentDescription = "",
                issueType = "Missing Label",
                severity = "Warning",
                description = "Missing content description",
                wcagSc = "1.1.1"
            )
        )

        val scorecard = AccessibilityScorecard.calculateScorecard(issues = issues, totalElementsScanned = 15)
        assertEquals(1, scorecard.criticalCount)
        assertEquals(1, scorecard.warningCount)
        assertEquals(80, scorecard.scorePercent) // 100 - 15 - 5 = 80
        assertEquals("B", scorecard.grade)
    }

    @Test
    fun testUtteranceFlowEstimatorIndicLanguage() {
        val nodes = listOf(
            FocusPathNode(
                sessionId = 1,
                screenName = "MainScreen",
                focusOrder = 1,
                className = "android.widget.TextView",
                text = "नमस्ते दुनिया में आपका स्वागत है",
                contentDescription = "",
                bounds = "0,0,100,100"
            )
        )

        val report = UtteranceFlowEstimator.estimateScreenFlow(
            screenName = "MainScreen",
            issues = emptyList(),
            focusNodes = nodes,
            languageCode = "hi"
        )

        assertEquals("MainScreen", report.screenName)
        assertTrue(report.totalWordCount > 0)
        assertNotNull(report.fatigueLevel)
    }

    @Test
    fun testAudioHapticRadarTargetTypes() {
        assertNotNull(AudioHapticRadar.TargetType.CRITICAL_UNLABELLED)
        assertNotNull(AudioHapticRadar.TargetType.WARNING_SMALL_TARGET)
        assertNotNull(AudioHapticRadar.TargetType.COMPLIANT_TARGET)
    }
}
