package com.example.amasamya.utils

import com.example.amasamya.db.ElementIssue
import com.example.amasamya.db.FocusPathNode

object UtteranceFlowEstimator {

    // Average TalkBack speech speed: ~180 words per minute (3 words per second)
    private const val WORDS_PER_SECOND = 3.0

    data class UtteranceItem(
        val nodeOrder: Int,
        val className: String,
        val spokenText: String,
        val wordCount: Int,
        val durationSeconds: Double
    )

    data class ScreenUtteranceReport(
        val screenName: String,
        val totalFocusableNodes: Int,
        val totalWordCount: Int,
        val estimatedReadingTimeSeconds: Int,
        val fatigueLevel: String, // "Low", "Moderate", "High"
        val utteranceItems: List<UtteranceItem>,
        val recommendations: List<String>
    )

    fun estimateScreenFlow(
        screenName: String,
        issues: List<ElementIssue>,
        focusNodes: List<FocusPathNode>
    ): ScreenUtteranceReport {
        val screenIssues = issues.filter { it.screenName == screenName }
        val screenFocusNodes = focusNodes.filter { it.screenName == screenName }

        val items = mutableListOf<UtteranceItem>()
        var totalWords = 0

        if (screenFocusNodes.isNotEmpty()) {
            screenFocusNodes.sortedBy { it.focusOrder }.forEachIndexed { index, node ->
                val text = when {
                    node.contentDescription.isNotBlank() -> node.contentDescription
                    node.text.isNotBlank() -> node.text
                    else -> "Unlabelled element ${node.className.substringAfterLast('.')}"
                }
                val wordCount = countWords(text)
                val duration = wordCount / WORDS_PER_SECOND
                totalWords += wordCount

                items.add(
                    UtteranceItem(
                        nodeOrder = index + 1,
                        className = node.className,
                        spokenText = text,
                        wordCount = wordCount,
                        durationSeconds = duration
                    )
                )
            }
        } else {
            // Fallback to synthesizing from issues if focus path nodes aren't recorded
            screenIssues.forEachIndexed { index, issue ->
                val text = when {
                    issue.contentDescription.isNotBlank() -> issue.contentDescription
                    issue.text.isNotBlank() -> issue.text
                    else -> "Issue element ${issue.className.substringAfterLast('.')}"
                }
                val wordCount = countWords(text)
                val duration = wordCount / WORDS_PER_SECOND
                totalWords += wordCount

                items.add(
                    UtteranceItem(
                        nodeOrder = index + 1,
                        className = issue.className,
                        spokenText = text,
                        wordCount = wordCount,
                        durationSeconds = duration
                    )
                )
            }
        }

        val totalDurationSeconds = (totalWords / WORDS_PER_SECOND).toInt()

        val fatigueLevel = when {
            totalDurationSeconds > 45 || items.size > 25 -> "High (Fatigue Risk)"
            totalDurationSeconds > 20 || items.size > 12 -> "Moderate"
            else -> "Low (Optimal)"
        }

        val recommendations = mutableListOf<String>()
        if (fatigueLevel.startsWith("High")) {
            recommendations.add("Consider grouping related text nodes into single parent containers to reduce screen reader swipe stops.")
        }
        if (items.any { it.spokenText.startsWith("Unlabelled element") }) {
            recommendations.add("Provide meaningful content descriptions for unlabelled graphics to prevent fallback announcements.")
        }
        if (items.any { it.wordCount > 30 }) {
            recommendations.add("Shorten verbose content descriptions so TalkBack users can navigate rapidly.")
        }
        if (recommendations.isEmpty()) {
            recommendations.add("Screen reader speech density is well-balanced.")
        }

        return ScreenUtteranceReport(
            screenName = screenName,
            totalFocusableNodes = items.size,
            totalWordCount = totalWords,
            estimatedReadingTimeSeconds = totalDurationSeconds,
            fatigueLevel = fatigueLevel,
            utteranceItems = items,
            recommendations = recommendations
        )
    }

    private fun countWords(input: String): Int {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return 0
        
        // Detect Indic scripts (Devanagari, Tamil, Telugu, Bengali, Gujarati, Kannada)
        val isIndic = trimmed.any { ch ->
            ch in '\u0900'..'\u0DFF'
        }

        return if (isIndic) {
            // Indic language syllable tokens are denser; count word boundaries plus explicit akshara clusters
            val words = trimmed.split(Regex("\\s+")).size
            val aksharaCount = trimmed.count { ch -> ch.isLetter() }
            (words + (aksharaCount / 5)).coerceAtLeast(words)
        } else {
            trimmed.split(Regex("\\s+")).size
        }
    }
}
