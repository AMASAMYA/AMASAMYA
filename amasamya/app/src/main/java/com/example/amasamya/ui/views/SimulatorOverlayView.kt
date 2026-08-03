package com.example.amasamya.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.example.amasamya.service.A11yAuditService

class SimulatorOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var activeFocusRect: Rect? = null
    private val ringPaint = Paint().apply {
        color = Color.parseColor("#00E5FF") // Vibrant Cyan
        style = Paint.Style.STROKE
        strokeWidth = 8f
        isAntiAlias = true
    }

    private val fillPaint = Paint().apply {
        color = Color.parseColor("#3300E5FF")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    init {
        setWillNotDraw(false)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        setupControls()
    }

    private fun setupControls() {
        val controlPanel = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#CC090B11"))
            setPadding(16, 12, 16, 12)
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        val titleTv = TextView(context).apply {
            text = "TALKBACK SIMULATOR"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 12f
            setTypeface(null, android.graphics.Typeface.BOLD)
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        val prevBtn = Button(context).apply {
            text = "◀ PREV"
            setTextColor(Color.WHITE)
            textSize = 11f
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
            setOnClickListener {
                A11yAuditService.instance?.simulateFocusPrevious()
            }
        }

        val nextBtn = Button(context).apply {
            text = "NEXT ▶"
            setTextColor(Color.WHITE)
            textSize = 11f
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
            setOnClickListener {
                A11yAuditService.instance?.simulateFocusNext()
            }
        }

        val actBtn = Button(context).apply {
            text = "ACTIVATE"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 11f
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
            setOnClickListener {
                A11yAuditService.instance?.simulateFocusActivate()
            }
        }

        controlPanel.addView(titleTv, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        controlPanel.addView(prevBtn)
        controlPanel.addView(nextBtn)
        controlPanel.addView(actBtn)

        val params = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM
        )
        addView(controlPanel, params)
    }

    fun updateFocusRect(rect: Rect?) {
        activeFocusRect = rect
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        activeFocusRect?.let { r ->
            canvas.drawRect(r, fillPaint)
            canvas.drawRect(r, ringPaint)
        }
    }
}
