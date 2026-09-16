package com.example.amasamya.utils

import com.example.amasamya.db.ElementIssue

object AccessibilityScorecard {

    data class ScorecardResult(
        val scorePercent: Int,
        val grade: String,
        val statusSummary: String,
        val totalElementsScanned: Int,
        val criticalCount: Int,
        val warningCount: Int,
        val infoCount: Int,
        val frictionLevel: String
    )

    fun calculateScorecard(
        issues: List<ElementIssue>,
        totalElementsScanned: Int = 0,
        frictionLevel: String = "Low Friction"
    ): ScorecardResult {
        val criticalCount = issues.count { it.severity == "Critical" }
        val warningCount = issues.count { it.severity == "Warning" }
        val infoCount = issues.count { it.severity == "Info" }

        var score = 100 - (criticalCount * 15) - (warningCount * 5)
        if (frictionLevel == "High Navigation Fatigue") {
            score -= 10
        } else if (frictionLevel == "Moderate Load") {
            score -= 5
        }

        if (score < 0) score = 0
        if (score > 100) score = 100

        val (grade, summary) = when {
            score >= 95 -> Pair("A+", "Exceptional - Fully Accessible & Barrier Free")
            score >= 85 -> Pair("A", "Good - Minor Warnings Only")
            score >= 70 -> Pair("B", "Moderate Friction - Accessibility Improvements Recommended")
            score >= 50 -> Pair("C", "Significant Barriers - Accessibility Remediation Required")
            else -> Pair("F", "Critical Non-Compliance - Severe Access Barriers")
        }

        return ScorecardResult(
            scorePercent = score,
            grade = grade,
            statusSummary = summary,
            totalElementsScanned = if (totalElementsScanned > 0) totalElementsScanned else issues.size + 10,
            criticalCount = criticalCount,
            warningCount = warningCount,
            infoCount = infoCount,
            frictionLevel = frictionLevel
        )
    }
}
