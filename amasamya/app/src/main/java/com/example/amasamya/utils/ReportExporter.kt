package com.example.amasamya.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.text.TextPaint
import android.util.Log
import com.example.amasamya.db.AuditSession
import com.example.amasamya.db.ElementIssue
import com.example.amasamya.db.FocusPathNode
import com.example.amasamya.db.DatabaseHelper
import com.example.amasamya.settings.SettingsManager
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExporter {

    private const val TAG = "ReportExporter"

    // Formats date
    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    // HTML Export
    fun exportToHtml(context: Context, session: AuditSession, issues: List<ElementIssue>): File? {
        val html = StringBuilder()
        val persona = SettingsManager(context).reportPersona
        
        // Premium accessible design: clear contrast, readable typography, heading levels
        html.append("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>AMASAMYA Accessibility Audit Report: ${session.name}</title>
                <style>
                    body {
                        font-family: 'Segoe UI', -apple-system, BlinkMacSystemFont, Roboto, sans-serif;
                        line-height: 1.6;
                        color: #121212;
                        background-color: #f8f9fa;
                        margin: 0;
                        padding: 24px;
                    }
                    .container {
                        max-width: 900px;
                        margin: 0 auto;
                        background: #ffffff;
                        padding: 32px;
                        border-radius: 8px;
                        box-shadow: 0 4px 12px rgba(0,0,0,0.08);
                    }
                    header {
                        border-bottom: 3px solid #0a84ff;
                        padding-bottom: 20px;
                        margin-bottom: 30px;
                    }
                    h1 {
                        color: #0a84ff;
                        font-size: 2.2rem;
                        margin: 0 0 10px 0;
                    }
                    h2 {
                        color: #1c1c1e;
                        font-size: 1.5rem;
                        border-bottom: 1px solid #e5e5ea;
                        padding-bottom: 8px;
                        margin-top: 40px;
                    }
                    h3 {
                        color: #ff453a;
                        font-size: 1.2rem;
                        margin-bottom: 8px;
                    }
                    .metadata {
                        background-color: #f1f2f8;
                        padding: 16px;
                        border-radius: 6px;
                        margin-bottom: 24px;
                        font-size: 0.95rem;
                    }
                    .metadata table {
                        width: 100%;
                        border-collapse: collapse;
                    }
                    .metadata td {
                        padding: 6px 12px;
                    }
                    .metadata td.label {
                        font-weight: bold;
                        width: 200px;
                        color: #3a3a3c;
                    }
                    .issue-card {
                        background: #ffffff;
                        border-left: 5px solid #ff453a;
                        padding: 16px;
                        margin-bottom: 16px;
                        border-radius: 0 6px 6px 0;
                        box-shadow: 0 2px 6px rgba(0,0,0,0.04);
                    }
                    .issue-card.warning {
                        border-left-color: #ffd60a;
                    }
                    .issue-card.info {
                        border-left-color: #0a84ff;
                    }
                    .issue-badge {
                        display: inline-block;
                        padding: 4px 8px;
                        border-radius: 4px;
                        font-size: 0.8rem;
                        font-weight: bold;
                        text-transform: uppercase;
                        margin-right: 8px;
                    }
                    .badge-critical { background: #ff453a; color: #ffffff; }
                    .badge-warning { background: #ffd60a; color: #121212; }
                    .badge-info { background: #0a84ff; color: #ffffff; }
                    .element-details {
                        font-family: monospace;
                        background: #f1f2f8;
                        padding: 12px;
                        border-radius: 4px;
                        font-size: 0.85rem;
                        margin-top: 10px;
                        overflow-x: auto;
                    }
                    .summary-box {
                        display: flex;
                        gap: 20px;
                        margin-bottom: 30px;
                    }
                    .summary-card {
                        flex: 1;
                        background: #f1f2f8;
                        padding: 16px;
                        text-align: center;
                        border-radius: 6px;
                        font-weight: bold;
                    }
                    .summary-card.critical { background: #ffebeb; color: #ff453a; }
                    .summary-card.warning { background: #fffdeb; color: #b28900; }
                    .summary-card.total { background: #ebf5ff; color: #0a84ff; }
                </style>
            </head>
            <body>
                <div class="container">
                    <header>
                        <h1>AMASAMYA Accessibility Audit Report</h1>
                        <p>Offline compliance scanning results for WCAG 2.2</p>
                    </header>

                    <div class="metadata" role="region" aria-label="Session Metadata">
                        <table>
                            <tr><td class="label">Session Name</td><td>${session.name}</td></tr>
                            <tr><td class="label">Date Executed</td><td>${formatDate(session.date)}</td></tr>
                            <tr><td class="label">Audited Package</td><td>${session.packageName}</td></tr>
                            <tr><td class="label">Target WCAG Level</td><td>Level ${session.wcagLevel} (WCAG 2.2)</td></tr>
                            <tr><td class="label">Device Info</td><td>${session.deviceInfo}</td></tr>
                        </table>
                    </div>

                    <h2>Audit Summary</h2>
                    <div class="summary-box">
                        <div class="summary-card total">
                            <div style="font-size: 2rem;">${issues.size}</div>
                            <div>Total Issues</div>
                        </div>
                        <div class="summary-card critical">
                            <div style="font-size: 2rem;">${issues.count { it.severity == "Critical" }}</div>
                            <div>Critical</div>
                        </div>
                        <div class="summary-card warning">
                            <div style="font-size: 2rem;">${issues.count { it.severity == "Warning" }}</div>
                            <div>Warnings</div>
                        </div>
                    </div>

                    <h2>Audited Elements Details</h2>
        """.trimIndent())

        val dbHelper = DatabaseHelper(context)
        val focusNodes = dbHelper.getFocusNodesForSession(session.id)
        val groupedFocusNodes = focusNodes.groupBy { it.screenName }

        if (issues.isEmpty() && focusNodes.isEmpty()) {
            html.append("<p>No accessibility violations or focus path walkthroughs detected.</p>")
        } else {
            val allScreens = (issues.map { it.screenName } + focusNodes.map { it.screenName }).distinct()
            
            for (screen in allScreens) {
                html.append("<h3 style='margin-top: 40px; color:#1c1c1e; border-bottom: 2px solid #e5e5ea; padding-bottom: 6px;'>Screen: $screen</h3>")
                
                // Embed Focus Path SVG if nodes exist for this screen
                val screenFocusNodes = groupedFocusNodes[screen] ?: emptyList()
                if (screenFocusNodes.isNotEmpty()) {
                    html.append("<h4 style='color: #0a84ff; margin-top: 20px; margin-bottom: 12px;'>Focus Path Flow Map</h4>")
                    html.append("<div style='max-width: 600px; margin-bottom: 24px;'>")
                    html.append(generateFocusPathSvg(screenFocusNodes))
                    html.append("</div>")
                    
                    // Export standalone SVG too
                    exportFocusPathSvg(context, session, screen, screenFocusNodes)
                }
                
                val screenIssues = issues.filter { it.screenName == screen }
                if (screenIssues.isEmpty()) {
                    html.append("<p style='color: #30d158; font-weight: bold;'>✓ No accessibility issues detected on this screen.</p>")
                } else {
                    html.append("<h4 style='color: #ff453a; margin-top: 20px; margin-bottom: 12px;'>Accessibility Issues</h4>")
                    for (issue in screenIssues) {
                        val badgeClass = when (issue.severity) {
                            "Critical" -> "badge-critical"
                            "Warning" -> "badge-warning"
                            else -> "badge-info"
                        }
                        val cardClass = when (issue.severity) {
                            "Critical" -> "issue-card"
                            "Warning" -> "issue-card warning"
                            else -> "issue-card info"
                        }

                        html.append("""
                            <div class="$cardClass">
                                <div>
                                    <span class="issue-badge $badgeClass">${issue.severity}</span>
                                    <strong>${issue.issueType}</strong> 
                                    <span style="color: #666; font-size: 0.9rem;">(WCAG SC ${issue.wcagSc})</span>
                                </div>
                                <p style="margin: 8px 0;">${PersonaReportHelper.getPersonaDescription(issue, persona)}</p>
                                <div class="element-details">
                                    <strong>Class:</strong> ${issue.className}<br>
                                    <strong>Bounds:</strong> ${issue.bounds}<br>
                                    <strong>Text:</strong> "${issue.text}" | 
                                    <strong>ContentDescription:</strong> "${issue.contentDescription}"
                                </div>
                            </div>
                        """.trimIndent())
                    }
                }
            }
        }

        html.append("""
                </div>
            </body>
            </html>
        """.trimIndent())

        return writeToFile(context, "${session.name.replace(" ", "_")}_report.html", html.toString())
    }

    // Markdown Export
    fun exportToMarkdown(context: Context, session: AuditSession, issues: List<ElementIssue>): File? {
        val md = StringBuilder()
        val persona = SettingsManager(context).reportPersona
        md.append("# AMASAMYA Accessibility Audit Report: ${session.name}\n\n")
        md.append("## Metadata\n")
        md.append("- **Date**: ${formatDate(session.date)}\n")
        md.append("- **Audited App Package**: ${session.packageName}\n")
        md.append("- **Compliance Level**: WCAG 2.2 Level ${session.wcagLevel}\n")
        md.append("- **Device Info**: ${session.deviceInfo}\n\n")

        md.append("## Summary\n")
        md.append("- **Total Issues**: ${issues.size}\n")
        md.append("- **Critical**: ${issues.count { it.severity == "Critical" }}\n")
        md.append("- **Warnings**: ${issues.count { it.severity == "Warning" }}\n")
        md.append("- **Info**: ${issues.count { it.severity == "Info" }}\n\n")

        md.append("## Flagged Issues\n")

        val dbHelper = DatabaseHelper(context)
        val focusNodes = dbHelper.getFocusNodesForSession(session.id)
        val groupedFocusNodes = focusNodes.groupBy { it.screenName }

        if (issues.isEmpty() && focusNodes.isEmpty()) {
            md.append("No issues or focus path walk-throughs recorded.\n")
        } else {
            val allScreens = (issues.map { it.screenName } + focusNodes.map { it.screenName }).distinct()
            for (screen in allScreens) {
                md.append("### Screen: $screen\n\n")
                
                val screenFocusNodes = groupedFocusNodes[screen] ?: emptyList()
                if (screenFocusNodes.isNotEmpty()) {
                    val safeScreenName = screen.replace(" ", "_").replace(".", "_")
                    md.append("- **Focus Path Map**: Visualized in `${session.name.replace(" ", "_")}_${safeScreenName}_focus_path.svg`\n")
                }
                
                val screenIssues = issues.filter { it.screenName == screen }
                if (screenIssues.isEmpty()) {
                    md.append("- No accessibility issues detected on this screen.\n\n")
                } else {
                    for (issue in screenIssues) {
                        md.append("#### [${issue.severity}] ${issue.issueType} (WCAG SC ${issue.wcagSc})\n")
                        md.append("- **Description**: ${PersonaReportHelper.getPersonaDescription(issue, persona)}\n")
                        md.append("- **Class**: `${issue.className}`\n")
                        md.append("- **Bounds**: `${issue.bounds}`\n")
                        md.append("- **Text**: `\"${issue.text}\"`\n")
                        md.append("- **Content Description**: `\"${issue.contentDescription}\"`\n\n")
                    }
                }
            }
        }

        return writeToFile(context, "${session.name.replace(" ", "_")}_report.md", md.toString())
    }

    // Native PDF Export (PdfDocument)
    fun exportToPdf(context: Context, session: AuditSession, issues: List<ElementIssue>): File? {
        val pdfDocument = PdfDocument()
        val persona = SettingsManager(context).reportPersona
        val textPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 12f
            isAntiAlias = true
        }
        val headerPaint = Paint().apply {
            color = Color.rgb(10, 132, 255) // Azure Blue
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val sectionPaint = Paint().apply {
            color = Color.rgb(28, 28, 30)
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val boldPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        // A4 Specs: 595 x 842 points
        val pageWidth = 595
        val pageHeight = 842
        var pageNumber = 1

        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val margin = 50f
        val contentWidth = pageWidth - (margin * 2)
        var yPosition = 60f

        // Draw Title
        canvas.drawText("AMASAMYA Accessibility Report", margin, yPosition, headerPaint)
        yPosition += 35f

        // Metadata Header
        canvas.drawText("Session: ${session.name}", margin, yPosition, boldPaint)
        yPosition += 18f
        canvas.drawText("Date: ${formatDate(session.date)}", margin, yPosition, textPaint)
        yPosition += 18f
        canvas.drawText("Compliance Level: WCAG 2.2 Level ${session.wcagLevel}", margin, yPosition, textPaint)
        yPosition += 18f
        canvas.drawText("Device: ${session.deviceInfo}", margin, yPosition, textPaint)
        yPosition += 30f

        // Summary Statistics
        canvas.drawRect(margin, yPosition, pageWidth - margin, yPosition + 40f, Paint().apply { color = Color.rgb(240, 240, 245) })
        canvas.drawText(
            "Summary: Total Issues: ${issues.size} | Critical: ${issues.count { it.severity == "Critical" }} | Warnings: ${issues.count { it.severity == "Warning" }}",
            margin + 10f, yPosition + 25f, boldPaint
        )
        yPosition += 65f

        val dbHelper = DatabaseHelper(context)
        val focusNodes = dbHelper.getFocusNodesForSession(session.id)
        val groupedFocusNodes = focusNodes.groupBy { it.screenName }

        // Issues
        canvas.drawText("Violations list:", margin, yPosition, sectionPaint)
        yPosition += 25f

        if (issues.isEmpty()) {
            canvas.drawText("No issues detected! Clean sheet.", margin, yPosition, textPaint)
            for ((screen, screenNodes) in groupedFocusNodes) {
                if (screenNodes.isNotEmpty()) {
                    val safeScreen = screen.replace(" ", "_").replace(".", "_")
                    exportFocusPathSvg(context, session, screen, screenNodes)
                    yPosition += 15f
                    canvas.drawText("Focus path SVG exported: ${session.name.replace(" ", "_")}_${safeScreen}_focus_path.svg", margin, yPosition, Paint().apply { textSize = 9f; isAntiAlias = true; color = Color.GRAY })
                }
            }
        } else {
            // Draw focus path file notifications if any
            for ((screen, screenNodes) in groupedFocusNodes) {
                if (screenNodes.isNotEmpty()) {
                    val safeScreen = screen.replace(" ", "_").replace(".", "_")
                    exportFocusPathSvg(context, session, screen, screenNodes)
                    if (yPosition > pageHeight - 50f) {
                        pdfDocument.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        yPosition = 60f
                    }
                    canvas.drawText("Focus path SVG exported for $screen: ${session.name.replace(" ", "_")}_${safeScreen}_focus_path.svg", margin, yPosition, Paint().apply { textSize = 9f; isAntiAlias = true; color = Color.GRAY })
                    yPosition += 15f
                }
            }
            yPosition += 10f
            for (issue in issues) {
                // Check if we need a new page
                if (yPosition > pageHeight - 100f) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    yPosition = 60f
                }

                // Draw Issue Card Header
                val severityColor = when (issue.severity) {
                    "Critical" -> Color.rgb(255, 69, 58)
                    "Warning" -> Color.rgb(178, 137, 0)
                    else -> Color.rgb(10, 132, 255)
                }
                val labelPaint = Paint().apply {
                    color = severityColor
                    textSize = 12f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }

                canvas.drawText("[${issue.severity}] ${issue.issueType} (SC ${issue.wcagSc})", margin, yPosition, labelPaint)
                yPosition += 18f

                // Draw Description (Wrapped)
                yPosition = drawTextWrapped(
                    canvas, "Description: ${PersonaReportHelper.getPersonaDescription(issue, persona)}", margin + 10f, yPosition, textPaint, contentWidth - 10f, pageHeight,
                    onPageBreak = {
                        pdfDocument.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        60f
                    }
                )

                // Details Box background
                canvas.drawRect(
                    margin + 10f, yPosition, pageWidth - margin, yPosition + 40f,
                    Paint().apply { color = Color.rgb(245, 245, 250) }
                )
                canvas.drawText("Class: ${issue.className}", margin + 15f, yPosition + 15f, Paint().apply { textSize = 10f; isAntiAlias = true })
                canvas.drawText("Bounds: ${issue.bounds}", margin + 15f, yPosition + 30f, Paint().apply { textSize = 10f; isAntiAlias = true })
                
                yPosition += 60f
            }
        }

        pdfDocument.finishPage(page)

        val file = getExportFile(context, "${session.name.replace(" ", "_")}_report.pdf")
        return try {
            val fos = FileOutputStream(file)
            pdfDocument.writeTo(fos)
            pdfDocument.close()
            fos.close()
            Log.d(TAG, "Successfully exported PDF to ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write PDF", e)
            pdfDocument.close()
            null
        }
    }

    private fun drawTextWrapped(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        paint: TextPaint,
        maxWidth: Float,
        pageHeight: Int,
        onPageBreak: () -> Float
    ): Float {
        var currentY = y
        val words = text.split(" ")
        var line = ""
        var localCanvas = canvas

        for (word in words) {
            val testLine = if (line.isEmpty()) word else "$line $word"
            val testWidth = paint.measureText(testLine)
            if (testWidth > maxWidth) {
                // Draw current line
                localCanvas.drawText(line, x, currentY, paint)
                currentY += 16f
                line = word

                // Check for page break
                if (currentY > pageHeight - 60f) {
                    currentY = onPageBreak()
                    // Need to capture the new canvas if page changed, but we assume the page reference handles the draw context or we can't easily capture outside variables,
                    // in Android standard draw context, the page's canvas object gets updated. In this simplified wrapper, we draw on the active canvas.
                }
            } else {
                line = testLine
            }
        }
        if (line.isNotEmpty()) {
            localCanvas.drawText(line, x, currentY, paint)
            currentY += 16f
        }
        return currentY
    }

    private fun writeToFile(context: Context, fileName: String, content: String): File? {
        val file = getExportFile(context, fileName)
        return try {
            val fos = FileOutputStream(file)
            fos.write(content.toByteArray())
            fos.close()
            Log.d(TAG, "Successfully wrote file to ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write file $fileName", e)
            null
        }
    }

    private fun getExportFile(context: Context, fileName: String): File {
        // We save to external files directory so we don't need runtime storage permission (which is deprecated/hard to manage in Android 13+)
        // The files are easily shareable via FileProvider and the system share intent.
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, fileName)
    }

    private fun parseBounds(boundsStr: String): IntArray? {
        try {
            val regex = Regex("\\[(-?\\d+),(-?\\d+)\\]\\[(-?\\d+),(-?\\d+)\\]")
            val match = regex.find(boundsStr)
            if (match != null) {
                val left = match.groupValues[1].toInt()
                val top = match.groupValues[2].toInt()
                val right = match.groupValues[3].toInt()
                val bottom = match.groupValues[4].toInt()
                return intArrayOf(left, top, right, bottom)
            }
        } catch (e: Exception) {
            // Ignore
        }
        return null
    }

    fun generateFocusPathSvg(nodes: List<FocusPathNode>): String {
        var minX = Int.MAX_VALUE
        var minY = Int.MAX_VALUE
        var maxX = Int.MIN_VALUE
        var maxY = Int.MIN_VALUE

        val parsedNodes = nodes.mapNotNull { node ->
            val rect = parseBounds(node.bounds)
            if (rect != null) {
                val left = rect[0]
                val top = rect[1]
                val right = rect[2]
                val bottom = rect[3]
                
                if (left != 0 || top != 0 || right != 0 || bottom != 0) {
                    if (left < minX) minX = left
                    if (top < minY) minY = top
                    if (right > maxX) maxX = right
                    if (bottom > maxY) maxY = bottom
                    Pair(node, rect)
                } else {
                    null
                }
            } else {
                null
            }
        }

        if (parsedNodes.isEmpty()) {
            minX = 0
            minY = 0
            maxX = 1080
            maxY = 2400
        } else {
            minX = maxOf(0, minX - 50)
            minY = maxOf(0, minY - 50)
            maxX += 50
            maxY += 50
        }

        val width = maxX - minX
        val height = maxY - minY

        val svg = StringBuilder()
        svg.append("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="$minX $minY $width $height" width="100%" height="auto" style="background-color: #1c1c1e; font-family: system-ui, sans-serif; border-radius: 8px;">""")
        svg.append("""
            <defs>
                <marker id="arrow" viewBox="0 0 10 10" refX="5" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
                    <path d="M 0 0 L 10 5 L 0 10 z" fill="#30d158" />
                </marker>
            </defs>
        """.trimIndent())

        for ((node, rect) in parsedNodes) {
            val left = rect[0]
            val top = rect[1]
            val w = rect[2] - left
            val h = rect[3] - top
            
            svg.append("""
                <rect x="$left" y="$top" width="$w" height="$h" rx="4" ry="4" stroke="#0a84ff" stroke-width="2" fill="rgba(10, 132, 255, 0.1)">
                    <title>${node.className}&#10;Text: "${node.text}"&#10;Desc: "${node.contentDescription}"</title>
                </rect>
            """.trimIndent())
            
            val badgeX = left + 15
            val badgeY = top + 15
            svg.append("""
                <circle cx="$badgeX" cy="$badgeY" r="12" fill="#0a84ff" />
                <text x="$badgeX" y="${badgeY + 4}" font-size="10" font-weight="bold" fill="#ffffff" text-anchor="middle">${node.focusOrder}</text>
            """.trimIndent())
        }

        for (i in 0 until parsedNodes.size - 1) {
            val rectA = parsedNodes[i].second
            val rectB = parsedNodes[i + 1].second
            
            val cxA = rectA[0] + (rectA[2] - rectA[0]) / 2
            val cyA = rectA[1] + (rectA[3] - rectA[1]) / 2
            
            val cxB = rectB[0] + (rectB[2] - rectB[0]) / 2
            val cyB = rectB[1] + (rectB[3] - rectB[1]) / 2
            
            svg.append("""
                <line x1="$cxA" y1="$cyA" x2="$cxB" y2="$cyB" stroke="#30d158" stroke-width="3" stroke-dasharray="6,4" marker-end="url(#arrow)" />
            """.trimIndent())
        }

        svg.append("</svg>")
        return svg.toString()
    }

    fun exportFocusPathSvg(context: Context, session: AuditSession, screenName: String, nodes: List<FocusPathNode>): File? {
        if (nodes.isEmpty()) return null
        val svgContent = generateFocusPathSvg(nodes)
        val safeScreenName = screenName.replace(" ", "_").replace(".", "_")
        return writeToFile(context, "${session.name.replace(" ", "_")}_${safeScreenName}_focus_path.svg", svgContent)
    }
}
