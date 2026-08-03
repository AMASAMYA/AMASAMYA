package com.example.amasamya.utils

import com.example.amasamya.db.ElementIssue
import com.example.amasamya.settings.SettingsManager
import java.util.regex.Pattern

object PersonaReportHelper {

    fun getPersonaDescription(issue: ElementIssue, persona: String): String {
        val type = issue.issueType
        val desc = issue.description
        val text = issue.text
        val contentDesc = issue.contentDescription
        val className = issue.className
        val wcagSc = issue.wcagSc
        val severity = issue.severity

        return when (persona) {
            SettingsManager.PERSONA_DEVELOPER -> getDeveloperDescription(type, desc, text, contentDesc, className, wcagSc, severity)
            SettingsManager.PERSONA_TESTER -> getTesterDescription(type, desc, text, contentDesc, className, wcagSc, severity)
            SettingsManager.PERSONA_DESIGNER -> getDesignerDescription(type, desc, text, contentDesc, className, wcagSc, severity)
            SettingsManager.PERSONA_PRODUCT_OWNER -> getProductOwnerDescription(type, desc, text, contentDesc, className, wcagSc, severity)
            else -> desc // fallback is General User
        }
    }

    private fun parseTargetDimensions(desc: String): Pair<Int, Int> {
        val pattern = Pattern.compile("measures only (\\d+) by (\\d+)")
        val matcher = pattern.matcher(desc)
        if (matcher.find()) {
            val w = matcher.group(1)?.toIntOrNull() ?: 0
            val h = matcher.group(2)?.toIntOrNull() ?: 0
            return Pair(w, h)
        }
        return Pair(0, 0)
    }

    private fun parseTargetExpected(desc: String): Int {
        if (desc.contains("at least 48")) return 48
        if (desc.contains("at least 24")) return 24
        return 48
    }

    private fun parseRedundantWord(desc: String): String {
        val pattern = Pattern.compile("contains the word '([^']+)'")
        val matcher = pattern.matcher(desc)
        if (matcher.find()) {
            return matcher.group(1) ?: "button"
        }
        return "button"
    }

    private fun parseContrastDetails(desc: String): Triple<String, String, String> {
        val ratioPattern = Pattern.compile("contrast score is only ([\\d.]+) out of 10")
        val targetPattern = Pattern.compile("should be at least ([\\d.]+) out of 10")
        val colorPattern = Pattern.compile("suggest changing the text color to (#\\w+)")

        var ratio = "0.0"
        var target = "4.5"
        var color = "#FFFFFF"

        val rm = ratioPattern.matcher(desc)
        if (rm.find()) {
            ratio = rm.group(1) ?: "0.0"
        }
        val tm = targetPattern.matcher(desc)
        if (tm.find()) {
            target = tm.group(1) ?: "4.5"
        }
        val cm = colorPattern.matcher(desc)
        if (cm.find()) {
            color = cm.group(1) ?: "#FFFFFF"
        }
        return Triple(ratio, target, color)
    }

    private fun parseFrictionCount(desc: String): Int {
        val pattern = Pattern.compile("cluttered with (\\d+) items")
        val matcher = pattern.matcher(desc)
        if (matcher.find()) {
            return matcher.group(1)?.toIntOrNull() ?: 0
        }
        return 0
    }

    private fun parseRedundantLabels(desc: String): Pair<String, String> {
        val samePattern = Pattern.compile("exact same label '([^']+)'")
        val diffPattern = Pattern.compile("one labeled '([^']+)' and the other empty")

        val sm = samePattern.matcher(desc)
        if (sm.find()) {
            val label = sm.group(1) ?: ""
            return Pair(label, label)
        }
        val dm = diffPattern.matcher(desc)
        if (dm.find()) {
            val label = dm.group(1) ?: ""
            return Pair(label, "")
        }
        return Pair("", "")
    }

