package com.example.aplicacionparacelular.ui.dashboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.aplicacionparacelular.R

class PlaytimeRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
        color = 0x336750A4
    }
    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
        color = ContextCompat.getColor(context, R.color.tertiary)
    }
    private val oval = RectF()
    private var startAngle = -90f
    private var sweepAngle = 0f

    fun bind(ring: PlaytimeRing) {
        startAngle = ring.startAngle
        sweepAngle = ring.sweepAngle
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = 220
        setMeasuredDimension(
            resolveSize(size, widthMeasureSpec),
            resolveSize(size, heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val stroke = width.coerceAtMost(height) * 0.10f
        trackPaint.strokeWidth = stroke
        arcPaint.strokeWidth = stroke
        val pad = stroke / 2f + 8f
        oval.set(pad, pad, width - pad, height - pad)
        canvas.drawArc(oval, -90f, 360f, false, trackPaint)
        if (sweepAngle > 0f) {
            canvas.drawArc(oval, startAngle, sweepAngle, false, arcPaint)
        }
    }
}
