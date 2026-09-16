package com.example.amasamya.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfRemediationTest {

    @Test
    fun testPdfRemediationEngineAnalysis() {
        val sampleText = """
        AMASAMYA PDF Guidelines
        Section 1: Mobile Accessibility
        [Figure] Architecture Diagram
        Buttons must measure 48x48dp.
        """.trimIndent()

        val report = PdfRemediationEngine.analyzeDocument("Sample.pdf", sampleText)

        assertEquals("Sample.pdf", report.documentName)
        assertTrue(report.totalNodes > 0)
        assertTrue(report.tagNodes.any { it.tagType == "H1" })
        assertTrue(report.tagNodes.any { it.tagType == "Figure" })

        val html = PdfRemediationEngine.generateRemediatedHtml(report)
        assertNotNull(html)
        assertTrue(html.contains("<html"))
        assertTrue(html.contains("<h1>AMASAMYA PDF Guidelines</h1>"))
    }
}