    private fun getDeveloperDescription(
        type: String,
        desc: String,
        text: String,
        contentDesc: String,
        className: String,
        wcagSc: String,
        severity: String
    ): String {
        return when (type) {
            "Target Size" -> {
                val (w, h) = parseTargetDimensions(desc)
                val target = parseTargetExpected(desc)
                "Touch target size ${w}x${h}dp is below WCAG recommendation. Remedy: Increase touch bounds to at least ${target}x${target}dp. In Jetpack Compose, use 'Modifier.requiredSizeIn(minWidth = ${target}.dp, minHeight = ${target}.dp)' or increase ContentPadding on Buttons. In XML layouts, set 'android:minWidth=\"${target}dp\"' and 'android:minHeight=\"${target}dp\"'."
            }
            "Missing Label" -> {
                "Interactive component has empty text and contentDescription. Remedy: Set a valid content description. In Jetpack Compose, add 'contentDescription = \"string_resource\"' to the Modifier/Composable. In XML layouts, set 'android:contentDescription=\"@string/label\"' on the View."
            }
            "Redundant Label" -> {
                if (desc.contains("exactly the same")) {
                    "The contentDescription '$contentDesc' is exactly the same as the visible text '$text'. This causes screen readers to read the same text twice in a row. Remedy: Remove the redundant 'contentDescription' property."
                } else {
                    val word = parseRedundantWord(desc)
                    "The description '$contentDesc' contains the redundant word '$word'. Screen readers automatically announce the widget role based on the class/semantics role. Remedy: Remove '$word' from the content description."
                }
            }
            "Focus Noise" -> {
                "Non-clickable element has focusable=true but is empty. Remedy: If decorative, set focusable=false or use 'Modifier.semantics { invisibleToUser() }' / 'android:importantForAccessibility=\"no\"'."
            }
            "Color Contrast" -> {
                val (ratio, target, color) = parseContrastDetails(desc)
                "Color contrast ratio ${ratio}:1 is below the target ${target}:1. Remedy: Change text color to '${color}' or adjust the background. In Compose, verify Color declarations in your theme. In XML, update the text color layout attribute."
            }
            "Friction Warning" -> {
                val count = parseFrictionCount(desc)
                "Cluttered screen with $count focus points. Remedy: Use 'Modifier.semantics(mergeDescendants = true) {}' on containers to group text blocks, or make secondary elements non-focusable."
            }
            "Redundant Click Target" -> {
                "Adjacent elements perform the same click action. Remedy: Group adjacent clickable icon and text views under a single clickable container with merged semantics, and remove clickability from the individual sub-elements."
            }
            "Missing Live Region" -> {
                "Background text update on $className lacks liveRegion. Remedy: In Compose, set 'liveRegion = LiveRegionMode.Polite' in Modifier semantics. In XML layouts, set 'android:accessibilityLiveRegion=\"polite\"'."
            }
            else -> desc
        }
    }

    private fun getTesterDescription(
        type: String,
        desc: String,
        text: String,
        contentDesc: String,
        className: String,
        wcagSc: String,
        severity: String
    ): String {
        return when (type) {
            "Target Size" -> {
                val (w, h) = parseTargetDimensions(desc)
                val target = parseTargetExpected(desc)
                "This item is too small to select easily (${w}x${h}dp). To verify: Try tapping the button using a physical finger. Observe if it requires precise aim or causes accidental clicks on neighboring elements. Recommended size is at least ${target}x${target}dp."
            }
            "Missing Label" -> {
                "The element is unlabelled. To verify: Turn on TalkBack and navigate to this item. Verify if TalkBack announces it as 'unlabelled button' or remains silent, leaving no verbal cue of its action."
            }
            "Redundant Label" -> {
                if (desc.contains("exactly the same")) {
                    "The description is duplicate. To verify: Turn on TalkBack and focus the item. Verify if TalkBack reads the text twice in a row (e.g. 'Back, Back'), causing repetitive feedback."
                } else {
                    val word = parseRedundantWord(desc)
                    "The description contains redundant role naming. To verify: Turn on TalkBack. Focus the item and verify if it double-announces the role (e.g., 'Submit button button'), causing repetitive speech feedback."
                }
            }
            "Focus Noise" -> {
                "Silent focus block. To verify: Turn on TalkBack and swipe through the screen. Verify if the focus cursor highlights an empty item that says nothing, forcing unnecessary extra swipes."
            }
            "Color Contrast" -> {
                val (ratio, target, color) = parseContrastDetails(desc)
                "Low contrast text. To verify: Check readability under dim lighting or simulated sunlight glare. Observe if the text blends into the background."
            }
            "Friction Warning" -> {
                val count = parseFrictionCount(desc)
                "High navigation friction. To verify: Swipe through the entire screen from top to bottom. Count the swipes needed. Verify if navigating takes too long ($count focus points)."
            }
            "Redundant Click Target" -> {
                "Adjacent elements do the same thing. To verify: Turn on TalkBack. Focus and click the adjacent elements. Verify if both trigger the same action or navigate to the same page."
            }
            "Missing Live Region" -> {
                "Unannounced status changes. To verify: Perform an action that triggers a status update (like submitting a form or starting a download). Verify if TalkBack immediately announces the completion status without requiring the user to swipe and find it manually."
            }
            else -> desc
        }
    }

