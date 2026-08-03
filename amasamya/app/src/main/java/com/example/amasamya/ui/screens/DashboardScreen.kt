package com.example.amasamya.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.example.amasamya.History
import com.example.amasamya.Settings as SettingsRoute
import com.example.amasamya.service.A11yAuditService
import com.example.amasamya.settings.SettingsManager
import com.example.amasamya.theme.*
import com.example.amasamya.db.DatabaseHelper
import com.example.amasamya.db.AuditSession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigate: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val scrollState = rememberScrollState()

    val dbHelper = remember { DatabaseHelper(context) }
    var sessions by remember { mutableStateOf<List<AuditSession>>(emptyList()) }
    var latestScore by remember { mutableStateOf<Int?>(null) }
    var previousScore by remember { mutableStateOf<Int?>(null) }

    fun calculateScore(sessionId: Long): Int {
        val issues = dbHelper.getIssuesForSession(sessionId)
        val critical = issues.count { it.severity == "Critical" }
        val warning = issues.count { it.severity == "Warning" }
        val info = issues.count { it.severity == "Info" }
        val score = 100 - (critical * 15 + warning * 5 + info * 1)
        return maxOf(0, score)
    }

    LaunchedEffect(sessions) {
        if (sessions.isNotEmpty()) {
            val latest = sessions[0]
            val prev = if (sessions.size > 1) sessions[1] else null
            
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val lScore = calculateScore(latest.id)
                val pScore = prev?.let { calculateScore(it.id) }
                latestScore = lScore
                previousScore = pScore
            }
        } else {
            latestScore = null
            previousScore = null
        }
    }

    // Delayed Welcome Announcement on Clean Launch
    LaunchedEffect(Unit) {
        if (!com.example.amasamya.MainActivity.hasAnnouncedLaunch) {
            com.example.amasamya.MainActivity.hasAnnouncedLaunch = true
            kotlinx.coroutines.delay(1000)
            val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
            if (manager.isEnabled) {
                try {
                    val event = android.view.accessibility.AccessibilityEvent.obtain(
                        android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT
                    )
                    event.text.add("Welcome to AMASAMYA accessibility audit tool. Ready to start session.")
                    event.className = "com.example.amasamya.MainActivity"
                    event.packageName = context.packageName
                    manager.sendAccessibilityEvent(event)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Service activity poll state
    var isServiceEnabled by remember { mutableStateOf(A11yAuditService.instance != null) }
    var isSessionActive by remember { mutableStateOf(A11yAuditService.instance?.isRecording() == true) }
    var activeSessionName by remember { mutableStateOf(A11yAuditService.instance?.getCurrentSessionName() ?: "") }
    var sessionNameInput by remember { mutableStateOf("") }
    var serviceStatusText by remember { mutableStateOf(A11yAuditService.latestStatus) }
    var showDisclosureDialog by remember { mutableStateOf(false) }
    var showOnboardingDialog by remember { mutableStateOf(settingsManager.showOnboarding) }
    var showWhatsNewDialog by remember { mutableStateOf(false) }
    var currentVersionName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val currentVersionCode = try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            currentVersionName = packageInfo.versionName ?: "1.0.3"
            androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(packageInfo).toInt()
        } catch (e: Exception) {
            4
        }
        val lastRun = settingsManager.lastRunVersionCode
        if (lastRun == -1) {
            settingsManager.lastRunVersionCode = currentVersionCode
        } else if (lastRun < currentVersionCode) {
            showWhatsNewDialog = true
            settingsManager.lastRunVersionCode = currentVersionCode
        }
    }

    if (showDisclosureDialog) {
        AlertDialog(
            onDismissRequest = { showDisclosureDialog = false },
            title = {
                Text(
                    text = "Accessibility Permission Disclosure",
                    fontWeight = FontWeight.Bold,
                    color = VibrantCyan
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "AMASAMYA requires enabling the Accessibility Service API to inspect other applications for compliance auditing.",
                        color = PureWhite
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Screen Auditing: Analyzes node dimensions, text sizes, and color contrast ratios.",
                        fontSize = 13.sp,
                        color = PureWhite.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "• Reading Order: Traces swipe sequences and TalkBack focus paths.",
                        fontSize = 13.sp,
                        color = PureWhite.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Data Privacy Notice: All analysis is executed locally. No personal data is collected, stored outside this device, or transmitted over the internet.",
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDisclosureDialog = false
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                    }
                ) {
                    Text("Enable Service", color = VibrantCyan)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDisclosureDialog = false }
                ) {
                    Text("Cancel", color = NeonRed)
                }
            },
            containerColor = DeepSpace,
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp
        )
    }

    if (showOnboardingDialog) {
        AlertDialog(
            onDismissRequest = {
                showOnboardingDialog = false
                settingsManager.showOnboarding = false
            },
            title = {
                Text(
                    text = "Welcome to AMASAMYA",
                    fontWeight = FontWeight.Bold,
                    color = VibrantCyan
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "AMASAMYA is a blind-first WCAG 2.2 accessibility auditing tool designed to audit layouts and TalkBack reading order offline.",
                        color = PureWhite
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "1. How to scan: Enable the accessibility service, start an audit session, and use the floating overlay button on any app you want to audit.",
                        fontSize = 13.sp,
                        color = PureWhite.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "2. What to expect: Scans analyze color contrast, touch target sizes, text labels, and swipe paths. Results are saved offline locally.",
                        fontSize = 13.sp,
                        color = PureWhite.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "3. 100% Offline: Audits execute entirely on this device. No data is stored externally, transmitted, or shared.",
                        fontSize = 13.sp,
                        color = PureWhite.copy(alpha = 0.8f)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showOnboardingDialog = false
                        settingsManager.showOnboarding = false
                    }
                ) {
                    Text("Get Started", color = VibrantCyan)
                }
            },
            containerColor = DeepSpace,
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp
        )
    }

    if (showWhatsNewDialog) {
        AlertDialog(
            onDismissRequest = { showWhatsNewDialog = false },
            title = {
                Text(
                    text = "What's New in v$currentVersionName",
                    fontWeight = FontWeight.Bold,
                    color = VibrantCyan,
                    modifier = Modifier.semantics { heading() }
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "We have added powerful new real-time diagnostics features for mobile accessibility testing:",
                        color = PureWhite,
                        fontSize = 14.sp
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "• Real-Time Diagnostics Switches",
                        fontWeight = FontWeight.Bold,
                        color = VibrantCyan,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Toggle overlay overlays directly under the new 'Real-Time Diagnostics Tools' section in Settings.",
                        color = PureWhite.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )

                    Text(
                        text = "• Live TalkBack Speech Captions",
                        fontWeight = FontWeight.Bold,
                        color = VibrantCyan,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Display visual text bubbles showing screen reader spoken descriptions at the bottom of the screen.",
                        color = PureWhite.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )

                    Text(
                        text = "• Live Focus Trail Visualizer",
                        fontWeight = FontWeight.Bold,
                        color = VibrantCyan,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Traces sequential path trails connecting focused views to map TalkBack reading order dynamically.",
                        color = PureWhite.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )

                    Text(
                        text = "• Live Touch Target Boundary Mapper",
                        fontWeight = FontWeight.Bold,
                        color = VibrantCyan,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Highlights clickable areas with Green/Yellow/Red borders based on their compliance size (dp).",
                        color = PureWhite.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )

                    Text(
                        text = "• Focus Loop & Trap Detector",
                        fontWeight = FontWeight.Bold,
                        color = VibrantCyan,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Flags warning alerts and sounds haptic buzzes when navigation gets trapped in a layout loop.",
                        color = PureWhite.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )

                    Text(
                        text = "• Live Contrast Drift Scanner",
                        fontWeight = FontWeight.Bold,
                        color = VibrantCyan,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Scans screen colors on focus and warns if contrast drops below WCAG 2.2 compliant ratios.",
                        color = PureWhite.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )

                    Text(
                        text = "• 100% Screen Reader Compatible",
                        fontWeight = FontWeight.Bold,
                        color = VibrantCyan,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "All vector overlays use bypass flags so they do not block TalkBack gestures or clutter the accessibility focus tree.",
                        color = PureWhite.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showWhatsNewDialog = false }
                ) {
                    Text("Awesome", color = VibrantCyan)
                }
            },
            containerColor = DeepSpace,
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp
        )
    }



    // Check for service state when app resumes using LifecycleObserver
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isServiceEnabled = A11yAuditService.instance != null
                isSessionActive = A11yAuditService.instance?.isRecording() == true
                activeSessionName = A11yAuditService.instance?.getCurrentSessionName() ?: ""
                sessions = dbHelper.getAllSessions()
                
                if (com.example.amasamya.MainActivity.hasAnnouncedLaunch) {
                    val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
                    if (manager.isEnabled) {
                        try {
                            val aEvent = android.view.accessibility.AccessibilityEvent.obtain(
                                android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT
                            )
                            aEvent.text.add("AMASAMYA Dashboard loaded successfully")
                            aEvent.className = "com.example.amasamya.MainActivity"
                            aEvent.packageName = context.packageName
                            manager.sendAccessibilityEvent(aEvent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        
        A11yAuditService.onStatusChanged = { status ->
            serviceStatusText = status
        }
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            A11yAuditService.onStatusChanged = null
        }
    }

    val backgroundBrush = remember {
        Brush.verticalGradient(
            colors = listOf(DeepSpace, MidnightBlue)
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = modifier.background(backgroundBrush)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header Top Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Text(
                    text = "AMASAMYA",
                    fontWeight = FontWeight.ExtraBold,
                    style = TextStyle(
                        brush = Brush.linearGradient(
                            colors = listOf(VibrantCyan, ElectricLavender)
                        )
                    ),
                    fontSize = 24.sp,
                    letterSpacing = 2.sp,
                    modifier = Modifier.semantics {
                        heading()
                    }
                )
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Service Status Banner
                if (!isServiceEnabled) {
                    Surface(
                        color = GlassySurface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, NeonRed.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Service Inactive",
                                color = NeonRed,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.semantics { heading() }
                            )
                            Text(
                                text = "AMASAMYA requires enabling its Accessibility Service in system settings to audit target applications.",
                                color = PureWhite,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                            Button(
                                onClick = {
                                    showDisclosureDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonRed,
                                    contentColor = PureWhite
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Enable Service", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Surface(
                        color = GlassySurface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(NeonGreen)
                                )
                                Text(
                                    text = "Accessibility Service is Active",
                                    color = NeonGreen,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Button(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonGreen,
                                    contentColor = DeepSpace
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Disable / Manage Service", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Service Status Card / Live Region
                if (serviceStatusText.isNotEmpty()) {
                    Surface(
                        color = GlassySurface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, VibrantCyan.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics(mergeDescendants = true) {}
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(VibrantCyan)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Status Announcement",
                                    fontSize = 12.sp,
                                    color = VibrantCyan,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = serviceStatusText,
                                    color = PureWhite,
                                    fontSize = 14.sp,
                                    modifier = Modifier.semantics {
                                        liveRegion = LiveRegionMode.Polite
                                    }
                                )
                            }
                        }
                    }
                }

                // Compliance Score Trend Card
                latestScore?.let { score ->
                    Surface(
                        color = GlassySurface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF2C3246)),
                        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Compliance Score Trend",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = VibrantCyan,
                                modifier = Modifier.semantics { heading() }
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "$score%",
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = PureWhite
                                    )
                                    Text(
                                        text = "Based on WCAG 2.2 rules penalty score",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when {
                                                previousScore == null -> VibrantCyan.copy(alpha = 0.15f)
                                                score > (previousScore ?: 0) -> NeonGreen.copy(alpha = 0.15f)
                                                score < (previousScore ?: 0) -> NeonRed.copy(alpha = 0.15f)
                                                else -> LightGrey.copy(alpha = 0.15f)
                                            }
                                        )
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    val (trendText, trendColor) = when {
                                        previousScore == null -> Pair("First Audit", VibrantCyan)
                                        score > (previousScore ?: 0) -> Pair("+${score - (previousScore ?: 0)}% Improvement", NeonGreen)
                                        score < (previousScore ?: 0) -> Pair("${score - (previousScore ?: 0)}% Decrease", NeonRed)
                                        else -> Pair("No Change", LightGrey)
                                    }
                                    Text(
                                        text = trendText,
                                        color = trendColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Session Recording Card
                Surface(
                    color = GlassySurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF2C3246)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Audit Session Controls",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite,
                            modifier = Modifier.semantics { heading() }
                        )

                        if (!isSessionActive) {
                            OutlinedTextField(
                                value = sessionNameInput,
                                onValueChange = { sessionNameInput = it },
                                label = { Text("Session Name (e.g. Settings Page Audit)") },
                                placeholder = { Text("Quick Audit") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = VibrantCyan,
                                    unfocusedBorderColor = LightGrey.copy(alpha = 0.5f),
                                    focusedLabelColor = VibrantCyan,
                                    unfocusedLabelColor = LightGrey.copy(alpha = 0.7f),
                                    focusedTextColor = PureWhite,
                                    unfocusedTextColor = PureWhite
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics {
                                        contentDescription = "Session Name input field"
                                    },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            Button(
                                onClick = {
                                    val service = A11yAuditService.instance
                                    if (service != null) {
                                        val name = if (sessionNameInput.isBlank()) "Quick Audit" else sessionNameInput
                                        service.startAuditSession(name)
                                        isSessionActive = true
                                        activeSessionName = name
                                    }
                                },
                                enabled = isServiceEnabled,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VibrantCyan,
                                    contentColor = DeepSpace,
                                    disabledContainerColor = VibrantCyan.copy(alpha = 0.3f),
                                    disabledContentColor = PureWhite.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clearAndSetSemantics {
                                        role = Role.Button
                                        contentDescription = "Start New Session"
                                        if (!isServiceEnabled) {
                                            disabled()
                                        } else {
                                            onClick(label = "Start a new audit session") {
                                                val service = A11yAuditService.instance
                                                if (service != null) {
                                                    val name = if (sessionNameInput.isBlank()) "Quick Audit" else sessionNameInput
                                                    service.startAuditSession(name)
                                                    isSessionActive = true
                                                    activeSessionName = name
                                                }
                                                true
                                            }
                                        }
                                    }
                            ) {
                                Text("Start New Session", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Recording: $activeSessionName",
                                    color = NeonGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Active WCAG Level: Level ${settingsManager.wcagLevel}",
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Button(
                                    onClick = {
                                        A11yAuditService.instance?.performFullScreenScan()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NeonGreen,
                                        contentColor = DeepSpace
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Scan Current Screen Now", fontWeight = FontWeight.Bold)
                                }
                                
                                Button(
                                    onClick = {
                                        A11yAuditService.instance?.stopAuditSession()
                                        isSessionActive = false
                                        activeSessionName = ""
                                        sessionNameInput = ""
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NeonRed,
                                        contentColor = PureWhite
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Stop & Save Session", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Quick Nav Links
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // View Reports Card
                    Button(
                        onClick = { onNavigate(History) },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GlassySurface,
                            contentColor = PureWhite
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(VibrantCyan, ElectricLavender)
                            )
                        ),
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clearAndSetSemantics {
                                role = Role.Button
                                contentDescription = "Reports. View saved compliance audit reports."
                                onClick(label = "Open saved reports history") {
                                    onNavigate(History)
                                    true
                                }
                            }
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Reports",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = VibrantCyan
                            )
                            Text(
                                text = "View saved compliance audit reports.",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    // Settings Card
                    Button(
                        onClick = { onNavigate(SettingsRoute) },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GlassySurface,
                            contentColor = PureWhite
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(VibrantCyan, ElectricLavender)
                            )
                        ),
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clearAndSetSemantics {
                                role = Role.Button
                                contentDescription = "Settings. Configure WCAG 2.2 audit rules level."
                                onClick(label = "Configure WCAG level and feedback rules") {
                                    onNavigate(SettingsRoute)
                                    true
                                }
                            }
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Settings",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = VibrantCyan
                            )
                            Text(
                                text = "Configure WCAG 2.2 audit rules level (A/AA/AAA).",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // Reference Info Panel
                Surface(
                    color = GlassySurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF2C3246)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {}
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "How to run audits",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                        Text(
                            text = "1. Enable the Accessibility Service.\n2. Start a session above.\n3. Open the target app you wish to test.\n4. Tap the floating scan button (or the native accessibility shortcut) to run a scan instantly.\n5. Alternatively, pull down the notification shade and tap 'Scan Screen'.\n6. Navigate the target app with TalkBack to record focused elements.\n7. Either double-tap and hold (long press) the floating scan button, or return to AMASAMYA and tap 'Stop & Save Session' to stop recording and save the report.",
                            color = LightGrey.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }
    }
}
