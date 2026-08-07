package com.example.amasamya.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.AccessibilityButtonController
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.KeyEvent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import com.example.amasamya.MainActivity
import com.example.amasamya.db.DatabaseHelper
import com.example.amasamya.db.ElementIssue
import com.example.amasamya.db.AuditSession
import com.example.amasamya.db.FocusPathNode
import com.example.amasamya.settings.SettingsManager
import com.example.amasamya.utils.ContrastAnalyzer
import com.example.amasamya.utils.VoiceCommandManager
import com.example.amasamya.ui.views.OverlayCanvasView
import com.example.amasamya.ui.views.SimulatorOverlayView
import com.example.amasamya.rules.ComplianceStandard
import java.util.Locale
import java.util.concurrent.Executor

class A11yAuditService : AccessibilityService(), TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "A11yAuditService"
        var instance: A11yAuditService? = null
            private set

        const val ACTION_START_SESSION = "com.example.amasamya.START_SESSION"
        const val ACTION_STOP_SESSION = "com.example.amasamya.STOP_SESSION"
        const val ACTION_TRIGGER_SCAN = "com.example.amasamya.TRIGGER_SCAN"
        const val ACTION_TOGGLE_FLOATING_BUTTON = "com.example.amasamya.TOGGLE_FLOATING_BUTTON"
        
        private const val NOTIFICATION_ID = 101
        private const val CHANNEL_ID = "AMASAMYA_AUDIT_CHANNEL"

        // Status variables for UI to observe
        var latestStatus: String = ""
            private set
        var onStatusChanged: ((String) -> Unit)? = null
            set(value) {
                field = value
                value?.invoke(latestStatus)
            }

        // Helper to update status thread-safely
        fun updateStatus(status: String) {
            latestStatus = status
            Handler(Looper.getMainLooper()).post {
                onStatusChanged?.invoke(status)
            }
        }

        // Dynamic Rule Evaluation Engine
        internal class NodeViolation(val type: String, val severity: String, val description: String, val wcagSc: String)

        internal class A11yNodeData(
            val className: String,
            val text: String,
            val contentDescription: String,
            val bounds: Rect?,
            val isClickable: Boolean,
            val isFocusable: Boolean,
            val isHeading: Boolean,
            val isVisibleToUser: Boolean,
            val hasTextInSubtree: Boolean,
            val width: Int = bounds?.width() ?: 0,
            val height: Int = bounds?.height() ?: 0,
            val left: Int = bounds?.left ?: 0,
            val top: Int = bounds?.top ?: 0,
            val isFocused: Boolean = false,
            val isEditable: Boolean = false,
            val liveRegion: Int = 0
        )

        internal fun evaluateNodeData(
            node: A11yNodeData,
            density: Float,
            wcagLevel: String,
            screenBitmap: Bitmap? = null,
            context: Context? = null
        ): List<NodeViolation> {
            val violations = mutableListOf<NodeViolation>()
            val className = node.className
            val isClickable = node.isClickable
            val isFocusable = node.isFocusable
            val text = node.text
            val desc = node.contentDescription
            val isHeading = node.isHeading
            val isVisibleToUser = node.isVisibleToUser

            val widthDp = node.width / density
            val heightDp = node.height / density

            // Ignore off-screen, 0-sized, or hidden elements
            val right = node.left + node.width
            val bottom = node.top + node.height
            val isOffscreen = right <= 0 || bottom <= 0
            if (!isVisibleToUser || widthDp <= 0f || heightDp <= 0f || isOffscreen) {
                return emptyList()
            }

            // Rule 1: Touch Target Size (WCAG 2.2 SC 2.5.8 (AA) / 2.5.5 (AAA))
            if (isClickable) {
                if (wcagLevel == SettingsManager.LEVEL_AAA) {
                    if (widthDp < 48f || heightDp < 48f) {
                        violations.add(
                            NodeViolation(
                                type = "Target Size",
                                severity = "Critical",
                                description = "This button or interactive item is too small to tap easily (it measures only ${widthDp.toInt()} by ${heightDp.toInt()} screen units). It should be at least 48 by 48 units. Making it larger helps everyone, especially users with shaky hands or visual impairments, tap it easily without accidentally hitting nearby items.",
                                wcagSc = "2.5.5"
                            )
                        )
                    }
                } else if (wcagLevel == SettingsManager.LEVEL_AA) {
                    if (widthDp < 24f || heightDp < 24f) {
                        violations.add(
                            NodeViolation(
                                type = "Target Size",
                                severity = "Warning",
                                description = "This button or interactive item is too small to tap easily (it measures only ${widthDp.toInt()} by ${heightDp.toInt()} screen units). It should be at least 24 by 24 units so that it can be reliably pressed by users.",
                                wcagSc = "2.5.8"
                            )
                        )
                    }
                }
            }

            // Rule 2: Missing Content Description / Label (WCAG 1.1.1 (A/AA/AAA) & 4.1.2)
            if ((isClickable || isFocusable) && (text.isBlank() && desc.isBlank())) {
                // Only flag if it doesn't have any text in its subtree (avoids container false positives)
                if (!node.hasTextInSubtree) {
                    if (className != "android.view.View" && className != "android.view.ViewGroup" && className != "android.widget.FrameLayout" && className != "android.widget.LinearLayout" && className != "android.widget.RelativeLayout" || isClickable) {
                        violations.add(
                            NodeViolation(
                                type = "Missing Label",
                                severity = "Critical",
                                description = "This button or interactive item does not have a text label or name. When a screen reader user navigates to it, the app will announce it as 'unlabeled' or say nothing. This makes it impossible to know what the item does. Please add a short, clear description (such as 'Search' or 'Menu') so that screen reader users know its purpose.",
                                wcagSc = "1.1.1"
                            )
                        )
                    }
                }
            }

            // Rule 3: Redundant Descriptions
            if (desc.isNotBlank()) {
                val descLower = desc.lowercase()
                val redundantWords = listOf("button", "btn", "image", "img", "icon", "photo")
                for (word in redundantWords) {
                    if (descLower.contains(" $word") || descLower.startsWith(word) || descLower.endsWith(word)) {
                        if ((word == "button" && className.contains("Button")) || (word == "image" && className.contains("ImageView")) || (word == "icon" && className.contains("ImageView"))) {
                            violations.add(
                                NodeViolation(
                                    type = "Redundant Label",
                                    severity = "Info",
                                    description = "The label '$desc' contains the word '$word'. Screen readers already announce the type of item (like saying 'button' or 'image' automatically). Repeating the word '$word' in the description makes the screen reader say it twice, which is repetitive and annoying. Please remove '$word' from the description.",
                                    wcagSc = "1.1.1"
                                )
                            )
                            break
                        }
                    }
                }

                if (text.isNotBlank() && desc.trim().equals(text.trim(), ignoreCase = true)) {
                    violations.add(
                        NodeViolation(
                            type = "Redundant Label",
                            severity = "Info",
                            description = "The description '$desc' is exactly the same as the visible text '$text'. This causes screen readers to read the same text twice in a row. Please remove the duplicate description to keep it clean.",
                            wcagSc = "1.1.1"
                        )
                    )
                }
            }

            // Rule 4: Focus Noise (Focusable element with no text/desc and no subtree text)
            if (isFocusable && !isClickable && text.isBlank() && desc.isBlank() && !node.hasTextInSubtree) {
                violations.add(
                    NodeViolation(
                        type = "Focus Noise",
                        severity = "Warning",
                        description = "This empty item is highlighted by screen readers, but it has no text or meaning. This forces screen reader users to swipe through a silent, empty space. Please make this item ignored by screen readers, or add a text label if it is important.",
                        wcagSc = "4.1.2"
                    )
                )
            }

            // Rule 5: Text Magnification Clipping (WCAG 1.4.4 Resize Text)
            val fontScale = context?.resources?.configuration?.fontScale ?: 1.0f
            if (fontScale > 1.1f && text.isNotBlank()) {
                val estimatedMinHeightDp = 18f * fontScale
                if (heightDp > 0 && heightDp < estimatedMinHeightDp) {
                    violations.add(
                        NodeViolation(
                            type = "Text Clipping",
                            severity = "Warning",
                            description = "At the current system font scale of ${String.format(Locale.US, "%.2f", fontScale)}x, this text element is only ${heightDp.toInt()}dp tall. This is below the recommended minimum height of ${estimatedMinHeightDp.toInt()}dp needed to prevent text from clipping or overlapping adjacent lines under WCAG SC 1.4.4.",
                            wcagSc = "1.4.4"
                        )
                    )
                }
            }

            // Rule 6: Color Contrast Check (WCAG 1.4.3 (AA) / 1.4.6 (AAA)) - High Contrast Aware
            val isHighContrastEnabled = try {
                context?.let { ctx ->
                    android.provider.Settings.Secure.getInt(ctx.contentResolver, "high_text_contrast_enabled", 0) == 1
                } ?: false
            } catch (e: Exception) {
                false
            }
            if (screenBitmap != null && node.bounds != null && text.isNotBlank() && wcagLevel != SettingsManager.LEVEL_A) {
                val isLargeText = heightDp >= 24f || isHeading
                val contrastResult = ContrastAnalyzer.analyzeContrast(screenBitmap, node.bounds, isLargeText, wcagLevel)
                if (contrastResult != null && !contrastResult.isCompliant) {
                    val sc = if (wcagLevel == SettingsManager.LEVEL_AAA) "1.4.6" else "1.4.3"
                    val targetRatio = if (wcagLevel == SettingsManager.LEVEL_AAA) (if (isLargeText) 4.5f else 7.0f) else (if (isLargeText) 3.0f else 4.5f)
                    
                    var descMsg = "The text is hard to read because the contrast between the text color and its background is too low. The contrast score is only ${String.format(Locale.US, "%.1f", contrastResult.ratio)} out of 10, but it should be at least $targetRatio out of 10. To make it easy to read for everyone (especially those with low vision), we suggest changing the text color to ${contrastResult.suggestedColorHex}."
                    if (isHighContrastEnabled) {
                        descMsg += " NOTE: System 'High Contrast Text' setting is active, but the app should natively provide sufficient contrast so it is accessible by default for all users."
                    } else {
                        descMsg += " Ensure the app natively supports high contrast styles, or works correctly when system high contrast overrides are active."
                    }
                    
                    violations.add(
                        NodeViolation(
                            type = "Color Contrast",
                            severity = if (wcagLevel == SettingsManager.LEVEL_AAA) "Warning" else "Critical",
                            description = descMsg,
                            wcagSc = sc
                        )
                    )
                }
            }

            return violations
        }

        internal fun evaluateScreenLevelRules(
            nodeList: List<A11yNodeData>,
            density: Float,
            wcagLevel: String,
            screenBitmap: Bitmap? = null,
            context: Context? = null
        ): List<NodeViolation> {
            val violations = mutableListOf<NodeViolation>()
            val visibleNodes = nodeList.filter { it.isVisibleToUser }

            // 1. Dark Mode Theme Verification
            val isNightMode = context?.let { ctx ->
                (ctx.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
            } ?: false
            if (!isNightMode) {
                violations.add(
                    NodeViolation(
                        type = "Dark Mode Support",
                        severity = "Info",
                        description = "This screen was scanned in Light Mode. Dark Mode has not been verified. Running audits with system dark mode enabled is recommended to test color readability and theme adaptations under WCAG SC 1.4.3.",
                        wcagSc = "1.4.3"
                    )
                )
            } else if (screenBitmap != null) {
                val w = screenBitmap.width
                val h = screenBitmap.height
                if (w > 50 && h > 50) {
                    val samplePoints = listOf(
                        Pair(10, 10),
                        Pair(w - 10, 10),
                        Pair(w / 2, 20),
                        Pair(w / 2, h - 20)
                    )
                    var brightCount = 0
                    for (pt in samplePoints) {
                        try {
                            val color = screenBitmap.getPixel(pt.first, pt.second)
                            val r = (color shr 16) and 0xFF
                            val g = (color shr 8) and 0xFF
                            val b = color and 0xFF
                            val brightness = 0.299 * r + 0.587 * g + 0.114 * b
                            if (brightness > 220.0) {
                                brightCount++
                            }
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                    if (brightCount >= 3) {
                        violations.add(
                            NodeViolation(
                                type = "Dark Mode Support",
                                severity = "Warning",
                                description = "The system is set to Dark Mode, but the screen background is bright (white/light). The app may be forcing light theme or failing to support dark theme resources. This can cause severe visual glare and eye strain for low-vision users. Ensure native dark theme assets are implemented.",
                                wcagSc = "1.4.3"
                            )
                        )
                    }
                }
            }

            // 2. Magnification Gesture & Reflow Check
            val isMagnificationEnabled = try {
                context?.let { ctx ->
                    android.provider.Settings.Secure.getInt(ctx.contentResolver, "accessibility_display_magnification_enabled", 0) == 1
                } ?: false
            } catch (e: Exception) {
                false
            }
            if (isMagnificationEnabled) {
                violations.add(
                    NodeViolation(
                        type = "Magnification Reflow",
                        severity = "Info",
                        description = "System screen magnification is enabled. Ensure layout controls and view containers support panning, zoom gestures, and reflow smoothly without breaking interactive bounds under WCAG SC 1.4.10 Reflow.",
                        wcagSc = "1.4.10"
                    )
                )
            }

            // 3. Friction Warning Rule
            val focusableCount = visibleNodes.count { it.isFocusable }
            val frictionLimit = if (wcagLevel == SettingsManager.LEVEL_AAA) 15 else 25
            if (focusableCount > frictionLimit) {
                violations.add(
                    NodeViolation(
                        type = "Friction Warning",
                        severity = "Warning",
                        description = "This screen is very cluttered with $focusableCount items that screen readers can focus on. A screen reader user has to swipe through every single item one by one, which is tiring and slow. Consider grouping related texts or buttons together under a single focus area to make navigation faster.",
                        wcagSc = "2.4.3"
                    )
                )
            }

            // 4. Redundant Targets Checker
            val clickableNodes = visibleNodes.filter { it.isClickable }
            val maxDistPx = 24f * density // 24dp distance threshold
            val flaggedPairs = mutableSetOf<String>()

            for (i in clickableNodes.indices) {
                val nodeA = clickableNodes[i]
                val labelA = getCleanLabel(nodeA)

                for (j in i + 1 until clickableNodes.size) {
                    val nodeB = clickableNodes[j]
                    val labelB = getCleanLabel(nodeB)

                    // Determine adjacency
                    if (areAdjacent(nodeA, nodeB, maxDistPx)) {
                        val isRedundant = when {
                            // Case 1: Both have identical non-empty labels
                            labelA.isNotEmpty() && labelB.isNotEmpty() && labelA.equals(labelB, ignoreCase = true) -> true
                            // Case 2: One has a label, the other is empty (e.g. icon next to text button)
                            (labelA.isNotEmpty() && labelB.isEmpty()) || (labelA.isEmpty() && labelB.isNotEmpty()) -> true
                            else -> false
                        }

                        if (isRedundant) {
                            val pairKey = if (nodeA.hashCode() < nodeB.hashCode()) "${nodeA.hashCode()}-${nodeB.hashCode()}" else "${nodeB.hashCode()}-${nodeA.hashCode()}"
                            if (pairKey !in flaggedPairs) {
                                flaggedPairs.add(pairKey)
                                val desc = if (labelA.isNotEmpty() && labelB.isNotEmpty()) {
                                    "Two adjacent clickable items have the exact same label '$labelA'. This makes the screen reader repeat the same option. Please combine them into a single item so users only have to click once."
                                } else {
                                    val nonEl = if (labelA.isNotEmpty()) labelA else labelB
                                    "Two adjacent clickable items (one labeled '$nonEl' and the other empty) do the same thing. Please combine them into a single, labeled button so screen reader users do not get confused by an extra empty button."
                                }

                                violations.add(
                                    NodeViolation(
                                        type = "Redundant Click Target",
                                        severity = "Warning",
                                        description = "$desc",
                                        wcagSc = "2.4.4"
                                    )
                                )
                            }
                        }
                    }
                }
            }

            return violations
        }

        private fun getCleanLabel(node: A11yNodeData): String {
            val label = if (node.text.isNotBlank()) node.text else node.contentDescription
            return label.trim()
        }

        private fun areAdjacent(nodeA: A11yNodeData, nodeB: A11yNodeData, maxDistPx: Float): Boolean {
            val boundsA = nodeA.bounds
            val boundsB = nodeB.bounds
            
            // If bounds are available, calculate rectangle distances
            if (boundsA != null && boundsB != null) {
                val dx = maxOf(0, maxOf(boundsA.left - boundsB.right, boundsB.left - boundsA.right))
                val dy = maxOf(0, maxOf(boundsA.top - boundsB.bottom, boundsB.top - boundsA.bottom))
                return dx <= maxDistPx && dy <= maxDistPx
            }
            
            // Fallback for JVM unit tests where bounds Rect might be null: use coordinates directly
            val dx = maxOf(0, maxOf(nodeA.left - (nodeB.left + nodeB.width), nodeB.left - (nodeA.left + nodeA.width)))
            val dy = maxOf(0, maxOf(nodeA.top - (nodeB.top + nodeB.height), nodeB.top - (nodeA.top + nodeA.height)))
            return dx <= maxDistPx && dy <= maxDistPx
        }

        internal fun checkMissingLiveRegion(node: A11yNodeData): NodeViolation? {
            if (node.isVisibleToUser && 
                node.text.isNotBlank() && 
                !node.isEditable && 
                !node.isFocused && 
                node.className.contains("TextView")
            ) {
                if (node.liveRegion == 0) { // ACCESSIBILITY_LIVE_REGION_NONE
                    return NodeViolation(
                        type = "Missing Live Region",
                        severity = "Warning",
                        description = "A message or alert on the screen was updated in the background, but it did not notify the screen reader. As a result, screen reader users will not know that the status has changed. Please add a liveRegion setting to let screen readers announce updates automatically.",
                        wcagSc = "4.1.3"
                    )
                }
            }
            return null
        }
    }

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var settingsManager: SettingsManager
    private val backgroundExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()
    
    private var currentSessionId: Long? = null
    private var currentSessionName: String = ""
    private var lastScreenName: String = "Unknown Screen"
    private val flaggedLiveRegions = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    
    private var readingOrderIndex = 0

    override fun onCreate() {
        super.onCreate()
        dbHelper = DatabaseHelper(this)
        settingsManager = SettingsManager(this)
        tts = TextToSpeech(this, this)
        createNotificationChannel()
    }

    private var lastAccessibilityButtonClickTime: Long = 0
    private var accessibilityButtonClickPendingRunnable: Runnable? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private fun handleAccessibilityButtonClick() {
        val currentTime = System.currentTimeMillis()
        val timeDiff = currentTime - lastAccessibilityButtonClickTime
        lastAccessibilityButtonClickTime = currentTime

        if (timeDiff < 1200) {
            // Double Click detected! Cancel any pending single click action and stop session.
            accessibilityButtonClickPendingRunnable?.let { mainHandler.removeCallbacks(it) }
            accessibilityButtonClickPendingRunnable = null
            
            if (currentSessionId != null) {
                stopAuditSession()
            } else {
                speak("No active audit session to stop.")
            }
        } else {
            // Single Click candidate: schedule execution after 1200ms
            accessibilityButtonClickPendingRunnable?.let { mainHandler.removeCallbacks(it) }
            val runnable = Runnable {
                if (currentSessionId == null) {
                    startAuditSession("Quick Scan Session")
                }
                performFullScreenScan()
                accessibilityButtonClickPendingRunnable = null
            }
            accessibilityButtonClickPendingRunnable = runnable
            mainHandler.postDelayed(runnable, 1200)
        }
    }

    fun updateAccessibilityButtonState(enabled: Boolean) {
        if (enabled) {
            showFloatingButton()
        } else {
            hideFloatingButton()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Accessibility Service Connected")

        // Show persistent notification immediately when service binds
        showNotification()

        // Apply setting dynamically on connection
        updateAccessibilityButtonState(settingsManager.isFloatingButtonEnabled)
        updateDiagnosticsState()
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        tts?.shutdown()
        accessibilityButtonClickPendingRunnable?.let { mainHandler.removeCallbacks(it) }
        voiceCommandManager?.stopListening()
        voiceCommandManager = null
        hideSimulatorOverlay()
        hideOverlayCanvas()
        hideFloatingButton() // Hide custom floating button overlay
        dismissNotification() // Dismiss notification when service is disabled/unbound
        Log.d(TAG, "Accessibility Service Destroyed")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            var result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.d(TAG, "US Locale not supported, falling back to default locale")
                result = tts?.setLanguage(Locale.getDefault())
            }
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Default Language not supported by TTS either")
            } else {
                isTtsReady = true
                Log.d(TAG, "TTS Initialized successfully")
            }
        } else {
            Log.e(TAG, "TTS Initialization failed")
        }
    }

    fun speak(text: String) {
        if (isTtsReady && settingsManager.isAudioFeedbackEnabled) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "AMASAMYA_TTS")
        }
        sendAccessibilityAnnouncement(text)
        updateStatus(text)
    }

    fun showToast(message: String) {
        mainHandler.post {
            android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendAccessibilityAnnouncement(text: String) {
        try {
            val manager = getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
            if (manager.isEnabled) {
                val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_ANNOUNCEMENT)
                event.text.add(text)
                event.className = javaClass.name
                event.packageName = packageName
                manager.sendAccessibilityEvent(event)
                Log.d(TAG, "Sent accessibility announcement: $text")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send accessibility announcement", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            when (intent.action) {
                ACTION_START_SESSION -> {
                    val name = intent.getStringExtra("session_name") ?: "Quick Session"
                    startAuditSession(name)
                }
                ACTION_STOP_SESSION -> {
                    stopAuditSession()
                }
                ACTION_TRIGGER_SCAN -> {
                    if (currentSessionId == null) {
                        startAuditSession("Quick Scan Session")
                    }
                    performFullScreenScan()
                }
                ACTION_TOGGLE_FLOATING_BUTTON -> {
                    val newValue = !settingsManager.isFloatingButtonEnabled
                    settingsManager.isFloatingButtonEnabled = newValue
                    updateAccessibilityButtonState(newValue)
                    showNotification()
                }
            }
        }
        return START_STICKY
    }

    private var swipeCount = 0
    private var focusOrderCounter = 0

    private var lastFocusX: Float? = null
    private var lastFocusY: Float? = null
    private val focusHistory = mutableListOf<FocusHistoryItem>()
    private var lastContrastCheckTime = 0L

    data class FocusHistoryItem(
        val viewId: String,
        val className: String,
        val bounds: Rect
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Run live diagnostics overlays first (if enabled)
        handleLiveDiagnostics(event)

        if (currentSessionId == null) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val name = event.className?.toString() ?: "Unknown Screen"
                // Simple check to see if it's an Activity or Window
                if (!name.contains("android.widget.") && !name.contains("android.view.")) {
                    val sessionId = currentSessionId
                    if (sessionId != null && swipeCount > 0) {
                        saveFrictionMetric(sessionId, lastScreenName, swipeCount)
                    }
                    lastScreenName = name
                    readingOrderIndex = 0
                    swipeCount = 0
                    focusOrderCounter = 0
                    flaggedLiveRegions.clear()
                }
            }
            AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED -> {
                swipeCount++
                // Sourced from TalkBack focus
                val source = event.source
                if (source != null) {
                    recordFocusPathNode(source)
                    auditFocusedElement(source)
                    source.recycle()
                }
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                val contentChangeTypes = event.contentChangeTypes
                if ((contentChangeTypes and AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT) != 0) {
                    val source = event.source
                    if (source != null) {
                        monitorLiveRegionChange(source)
                        source.recycle()
                    }
                }
            }
        }
    }

    private fun handleLiveDiagnostics(event: AccessibilityEvent) {
        if (overlayCanvasView == null) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                // Clear trails on screen transition
                lastFocusX = null
                lastFocusY = null
                overlayCanvasView?.clearFocusTrail()
                overlayCanvasView?.clearTouchTargets()
                if (settingsManager.isTouchTargetMapperEnabled) {
                    scanWindowForTouchTargets()
                }
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                if (settingsManager.isTouchTargetMapperEnabled) {
                    scanWindowForTouchTargets()
                }
            }
            AccessibilityEvent.TYPE_ANNOUNCEMENT -> {
                if (settingsManager.isLiveCaptionsEnabled) {
                    val text = event.text?.joinToString(" ")
                    if (!text.isNullOrEmpty()) {
                        overlayCanvasView?.updateCaptions(text)
                    }
                }
            }
            AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED -> {
                val source = event.source
                if (source != null) {
                    val rect = Rect()
                    source.getBoundsInScreen(rect)
                    val endX = rect.centerX().toFloat()
                    val endY = rect.centerY().toFloat()

                    // 1. Live Speech Captions
                    if (settingsManager.isLiveCaptionsEnabled) {
                        val speechText = getEventSpeechText(event, source)
                        if (speechText.isNotEmpty()) {
                            overlayCanvasView?.updateCaptions(speechText)
                        }
                    }

                    // 2. Live Focus Trail
                    if (settingsManager.isLiveFocusTrailEnabled) {
                        val startX = lastFocusX
                        val startY = lastFocusY
                        if (startX != null && startY != null) {
                            overlayCanvasView?.addFocusSegment(startX, startY, endX, endY)
                        }
                        lastFocusX = endX
                        lastFocusY = endY
                    }

                    // 3. Focus Loop / Trap Detector
                    if (settingsManager.isFocusTrapDetectorEnabled) {
                        val item = FocusHistoryItem(
                            viewId = source.viewIdResourceName ?: "no_id",
                            className = source.className?.toString() ?: "no_class",
                            bounds = Rect(rect)
                        )
                        focusHistory.add(item)
                        if (focusHistory.size > 8) {
                            focusHistory.removeAt(0)
                        }
                        if (detectFocusLoop()) {
                            performHapticFeedback()
                            overlayCanvasView?.updateCaptions("FOCUS TRAP WARNING: Navigation loop detected!")
                        }
                    }

                    // 4. Live Contrast Drift Scanner
                    if (settingsManager.isContrastDriftScannerEnabled) {
                        val now = android.os.SystemClock.uptimeMillis()
                        if (now - lastContrastCheckTime >= 1000) { // Limit to 1 per second
                            lastContrastCheckTime = now
                            checkContrastRealTime(rect, source)
                        }
                    }

                    // 5. Touch Target Mapper update
                    if (settingsManager.isTouchTargetMapperEnabled) {
                        scanWindowForTouchTargets()
                    }

                    source.recycle()
                }
            }
        }
    }

    private fun scanWindowForTouchTargets() {
        if (overlayCanvasView == null || !settingsManager.isTouchTargetMapperEnabled) return
        val root = rootInActiveWindow ?: return
        val targets = mutableListOf<OverlayCanvasView.TouchTarget>()
        val density = resources.displayMetrics.density
        traverseNodeForTouchTargets(root, targets, density)
        root.recycle()
        overlayCanvasView?.updateTouchTargets(targets)
    }

    private fun traverseNodeForTouchTargets(node: AccessibilityNodeInfo, list: MutableList<OverlayCanvasView.TouchTarget>, density: Float) {
        if (node.isClickable) {
            val rect = Rect()
            node.getBoundsInScreen(rect)
            val widthDp = (rect.width() / density).toInt()
            val heightDp = (rect.height() / density).toInt()
            
            val color = when {
                widthDp >= 48 && heightDp >= 48 -> Color.GREEN
                widthDp >= 24 && heightDp >= 24 -> Color.YELLOW
                else -> Color.RED
            }
            list.add(OverlayCanvasView.TouchTarget(Rect(rect), widthDp, heightDp, color))
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                traverseNodeForTouchTargets(child, list, density)
                child.recycle()
            }
        }
    }

    private fun getEventSpeechText(event: AccessibilityEvent, node: AccessibilityNodeInfo): String {
        val label = node.contentDescription?.toString() ?: node.text?.toString() ?: ""
        val role = getReadableRoleName(node.className?.toString())
        val state = getReadableStateDescription(node)

        val parts = mutableListOf<String>()
        if (label.isNotEmpty()) parts.add(label)
        if (role.isNotEmpty()) parts.add(role)
        if (state.isNotEmpty()) parts.add(state)

        return parts.joinToString(", ")
    }

    private fun getReadableRoleName(className: String?): String {
        if (className == null) return ""
        return when {
            className.contains("Button") -> "Button"
            className.contains("EditText") || className.contains("TextField") -> "Edit Box"
            className.contains("CheckBox") -> "Checkbox"
            className.contains("Switch") -> "Switch"
            className.contains("RadioButton") -> "Radio Button"
            className.contains("Image") -> "Image"
            className.contains("TextView") -> ""
            else -> ""
        }
    }

    private fun getReadableStateDescription(node: AccessibilityNodeInfo): String {
        val parts = mutableListOf<String>()
        if (node.isCheckable) {
            parts.add(if (node.isChecked) "checked" else "not checked")
        }
        if (!node.isEnabled) {
            parts.add("disabled")
        }
        if (node.isSelected) {
            parts.add("selected")
        }
        return parts.joinToString(", ")
    }

    private fun detectFocusLoop(): Boolean {
        val size = focusHistory.size
        if (size < 4) return false
        for (p in 1..4) {
            if (size >= 2 * p) {
                var isMatch = true
                for (i in 0 until p) {
                    val item1 = focusHistory[size - 2 * p + i]
                    val item2 = focusHistory[size - p + i]
                    if (item1.viewId != item2.viewId || item1.bounds != item2.bounds) {
                        isMatch = false
                        break
                    }
                }
                if (isMatch) return true
            }
        }
        return false
    }

    private fun checkContrastRealTime(rect: Rect, node: AccessibilityNodeInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val density = resources.displayMetrics.density
            val heightDp = rect.height() / density
            val isLargeText = heightDp >= 24f || node.isHeading
            val wcagLevel = settingsManager.wcagLevel
            val mainExecutor = Executor { command -> mainHandler.post(command) }
            
            try {
                takeScreenshot(android.view.Display.DEFAULT_DISPLAY, mainExecutor, object : TakeScreenshotCallback {
                    override fun onSuccess(screenshotResult: ScreenshotResult) {
                        val hardwareBuffer = screenshotResult.hardwareBuffer
                        val bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, null)
                        hardwareBuffer.close()
                        
                        if (bitmap != null) {
                            val softwareBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
                            bitmap.recycle()
                            
                            val result = ContrastAnalyzer.analyzeContrast(softwareBitmap, rect, isLargeText, wcagLevel)
                            softwareBitmap.recycle()
                            
                            if (result != null && !result.isCompliant) {
                                if (settingsManager.isHapticFeedbackEnabled) {
                                    performHapticFeedback()
                                }
                                overlayCanvasView?.updateCaptions(
                                    "CONTRAST WARNING: Low contrast ratio ${String.format("%.1f", result.ratio)}:1 (Min target ${if (wcagLevel == "AAA") "7.0" else "4.5"}:1)"
                                )
                            }
                        }
                    }

                    override fun onFailure(errorCode: Int) {
                        Log.e(TAG, "Real-time contrast screenshot failed: $errorCode")
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "Error taking dynamic screenshot for contrast", e)
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service Interrupted")
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        return super.onKeyEvent(event)
    }

    // Session Management
    fun isRecording(): Boolean {
        return currentSessionId != null
    }

    fun getCurrentSessionName(): String {
        return currentSessionName
    }

    fun startAuditSession(name: String) {
        if (currentSessionId != null) {
            speak("Session is already recording.")
            return
        }

        val deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"
        val session = AuditSession(
            name = name,
            date = System.currentTimeMillis(),
            packageName = packageName,
            deviceInfo = deviceInfo,
            wcagLevel = settingsManager.wcagLevel
        )
        
        currentSessionId = dbHelper.insertSession(session)
        currentSessionName = name
        readingOrderIndex = 0
        swipeCount = 0
        focusOrderCounter = 0
        flaggedLiveRegions.clear()
        
        showNotification()
        speak("Accessibility audit session started for $name.")
        updateFloatingButtonVisuals()
        Log.d(TAG, "Audit session started with ID: $currentSessionId")
    }

    fun stopAuditSession() {
        val sessionId = currentSessionId
        if (sessionId == null) {
            speak("No active audit session.")
            return
        }
        
        if (swipeCount > 0) {
            saveFrictionMetric(sessionId, lastScreenName, swipeCount)
        }
        
        speak("Session stopped. Report saved offline.")
        currentSessionId = null
        currentSessionName = ""
        swipeCount = 0
        focusOrderCounter = 0
        flaggedLiveRegions.clear()
        showNotification()
        updateFloatingButtonVisuals()
        Log.d(TAG, "Audit session stopped")
    }

    private var floatingButtonView: View? = null
    private var overlayCanvasView: OverlayCanvasView? = null
    private var simulatorOverlayView: SimulatorOverlayView? = null
    private var voiceCommandManager: VoiceCommandManager? = null

    fun simulateFocusNext() {
        val root = rootInActiveWindow ?: return
        val focusableNodes = mutableListOf<AccessibilityNodeInfo>()
        collectFocusableNodes(root, focusableNodes)
        if (focusableNodes.isNotEmpty()) {
            val currentlyFocused = focusableNodes.indexOfFirst { it.isAccessibilityFocused }
            val nextIndex = if (currentlyFocused >= 0 && currentlyFocused < focusableNodes.size - 1) currentlyFocused + 1 else 0
            val target = focusableNodes[nextIndex]
            target.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
            val rect = Rect()
            target.getBoundsInScreen(rect)
            simulatorOverlayView?.updateFocusRect(rect)
            val label = target.contentDescription?.toString() ?: target.text?.toString() ?: "Item"
            speak(label)
        }
        root.recycle()
    }

    fun simulateFocusPrevious() {
        val root = rootInActiveWindow ?: return
        val focusableNodes = mutableListOf<AccessibilityNodeInfo>()
        collectFocusableNodes(root, focusableNodes)
        if (focusableNodes.isNotEmpty()) {
            val currentlyFocused = focusableNodes.indexOfFirst { it.isAccessibilityFocused }
            val prevIndex = if (currentlyFocused > 0) currentlyFocused - 1 else focusableNodes.size - 1
            val target = focusableNodes[prevIndex]
            target.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
            val rect = Rect()
            target.getBoundsInScreen(rect)
            simulatorOverlayView?.updateFocusRect(rect)
            val label = target.contentDescription?.toString() ?: target.text?.toString() ?: "Item"
            speak(label)
        }
        root.recycle()
    }

    fun simulateFocusActivate() {
        val root = rootInActiveWindow ?: return
        val focusableNodes = mutableListOf<AccessibilityNodeInfo>()
        collectFocusableNodes(root, focusableNodes)
        val currentlyFocusedNode = focusableNodes.find { it.isAccessibilityFocused }
        if (currentlyFocusedNode != null) {
            currentlyFocusedNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            speak("Activated")
        }
        root.recycle()
    }

    private fun collectFocusableNodes(node: AccessibilityNodeInfo, list: MutableList<AccessibilityNodeInfo>) {
        if (node.isFocusable || node.isClickable) {
            list.add(node)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                collectFocusableNodes(child, list)
            }
        }
    }

    fun updateDiagnosticsState() {
        // Manage Voice Commands
        if (settingsManager.isVoiceCommandsEnabled) {
            if (voiceCommandManager == null) {
                voiceCommandManager = VoiceCommandManager(this) { msg ->
                    showToast(msg)
                }
            }
            voiceCommandManager?.startListening()
        } else {
            voiceCommandManager?.stopListening()
            voiceCommandManager = null
        }

        // Manage Simulator Overlay
        if (settingsManager.isSimulatorModeEnabled) {
            showSimulatorOverlay()
        } else {
            hideSimulatorOverlay()
        }

        val anyEnabled = settingsManager.isLiveCaptionsEnabled ||
                settingsManager.isLiveFocusTrailEnabled ||
                settingsManager.isTouchTargetMapperEnabled ||
                settingsManager.isFocusTrapDetectorEnabled ||
                settingsManager.isContrastDriftScannerEnabled

        if (anyEnabled) {
            showOverlayCanvas()
            overlayCanvasView?.apply {
                showFocusTrail = settingsManager.isLiveFocusTrailEnabled
                showTouchTargets = settingsManager.isTouchTargetMapperEnabled
                showCaptions = settingsManager.isLiveCaptionsEnabled
                
                // Clear active states if disabled
                if (!showFocusTrail) clearFocusTrail()
                if (!showTouchTargets) clearTouchTargets()
                if (!showCaptions) updateCaptions("")
            }
        } else {
            hideOverlayCanvas()
        }
    }

    private fun showOverlayCanvas() {
        if (overlayCanvasView != null) return // Already showing
        
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val view = OverlayCanvasView(this)
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        try {
            wm.addView(view, params)
            overlayCanvasView = view
            Log.d(TAG, "Added OverlayCanvasView to WindowManager")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding OverlayCanvasView", e)
        }
    }

    private fun hideOverlayCanvas() {
        val view = overlayCanvasView ?: return
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        try {
            wm.removeView(view)
            overlayCanvasView = null
            Log.d(TAG, "Removed OverlayCanvasView from WindowManager")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing OverlayCanvasView", e)
        }
    }

    private fun showSimulatorOverlay() {
        if (simulatorOverlayView != null) return
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val view = SimulatorOverlayView(this)
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        try {
            wm.addView(view, params)
            simulatorOverlayView = view
            Log.d(TAG, "Added SimulatorOverlayView to WindowManager")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding SimulatorOverlayView", e)
        }
    }

    private fun hideSimulatorOverlay() {
        val view = simulatorOverlayView ?: return
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        try {
            wm.removeView(view)
            simulatorOverlayView = null
            Log.d(TAG, "Removed SimulatorOverlayView from WindowManager")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing SimulatorOverlayView", e)
        }
    }

    private fun handleFloatingButtonClick() {
        if (currentSessionId == null) {
            startAuditSession("Quick Scan Session")
        }
        performFullScreenScan()
    }

    fun showFloatingButton() {
        if (floatingButtonView != null) return // Already showing
        
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val density = resources.displayMetrics.density
        val buttonSize = (64 * density).toInt() // Circular button size
        
        val shape = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#007FFF")) // AzureBlue
            setStroke((2 * density).toInt(), Color.WHITE)
        }
        
        val container = FrameLayout(this).apply {
            background = shape
            elevation = 8 * density
            contentDescription = "AMASAMYA Scan. Double tap to scan screen."
            isFocusable = true
            isClickable = true
            isLongClickable = true
            accessibilityDelegate = object : View.AccessibilityDelegate() {
                override fun onInitializeAccessibilityNodeInfo(host: View, info: android.view.accessibility.AccessibilityNodeInfo) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    info.className = "android.widget.Button"
                    info.isClickable = true
                    info.isLongClickable = true
                    info.addAction(android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK)
                    info.addAction(android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_LONG_CLICK)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (currentSessionId != null) {
                            info.stateDescription = "Active session recording. Double tap to scan. Double tap and hold to stop session."
                        } else {
                            info.stateDescription = "Ready to start session. Double tap to start session."
                        }
                    }
                }

                override fun performAccessibilityAction(host: View, action: Int, args: android.os.Bundle?): Boolean {
                    if (action == android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK) {
                        host.performClick()
                        return true
                    }
                    if (action == android.view.accessibility.AccessibilityNodeInfo.ACTION_LONG_CLICK) {
                        return host.performLongClick()
                    }
                    return super.performAccessibilityAction(host, action, args)
                }
            }
        }
        
        val textView = TextView(this).apply {
            text = "SCAN"
            setTextColor(Color.WHITE)
            textSize = 12f
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        
        container.addView(textView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        
        val params = WindowManager.LayoutParams(
            buttonSize,
            buttonSize,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = (24 * density).toInt() // Right margin
            y = (120 * density).toInt() // Offset bottom to float above system button
        }
        
        val gestureDetector = android.view.GestureDetector(this, object : android.view.GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                container.performClick()
                return true
            }
            override fun onLongPress(e: MotionEvent) {
                container.performLongClick()
            }
        })

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false
        
        container.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isDragging = true
                    }
                    if (isDragging) {
                        params.x = initialX - dx.toInt()
                        params.y = initialY - dy.toInt()
                        try {
                            wm.updateViewLayout(container, params)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error updating overlay layout", e)
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    true
                }
                else -> false
            }
        }
        
        container.setOnClickListener {
            handleFloatingButtonClick()
        }
        
        container.setOnLongClickListener {
            if (currentSessionId != null) {
                stopAuditSession()
            } else {
                speak("No active session to stop.")
            }
            true
        }
        
        try {
            wm.addView(container, params)
            floatingButtonView = container
            updateFloatingButtonVisuals()
            Log.d(TAG, "Added custom floating scan button overlay")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding custom floating button overlay", e)
        }
    }

    fun hideFloatingButton() {
        val view = floatingButtonView ?: return
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        try {
            wm.removeView(view)
            floatingButtonView = null
            Log.d(TAG, "Removed custom floating scan button overlay")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing custom floating button overlay", e)
        }
    }

    private fun updateFloatingButtonVisuals() {
        val view = floatingButtonView as? FrameLayout ?: return
        val density = resources.displayMetrics.density
        val color = if (currentSessionId != null) "#00E676" else "#007FFF" // Green when active/recording, AzureBlue when inactive
        val shape = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor(color))
            setStroke((2 * density).toInt(), Color.WHITE)
        }
        view.background = shape
        val textView = view.getChildAt(0) as? TextView
        textView?.text = "SCAN"
        view.contentDescription = if (currentSessionId != null) {
            "AMASAMYA Scan active. Double tap to scan screen, long press to stop session."
        } else {
            "AMASAMYA Scan inactive. Double tap to start session and scan."
        }
        try {
            view.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun showNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun dismissNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID)
    }

    // Full Screen Traverser (Automated Scan)
    fun performFullScreenScan() {
        val sessionId = currentSessionId
        if (sessionId == null) {
            speak("Cannot scan. Please start an audit session first.")
            return
        }

        val rootNode = rootInActiveWindow
        if (rootNode == null) {
            speak("Scan failed. No active layout found.")
            return
        }

        speak("Scanning complete screen. Please wait.")

        val initialCount = dbHelper.getIssuesForSession(sessionId).size
        val density = resources.displayMetrics.density
        val wcagLevel = settingsManager.wcagLevel

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val mainExecutor = Executor { command -> Handler(Looper.getMainLooper()).post(command) }
            try {
                takeScreenshot(android.view.Display.DEFAULT_DISPLAY, mainExecutor, object : TakeScreenshotCallback {
                    override fun onSuccess(screenshotResult: ScreenshotResult) {
                        val hardwareBuffer = screenshotResult.hardwareBuffer
                        val bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, null)
                        val softwareBitmap = bitmap?.copy(Bitmap.Config.ARGB_8888, false)
                        
                        val nodeList = mutableListOf<A11yNodeData>()
                        captureLayoutHierarchy(rootNode, nodeList)
                        rootNode.recycle()
                        
                        bitmap?.recycle()
                        hardwareBuffer.close()
                        
                        runScanAnalysis(sessionId, nodeList, density, wcagLevel, softwareBitmap, initialCount)
                    }

                    override fun onFailure(errorCode: Int) {
                        Log.e(TAG, "Screenshot failed: $errorCode. Performing text-only scan.")
                        val nodeList = mutableListOf<A11yNodeData>()
                        captureLayoutHierarchy(rootNode, nodeList)
                        rootNode.recycle()
                        
                        runScanAnalysis(sessionId, nodeList, density, wcagLevel, null, initialCount)
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initiate screenshot scan", e)
                val nodeList = mutableListOf<A11yNodeData>()
                captureLayoutHierarchy(rootNode, nodeList)
                rootNode.recycle()
                
                runScanAnalysis(sessionId, nodeList, density, wcagLevel, null, initialCount)
            }
        } else {
            val nodeList = mutableListOf<A11yNodeData>()
            captureLayoutHierarchy(rootNode, nodeList)
            rootNode.recycle()
            
            runScanAnalysis(sessionId, nodeList, density, wcagLevel, null, initialCount)
        }
    }

    private fun runScanAnalysis(
        sessionId: Long,
        nodeList: List<A11yNodeData>,
        density: Float,
        wcagLevel: String,
        screenBitmap: Bitmap?,
        initialCount: Int
    ) {
        backgroundExecutor.execute {
            try {
                for (nodeData in nodeList) {
                    val issues = evaluateNodeData(nodeData, density, wcagLevel, screenBitmap, this@A11yAuditService)
                    for (issue in issues) {
                        val dbIssue = ElementIssue(
                            sessionId = sessionId,
                            screenName = lastScreenName,
                            className = nodeData.className,
                            bounds = getBoundsString(nodeData.bounds),
                            text = nodeData.text,
                            contentDescription = nodeData.contentDescription,
                            issueType = issue.type,
                            severity = issue.severity,
                            description = issue.description,
                            wcagSc = issue.wcagSc
                        )
                        dbHelper.insertIssue(dbIssue)
                    }
                }
                
                // Screen level rules (Friction & Redundancy)
                val screenIssues = evaluateScreenLevelRules(nodeList, density, wcagLevel, screenBitmap, this@A11yAuditService)
                for (issue in screenIssues) {
                    val dbIssue = ElementIssue(
                        sessionId = sessionId,
                        screenName = lastScreenName,
                        className = "Screen Level Rule",
                        bounds = "[0,0][0,0]",
                        text = "",
                        contentDescription = "",
                        issueType = issue.type,
                        severity = issue.severity,
                        description = issue.description,
                        wcagSc = issue.wcagSc
                    )
                    dbHelper.insertIssue(dbIssue)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error performing background scan analysis", e)
            } finally {
                screenBitmap?.recycle()
                Handler(Looper.getMainLooper()).post {
                    announceScanResults(sessionId, initialCount, wcagLevel)
                }
            }
        }
    }

    private fun saveFrictionMetric(sessionId: Long, screenName: String, count: Int) {
        backgroundExecutor.execute {
            try {
                val dbIssue = ElementIssue(
                    sessionId = sessionId,
                    screenName = screenName,
                    className = "Screen Navigation",
                    bounds = "[0,0][0,0]",
                    text = "",
                    contentDescription = "",
                    issueType = "Friction Metric",
                    severity = "Info",
                    description = "TalkBack user performed $count swipe focus navigation movements on this screen.",
                    wcagSc = "2.4.3"
                )
                dbHelper.insertIssue(dbIssue)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save swipe friction metric", e)
            }
        }
    }

    private fun announceScanResults(sessionId: Long, initialCount: Int, wcagLevel: String) {
        val newCount = dbHelper.getIssuesForSession(sessionId).size
        val issuesFound = newCount - initialCount

        if (issuesFound > 0) {
            speak("Scan completed. Found $issuesFound accessibility issues under WCAG 2.2 Level $wcagLevel. Open the report to review the issues.")
        } else {
            speak("Scan completed. No new accessibility issues found under WCAG 2.2 Level $wcagLevel.")
        }
    }

    private fun hasTextInSubtree(node: AccessibilityNodeInfo): Boolean {
        if (!node.text.isNullOrBlank() || !node.contentDescription.isNullOrBlank()) {
            return true
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val hasText = hasTextInSubtree(child)
                child.recycle()
                if (hasText) return true
            }
        }
        return false
    }

    private fun captureLayoutHierarchy(node: AccessibilityNodeInfo?, list: MutableList<A11yNodeData>) {
        if (node == null) return
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        val hasText = hasTextInSubtree(node)
        list.add(
            A11yNodeData(
                className = node.className?.toString() ?: "",
                text = node.text?.toString() ?: "",
                contentDescription = node.contentDescription?.toString() ?: "",
                bounds = bounds,
                isClickable = node.isClickable,
                isFocusable = node.isFocusable,
                isHeading = node.isHeading,
                isVisibleToUser = node.isVisibleToUser,
                hasTextInSubtree = hasText,
                isFocused = node.isFocused || node.isAccessibilityFocused,
                isEditable = node.isEditable,
                liveRegion = node.liveRegion
            )
        )
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            captureLayoutHierarchy(child, list)
            child?.recycle()
        }
    }

    // Continuous Live Element Audit
    private fun auditFocusedElement(node: AccessibilityNodeInfo) {
        val sessionId = currentSessionId ?: return
        val density = resources.displayMetrics.density
        val wcagLevel = settingsManager.wcagLevel

        // Extract properties on main thread (AccessibilityNodeInfo is not thread-safe)
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        val className = node.className?.toString() ?: "Unknown"
        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        val isClickable = node.isClickable
        val isFocusable = node.isFocusable
        val isHeading = node.isHeading
        val isVisible = node.isVisibleToUser
        val hasText = hasTextInSubtree(node)
        val isFocused = node.isFocused || node.isAccessibilityFocused
        val isEditable = node.isEditable
        val liveRegion = node.liveRegion

        backgroundExecutor.execute {
            try {
                val nodeData = A11yNodeData(
                    className = className,
                    text = text,
                    contentDescription = desc,
                    bounds = bounds,
                    isClickable = isClickable,
                    isFocusable = isFocusable,
                    isHeading = isHeading,
                    isVisibleToUser = isVisible,
                    hasTextInSubtree = hasText,
                    isFocused = isFocused,
                    isEditable = isEditable,
                    liveRegion = liveRegion
                )
                val issues = evaluateNodeData(nodeData, density, wcagLevel, null, this@A11yAuditService)
                if (issues.isNotEmpty()) {
                    readingOrderIndex++
                    for (issue in issues) {
                        val dbIssue = ElementIssue(
                            sessionId = sessionId,
                            screenName = lastScreenName,
                            className = nodeData.className,
                            bounds = getBoundsString(nodeData.bounds),
                            text = nodeData.text,
                            contentDescription = nodeData.contentDescription,
                            issueType = issue.type,
                            severity = issue.severity,
                            description = "${issue.description} (Found at navigation index $readingOrderIndex)",
                            wcagSc = issue.wcagSc
                        )
                        dbHelper.insertIssue(dbIssue)
                    }

                    // Play short warning indicator if haptics enabled
                    if (settingsManager.isHapticFeedbackEnabled) {
                        Handler(Looper.getMainLooper()).post {
                            performHapticFeedback()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error performing background live focus audit", e)
            }
        }
    }

    private fun monitorLiveRegionChange(node: AccessibilityNodeInfo) {
        val sessionId = currentSessionId ?: return
        
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        val className = node.className?.toString() ?: ""
        val text = node.text?.toString() ?: ""
        val isVisible = node.isVisibleToUser
        val isFocused = node.isFocused || node.isAccessibilityFocused
        val isEditable = node.isEditable
        val liveRegion = node.liveRegion
        
        val nodeData = A11yNodeData(
            className = className,
            text = text,
            contentDescription = "",
            bounds = bounds,
            isClickable = node.isClickable,
            isFocusable = node.isFocusable,
            isHeading = node.isHeading,
            isVisibleToUser = isVisible,
            hasTextInSubtree = true,
            isFocused = isFocused,
            isEditable = isEditable,
            liveRegion = liveRegion
        )
        
        val violation = checkMissingLiveRegion(nodeData)
        if (violation != null) {
            val boundsStr = getBoundsString(bounds)
            val key = "$boundsStr-$className"
            if (flaggedLiveRegions.add(key)) {
                backgroundExecutor.execute {
                    try {
                        val dbIssue = ElementIssue(
                            sessionId = sessionId,
                            screenName = lastScreenName,
                            className = className,
                            bounds = boundsStr,
                            text = text,
                            contentDescription = "",
                            issueType = violation.type,
                            severity = violation.severity,
                            description = violation.description,
                            wcagSc = violation.wcagSc
                        )
                        dbHelper.insertIssue(dbIssue)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to insert missing live region issue", e)
                    }
                }
            }
        }
    }

    private fun recordFocusPathNode(node: AccessibilityNodeInfo) {
        val sessionId = currentSessionId ?: return
        
        // Extract properties on the main thread
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        val boundsStr = getBoundsString(bounds)
        val className = node.className?.toString() ?: "Unknown"
        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        
        focusOrderCounter++
        val order = focusOrderCounter
        val screen = lastScreenName
        
        backgroundExecutor.execute {
            try {
                val dbNode = FocusPathNode(
                    sessionId = sessionId,
                    screenName = screen,
                    className = className,
                    bounds = boundsStr,
                    text = text,
                    contentDescription = desc,
                    focusOrder = order
                )
                dbHelper.insertFocusNode(dbNode)
                Log.d(TAG, "Recorded focus path node: $className order $order")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to record focus path node", e)
            }
        }
    }

    private fun getBoundsString(rect: Rect?): String {
        if (rect == null) return "[0,0][0,0]"
        return "[${rect.left},${rect.top}][${rect.right},${rect.bottom}]"
    }

    private fun getBoundsString(node: AccessibilityNodeInfo): String {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        return getBoundsString(rect)
    }



    private fun performHapticFeedback() {
        // Quick short vibration buzz
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(100, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(100)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Haptic feedback failed", e)
        }
    }

    // System Notification Controls
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AMASAMYA Service Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows recording controls for AMASAMYA audit sessions"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val startSessionIntent = Intent(this, A11yAuditService::class.java).apply { action = ACTION_START_SESSION }
        val startSessionPendingIntent = PendingIntent.getService(
            this, 0, startSessionIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val triggerScanIntent = Intent(this, A11yAuditService::class.java).apply { action = ACTION_TRIGGER_SCAN }
        val triggerScanPendingIntent = PendingIntent.getService(
            this, 1, triggerScanIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopSessionIntent = Intent(this, A11yAuditService::class.java).apply { action = ACTION_STOP_SESSION }
        val stopSessionPendingIntent = PendingIntent.getService(
            this, 2, stopSessionIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val toggleFloatingIntent = Intent(this, A11yAuditService::class.java).apply { action = ACTION_TOGGLE_FLOATING_BUTTON }
        val toggleFloatingPendingIntent = PendingIntent.getService(
            this, 3, toggleFloatingIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val isSessionActive = currentSessionId != null
        val title = if (isSessionActive) "AMASAMYA: Session Active" else "AMASAMYA: Service Active"
        val contentText = if (isSessionActive) "Recording session: $currentSessionName" else "Ready to run accessibility audits"

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        builder.setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setContentIntent(pendingIntent)

        val isFloatingEnabled = settingsManager.isFloatingButtonEnabled
        val toggleLabel = if (isFloatingEnabled) "Hide Button" else "Show Button"
        val toggleIcon = if (isFloatingEnabled) android.R.drawable.ic_menu_close_clear_cancel else android.R.drawable.ic_menu_add

        if (isSessionActive) {
            builder.addAction(
                android.R.drawable.ic_menu_search,
                "Start Scan",
                triggerScanPendingIntent
            )
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop Session",
                stopSessionPendingIntent
            )
            builder.addAction(
                toggleIcon,
                toggleLabel,
                toggleFloatingPendingIntent
            )
        } else {
            builder.addAction(
                android.R.drawable.ic_menu_search,
                "Start Scan",
                triggerScanPendingIntent
            )
            builder.addAction(
                android.R.drawable.ic_media_play,
                "Start Session",
                startSessionPendingIntent
            )
            builder.addAction(
                toggleIcon,
                toggleLabel,
                toggleFloatingPendingIntent
            )
        }

        return builder.build()
    }
}
