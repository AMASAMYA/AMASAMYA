package com.example.amasamya.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.os.SystemClock
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View

class OverlayCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    data class TrailSegment(
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float,
        val timestamp: Long
    )

    data class TouchTarget(
        val rect: Rect,
        val widthDp: Int,
        val heightDp: Int,
        val color: Int
    )

    var showFocusTrail = false
    var showTouchTargets = false
    var showCaptions = false

    private var captionText: String = ""
    private val trailSegments = mutableListOf<TrailSegment>()
    private val touchTargets = mutableListOf<TouchTarget>()

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4FC3F7") // VibrantCyan
        strokeWidth = 6f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD60A") // AmberGold
        style = Paint.Style.FILL
    }

    private val targetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val targetTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        style = Paint.Style.FILL
    }

    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 34f
    }

    private val captionBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E6090B11") // 90% opacity DeepSpace
        style = Paint.Style.FILL
    }

    private val captionBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4FC3F7") // VibrantCyan
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    fun updateCaptions(text: String) {
        captionText = text
        postInvalidate()
    }

    fun addFocusSegment(startX: Float, startY: Float, endX: Float, endY: Float) {
        if (!showFocusTrail) return
        trailSegments.add(TrailSegment(startX, startY, endX, endY, SystemClock.uptimeMillis()))
        if (trailSegments.size > 20) {
            trailSegments.removeAt(0)
        }
        postInvalidate()
    }

    fun clearFocusTrail() {
        trailSegments.clear()
        postInvalidate()
    }

    fun updateTouchTargets(targets: List<TouchTarget>) {
        touchTargets.clear()
        if (showTouchTargets) {
            touchTargets.addAll(targets)
        }
        postInvalidate()
    }

    fun clearTouchTargets() {
        touchTargets.clear()
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val now = SystemClock.uptimeMillis()

        // 1. Draw Touch Targets boundaries
        if (showTouchTargets) {
            for (target in touchTargets) {
                targetPaint.color = target.color
                canvas.drawRect(target.rect, targetPaint)

                val label = "${target.widthDp}x${target.heightDp} dp"
                val textWidth = targetTextPaint.measureText(label)
                val textHeight = 32f
                val tagRect = RectF(
                    target.rect.left.toFloat(),
                    target.rect.top.toFloat() - textHeight - 8f,
                    target.rect.left + textWidth + 16f,
                    target.rect.top.toFloat()
                )
                if (tagRect.top < 0) {
                    tagRect.offset(0f, target.rect.height().toFloat() + textHeight + 8f)
                }

                val tagBgPaint = Paint().apply {
                    color = target.color
                    style = Paint.Style.FILL
                    alpha = 200
                }
                canvas.drawRoundRect(tagRect, 6f, 6f, tagBgPaint)
                canvas.drawText(
                    label,
                    tagRect.left + 8f,
                    tagRect.bottom - 8f,
                    targetTextPaint
                )
            }
        }

        // 2. Draw Focus Trail paths
        if (showFocusTrail) {
            val activeSegments = trailSegments.filter { now - it.timestamp <= 3000 }
            for (segment in activeSegments) {
                val elapsed = now - segment.timestamp
                val alpha = ((3000 - elapsed).coerceIn(0, 3000) * 255 / 3000).toInt()
                
                linePaint.alpha = alpha
                canvas.drawLine(segment.startX, segment.startY, segment.endX, segment.endY, linePaint)
                drawArrowhead(canvas, segment.startX, segment.startY, segment.endX, segment.endY, alpha)
            }
        }

        // 3. Draw Speech Captions Box
        if (showCaptions && captionText.isNotEmpty()) {
            val screenWidth = width.toFloat()
            val screenHeight = height.toFloat()

            val margin = 40f
            val padding = 30f
            val rectLeft = margin
            val rectRight = screenWidth - margin
            val maxTextWidth = rectRight - rectLeft - (padding * 2)

            val staticLayout = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                StaticLayout.Builder.obtain(captionText, 0, captionText.length, textPaint, maxTextWidth.toInt())
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                StaticLayout(captionText, textPaint, maxTextWidth.toInt(), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false)
            }

            val boxHeight = staticLayout.height + (padding * 2)
            val rectBottom = screenHeight - 140f
            val rectTop = rectBottom - boxHeight

            val captionRect = RectF(rectLeft, rectTop, rectRight, rectBottom)

            canvas.drawRoundRect(captionRect, 16f, 16f, captionBgPaint)
            canvas.drawRoundRect(captionRect, 16f, 16f, captionBorderPaint)

            canvas.save()
            canvas.translate(rectLeft + padding, rectTop + padding)
            staticLayout.draw(canvas)
            canvas.restore()
        }
    }

    private fun drawArrowhead(canvas: Canvas, fromX: Float, fromY: Float, toX: Float, toY: Float, alpha: Int) {
        val dx = toX - fromX
        val dy = toY - fromY
        val len = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
        if (len < 24f) return

        val ux = dx / len
        val uy = dy / len

        val arrowLength = 26f
        val arrowWidth = 18f

        val cx = toX - ux * arrowLength
        val cy = toY - uy * arrowLength

        val ax = cx + uy * (arrowWidth / 2)
        val ay = cy - ux * (arrowWidth / 2)
        val bx = cx - uy * (arrowWidth / 2)
        val by = cy + ux * (arrowWidth / 2)

        val path = Path().apply {
            moveTo(toX, toY)
            lineTo(ax, ay)
            lineTo(bx, by)
            close()
        }

        headPaint.alpha = alpha
        canvas.drawPath(path, headPaint)
    }
}
