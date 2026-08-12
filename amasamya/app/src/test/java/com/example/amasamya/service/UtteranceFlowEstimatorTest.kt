package com.example.amasamya.service

import com.example.amasamya.db.ElementIssue
import com.example.amasamya.db.FocusPathNode
import com.example.amasamya.utils.UtteranceFlowEstimator
import org.junit.Assert.*
import org.junit.Test

class UtteranceFlowEstimatorTest {

    @Test
    fun testEstimateScreenFlowWithFocusNodes() {
        val focusNodes = listOf(
            FocusPathNode(
                id = 1,
                sessionId = 1,
                screenName = "CheckoutScreen",
                className = "android.widget.TextView",
                bounds = "[0,0][100,50]",
                text = "Welcome to Secure Checkout",
                contentDescription = "",
                focusOrder = 0
            ),
            FocusPathNode(
                id = 2,
                sessionId = 1,
                screenName = "CheckoutScreen",
                className = "android.widget.Button",
                bounds = "[0,60][100,110]",
                text = "Pay Now",
                contentDescription = "Pay Now $49.99 securely",
                focusOrder = 1
            )
        )

        val report = UtteranceFlowEstimator.estimateScreenFlow(
            screenName = "CheckoutScreen",
            issues = emptyList(),
            focusNodes = focusNodes
        )

        assertNotNull(report)
        assertEquals("CheckoutScreen", report.screenName)
        assertEquals(2, report.totalFocusableNodes)
        assertTrue(report.totalWordCount > 0)
        assertEquals("Low (Optimal)", report.fatigueLevel)
    }

    @Test
    fun testHighFatigueDetection() {
        val focusNodes = (1..30).map { i ->
            FocusPathNode(
                id = i.toLong(),
                sessionId = 1,
                screenName = "DenseScreen",
                className = "android.widget.TextView",
                bounds = "[0,${i * 10}][100,${i * 10 + 10}]",
                text = "Item row number $i detailed accessibility descriptive string with extra long text words",
                contentDescription = "",
                focusOrder = i - 1
            )
        }

        val report = UtteranceFlowEstimator.estimateScreenFlow(
            screenName = "DenseScreen",
            issues = emptyList(),
            focusNodes = focusNodes
        )

        assertNotNull(report)
        assertTrue(report.fatigueLevel.startsWith("High"))
        assertTrue(report.recommendations.isNotEmpty())
    }
}
