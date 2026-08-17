package com.example.amasamya.utils

import android.content.Context
import com.example.amasamya.db.AuditSession
import com.example.amasamya.db.ElementIssue
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object VpatExporter {

    data class VpatCriterion(
        val scNumber: String,
        val scName: String,
        val wcagLevel: String,
        val gigwRule: String,
        val is17802Rule: String,
        val section508Rule: String = "Section 508 E205.4",
        val en301549Rule: String = "EN 301 549 Ch 9.1",
        val conformanceLevel: String, // "Supports", "Supports with Exceptions", "Does Not Support"
        val remarks: String
    )

    fun generateVpatMarkdown(
        session: AuditSession,
        issues: List<ElementIssue>
    ): String {
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(session.date))
        val criteriaList = evaluateCriteria(issues)
        val sb = StringBuilder()

        sb.append("# AMASAMYA Accessibility Conformance Report (VPAT 2.4 / IS 17802 / GIGW 3.0 / Section 508 / EN 301 549 Edition)\n\n")
        sb.append("**Name of Product/Application:** ").append(session.name).append("\n")
        sb.append("**Package Name / ID:** `").append(session.packageName).append("`\n")
        sb.append("**Report Date:** ").append(dateStr).append("\n")
        sb.append("**Evaluation Standards:** WCAG 2.2 AA, GIGW 3.0, IS 17802, US Section 508, EU EN 301 549\n")
        sb.append("**Evaluation Tool:** AMASAMYA Accessibility Engine (Built by Akhilesh Malani)\n\n")

        sb.append("## Executive Summary\n")
        val totalIssues = issues.size
        val criticalCount = issues.count { it.severity == "Critical" }
        val warningCount = issues.count { it.severity == "Warning" }
        if (totalIssues == 0) {
            sb.append("The application **Supports** all evaluated accessibility criteria under WCAG 2.2, GIGW 3.0, and IS 17802. Zero automated accessibility violations were detected.\n\n")
        } else {
            sb.append("The application **Supports with Exceptions** or **Does Not Support** specific criteria due to ").append(totalIssues).append(" detected compliance issues (").append(criticalCount).append(" critical violations, ").append(warningCount).append(" warnings).\n\n")
        }

        sb.append("## Detailed VPAT Conformance Matrix\n\n")
        sb.append("| Criteria (WCAG / GIGW / IS 17802) | Level | Conformance Level | Remarks and Explanations |\n")
        sb.append("| :--- | :--- | :--- | :--- |\n")

        criteriaList.forEach { c ->
            sb.append("| **").append(c.scNumber).append(" ").append(c.scName).append("**<br>*GIGW 3.0: ").append(c.gigwRule).append(" | IS 17802: ").append(c.is17802Rule).append("* | ")
                .append(c.wcagLevel).append(" | ")
                .append("**").append(c.conformanceLevel).append("** | ")
                .append(c.remarks).append(" |\n")
        }

        sb.append("\n---\n")
        sb.append("*Report generated automatically by AMASAMYA Accessibility Audit Engine*\n")
        return sb.toString()
    }

    fun generateVpatHtml(
        session: AuditSession,
        issues: List<ElementIssue>
    ): String {
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(session.date))
        val criteriaList = evaluateCriteria(issues)
        val sb = StringBuilder()

        sb.append("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <title>VPAT / ACR Report - ${session.name}</title>
                <style>
                    body { font-family: 'Segoe UI', system-ui, sans-serif; background: #0b0f19; color: #f8fafc; padding: 24px; line-height: 1.6; }
                    h1 { color: #00e5ff; border-bottom: 2px solid #2c3246; padding-bottom: 8px; }
                    h2 { color: #30d158; margin-top: 24px; }
                    table { width: 100%; border-collapse: collapse; margin-top: 16px; font-size: 14px; }
                    th, td { border: 1px solid #2c3246; padding: 10px 12px; text-align: left; vertical-align: top; }
                    th { background: #1a2234; color: #00e5ff; }
                    tr:nth-child(even) { background: #111827; }
                    .pass { color: #30d158; font-weight: bold; }
                    .warning { color: #ffb300; font-weight: bold; }
                    .fail { color: #ff3b30; font-weight: bold; }
                    .badge { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 11px; font-weight: bold; }
                </style>
            </head>
            <body>
                <h1>AMASAMYA VPAT 2.4 / ACR Conformance Report</h1>
                <p><strong>Application:</strong> ${session.name} (<code>${session.packageName}</code>)</p>
                <p><strong>Date:</strong> $dateStr</p>
                <p><strong>Standards Covered:</strong> WCAG 2.2 AA, GIGW 3.0 (India Government Guidelines), IS 17802 (BIS)</p>
                
                <h2>Compliance Matrix</h2>
                <table>
                    <thead>
                        <tr>
                            <th>Criteria</th>
                            <th>Level</th>
                            <th>Status</th>
                            <th>Remarks & Findings</th>
                        </tr>
                    </thead>
                    <tbody>
        """.trimIndent())

        criteriaList.forEach { c ->
            val statusClass = when (c.conformanceLevel) {
                "Supports" -> "pass"
                "Supports with Exceptions" -> "warning"
                else -> "fail"
            }
            sb.append("""
                <tr>
                    <td><strong>${c.scNumber} ${c.scName}</strong><br><small style="color: #94a3b8;">GIGW 3.0: ${c.gigwRule} | IS 17802: ${c.is17802Rule}</small></td>
                    <td>${c.wcagLevel}</td>
                    <td class="$statusClass">${c.conformanceLevel}</td>
                    <td>${c.remarks}</td>
                </tr>
            """.trimIndent())
        }

        sb.append("""
                    </tbody>
                </table>
            </body>
            </html>
        """.trimIndent())

        return sb.toString()
    }

    fun exportVpatFile(context: Context, session: AuditSession, issues: List<ElementIssue>, format: String): File? {
        return try {
            val dir = File(context.getExternalFilesDir(null), "vpat_reports")
            if (!dir.exists()) dir.mkdirs()

            val safeName = session.name.replace(Regex("[^a-zA-Z0-9]"), "_")
            val fileName = "VPAT_${safeName}_${session.id}.${if (format == "html") "html" else "md"}"
            val file = File(dir, fileName)

            val content = if (format == "html") generateVpatHtml(session, issues) else generateVpatMarkdown(session, issues)
            FileOutputStream(file).use { fos ->
                fos.write(content.toByteArray())
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun evaluateCriteria(issues: List<ElementIssue>): List<VpatCriterion> {
        val map = mutableListOf<VpatCriterion>()

        // 1.1.1 Non-text Content
        val labelIssues = issues.filter { it.wcagSc == "1.1.1" || it.issueType.contains("Label") }
        map.add(
            VpatCriterion(
                scNumber = "1.1.1",
                scName = "Non-text Content",
                wcagLevel = "A",
                gigwRule = "Rule 4.1 (Text Alternatives)",
                is17802Rule = "IS 17802 Sec 5.1",
                conformanceLevel = if (labelIssues.isEmpty()) "Supports" else if (labelIssues.any { it.severity == "Critical" }) "Does Not Support" else "Supports with Exceptions",
                remarks = if (labelIssues.isEmpty()) "All focusable graphics and interactive controls provide valid content descriptions." else "${labelIssues.size} element(s) lack non-text alternatives (e.g., ${labelIssues.firstOrNull()?.className ?: ""})."
            )
        )

        // 2.5.5 / 2.5.8 Touch Target Size
        val targetIssues = issues.filter { it.wcagSc == "2.5.5" || it.wcagSc == "2.5.8" || it.issueType.contains("Target") }
        map.add(
            VpatCriterion(
                scNumber = "2.5.5 / 2.5.8",
                scName = "Target Size (Minimum)",
                wcagLevel = "AA",
                gigwRule = "Rule 6.3 (Touch Accessibility)",
                is17802Rule = "IS 17802 Sec 7.2 (48dp Minimum)",
                conformanceLevel = if (targetIssues.isEmpty()) "Supports" else "Supports with Exceptions",
                remarks = if (targetIssues.isEmpty()) "All interactive controls meet or exceed the minimum 48dp target size requirement." else "${targetIssues.size} control(s) fall below the 48dp minimum dimension threshold."
            )
        )

        // 1.4.3 Contrast (Minimum)
        val contrastIssues = issues.filter { it.wcagSc == "1.4.3" || it.issueType.contains("Contrast") }
        map.add(
            VpatCriterion(
                scNumber = "1.4.3",
                scName = "Contrast (Minimum)",
                wcagLevel = "AA",
                gigwRule = "Rule 5.2 (Visual Contrast)",
                is17802Rule = "IS 17802 Sec 6.1 (4.5:1 Ratio)",
                conformanceLevel = if (contrastIssues.isEmpty()) "Supports" else "Supports with Exceptions",
                remarks = if (contrastIssues.isEmpty()) "Text and graphical indicators satisfy the 4.5:1 contrast ratio threshold." else "${contrastIssues.size} element(s) feature low contrast against background surfaces."
            )
        )

        // 2.4.3 Focus Order & Traps
        val focusIssues = issues.filter { it.wcagSc == "2.4.3" || it.issueType.contains("Focus") }
        map.add(
            VpatCriterion(
                scNumber = "2.4.3",
                scName = "Focus Order",
                wcagLevel = "A",
                gigwRule = "Rule 7.1 (Logical Navigation)",
                is17802Rule = "IS 17802 Sec 8.4",
                conformanceLevel = if (focusIssues.isEmpty()) "Supports" else if (focusIssues.any { it.severity == "Critical" }) "Does Not Support" else "Supports with Exceptions",
                remarks = if (focusIssues.isEmpty()) "Sequential screen reader focus order is logical and uninhibited." else "${focusIssues.size} focus sequence or focus trap issue(s) detected."
            )
        )

        return map
    }
}
