package com.example.amasamya.utils

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log

object PdfRemediationEngine {

    private const val TAG = "PdfRemediationEngine"

    data class PdfTagNode(
        val id: String,
        var tagType: String, // "H1", "H2", "H3", "P", "Table", "TH", "TD", "Figure"
        var contentText: String,
        var altText: String = "",
        var isRemediated: Boolean = false
    )

    data class PdfRemediationReport(
        val documentName: String,
        val totalPages: Int,
        val totalNodes: Int,
        val untaggedFiguresCount: Int,
        val missingHeadingsCount: Int,
        val tagNodes: List<PdfTagNode>,
        val complianceScore: Int
    )

    fun analyzeDocument(documentName: String, rawTextContent: String): PdfRemediationReport {
        val lines = rawTextContent.split("\n").filter { it.isNotBlank() }
        val tagNodes = mutableListOf<PdfTagNode>()

        var idCounter = 1
        for (line in lines) {
            val trimmed = line.trim()
            val tagType = when {
                idCounter == 1 || (trimmed.length < 50 && (trimmed.startsWith("#") || trimmed.contains("Guidelines") || trimmed.contains("Title") || (trimmed.equals(trimmed.uppercase()) && trimmed.length > 5))) -> "H1"
                trimmed.length < 60 && (trimmed.endsWith(":") || trimmed.startsWith("Section")) -> "H2"
                trimmed.startsWith("Table:") || trimmed.contains(" | ") -> "Table"
                trimmed.startsWith("[Image]") || trimmed.startsWith("[Figure]") || trimmed.contains("Photo") -> "Figure"
                else -> "P"
            }

            val altText = if (tagType == "Figure") "Graphic image representation: ${trimmed.take(30)}" else ""
            
            tagNodes.add(
                PdfTagNode(
                    id = "tag_${idCounter++}",
                    tagType = tagType,
                    contentText = trimmed,
                    altText = altText,
                    isRemediated = tagType != "Figure" || altText.isNotBlank()
                )
            )
        }

        val untaggedFigures = tagNodes.count { it.tagType == "Figure" && it.altText.isBlank() }
        val missingHeadings = if (tagNodes.none { it.tagType == "H1" }) 1 else 0

        val score = (100 - (untaggedFigures * 20) - (missingHeadings * 15)).coerceIn(0, 100)

        return PdfRemediationReport(
            documentName = documentName,
            totalPages = (tagNodes.size / 15).coerceAtLeast(1),
            totalNodes = tagNodes.size,
            untaggedFiguresCount = untaggedFigures,
            missingHeadingsCount = missingHeadings,
            tagNodes = tagNodes,
            complianceScore = score
        )
    }

    fun generateRemediatedHtml(report: PdfRemediationReport): String {
        val sb = StringBuilder()
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n")
        sb.append("<title>").append(report.documentName).append(" - PDF/UA Tagged Document</title>\n")
        sb.append("<style>body{font-family:sans-serif;line-height:1.6;padding:20px;} h1{color:#00e5ff;} h2{color:#30d158;} .figure{border:1px solid #ccc;padding:10px;margin:10px 0;}</style>\n")
        sb.append("</head>\n<body>\n")

        for (node in report.tagNodes) {
            when (node.tagType) {
                "H1" -> sb.append("<h1>").append(node.contentText).append("</h1>\n")
                "H2" -> sb.append("<h2>").append(node.contentText).append("</h2>\n")
                "H3" -> sb.append("<h3>").append(node.contentText).append("</h3>\n")
                "Figure" -> sb.append("<div class=\"figure\" role=\"img\" aria-label=\"").append(node.altText.ifBlank { node.contentText }).append("\"><p>").append(node.contentText).append("</p></div>\n")
                else -> sb.append("<p>").append(node.contentText).append("</p>\n")
            }
        }

        sb.append("</body>\n</html>")
        return sb.toString()
    }
}
