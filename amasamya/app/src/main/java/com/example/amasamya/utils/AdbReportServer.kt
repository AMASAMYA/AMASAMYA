package com.example.amasamya.utils

import android.content.Context
import android.util.Log
import com.example.amasamya.db.AuditSession
import com.example.amasamya.db.DatabaseHelper
import com.example.amasamya.db.ElementIssue
import com.example.amasamya.db.FocusPathNode
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

object AdbReportServer {
    private const val TAG = "AdbReportServer"
    private const val PORT = 8080
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private var serverThread: Thread? = null

    fun start(context: Context) {
        synchronized(this) {
            if (isRunning) return
            isRunning = true
        }
        serverThread = thread(start = true, name = "AdbReportServerThread") {
            try {
                serverSocket = ServerSocket(PORT)
                Log.d(TAG, "Server started on port ${PORT}")
                while (isRunning) {
                    val socket = serverSocket?.accept() ?: break
                    thread {
                        handleConnection(context, socket)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server error", e)
            } finally {
                stop()
            }
        }
    }

    fun stop() {
        synchronized(this) {
            if (!isRunning) return
            isRunning = false
        }
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            // Ignore
        }
        serverSocket = null
        serverThread = null
        Log.d(TAG, "Server stopped")
    }

    private fun handleConnection(context: Context, socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val output = socket.getOutputStream()
            val requestLine = reader.readLine() ?: return
            Log.d(TAG, "Request: ${requestLine}")

            val parts = requestLine.split("\\s+".toRegex())
            if (parts.size < 2 || parts[0] != "GET") {
                sendResponse(output, 400, "Bad Request", "text/plain", "Only GET method is supported".toByteArray())
                return
            }

            val uri = parts[1]
            val dbHelper = DatabaseHelper(context)

            if (uri == "/" || uri == "/index.html") {
                val sessions = dbHelper.getAllSessions()
                val html = generateDashboardHtml(sessions)
                sendResponse(output, 200, "OK", "text/html; charset=utf-8", html.toByteArray(Charsets.UTF_8))
            } else if (uri.startsWith("/report/")) {
                val sessionIdStr = uri.substringAfter("/report/").substringBefore("/")
                val sessionId = sessionIdStr.toLongOrNull()
                val session = sessionId?.let { dbHelper.getSession(it) }
                if (session != null) {
                    val issues = dbHelper.getIssuesForSession(sessionId)
                    val focusNodes = dbHelper.getFocusNodesForSession(sessionId)
                    val html = generateReportHtml(context, session, issues, focusNodes)
                    sendResponse(output, 200, "OK", "text/html; charset=utf-8", html.toByteArray(Charsets.UTF_8))
                } else {
                    sendResponse(output, 404, "Not Found", "text/plain", "Session not found".toByteArray())
                }
            } else {
                sendResponse(output, 404, "Not Found", "text/plain", "Not Found".toByteArray())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling connection", e)
        } finally {
            try {
                socket.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun sendResponse(output: OutputStream, statusCode: Int, statusText: String, contentType: String, content: ByteArray) {
        val headers = "HTTP/1.1 ${statusCode} ${statusText}\r\n" +
                "Content-Type: ${contentType}\r\n" +
                "Content-Length: ${content.size}\r\n" +
                "Connection: close\r\n" +
                "\r\n"
        output.write(headers.toByteArray(Charsets.UTF_8))
        output.write(content)
        output.flush()
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun generateDashboardHtml(sessions: List<AuditSession>): String {
        val listHtml = StringBuilder()
        if (sessions.isEmpty()) {
            listHtml.append("<div class=\"no-sessions\">No compliance scan sessions saved yet. Start scanning from the app.</div>")
        } else {
            listHtml.append("<div class=\"session-grid\">")
            for (s in sessions) {
                listHtml.append("""
                    <a href="/report/${s.id}" class="session-card">
                        <div class="session-name">${s.name}</div>
                        <div class="session-meta">
                            <strong>Date:</strong> ${formatDate(s.date)}<br>
                            <strong>Package:</strong> ${s.packageName}<br>
                            <strong>Device:</strong> ${s.deviceInfo}
                        </div>
                        <span class="badge badge-cyan">Level ${s.wcagLevel}</span>
                    </a>
                """.trimIndent())
            }
            listHtml.append("</div>")
        }

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>AMASAMYA ADB Report Host</title>
                <style>
                    body {
                        background-color: #0b0e14;
                        color: #ffffff;
                        font-family: 'Segoe UI', -apple-system, BlinkMacSystemFont, Roboto, sans-serif;
                        margin: 0;
                        padding: 40px;
                    }
                    .container {
                        max-width: 1000px;
                        margin: 0 auto;
                    }
                    h1 {
                        color: #00e5ff;
                        text-shadow: 0 0 10px rgba(0, 229, 255, 0.4);
                        font-size: 2.5rem;
                        margin-bottom: 10px;
                        font-weight: 800;
                    }
                    h2 {
                        color: #d500f9;
                        margin-top: 40px;
                        margin-bottom: 20px;
                        text-shadow: 0 0 10px rgba(213, 0, 249, 0.3);
                    }
                    .card {
                        background: rgba(30, 41, 59, 0.4);
                        border: 1px solid #2c3246;
                        border-radius: 12px;
                        padding: 24px;
                        margin-bottom: 24px;
                        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
                    }
                    .session-grid {
                        display: grid;
                        grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
                        gap: 20px;
                    }
                    .session-card {
                        background: rgba(21, 27, 38, 0.6);
                        border: 1px solid #2c3246;
                        border-radius: 12px;
                        padding: 20px;
                        transition: all 0.3s ease;
                        text-decoration: none;
                        color: inherit;
                        display: block;
                    }
                    .session-card:hover {
                        border-color: #00e5ff;
                        box-shadow: 0 0 15px rgba(0, 229, 255, 0.3);
                        transform: translateY(-2px);
                    }
                    .session-name {
                        font-size: 1.25rem;
                        font-weight: bold;
                        color: #ffffff;
                        margin-bottom: 8px;
                    }
                    .session-meta {
                        font-size: 0.9rem;
                        color: #8f9cae;
                        margin-bottom: 12px;
                        line-height: 1.5;
                    }
                    .badge {
                        display: inline-block;
                        padding: 4px 8px;
                        border-radius: 4px;
                        font-size: 0.8rem;
                        font-weight: bold;
                        text-transform: uppercase;
                    }
                    .badge-cyan { background: rgba(0, 229, 255, 0.15); color: #00e5ff; border: 1px solid #00e5ff; }
                    .no-sessions {
                        text-align: center;
                        color: #8f9cae;
                        padding: 40px;
                        font-size: 1.1rem;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>AMASAMYA Accessibility Audit</h1>
                    <p style="color: #8f9cae; margin-top: 0; margin-bottom: 30px;">Local ADB Report Host</p>
                    <div class="card">
                        <p style="color: #8f9cae; margin: 0; font-size: 1.05rem; line-height: 1.6;">
                            Welcome to the local AMASAMYA audit host. Connect your device via USB/Wi-Fi and run 
                            <code style="color: #00e676; background: rgba(0,0,0,0.3); padding: 2px 6px; border-radius: 4px;">adb forward tcp:8080 tcp:8080</code> 
                            on your computer to access compliance reports and interactive focus path flow maps.
                        </p>
                    </div>
                    
                    <h2>Saved Scan Sessions</h2>
                    $listHtml
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun generateReportHtml(
        context: Context,
        session: AuditSession,
        issues: List<ElementIssue>,
        focusNodes: List<FocusPathNode>
    ): String {
        val html = StringBuilder()
        
        // CSS + Header
        html.append("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>AMASAMYA Audit: ${session.name}</title>
                <style>
                    body {
                        background-color: #0b0e14;
                        color: #ffffff;
                        font-family: 'Segoe UI', -apple-system, BlinkMacSystemFont, Roboto, sans-serif;
                        margin: 0;
                        padding: 40px;
                    }
                    .container {
                        max-width: 1000px;
                        margin: 0 auto;
                    }
                    a.back-btn {
                        color: #00e5ff;
                        text-decoration: none;
                        font-weight: bold;
                        display: inline-block;
                        margin-bottom: 20px;
                    }
                    a.back-btn:hover {
                        text-decoration: underline;
                    }
                    h1 {
                        color: #00e5ff;
                        text-shadow: 0 0 10px rgba(0, 229, 255, 0.4);
                        font-size: 2.2rem;
                        margin: 0 0 10px 0;
                    }
                    .meta-card {
                        background: rgba(30, 41, 59, 0.4);
                        border: 1px solid #2c3246;
                        border-radius: 12px;
                        padding: 24px;
                        margin-bottom: 30px;
                    }
                    .meta-grid {
                        display: grid;
                        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                        gap: 15px;
                    }
                    .meta-item strong {
                        color: #8f9cae;
                        display: block;
                        font-size: 0.85rem;
                        margin-bottom: 4px;
                    }
                    .summary-row {
                        display: flex;
                        gap: 20px;
                        margin-bottom: 30px;
                    }
                    .summary-box {
                        flex: 1;
                        background: rgba(21, 27, 38, 0.6);
                        border: 1px solid #2c3246;
                        border-radius: 12px;
                        padding: 20px;
                        text-align: center;
                    }
                    .summary-box.critical { border-color: #ff1744; box-shadow: 0 0 10px rgba(255, 23, 68, 0.15); }
                    .summary-box.warning { border-color: #ffea00; box-shadow: 0 0 10px rgba(255, 234, 0, 0.15); }
                    .summary-value {
                        font-size: 2.2rem;
                        font-weight: 800;
                        margin-bottom: 4px;
                    }
                    .summary-value.critical { color: #ff1744; }
                    .summary-value.warning { color: #ffea00; }
                    .summary-value.total { color: #00e5ff; }
                    .section-title {
                        color: #d500f9;
                        text-shadow: 0 0 10px rgba(213, 0, 249, 0.2);
                        border-bottom: 1px solid #2c3246;
                        padding-bottom: 8px;
                        margin-top: 40px;
                        margin-bottom: 20px;
                    }
                    .issue-card {
                        background: rgba(30, 41, 59, 0.3);
                        border-left: 5px solid #ff1744;
                        border-top: 1px solid #2c3246;
                        border-right: 1px solid #2c3246;
                        border-bottom: 1px solid #2c3246;
                        padding: 20px;
                        margin-bottom: 20px;
                        border-radius: 0 12px 12px 0;
                    }
                    .issue-card.warning { border-left-color: #ffea00; }
                    .issue-card.info { border-left-color: #00e5ff; }
                    .issue-header {
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        margin-bottom: 12px;
                    }
                    .badge {
                        padding: 4px 8px;
                        border-radius: 4px;
                        font-size: 0.75rem;
                        font-weight: bold;
                        text-transform: uppercase;
                    }
                    .badge-critical { background: rgba(255, 23, 68, 0.15); color: #ff1744; border: 1px solid #ff1744; }
                    .badge-warning { background: rgba(255, 234, 0, 0.15); color: #ffea00; border: 1px solid #ffea00; }
                    .badge-info { background: rgba(0, 229, 255, 0.15); color: #00e5ff; border: 1px solid #00e5ff; }
                    
                    .code-details {
                        font-family: monospace;
                        background: #07090d;
                        padding: 14px;
                        border-radius: 6px;
                        font-size: 0.85rem;
                        margin-top: 15px;
                        border: 1px solid #1a2230;
                        overflow-x: auto;
                    }
                    .tabs {
                        display: flex;
                        margin-top: 20px;
                        border-bottom: 1px solid #2c3246;
                    }
                    .tab-btn {
                        background: none;
                        border: none;
                        color: #8f9cae;
                        padding: 10px 20px;
                        cursor: pointer;
                        font-weight: bold;
                        font-size: 0.9rem;
                    }
                    .tab-btn.active {
                        color: #00e5ff;
                        border-bottom: 2px solid #00e5ff;
                    }
                    .tab-content {
                        display: none;
                    }
                    .tab-content.active {
                        display: block;
                    }
                    .btn-copy {
                        background: rgba(213, 0, 249, 0.1);
                        border: 1px solid #d500f9;
                        color: #d500f9;
                        padding: 6px 12px;
                        border-radius: 4px;
                        cursor: pointer;
                        font-size: 0.8rem;
                        font-weight: bold;
                        transition: all 0.2s ease;
                        margin-top: 15px;
                    }
                    .btn-copy:hover {
                        background: #d500f9;
                        color: #ffffff;
                    }
                </style>
                <script>
                    function switchTab(issueId, tabName) {
                        const tabs = document.querySelectorAll('.tab-btn-' + issueId);
                        const contents = document.querySelectorAll('.tab-content-' + issueId);
                        
                        tabs.forEach(t => t.classList.remove('active'));
                        contents.forEach(c => c.classList.remove('active'));
                        
                        document.getElementById('btn-' + issueId + '-' + tabName).classList.add('active');
                        document.getElementById('content-' + issueId + '-' + tabName).classList.add('active');
                    }
                    
                    function copyToClipboard(text) {
                        navigator.clipboard.writeText(text).then(function() {
                            alert('Copied Jira/GitHub bug description to clipboard!');
                        }, function(err) {
                            alert('Could not copy text: ', err);
                        });
                    }
                </script>
            </head>
            <body>
                <div class="container">
                    <a href="/" class="back-btn">← Back to Dashboard</a>
                    <h1>Audit Session: ${session.name}</h1>
                    <p style="color: #8f9cae; margin-top: 0; margin-bottom: 24px;">Compliance Scan Report</p>
                    
                    <div class="meta-card">
                        <div class="meta-grid">
                            <div class="meta-item">
                                <strong>App Package</strong>
                                ${session.packageName}
                            </div>
                            <div class="meta-item">
                                <strong>Date Audited</strong>
                                ${formatDate(session.date)}
                            </div>
                            <div class="meta-item">
                                <strong>WCAG Level</strong>
                                Level ${session.wcagLevel}
                            </div>
                            <div class="meta-item">
                                <strong>Device Info</strong>
                                ${session.deviceInfo}
                            </div>
                        </div>
                    </div>
                    
                    <div class="summary-row">
                        <div class="summary-box">
                            <div class="summary-value total">${issues.size}</div>
                            <div style="color: #8f9cae; font-size: 0.9rem;">Total Issues</div>
                        </div>
                        <div class="summary-box critical">
                            <div class="summary-value critical">${issues.count { it.severity == "Critical" }}</div>
                            <div style="color: #8f9cae; font-size: 0.9rem;">Critical</div>
                        </div>
                        <div class="summary-box warning">
                            <div class="summary-value warning">${issues.count { it.severity == "Warning" }}</div>
                            <div style="color: #8f9cae; font-size: 0.9rem;">Warnings</div>
                        </div>
                    </div>
        """.trimIndent())

        // Focus Path Flow Map
        val groupedFocusNodes = focusNodes.groupBy { it.screenName }
        val allScreens = (issues.map { it.screenName } + focusNodes.map { it.screenName }).distinct()

        if (issues.isEmpty() && focusNodes.isEmpty()) {
            html.append("<p style='color: #00e676; text-align: center; padding: 40px;'>✓ No compliance violations or focus paths detected.</p>")
        } else {
            for (screen in allScreens) {
                html.append("<h2 class=\"section-title\">Screen: $screen</h2>")

                // Embed SVG
                val screenFocusNodes = groupedFocusNodes[screen] ?: emptyList()
                if (screenFocusNodes.isNotEmpty()) {
                    html.append("<h3 style=\"color: #00e5ff; margin-bottom: 10px;\">Focus Path Flow Map</h3>")
                    html.append("<div style=\"max-width: 600px; margin-bottom: 30px;\">")
                    html.append(ReportExporter.generateFocusPathSvg(screenFocusNodes))
                    html.append("</div>")
                }

                // Show screen issues
                val screenIssues = issues.filter { it.screenName == screen }
                if (screenIssues.isEmpty()) {
                    html.append("<p style='color: #00e676; font-weight: bold;'>✓ No accessibility violations detected on this screen.</p>")
                } else {
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

                        val (composeFix, xmlFix) = getRemediationSuggestions(issue.issueType)
                        val jiraBugText = """
                            # [A11y Bug] ${issue.issueType} on ${issue.screenName}
                            **Severity:** ${issue.severity}
                            **WCAG SC:** ${issue.wcagSc}
                            **Class:** ${issue.className}
                            **Bounds:** ${issue.bounds}
                            
                            **Description:**
                            ${issue.description}
                            
                            **Remediation Suggestion:**
                            - Compose: ${composeFix.replace("\n", " ")}
                            - XML: ${xmlFix.replace("\n", " ")}
                        """.trimIndent().replace("'", "\\'")

                        html.append("""
                            <div class="${cardClass}">
                                <div class="issue-header">
                                    <div>
                                        <span class="badge ${badgeClass}">${issue.severity}</span>
                                        <strong style="margin-left: 10px; font-size: 1.1rem;">${issue.issueType}</strong>
                                    </div>
                                    <span style="color: #8f9cae; font-size: 0.9rem; font-weight: bold;">WCAG SC ${issue.wcagSc}</span>
                                </div>
                                <p style="margin: 12px 0 6px 0; color: #e2e8f0; line-height: 1.6;">${issue.description}</p>
                                
                                <div class="code-details">
                                    <strong>Element Class:</strong> ${issue.className}<br>
                                    <strong>Bounds coordinates:</strong> ${issue.bounds}<br>
                                    ${if (issue.text.isNotBlank()) "<strong>Text:</strong> \"${issue.text}\"<br>" else ""}
                                    ${if (issue.contentDescription.isNotBlank()) "<strong>Content Description:</strong> \"${issue.contentDescription}\"<br>" else ""}
                                </div>
                                
                                <div class="tabs">
                                    <button id="btn-${issue.id}-compose" class="tab-btn tab-btn-${issue.id} active" onclick="switchTab(${issue.id}, 'compose')">Jetpack Compose</button>
                                    <button id="btn-${issue.id}-xml" class="tab-btn tab-btn-${issue.id}" onclick="switchTab(${issue.id}, 'xml')">Android XML</button>
                                </div>
                                
                                <div id="content-${issue.id}-compose" class="tab-content tab-content-${issue.id} active">
                                    <pre class="code-details" style="margin-top: 10px; color: #00e676;">${escapeHtml(composeFix)}</pre>
                                </div>
                                
                                <div id="content-${issue.id}-xml" class="tab-content tab-content-${issue.id}">
                                    <pre class="code-details" style="margin-top: 10px; color: #00e676;">${escapeHtml(xmlFix)}</pre>
                                </div>
                                
                                <button class="btn-copy" onclick="copyToClipboard('${escapeJs(jiraBugText)}')">Copy Jira / GitHub Markdown Bug</button>
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
        return html.toString()
    }

    private fun escapeHtml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
    }

    private fun escapeJs(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("'", "\\'")
            .replace("\"", "\\\"")
    }

    private fun getRemediationSuggestions(issueType: String): Pair<String, String> {
        return when (issueType) {
            "Target Size" -> {
                val compose = """
                    // Ensure touch targets meet minimum dimensions (48dp x 48dp or 24dp x 24dp)
                    Modifier
                        .minimumInteractiveComponentSize() // material components default
                        .size(48.dp) // explicit size assignment
                """.trimIndent()
                val xml = """
                    <!-- Ensure minimum dimensions on interactive views -->
                    <ImageButton
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:minWidth="48dp"
                        android:minHeight="48dp"
                        android:padding="12dp" />
                """.trimIndent()
                Pair(compose, xml)
            }
            "Missing Label", "Missing Content Description" -> {
                val compose = """
                    // Provide screen reader friendly descriptions
                    Image(
                        painter = painterResource(id = R.drawable.ic_example),
                        contentDescription = "Descriptive text here" 
                    )
                    // OR 
                    Modifier.semantics {
                        contentDescription = "Descriptive text here"
                    }
                """.trimIndent()
                val xml = """
                    <!-- Always specify descriptive labels for image assets -->
                    <ImageView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:src="@drawable/ic_example"
                        android:contentDescription="Descriptive text here" />
                """.trimIndent()
                Pair(compose, xml)
            }
            "Redundant Label", "Focus Noise" -> {
                val compose = """
                    // Merge elements together or clear semantic nodes to reduce screen reader clutter
                    Modifier.clearAndSetSemantics { } // silences element entirely
                    // OR
                    Modifier.semantics(mergeDescendants = true) {
                        // groups descendants for a single unified read-out
                    }
                """.trimIndent()
                val xml = """
                    <!-- Mark purely decorative or noise elements as unimportant -->
                    <ImageView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:importantForAccessibility="no" />
                """.trimIndent()
                Pair(compose, xml)
            }
            else -> {
                val compose = """
                    // Define custom semantic properties to resolve issues
                    Modifier.semantics {
                        // customize accessibility attributes
                    }
                """.trimIndent()
                val xml = """
                    <!-- Mark views as focusable / accessible -->
                    <View
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:focusable="true" />
                """.trimIndent()
                Pair(compose, xml)
            }
        }
    }
}