    private fun getDesignerDescription(
        type: String,
        desc: String,
        text: String,
        contentDesc: String,
        className: String,
        wcagSc: String,
        severity: String
    ): String {
        return when (type) {
            "Target Size" -> {
                val (w, h) = parseTargetDimensions(desc)
                val target = parseTargetExpected(desc)
                "Tap target area (${w}x${h}dp) is below the minimum layout guideline of ${target}x${target}dp. Action: Expand the clickable boundary box or add extra touch padding around the element to make it touch-friendly."
            }
            "Missing Label" -> {
                "An interactive element (such as an icon button) has no text label or description. Action: Ensure that every graphical control has an underlying text description explaining its function, not its visual appearance."
            }
            "Redundant Label" -> {
                if (desc.contains("exactly the same")) {
                    "Avoid using contentDescription that matches visible text. Screen readers automatically read the visible text, so setting a duplicate contentDescription causes double-reading."
                } else {
                    val word = parseRedundantWord(desc)
                    "Avoid including descriptor words like '$word' in the description of interactive controls. The platform automatically appends the control's role. Keep labels concise."
                }
            }
            "Focus Noise" -> {
                "Decorative layout elements or empty container backgrounds are in the screen reader's focus path. They should be marked as invisible to screen readers to keep the user experience clean."
            }
            "Color Contrast" -> {
                val (ratio, target, color) = parseContrastDetails(desc)
                "The text contrast ratio is ${ratio}:1, failing the WCAG requirement of ${target}:1. Action: Adjust the text or background color. Suggested compliant hex: ${color}."
            }
            "Friction Warning" -> {
                val count = parseFrictionCount(desc)
                "This screen has high information density ($count focus points). Redraw the layout to group related texts or buttons into single visual card components with merged accessibility states."
            }
            "Redundant Click Target" -> {
                "Separate clickable targets are placed for an icon and its text label. Design them as a single combined button with a unified ripple feedback effect."
            }
            "Missing Live Region" -> {
                "Ensure dynamic status changes (like confirmation banners or loading indicators) are visually positioned and tagged to be announced instantly, preventing visual state desynchronization."
            }
            else -> desc
        }
    }

    private fun getProductOwnerDescription(
        type: String,
        desc: String,
        text: String,
        contentDesc: String,
        className: String,
        wcagSc: String,
        severity: String
    ): String {
        return when (type) {
            "Target Size" -> {
                val target = parseTargetExpected(desc)
                "Touch targets below ${target}x${target}dp create high friction. Elderly users or users with motor impairments will struggle to press them. Violates WCAG SC ${wcagSc} (${severity})."
            }
            "Missing Label" -> {
                "Critical compliance barrier. Blind users cannot navigate past this screen because essential buttons are unlabelled, violating WCAG SC 1.1.1 (Level A). High risk of abandonment."
            }
            "Redundant Label" -> {
                "Minor usability friction. Repetitive audio reading ('Home button button') slows down screen reader users and makes the app feel unpolished. Violates WCAG SC 1.1.1."
            }
            "Focus Noise" -> {
                "Aesthetic and navigation clutter. Forces screen reader users to swipe through empty, silent slots. Violates WCAG SC 4.1.2."
            }
            "Color Contrast" -> {
                val (ratio, target, color) = parseContrastDetails(desc)
                "Legibility barrier for users with low vision or working in bright environments. Contrast ratio of ${ratio}:1 fails WCAG SC ${wcagSc} target of ${target}:1. Compliance blocker."
            }
            "Friction Warning" -> {
                val count = parseFrictionCount(desc)
                "Screen has high traversal count ($count items), leading to fatigue and high drop-off rates for assistive tech users. Simplify screen structure. Violates WCAG SC 2.4.3."
            }
            "Redundant Click Target" -> {
                "Redundant options double the time to read the screen. Combine them to streamline the user journey and reduce cognitive load. Violates WCAG SC 2.4.4."
            }
            "Missing Live Region" -> {
                "Silent updates. Users won't know if their action succeeded or failed, leading to double-submissions or confusion. Violates WCAG SC 4.1.3."
            }
            else -> desc
        }
    }
}
