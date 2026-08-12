package com.example.amasamya.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.amasamya.db.DatabaseHelper
import com.example.amasamya.db.AuditSession
import com.example.amasamya.db.ElementIssue
import com.example.amasamya.utils.ReportExporter
import com.example.amasamya.utils.PersonaReportHelper
import com.example.amasamya.settings.SettingsManager
import com.example.amasamya.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailScreen(
    sessionId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToFocusPath: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }
    val settingsManager = remember { SettingsManager(context) }

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        val ttsInstance = TextToSpeech(context) { status -> }
        tts = ttsInstance
        onDispose {
            ttsInstance.stop()
            ttsInstance.shutdown()
        }
    }

    var session by remember { mutableStateOf<AuditSession?>(null) }
    var issues by remember { mutableStateOf<List<ElementIssue>>(emptyList()) }
    var screensWithFocusPaths by remember { mutableStateOf<List<String>>(emptyList()) }
    var focusNodes by remember { mutableStateOf<List<com.example.amasamya.db.FocusPathNode>>(emptyList()) }
    var selectedPersona by remember { mutableStateOf(settingsManager.reportPersona) }
    var isPersonaDropdownExpanded by remember { mutableStateOf(false) }
    var showTestExporterDialog by remember { mutableStateOf(false) }
    var selectedTestFramework by remember { mutableStateOf(com.example.amasamya.utils.TestScriptExporter.TestFramework.JETPACK_COMPOSE) }

    LaunchedEffect(sessionId) {
        session = dbHelper.getSession(sessionId)
        issues = dbHelper.getIssuesForSession(sessionId)
        val allFocusNodes = dbHelper.getFocusNodesForSession(sessionId)
        focusNodes = allFocusNodes
        screensWithFocusPaths = allFocusNodes.map { it.screenName }.distinct()
        
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
        if (manager.isEnabled) {
            try {
                val event = android.view.accessibility.AccessibilityEvent.obtain(
                    android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT
                )
                event.text.add("Report details screen loaded successfully")
                event.className = "com.example.amasamya.ui.screens.ReportDetailScreen"
                event.packageName = context.packageName
                manager.sendAccessibilityEvent(event)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val backgroundBrush = remember {
        Brush.verticalGradient(
            colors = listOf(DeepSpace, MidnightBlue)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = session?.name ?: "Report Details",
                        fontWeight = FontWeight.Bold,
                        color = PureWhite,
                        modifier = Modifier.semantics { heading() }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back to history",
                            tint = PureWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent,
        modifier = modifier.background(backgroundBrush)
    ) { paddingValues ->
        if (session == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = VibrantCyan)
            }
        } else {
            val activeSession = session!!
            val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
            val formattedDate = remember(activeSession.date) { sdf.format(Date(activeSession.date)) }

            // Get all unique screens
            val allScreens = remember(issues, screensWithFocusPaths) {
                (issues.map { it.screenName } + screensWithFocusPaths).distinct()
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Session Info Card
                item {
                    Surface(
                        color = GlassySurface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF2C3246)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Session Info",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = VibrantCyan,
                                    modifier = Modifier.semantics { heading() }
                                )
                                Button(
                                    onClick = { speakSummary(tts, activeSession.name, issues) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = VibrantCyan,
                                        contentColor = DeepSpace
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.semantics {
                                        role = Role.Button
                                        contentDescription = "Read screen compliance report summary aloud"
                                    }
                                ) {
                                    Text("Read Summary", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(text = "App Package: ${activeSession.packageName}", color = PureWhite, fontSize = 14.sp)
                            Text(text = "WCAG Level: Level ${activeSession.wcagLevel} (WCAG 2.2)", color = PureWhite, fontSize = 14.sp)
                            Text(text = "Device Info: ${activeSession.deviceInfo}", color = PureWhite, fontSize = 14.sp)
                            Text(text = "Audit Date: $formattedDate", color = PureWhite, fontSize = 14.sp)
                        }
                    }
                }

                // Horizontal Metric Cards Grid
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Metric Total
                        Surface(
                            color = GlassySurface,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, VibrantCyan.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Issues", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("${issues.size}", fontSize = 24.sp, color = VibrantCyan, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        // Metric Critical
                        Surface(
                            color = GlassySurface,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, NeonRed.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Critical", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("${issues.count { it.severity == "Critical" }}", fontSize = 24.sp, color = NeonRed, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        // Metric Warnings
                        Surface(
                            color = GlassySurface,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Warnings", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("${issues.count { it.severity == "Warning" }}", fontSize = 24.sp, color = AmberGold, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }

                // Persona Selection Dropdown Card
                item {
                    Surface(
                        color = GlassySurface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF2C3246)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Report Target Audience",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = VibrantCyan,
                                modifier = Modifier.semantics { heading() }
                            )
                            Text(
                                text = "Choose a persona to dynamically tailor the compliance descriptions and exported documents to their perspective.",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            ExposedDropdownMenuBox(
                                expanded = isPersonaDropdownExpanded,
                                onExpandedChange = { isPersonaDropdownExpanded = !isPersonaDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = when (selectedPersona) {
                                        SettingsManager.PERSONA_DEVELOPER -> "Developer (Code remediation)"
                                        SettingsManager.PERSONA_TESTER -> "Tester (Steps to reproduce)"
                                        SettingsManager.PERSONA_DESIGNER -> "Designer (Visual & spacing)"
                                        SettingsManager.PERSONA_PRODUCT_OWNER -> "Product Owner (Compliance & risk)"
                                        else -> "General User (Simple language)"
                                    },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Selected Persona", color = LightGrey) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPersonaDropdownExpanded) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = VibrantCyan,
                                        unfocusedBorderColor = LightGrey.copy(alpha = 0.5f),
                                        focusedLabelColor = VibrantCyan,
                                        unfocusedLabelColor = LightGrey.copy(alpha = 0.7f),
                                        focusedTextColor = PureWhite,
                                        unfocusedTextColor = PureWhite
                                    ),
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                        .semantics(mergeDescendants = true) {
                                            role = Role.Button
                                            stateDescription = if (isPersonaDropdownExpanded) "Expanded" else "Collapsed"
                                        }
                                )
                                
                                ExposedDropdownMenu(
                                    expanded = isPersonaDropdownExpanded,
                                    onDismissRequest = { isPersonaDropdownExpanded = false }
                                ) {
                                    val personas = listOf(
                                        SettingsManager.PERSONA_GENERAL_USER to "General User (Simple language)",
                                        SettingsManager.PERSONA_DEVELOPER to "Developer (Code remediation)",
                                        SettingsManager.PERSONA_TESTER to "Tester (Steps to reproduce)",
                                        SettingsManager.PERSONA_DESIGNER to "Designer (Visual & spacing)",
                                        SettingsManager.PERSONA_PRODUCT_OWNER to "Product Owner (Compliance & risk)"
                                    )
                                    personas.forEach { (persona, label) ->
                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            onClick = {
                                                selectedPersona = persona
                                                settingsManager.reportPersona = persona
                                                isPersonaDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Export Options Section
                item {
                    Surface(
                        color = GlassySurface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF2C3246)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Export & Share Report",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = VibrantCyan,
                                modifier = Modifier.semantics { heading() }
                            )
                            
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // PDF Button
                                Button(
                                    onClick = {
                                        val file = ReportExporter.exportToPdf(context, activeSession, issues)
                                        if (file != null) {
                                            shareReportFile(context, file, "application/pdf")
                                        } else {
                                            Toast.makeText(context, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = VibrantCyan,
                                        contentColor = DeepSpace
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).semantics {
                                        contentDescription = "Export and share report as PDF"
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("PDF", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }

                                // HTML Button
                                Button(
                                    onClick = {
                                        val file = ReportExporter.exportToHtml(context, activeSession, issues)
                                        if (file != null) {
                                            shareReportFile(context, file, "text/html")
                                        } else {
                                            Toast.makeText(context, "Failed to generate HTML", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = VibrantCyan,
                                        contentColor = DeepSpace
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).semantics {
                                        contentDescription = "Export and share report as HTML"
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("HTML", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }

                                // Markdown Button
                                Button(
                                    onClick = {
                                        val file = ReportExporter.exportToMarkdown(context, activeSession, issues)
                                        if (file != null) {
                                            shareReportFile(context, file, "text/plain")
                                        } else {
                                            Toast.makeText(context, "Failed to generate Markdown", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = VibrantCyan,
                                        contentColor = DeepSpace
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).semantics {
                                        contentDescription = "Export and share report as Markdown"
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("MD", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Automated UI Test Exporter Button
                            Button(
                                onClick = { showTestExporterDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AmberGold,
                                    contentColor = DeepSpace
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics {
                                        contentDescription = "Export automated Compose or Espresso UI test script"
                                    }
                            ) {
                                Text("⚡ Export Automated UI Test Script", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Issues List grouped by Screen with Focus Path button
                if (allScreens.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Zero accessibility violations found on this session!\nMatches all WCAG Level ${activeSession.wcagLevel} compliance checks.",
                                color = NeonGreen,
                                textAlign = TextAlign.Center,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 22.sp
                            )
                        }
                    }
                } else {
                    allScreens.forEach { screenName ->
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp, bottom = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Screen: $screenName",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PureWhite,
                                        modifier = Modifier.weight(1f).semantics { heading() }
                                    )
                                    
                                    if (screensWithFocusPaths.contains(screenName)) {
                                        TextButton(
                                            onClick = {
                                                onNavigateToFocusPath(screenName)
                                            },
                                            colors = ButtonDefaults.textButtonColors(contentColor = VibrantCyan),
                                            modifier = Modifier.semantics {
                                                contentDescription = "View Focus Path Map for screen $screenName"
                                            }
                                        ) {
                                            Text("View Focus Path Map", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                HorizontalDivider(color = Color(0xFF2C3246), thickness = 1.dp)
                            }

                            val utteranceReport = remember(screenName, issues, focusNodes) {
                                com.example.amasamya.utils.UtteranceFlowEstimator.estimateScreenFlow(screenName, issues, focusNodes)
                            }
                            UtteranceFlowCard(report = utteranceReport)
                        }

                        val screenIssues = issues.filter { it.screenName == screenName }
                        if (screenIssues.isEmpty()) {
                            item {
                                Surface(
                                    color = GlassySurface,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp)
                                ) {
                                    Text(
                                        text = "✓ No issues detected on this screen.",
                                        color = NeonGreen,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        } else {
                            items(screenIssues) { issue ->
                                IssueViolationCard(issue = issue, persona = selectedPersona)
                            }
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        if (showTestExporterDialog && session != null) {
            val activeSession = session!!
            val generatedCode = com.example.amasamya.utils.TestScriptExporter.generateFullTestClass(
                sessionName = activeSession.name,
                packageName = activeSession.packageName,
                issues = issues,
                framework = selectedTestFramework
            )

            AlertDialog(
                onDismissRequest = { showTestExporterDialog = false },
                containerColor = DeepSpace,
                titleContentColor = PureWhite,
                textContentColor = TextSecondary,
                title = {
                    Text(
                        text = "⚡ Automated UI Test Script Exporter",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = VibrantCyan,
                        modifier = Modifier.semantics { heading() }
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Generated automated regression assertions for all ${issues.size} accessibility issues.",
                            fontSize = 13.sp,
                            color = LightGrey
                        )

                        // Framework Selector Buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    selectedTestFramework = com.example.amasamya.utils.TestScriptExporter.TestFramework.JETPACK_COMPOSE
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedTestFramework == com.example.amasamya.utils.TestScriptExporter.TestFramework.JETPACK_COMPOSE) VibrantCyan else GlassySurface,
                                    contentColor = if (selectedTestFramework == com.example.amasamya.utils.TestScriptExporter.TestFramework.JETPACK_COMPOSE) DeepSpace else PureWhite
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).semantics {
                                    role = Role.RadioButton
                                    contentDescription = "Jetpack Compose UI Test Framework"
                                }
                            ) {
                                Text("Compose Test", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    selectedTestFramework = com.example.amasamya.utils.TestScriptExporter.TestFramework.ESPRESSO
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedTestFramework == com.example.amasamya.utils.TestScriptExporter.TestFramework.ESPRESSO) VibrantCyan else GlassySurface,
                                    contentColor = if (selectedTestFramework == com.example.amasamya.utils.TestScriptExporter.TestFramework.ESPRESSO) DeepSpace else PureWhite
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).semantics {
                                    role = Role.RadioButton
                                    contentDescription = "Espresso UI Test Framework"
                                }
                            ) {
                                Text("Espresso Test", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Code Preview Box
                        Surface(
                            color = MidnightBlue,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF2C3246)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                        ) {
                            LazyColumn(modifier = Modifier.padding(12.dp)) {
                                item {
                                    Text(
                                        text = generatedCode,
                                        color = NeonGreen,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        modifier = Modifier.semantics {
                                            contentDescription = "Generated Kotlin test code preview"
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("AMASAMYA Automated Test", generatedCode)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Test code copied to clipboard!", Toast.LENGTH_SHORT).show()
                            showTestExporterDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VibrantCyan,
                            contentColor = DeepSpace
                        )
                    ) {
                        Text("Copy Code", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showTestExporterDialog = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = LightGrey)
                    ) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@Composable
fun IssueViolationCard(issue: ElementIssue, persona: String, modifier: Modifier = Modifier) {
    var isExpanded by remember { mutableStateOf(false) }
    val severityColor = when (issue.severity) {
        "Critical" -> NeonRed
        "Warning" -> AmberGold
        else -> VibrantCyan
    }

    Button(
        onClick = { isExpanded = !isExpanded },
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = GlassySurface,
            contentColor = PureWhite
        ),
        border = BorderStroke(1.dp, severityColor.copy(alpha = 0.5f)),
        contentPadding = PaddingValues(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                onClick(label = if (isExpanded) "Collapse issue details" else "Expand issue details") {
                    isExpanded = !isExpanded
                    true
                }
            }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(severityColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = issue.severity,
                            color = severityColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = issue.issueType,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite,
                        fontSize = 15.sp
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "SC ${issue.wcagSc}",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = PersonaReportHelper.getPersonaDescription(issue, persona),
                color = LightGrey,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                // Technical details section in code style font
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(DeepSpace)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Element: ${issue.className}",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Bounds: ${issue.bounds}",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (issue.text.isNotBlank()) {
                        Text(
                            text = "Text: \"${issue.text}\"",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    if (issue.contentDescription.isNotBlank()) {
                        Text(
                            text = "ContentDescription: \"${issue.contentDescription}\"",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Code Suggestions",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = VibrantCyan,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))

                var selectedTab by remember { mutableStateOf(0) }
                val (composeCode, xmlCode) = getRemediationSuggestions(issue.issueType)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TabButton(
                        text = "Jetpack Compose",
                        isSelected = selectedTab == 0,
                        onClick = { selectedTab = 0 }
                    )
                    TabButton(
                        text = "Android XML",
                        isSelected = selectedTab == 1,
                        onClick = { selectedTab = 1 }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(DeepSpace)
                        .padding(12.dp)
                ) {
                    Text(
                        text = if (selectedTab == 0) composeCode else xmlCode,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = NeonGreen,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val context = LocalContext.current
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("AMASAMYA A11y Bug", formatJiraBugMarkdown(issue, composeCode, xmlCode))
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Bug markdown copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricLavender.copy(alpha = 0.2f),
                        contentColor = ElectricLavender
                    ),
                    border = BorderStroke(1.dp, ElectricLavender),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().semantics {
                        role = Role.Button
                        contentDescription = "Copy Jira or GitHub markdown bug report"
                    }
                ) {
                    Text("Copy Jira/GitHub Bug", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TabButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) VibrantCyan.copy(alpha = 0.2f) else Color.Transparent,
            contentColor = if (isSelected) VibrantCyan else LightGrey
        ),
        border = BorderStroke(1.dp, if (isSelected) VibrantCyan else Color(0xFF2C3246)),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text = text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

private fun getRemediationSuggestions(issueType: String): Pair<String, String> {
    val fix = com.example.amasamya.utils.RemediationGenerator.generateFix(issueType, issueType, null, null)
    return Pair(fix.composeSnippet, fix.xmlSnippet)
}

private fun formatJiraBugMarkdown(issue: ElementIssue, composeFix: String, xmlFix: String): String {
    return """
        # [A11y Bug] ${issue.issueType} on ${issue.screenName}
        
        **Severity:** ${issue.severity}
        **WCAG Success Criterion:** ${issue.wcagSc}
        **Class Name:** `${issue.className}`
        **Bounds:** `${issue.bounds}`
        
        ## Description
        ${issue.description}
        
        ## How to Fix
        ### Jetpack Compose
        ```kotlin
        $composeFix
        ```
        
        ### Android XML
        ```xml
        $xmlFix
        ```
        
        *Reported by AMASAMYA Accessibility Tool*
    """.trimIndent()
}

private fun speakSummary(tts: TextToSpeech?, sessionName: String, issues: List<ElementIssue>) {
    if (tts == null) return
    val total = issues.size
    val criticalCount = issues.count { it.severity == "Critical" }
    val warningCount = issues.count { it.severity == "Warning" }
    val infoCount = issues.count { it.severity == "Info" }

    val sb = java.lang.StringBuilder()
    sb.append("Summary for session: $sessionName. ")
    if (total == 0) {
        sb.append("Congratulations! No compliance violations were detected in this session.")
    } else {
        sb.append("Found $total accessibility issues. ")
        sb.append("$criticalCount critical issues, $warningCount warnings, and $infoCount info level issues. ")
        
        val bySeverity = issues.groupBy { it.severity }
        for (severity in listOf("Critical", "Warning", "Info")) {
            val sevIssues = bySeverity[severity] ?: emptyList()
            if (sevIssues.isNotEmpty()) {
                sb.append("Under $severity severity: ")
                val byType = sevIssues.groupBy { it.issueType }
                val typeSummaries = byType.map { (type, typeList) ->
                    "${typeList.size} ${type} violations"
                }
                sb.append(typeSummaries.joinToString(", ")).append(". ")
            }
        }
    }
    
    tts.speak(sb.toString(), TextToSpeech.QUEUE_FLUSH, null, "SummaryUtteranceId")
}

// Sharing helper via FileProvider
private fun shareReportFile(context: Context, file: File, mimeType: String) {
    try {
        val authority = "${context.packageName}.fileprovider"
        val contentUri: Uri = FileProvider.getUriForFile(context, authority, file)
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "AMASAMYA Accessibility Report: ${file.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        val chooser = Intent.createChooser(shareIntent, "Share AMASAMYA Report")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "Error sharing file: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun UtteranceFlowCard(
    report: com.example.amasamya.utils.UtteranceFlowEstimator.ScreenUtteranceReport,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val fatigueColor = when {
        report.fatigueLevel.startsWith("High") -> NeonRed
        report.fatigueLevel.startsWith("Moderate") -> AmberGold
        else -> NeonGreen
    }

    Surface(
        color = GlassySurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, fatigueColor.copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {
                        role = Role.Button
                        onClick(label = if (isExpanded) "Collapse reading time breakdown" else "Expand reading time breakdown") {
                            isExpanded = !isExpanded
                            true
                        }
                    }
            ) {
                Column {
                    Text(
                        text = "⏱️ Screen Reader Reading Time & Speech Flow",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = VibrantCyan,
                        modifier = Modifier.semantics { heading() }
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "~${report.estimatedReadingTimeSeconds}s TalkBack reading time · ${report.totalWordCount} words · ${report.totalFocusableNodes} focus stops",
                        color = LightGrey,
                        fontSize = 12.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(fatigueColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = report.fatigueLevel,
                            color = fatigueColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(onClick = { isExpanded = !isExpanded }) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse reading flow details" else "Expand reading flow details",
                            tint = TextSecondary
                        )
                    }
                }
            }

            if (isExpanded) {
                HorizontalDivider(color = Color(0xFF2C3246), thickness = 1.dp)

                Text(
                    text = "Screen Reader Recommendations",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = PureWhite
                )

                report.recommendations.forEach { rec ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text("•", color = VibrantCyan, fontSize = 13.sp)
                        Text(rec, color = TextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Spoken Utterance Chain (${report.utteranceItems.size} nodes)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = PureWhite
                )

                Surface(
                    color = DeepSpace,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF2C3246)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                ) {
                    LazyColumn(modifier = Modifier.padding(10.dp)) {
                        items(report.utteranceItems) { item ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "#${item.nodeOrder} ${item.className.substringAfterLast('.')}",
                                    color = VibrantCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "\"${item.spokenText}\"",
                                    color = NeonGreen,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "${item.wordCount} words · ~${String.format(java.util.Locale.US, "%.1f", item.durationSeconds)}s",
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                                HorizontalDivider(color = Color(0xFF2C3246).copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
