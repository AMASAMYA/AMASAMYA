package com.example.amasamya.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.example.amasamya.db.ElementIssue
import java.io.File
import java.io.FileOutputStream

object AnnotatedScreenshotExporter {

    fun generateAnnotatedBitmap(originalBitmap: Bitmap, issues: List<ElementIssue>): Bitmap {
        val mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        val strokePaintCritical = Paint().apply {
            color = Color.parseColor("#FF3B30") // Neon Red
            style = Paint.Style.STROKE
            strokeWidth = 6f
            isAntiAlias = true
        }

        val fillPaintCritical = Paint().apply {
            color = Color.parseColor("#33FF3B30")
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val strokePaintWarning = Paint().apply {
            color = Color.parseColor("#FFB300") // Amber Gold
            style = Paint.Style.STROKE
            strokeWidth = 5f
            isAntiAlias = true
        }

        val fillPaintWarning = Paint().apply {
            color = Color.parseColor("#33FFB300")
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val badgeBgPaint = Paint().apply {
            color = Color.parseColor("#131824") // Dark surface
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val badgeTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 24f
            isAntiAlias = true
            isFakeBoldText = true
        }

        issues.forEachIndexed { index, issue ->
            val rect = parseBounds(issue.bounds)
            if (rect != null && rect.width() > 0 && rect.height() > 0) {
                val rectF = RectF(rect)
                val isCritical = issue.severity == "Critical"
                val strokePaint = if (isCritical) strokePaintCritical else strokePaintWarning
                val fillPaint = if (isCritical) fillPaintCritical else fillPaintWarning

                // Draw bounding box fill and outline
                canvas.drawRoundRect(rectF, 8f, 8f, fillPaint)
                canvas.drawRoundRect(rectF, 8f, 8f, strokePaint)

                // Draw badge number label
                val badgeText = "#${index + 1} (${issue.wcagSc})"
                val textWidth = badgeTextPaint.measureText(badgeText)
                val badgeRect = RectF(
                    rectF.left,
                    (rectF.top - 36f).coerceAtLeast(10f),
                    rectF.left + textWidth + 16f,
                    (rectF.top - 36f).coerceAtLeast(10f) + 32f
                )
                canvas.drawRoundRect(badgeRect, 6f, 6f, badgeBgPaint)
                canvas.drawText(badgeText, badgeRect.left + 8f, badgeRect.bottom - 8f, badgeTextPaint)
            }
        }

        return mutableBitmap
    }

    fun exportAnnotatedScreenshotFile(context: Context, originalBitmap: Bitmap, issues: List<ElementIssue>, fileName: String): File? {
        return try {
            val annotated = generateAnnotatedBitmap(originalBitmap, issues)
            val dir = File(context.getExternalFilesDir(null), "annotated_screenshots")
            if (!dir.exists()) dir.mkdirs()

            val file = File(dir, fileName)
            FileOutputStream(file).use { fos ->
                annotated.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
            annotated.recycle()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseBounds(boundsStr: String): Rect? {
        return try {
            val pattern = java.util.regex.Pattern.compile("\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]")
            val matcher = pattern.matcher(boundsStr)
            if (matcher.find()) {
                val left = matcher.group(1)?.toInt() ?: 0
                val top = matcher.group(2)?.toInt() ?: 0
                val right = matcher.group(3)?.toInt() ?: 0
                val bottom = matcher.group(4)?.toInt() ?: 0
                Rect(left, top, right, bottom)
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
