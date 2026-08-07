package com.example.amasamya.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amasamya.service.A11yAuditService
import com.example.amasamya.settings.SettingsManager
import com.example.amasamya.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }

    var selectedLevel by remember { mutableStateOf(settingsManager.wcagLevel) }
    var selectedPersona by remember { mutableStateOf(settingsManager.reportPersona) }
    var audioFeedbackEnabled by remember { mutableStateOf(settingsManager.isAudioFeedbackEnabled) }
    var hapticFeedbackEnabled by remember { mutableStateOf(settingsManager.isHapticFeedbackEnabled) }
    var floatingButtonEnabled by remember { mutableStateOf(settingsManager.isFloatingButtonEnabled) }
    var adbServerEnabled by remember { mutableStateOf(settingsManager.isAdbServerEnabled) }
    var liveCaptionsEnabled by remember { mutableStateOf(settingsManager.isLiveCaptionsEnabled) }
    var liveFocusTrailEnabled by remember { mutableStateOf(settingsManager.isLiveFocusTrailEnabled) }
    var touchTargetMapperEnabled by remember { mutableStateOf(settingsManager.isTouchTargetMapperEnabled) }
    var focusTrapDetectorEnabled by remember { mutableStateOf(settingsManager.isFocusTrapDetectorEnabled) }
    var contrastDriftScannerEnabled by remember { mutableStateOf(settingsManager.isContrastDriftScannerEnabled) }
    var selectedStandard by remember { mutableStateOf(settingsManager.complianceStandard) }
    var voiceCommandsEnabled by remember { mutableStateOf(settingsManager.isVoiceCommandsEnabled) }
    var simulatorModeEnabled by remember { mutableStateOf(settingsManager.isSimulatorModeEnabled) }

    LaunchedEffect(Unit) {
        val manager = context.getSystemService(android.content.Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
        if (manager.isEnabled) {
            try {
                val event = android.view.accessibility.AccessibilityEvent.obtain(
                    android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT
                )
                event.text.add("Settings screen loaded successfully")
                event.className = "com.example.amasamya.ui.screens.SettingsScreen"
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
                        text = "Settings",
                        fontWeight = FontWeight.Bold,
                        color = PureWhite,
                        modifier = Modifier.semantics { heading() }
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back to dashboard",
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
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Heading: Compliance Standard Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "WCAG Compliance Level",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = VibrantCyan,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = "Select the target WCAG 2.2 rules level. AMASAMYA will validate touch target bounds and contrast checks against this level.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }

            // Options selection using custom Surface blocks
            val options = listOf(
                SettingsManager.LEVEL_A to "Level A (Basic check: missing labels only)",
                SettingsManager.LEVEL_AA to "Level AA (Default check: target sizes >= 24dp, contrast 4.5:1)",
                SettingsManager.LEVEL_AAA to "Level AAA (Enhanced check: target sizes >= 48dp, contrast 7.0:1)"
            )

            Column(
                modifier = Modifier
                    .selectableGroup()
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                options.forEach { (level, description) ->
                    val isSelected = selectedLevel == level
                    Button(
                        onClick = {
                            selectedLevel = level
                            settingsManager.wcagLevel = level
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GlassySurface,
                            contentColor = PureWhite
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) VibrantCyan else Color(0xFF2C3246)
                        ),
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clearAndSetSemantics {
                                role = Role.RadioButton
                                selected = isSelected
                                contentDescription = "WCAG 2.2 Level $level. $description"
                                onClick(label = "Select WCAG 2.2 Level $level") {
                                    selectedLevel = level
                                    settingsManager.wcagLevel = level
                                    true
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null, // Handled by Surface onClick
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = VibrantCyan,
                                    unselectedColor = LightGrey.copy(alpha = 0.5f)
                                )
                            )
                            Column {
                                Text(
                                    text = "WCAG 2.2 Level $level",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) VibrantCyan else PureWhite,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = description,
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF2C3246), thickness = 1.dp)

            // Heading: Global Compliance Standards
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Compliance Standard System",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = VibrantCyan,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = "Select international accessibility standards (WCAG 2.2, US Section 508, European EN 301 549) to apply during testing.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }

            val standardsList = listOf(
                SettingsManager.STANDARD_WCAG_2_2 to "W3C Web Content Accessibility Guidelines (Default)",
                SettingsManager.STANDARD_SECTION_508 to "US Federal Government Standard (Section 508)",
                SettingsManager.STANDARD_EN_301_549 to "European Accessibility Act (EN 301 549)"
            )

            Column(
                modifier = Modifier
                    .selectableGroup()
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                standardsList.forEach { (standard, description) ->
                    val isSelected = selectedStandard == standard
                    Button(
                        onClick = {
                            selectedStandard = standard
                            settingsManager.complianceStandard = standard
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GlassySurface,
                            contentColor = PureWhite
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) VibrantCyan else Color(0xFF2C3246)
                        ),
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clearAndSetSemantics {
                                role = Role.RadioButton
                                selected = isSelected
                                contentDescription = "Compliance standard $standard. $description"
                                onClick(label = "Select $standard compliance standard") {
                                    selectedStandard = standard
                                    settingsManager.complianceStandard = standard
                                    true
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = VibrantCyan,
                                    unselectedColor = LightGrey.copy(alpha = 0.5f)
                                )
                            )
                            Column {
                                Text(
                                    text = standard,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) VibrantCyan else PureWhite,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = description,
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF2C3246), thickness = 1.dp)

            // Heading: User Type Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "User Type / Persona",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = VibrantCyan,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = "Select your user type to customize the accessibility report language and suggestions.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }

            // Options selection for Persona using custom Surface blocks
            val personas = listOf(
                SettingsManager.PERSONA_GENERAL_USER to "General User (Simple language)",
                SettingsManager.PERSONA_DEVELOPER to "Developer (Code details & remediation)",
                SettingsManager.PERSONA_TESTER to "Tester (Reproduction steps & checks)",
                SettingsManager.PERSONA_DESIGNER to "Designer (Visual alignment & spacing)",
                SettingsManager.PERSONA_PRODUCT_OWNER to "Product Owner (Compliance & risk overview)"
            )

            Column(
                modifier = Modifier
                    .selectableGroup()
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                personas.forEach { (persona, description) ->
                    val isSelected = selectedPersona == persona
                    Button(
                        onClick = {
                            selectedPersona = persona
                            settingsManager.reportPersona = persona
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GlassySurface,
                            contentColor = PureWhite
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) ElectricLavender else Color(0xFF2C3246)
                        ),
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clearAndSetSemantics {
                                role = Role.RadioButton
                                selected = isSelected
                                contentDescription = "$persona. $description"
                                onClick(label = "Select $persona persona") {
                                    selectedPersona = persona
                                    settingsManager.reportPersona = persona
                                    true
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = ElectricLavender,
                                    unselectedColor = LightGrey.copy(alpha = 0.5f)
                                )
                            )
                            Column {
                                Text(
                                    text = persona,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) ElectricLavender else PureWhite,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = description,
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF2C3246), thickness = 1.dp)

            // Heading: Floating Scan Button Control
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Floating Scan Button",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = VibrantCyan,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = "Control how you start and stop audit scans from other apps.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }

            // Accessibility Floating Button Toggle inside a Glassy Card
            Surface(
                color = GlassySurface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF2C3246)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = floatingButtonEnabled,
                            role = Role.Switch,
                            onValueChange = { newValue ->
                                floatingButtonEnabled = newValue
                                settingsManager.isFloatingButtonEnabled = newValue
                                A11yAuditService.instance?.updateAccessibilityButtonState(newValue)
                            }
                        )
                        .semantics(mergeDescendants = true) {}
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Show Scan Floating Button",
                            fontWeight = FontWeight.SemiBold,
                            color = PureWhite,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Show a floating scan button on the screen to start/stop scan sessions. If disabled, use notification controls instead.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                    Switch(
                        checked = floatingButtonEnabled,
                        onCheckedChange = null,
                        modifier = Modifier.clearAndSetSemantics {},
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DeepSpace,
                            checkedTrackColor = VibrantCyan,
                            uncheckedThumbColor = LightGrey,
                            uncheckedTrackColor = Color(0xFF2C3246)
                        )
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF2C3246), thickness = 1.dp)

            // Audio & Haptic Controls
            Text(
                text = "Feedback Settings",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = VibrantCyan,
                modifier = Modifier.semantics { heading() }
            )

            // Audio Feedback Toggle inside a Glassy Card
            Surface(
                color = GlassySurface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF2C3246)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = audioFeedbackEnabled,
                            role = Role.Switch,
                            onValueChange = { newValue ->
                                audioFeedbackEnabled = newValue
                                settingsManager.isAudioFeedbackEnabled = newValue
                            }
                        )
                        .semantics(mergeDescendants = true) {}
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Audio & TTS Announcements",
                            fontWeight = FontWeight.SemiBold,
                            color = PureWhite,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Speak scan details and audio warnings via Text-to-Speech.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                    Switch(
                        checked = audioFeedbackEnabled,
                        onCheckedChange = null,
                        modifier = Modifier.clearAndSetSemantics {},
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DeepSpace,
                            checkedTrackColor = VibrantCyan,
                            uncheckedThumbColor = LightGrey,
                            uncheckedTrackColor = Color(0xFF2C3246)
                        )
                    )
                }
            }

            // Haptic Feedback Toggle inside a Glassy Card
            Surface(
                color = GlassySurface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF2C3246)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = hapticFeedbackEnabled,
                            role = Role.Switch,
                            onValueChange = { newValue ->
                                hapticFeedbackEnabled = newValue
                                settingsManager.isHapticFeedbackEnabled = newValue
                            }
                        )
                        .semantics(mergeDescendants = true) {}
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Haptic Vibration Warnings",
                            fontWeight = FontWeight.SemiBold,
                            color = PureWhite,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Vibrate when a rule fails during live TalkBack navigation.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                    Switch(
                        checked = hapticFeedbackEnabled,
                        onCheckedChange = null,
                        modifier = Modifier.clearAndSetSemantics {},
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DeepSpace,
                            checkedTrackColor = VibrantCyan,
                            uncheckedThumbColor = LightGrey,
                            uncheckedTrackColor = Color(0xFF2C3246)
                        )
                    )
                }
            }

            // ADB Report Server Toggle inside a Glassy Card
            Surface(
                color = GlassySurface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF2C3246)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = adbServerEnabled,
                            role = Role.Switch,
                            onValueChange = { newValue ->
                                adbServerEnabled = newValue
                                settingsManager.isAdbServerEnabled = newValue
                                if (newValue) {
                                    com.example.amasamya.utils.AdbReportServer.start(context)
                                } else {
                                    com.example.amasamya.utils.AdbReportServer.stop()
                                }
                            }
                        )
                        .semantics(mergeDescendants = true) {}
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable ADB Report Host (Port 8080)",
                            fontWeight = FontWeight.SemiBold,
                            color = PureWhite,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Host compliance reports locally on port 8080. Access from your PC via 'adb forward tcp:8080 tcp:8080'.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                    Switch(
                        checked = adbServerEnabled,
                        onCheckedChange = null,
                        modifier = Modifier.clearAndSetSemantics {},
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DeepSpace,
                            checkedTrackColor = VibrantCyan,
                            uncheckedThumbColor = LightGrey,
                            uncheckedTrackColor = Color(0xFF2C3246)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Real-Time Diagnostics Tools",
                fontWeight = FontWeight.Bold,
                color = VibrantCyan,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 1. Live Captions
            Surface(
                color = GlassySurface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF2C3246)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = liveCaptionsEnabled,
                            role = Role.Switch,
                            onValueChange = { newValue ->
                                liveCaptionsEnabled = newValue
                                settingsManager.isLiveCaptionsEnabled = newValue
                                com.example.amasamya.service.A11yAuditService.instance?.updateDiagnosticsState()
                            }
                        )
                        .semantics(mergeDescendants = true) {}
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Live TalkBack Speech Captions",
                            fontWeight = FontWeight.SemiBold,
                            color = PureWhite,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Show text captions of screen reader announcements as a overlay.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                    Switch(
                        checked = liveCaptionsEnabled,
                        onCheckedChange = null,
                        modifier = Modifier.clearAndSetSemantics {},
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DeepSpace,
                            checkedTrackColor = VibrantCyan,
                            uncheckedThumbColor = LightGrey,
                            uncheckedTrackColor = Color(0xFF2C3246)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Live Focus Trail
            Surface(
                color = GlassySurface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF2C3246)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = liveFocusTrailEnabled,
                            role = Role.Switch,
                            onValueChange = { newValue ->
                                liveFocusTrailEnabled = newValue
                                settingsManager.isLiveFocusTrailEnabled = newValue
                                com.example.amasamya.service.A11yAuditService.instance?.updateDiagnosticsState()
                            }
                        )
                        .semantics(mergeDescendants = true) {}
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Live Focus Trail Visualizer",
                            fontWeight = FontWeight.SemiBold,
                            color = PureWhite,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Draw paths connecting focused elements on the screen in real time.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                    Switch(
                        checked = liveFocusTrailEnabled,
                        onCheckedChange = null,
                        modifier = Modifier.clearAndSetSemantics {},
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DeepSpace,
                            checkedTrackColor = VibrantCyan,
                            uncheckedThumbColor = LightGrey,
                            uncheckedTrackColor = Color(0xFF2C3246)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Touch Target Mapper
            Surface(
                color = GlassySurface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF2C3246)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = touchTargetMapperEnabled,
                            role = Role.Switch,
                            onValueChange = { newValue ->
                                touchTargetMapperEnabled = newValue
                                settingsManager.isTouchTargetMapperEnabled = newValue
                                com.example.amasamya.service.A11yAuditService.instance?.updateDiagnosticsState()
                            }
                        )
                        .semantics(mergeDescendants = true) {}
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Live Touch Target Boundary Mapper",
                            fontWeight = FontWeight.SemiBold,
                            color = PureWhite,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Draw green/yellow/red overlays showing interactive element boundaries.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                    Switch(
                        checked = touchTargetMapperEnabled,
                        onCheckedChange = null,
                        modifier = Modifier.clearAndSetSemantics {},
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DeepSpace,
                            checkedTrackColor = VibrantCyan,
                            uncheckedThumbColor = LightGrey,
                            uncheckedTrackColor = Color(0xFF2C3246)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Focus Trap Detector
            Surface(
                color = GlassySurface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF2C3246)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = focusTrapDetectorEnabled,
                            role = Role.Switch,
                            onValueChange = { newValue ->
                                focusTrapDetectorEnabled = newValue
                                settingsManager.isFocusTrapDetectorEnabled = newValue
                                com.example.amasamya.service.A11yAuditService.instance?.updateDiagnosticsState()
                            }
                        )
                        .semantics(mergeDescendants = true) {}
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Keyboard Focus Trap & Loop Detector",
                            fontWeight = FontWeight.SemiBold,
                            color = PureWhite,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Warn if hardware keyboard tab focus gets stuck in a loop or lost.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                    Switch(
                        checked = focusTrapDetectorEnabled,
                        onCheckedChange = null,
                        modifier = Modifier.clearAndSetSemantics {},
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DeepSpace,
                            checkedTrackColor = VibrantCyan,
                            uncheckedThumbColor = LightGrey,
                            uncheckedTrackColor = Color(0xFF2C3246)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 5. Contrast Drift Scanner
            Surface(
                color = GlassySurface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF2C3246)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = contrastDriftScannerEnabled,
                            role = Role.Switch,
                            onValueChange = { newValue ->
                                contrastDriftScannerEnabled = newValue
                                settingsManager.isContrastDriftScannerEnabled = newValue
                                com.example.amasamya.service.A11yAuditService.instance?.updateDiagnosticsState()
                            }
                        )
                        .semantics(mergeDescendants = true) {}
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Live Contrast Drift Scanner",
                            fontWeight = FontWeight.SemiBold,
                            color = PureWhite,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Warn with haptic pulse if dynamic contrast falls below 4.5:1.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                    Switch(
                        checked = contrastDriftScannerEnabled,
                        onCheckedChange = null,
                        modifier = Modifier.clearAndSetSemantics {},
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DeepSpace,
                            checkedTrackColor = VibrantCyan,
                            uncheckedThumbColor = LightGrey,
                            uncheckedTrackColor = Color(0xFF2C3246)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 6. Voice Commands
            Surface(
                color = GlassySurface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF2C3246)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = voiceCommandsEnabled,
                            role = Role.Switch,
                            onValueChange = { newValue ->
                                voiceCommandsEnabled = newValue
                                settingsManager.isVoiceCommandsEnabled = newValue
                                com.example.amasamya.service.A11yAuditService.instance?.updateDiagnosticsState()
                            }
                        )
                        .semantics(mergeDescendants = true) {}
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hands-Free Voice Commands",
                            fontWeight = FontWeight.SemiBold,
                            color = PureWhite,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Trigger audits via voice commands ('scan screen', 'start session', 'stop session').",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                    Switch(
                        checked = voiceCommandsEnabled,
                        onCheckedChange = null,
                        modifier = Modifier.clearAndSetSemantics {},
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DeepSpace,
                            checkedTrackColor = VibrantCyan,
                            uncheckedThumbColor = LightGrey,
                            uncheckedTrackColor = Color(0xFF2C3246)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 7. TalkBack Simulator Mode
            Surface(
                color = GlassySurface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF2C3246)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = simulatorModeEnabled,
                            role = Role.Switch,
                            onValueChange = { newValue ->
                                simulatorModeEnabled = newValue
                                settingsManager.isSimulatorModeEnabled = newValue
                                com.example.amasamya.service.A11yAuditService.instance?.updateDiagnosticsState()
                            }
                        )
                        .semantics(mergeDescendants = true) {}
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Guided Screen Reader Simulator",
                            fontWeight = FontWeight.SemiBold,
                            color = PureWhite,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Simulate TalkBack focus traversal & spoken feedback without system gestures.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                    Switch(
                        checked = simulatorModeEnabled,
                        onCheckedChange = null,
                        modifier = Modifier.clearAndSetSemantics {},
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DeepSpace,
                            checkedTrackColor = VibrantCyan,
                            uncheckedThumbColor = LightGrey,
                            uncheckedTrackColor = Color(0xFF2C3246)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Heading: Beta Tester Feedback & Review
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Beta Tester Feedback & Review",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = VibrantCyan,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = "Provide feedback directly to help us meet Google Play testing engagement requirements.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }

            Surface(
                color = GlassySurface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, VibrantCyan.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "💬 Send Direct Feedback to Developer",
                        fontWeight = FontWeight.Bold,
                        color = PureWhite,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Found an accessibility bug or have a suggestion? Send your notes to our dev team directly.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:accessitestai@gmail.com")
                                        putExtra(Intent.EXTRA_SUBJECT, "AMASAMYA Beta Tester Feedback (v1.0.6)")
                                        putExtra(Intent.EXTRA_TEXT, "Hello AMASAMYA Team,\n\nHere is my feedback on the app:\n\n")
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Send Tester Feedback"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open email client.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VibrantCyan,
                                contentColor = DeepSpace
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .semantics {
                                    role = Role.Button
                                    contentDescription = "Send feedback via email to developer team"
                                }
                        ) {
                            Text("Email Feedback", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                try {
                                    val appPackageName = context.packageName
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName"))
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
                                    context.startActivity(intent)
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2C3246),
                                contentColor = PureWhite
                            ),
                            border = BorderStroke(1.dp, VibrantCyan),
                            modifier = Modifier
                                .weight(1f)
                                .semantics {
                                    role = Role.Button
                                    contentDescription = "Open Google Play Store feedback page"
                                }
                        ) {
                            Text("Play Store Review", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            var showPrivacyDialog by remember { mutableStateOf(false) }

            Surface(
                color = GlassySurface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF2C3246)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPrivacyDialog = true }
                        .semantics(mergeDescendants = true) {
                            role = Role.Button
                            contentDescription = "Privacy and Data Safety. Read about data handling, local audits, and offline rules."
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Privacy & Data Safety",
                            fontWeight = FontWeight.SemiBold,
                            color = PureWhite,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Learn how AMASAMYA handles audit data, accessibility service permissions, and privacy offline.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = VibrantCyan
                    )
                }
            }

            if (showPrivacyDialog) {
                AlertDialog(
                    onDismissRequest = { showPrivacyDialog = false },
                    title = {
                        Text(
                            text = "Privacy & Data Safety",
                            fontWeight = FontWeight.Bold,
                            color = VibrantCyan
                        )
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "AMASAMYA operates 100% offline. We value your privacy and security above all.",
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• Audits run locally: Inspection of screen elements and TalkBack reading paths is executed entirely on your device. No screen contents, DOM structures, or text data are ever sent to any remote server.",
                                fontSize = 13.sp,
                                color = PureWhite.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "• Offline Storage: Session data and WCAG reports are saved locally on your device in a private SQLite database. You can delete them at any time from the History page.",
                                fontSize = 13.sp,
                                color = PureWhite.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "• No Analytics/Ad Trackers: The app contains zero analytics packages, ad SDKs, or third-party trackers. The developer collects no telemetry.",
                                fontSize = 13.sp,
                                color = PureWhite.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "• Google Play Diagnostics: Standard crash logs (ANRs) are collected automatically by Google Play, but they do not contain your audit sessions or personal data.",
                                fontSize = 13.sp,
                                color = PureWhite.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "• Open Source: The codebase is fully transparent and inspectable on GitHub at github.com/AMASAMYA/AMASAMYA.",
                                fontSize = 13.sp,
                                color = PureWhite.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "• Developer Contact: For questions or feedback, email Akhilesh Malani directly at akhilesh.malani@gmail.com.",
                                fontSize = 13.sp,
                                color = PureWhite.copy(alpha = 0.8f)
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { showPrivacyDialog = false }
                        ) {
                            Text("Dismiss", color = VibrantCyan)
                        }
                    },
                    containerColor = DeepSpace,
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 6.dp
                )
            }
        }
    }
}
