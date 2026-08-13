package com.example.amasamya.service

import com.example.amasamya.db.AuditSession
import com.example.amasamya.db.ElementIssue
import com.example.amasamya.utils.VpatExporter
import org.junit.Assert.*
import org.junit.Test

class VpatExporterTest {

    @Test
    fun testGenerateVpatMarkdown() {
        val sampleSession = AuditSession(
            id = 1,
            name = "Digital India Portal Audit",
            date = System.currentTimeMillis(),
            packageName = "in.gov.portal",
            deviceInfo = "Android 14 (Pixel 7)",
            wcagLevel = "AA"
        )

        val sampleIssues = listOf(
            ElementIssue(
                id = 1,
                sessionId = 1,
                screenName = "MainScreen",
                className = "android.widget.ImageButton",
                bounds = "[0,0][30,30]",
                text = "",
                contentDescription = "",
                issueType = "Missing Label",
                severity = "Critical",
                description = "Icon button lacks text alternative",
                wcagSc = "1.1.1"
            )
        )

        val md = VpatExporter.generateVpatMarkdown(sampleSession, sampleIssues)
        assertNotNull(md)
        assertTrue(md.contains("Accessibility Conformance Report"))
        assertTrue(md.contains("GIGW 3.0"))
        assertTrue(md.contains("IS 17802"))
        assertTrue(md.contains("Does Not Support"))
    }

    @Test
    fun testGenerateVpatHtml() {
        val sampleSession = AuditSession(
            id = 2,
            name = "Compliant Banking Portal",
            date = System.currentTimeMillis(),
            packageName = "com.bank.app",
            deviceInfo = "Android 14 (Pixel 7)",
            wcagLevel = "AA"
        )

        val html = VpatExporter.generateVpatHtml(sampleSession, emptyList())
        assertNotNull(html)
        assertTrue(html.contains("VPAT 2.4 / ACR Conformance Report"))
        assertTrue(html.contains("Supports"))
        assertTrue(html.contains("IS 17802"))
    }
}
