package com.angel.ibanez.dehabitos.utils

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class WavyPathView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        style = Paint.Style.STROKE
        strokeWidth = 28f // Un poco más grueso para que destaque
        strokeCap = Paint.Cap.ROUND
    }

    private val path = Path()
    private var startX = 0.5f
    private var endX = 0.5f

    fun setPathConfig(startBias: Float, endBias: Float, pathColor: Int, isDashed: Boolean = false) {
        this.startX = startBias
        this.endX = endBias
        this.paint.color = pathColor
        
        // Efecto visual: si es punteado, los puntos son más grandes y espaciados
        if (isDashed) {
            this.paint.pathEffect = DashPathEffect(floatArrayOf(50f, 40f), 0f)
            this.paint.alpha = 180 // Un poco más transparente para lo "futuro"
        } else {
            this.paint.pathEffect = null
            this.paint.alpha = 255
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val w = width.toFloat()
        val h = height.toFloat()
        
        val x1 = startX * w
        val y1 = 0f
        val x2 = endX * w
        val y2 = h
        
        path.reset()
        path.moveTo(x1, y1)
        
        // Curva de Bézier más pronunciada y orgánica
        // Desplazamos los puntos de control para que la curva no sea tan simétrica
        val controlY1 = h * 0.25f
        val controlY2 = h * 0.75f
        path.cubicTo(x1, controlY1, x2, controlY2, x2, y2)
        
        canvas.drawPath(path, paint)
    }
}
