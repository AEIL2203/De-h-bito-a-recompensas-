package com.angel.ibanez.dehabitos.utils

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import com.angel.ibanez.dehabitos.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class GameMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var currentLevel = 5
        set(value) {
            field = value.coerceIn(1, 15)
            invalidate()
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val firstWorld: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.camino_1)
    private var animationPhase = 0f
    private var pressedLevel = -1
    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 2400L
        repeatCount = ValueAnimator.INFINITE
        interpolator = AccelerateDecelerateInterpolator()
        addUpdateListener {
            animationPhase = it.animatedValue as Float
            postInvalidateOnAnimation()
        }
    }
    private val points = listOf(
        .292f to .846f, .432f to .800f, .598f to .786f,
        .513f to .727f, .690f to .664f, .707f to .584f,
        .615f to .548f, .440f to .532f, .334f to .521f,
        .330f to .468f, .420f to .393f, .378f to .329f,
        .510f to .290f, .615f to .244f, .504f to .163f
    )

    private enum class ParticleType { SPARKLE, LEAF, RIPPLE, SPLASH }
    private data class Particle(
        var x: Float, var y: Float,
        var vx: Float, var vy: Float,
        var life: Float, val maxLife: Float,
        var size: Float,
        val type: ParticleType,
        val color: Int
    )
    private val particles = mutableListOf<Particle>()

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
        isClickable = true
        contentDescription = "Mapa de progreso"
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!animator.isStarted) animator.start()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val scale = min(width / 360f, height / 760f)
        val touched = points.indexOfFirst { point ->
            val dx = event.x - point.first * width
            val dy = event.y - point.second * height
            dx * dx + dy * dy <= 28f * scale * 28f * scale
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                pressedLevel = touched
                invalidate()
                return touched >= 0
            }
            MotionEvent.ACTION_UP -> {
                val selected = pressedLevel
                pressedLevel = -1
                invalidate()
                if (selected >= 0 && selected == touched) performClick()
                return selected >= 0
            }
            MotionEvent.ACTION_CANCEL -> {
                pressedLevel = -1
                invalidate()
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val scale = min(w / 360f, h / 760f)
        updateParticles(w, h, scale)
        drawIllustratedWorld(canvas, w, h)
        drawParticles(canvas, scale)
        drawWorlds(canvas, h, scale)
        points.forEachIndexed { index, point -> drawLevel(canvas, index + 1, point.first * w, point.second * h, scale) }
        drawPlayer(canvas, w, h, scale)
    }

    private fun drawIllustratedWorld(canvas: Canvas, w: Float, h: Float) {
        paint.alpha = 255
        canvas.drawBitmap(firstWorld, null, RectF(0f, 0f, w, h), paint)
        paint.color = Color.argb(45, 16, 37, 63)
        canvas.drawRect(0f, 0f, w * .19f, h, paint)
        paint.color = Color.argb(28, 8, 25, 42)
        canvas.drawRect(0f, h * .94f, w, h, paint)
    }

    private fun spawnParticles(w: Float, h: Float, scale: Float) {
        if (particles.size > 250) return
        if (Random.nextFloat() < 0.12f) {
            particles.add(Particle(
                x = (0.45f + Random.nextFloat() * 0.10f) * w,
                y = (0.04f + Random.nextFloat() * 0.06f) * h,
                vx = (Random.nextFloat() - 0.5f) * 0.4f * scale,
                vy = -(0.3f + Random.nextFloat() * 0.4f) * scale,
                life = 30f + Random.nextFloat() * 40f,
                maxLife = 70f,
                size = (1.5f + Random.nextFloat() * 2.5f) * scale,
                type = ParticleType.SPARKLE,
                color = Color.rgb(255, 230, 120)
            ))
        }
        if (Random.nextFloat() < 0.18f) {
            particles.add(Particle(
                x = (0.15f + Random.nextFloat() * 0.70f) * w,
                y = (0.33f + Random.nextFloat() * 0.20f) * h,
                vx = (Random.nextFloat() - 0.5f) * 0.6f * scale,
                vy = (0.4f + Random.nextFloat() * 0.4f) * scale,
                life = 50f + Random.nextFloat() * 60f,
                maxLife = 110f,
                size = (2f + Random.nextFloat() * 3f) * scale,
                type = ParticleType.LEAF,
                color = Color.rgb(255, 195, 50)
            ))
        }
        if (Random.nextFloat() < 0.10f) {
            particles.add(Particle(
                x = (0.45f + Random.nextFloat() * 0.25f) * w,
                y = (0.55f + Random.nextFloat() * 0.22f) * h,
                vx = 0f,
                vy = 0f,
                life = 20f + Random.nextFloat() * 30f,
                maxLife = 50f,
                size = (6f + Random.nextFloat() * 10f) * scale,
                type = ParticleType.RIPPLE,
                color = Color.rgb(160, 235, 255)
            ))
        }
        if (Random.nextFloat() < 0.25f) {
            particles.add(Particle(
                x = (0.50f + Random.nextFloat() * 0.12f) * w,
                y = (0.40f + Random.nextFloat() * 0.35f) * h,
                vx = (Random.nextFloat() - 0.5f) * 1.2f * scale,
                vy = -(0.8f + Random.nextFloat() * 1.0f) * scale,
                life = 15f + Random.nextFloat() * 25f,
                maxLife = 40f,
                size = (1.5f + Random.nextFloat() * 2f) * scale,
                type = ParticleType.SPLASH,
                color = Color.rgb(200, 245, 255)
            ))
        }
    }

    private fun updateParticles(w: Float, h: Float, scale: Float) {
        spawnParticles(w, h, scale)
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.x += p.vx
            p.y += p.vy
            if (p.type == ParticleType.SPLASH) p.vy += 0.08f * scale
            if (p.type == ParticleType.LEAF) p.x += sin(p.life * 0.15f) * 0.2f * scale
            p.life -= 1f
            if (p.life <= 0 || p.y > h * 1.05f) iterator.remove()
        }
    }

    private fun drawParticles(canvas: Canvas, scale: Float) {
        for (p in particles) {
            val progress = 1f - p.life / p.maxLife
            val alpha = (255 * sin(progress * PI.toFloat())).toInt().coerceIn(0, 255)
            when (p.type) {
                ParticleType.RIPPLE -> {
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 1.5f * scale
                    paint.color = Color.argb((alpha * 0.6f).toInt(), Color.red(p.color), Color.green(p.color), Color.blue(p.color))
                    val rx = p.size * (0.5f + progress * 1.5f)
                    val ry = rx * 0.35f
                    canvas.drawOval(RectF(p.x - rx, p.y - ry, p.x + rx, p.y + ry), paint)
                }
                else -> {
                    paint.style = Paint.Style.FILL
                    paint.color = Color.argb(alpha, Color.red(p.color), Color.green(p.color), Color.blue(p.color))
                    canvas.drawCircle(p.x, p.y, p.size * (1f - progress * 0.4f), paint)
                }
            }
        }
        paint.style = Paint.Style.FILL
    }

    private fun drawPlayer(canvas: Canvas, w: Float, h: Float, scale: Float) {
        val point = points[(currentLevel - 1).coerceIn(points.indices)]
        val x = point.first * w
        val bounce = sin(animationPhase * Math.PI * 2.0).toFloat() * 4f * scale
        val y = point.second * h - 38f * scale + bounce
        paint.color = Color.argb((90 + animationPhase * 70).toInt(), 91, 209, 245)
        canvas.drawCircle(x, y, (21f + animationPhase * 3f) * scale, paint)
        paint.setShadowLayer(7f * scale, 0f, 4f * scale, Color.argb(100, 30, 55, 80))
        paint.color = Color.WHITE
        canvas.drawCircle(x, y, 17f * scale, paint)
        paint.clearShadowLayer()
        paint.color = Color.rgb(52, 173, 218)
        canvas.drawCircle(x, y, 13f * scale, paint)
        paint.color = Color.rgb(245, 184, 119)
        canvas.drawCircle(x, y, 8f * scale, paint)
        paint.color = Color.rgb(74, 43, 30)
        canvas.drawArc(RectF(x - 8f * scale, y - 9f * scale, x + 8f * scale, y + 5f * scale), 180f, 180f, true, paint)
        val pin = Path().apply {
            moveTo(x - 8f * scale, y + 12f * scale)
            lineTo(x, y + 24f * scale)
            lineTo(x + 8f * scale, y + 12f * scale)
            close()
        }
        paint.color = Color.WHITE
        canvas.drawPath(pin, paint)
    }

    private fun drawLevel(canvas: Canvas, level: Int, x: Float, y: Float, scale: Float) {
        val pressedScale = if (pressedLevel == level - 1) .88f else 1f
        val radius = 19f * scale * pressedScale
        if (level == currentLevel) {
            paint.color = Color.argb((70 + animationPhase * 70).toInt(), 78, 205, 245)
            canvas.drawCircle(x, y, radius + (6f + animationPhase * 5f) * scale, paint)
        }
        paint.setShadowLayer(3f * scale, 0f, 3f * scale, Color.argb(110, 55, 39, 65))
        paint.color = when {
            level < currentLevel -> Color.rgb(104, 183, 63)
            level == currentLevel -> Color.rgb(32, 164, 215)
            else -> Color.rgb(139, 75, 169)
        }
        canvas.drawCircle(x, y, radius, paint)
        paint.clearShadowLayer()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f * scale
        paint.color = Color.WHITE
        canvas.drawCircle(x, y, radius - 2f * scale, paint)
        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.create("sans-serif-rounded", Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 18f * scale
        paint.color = Color.WHITE
        canvas.drawText(level.toString(), x, y + 6f * scale, paint)
        if (level > currentLevel + 3) {
            paint.color = Color.argb(120, 60, 48, 69)
            canvas.drawCircle(x, y, radius, paint)
        }
    }

    private fun drawWorlds(canvas: Canvas, h: Float, scale: Float) {
        val labels = listOf("Mundo 4\nConstancia", "Mundo 3\nAprendizaje", "Mundo 2\nResponsable", "Mundo 1\nComienzo")
        val colors = listOf(0xFFD4A7F2.toInt(), 0xFFB8DDF5.toInt(), 0xFFC9E6C7.toInt(), 0xFFFFE39A.toInt())
        val ranges = listOf(13..15, 9..12, 5..8, 1..4)
        val ys = listOf(.17f, .36f, .55f, .74f)
        labels.forEachIndexed { index, label ->
            val range = ranges[index]
            val locked = currentLevel < range.first
            val completed = if (currentLevel < range.first) 0 else min(currentLevel - range.first + 1, range.count())
            val left = 8f * scale
            val top = ys[index] * h
            val rect = RectF(left, top, left + 62f * scale, top + 86f * scale)
            paint.color = if (locked) Color.argb(235, 210, 210, 210) else Color.argb(235, 255, 255, 255)
            canvas.drawRoundRect(rect, 18f * scale, 18f * scale, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f * scale
            paint.color = if (locked) Color.rgb(160, 160, 160) else colors[index]
            canvas.drawRoundRect(rect, 18f * scale, 18f * scale, paint)
            paint.style = Paint.Style.FILL
            paint.color = if (locked) Color.rgb(150, 150, 150) else colors[index]
            canvas.drawCircle(rect.centerX(), top + 22f * scale, 15f * scale, paint)
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.create("sans-serif-rounded", Typeface.BOLD)
            paint.color = if (locked) Color.rgb(120, 120, 120) else Color.rgb(65, 57, 70)
            paint.textSize = 8f * scale
            label.split("\n").forEachIndexed { line, text -> canvas.drawText(text, rect.centerX(), top + (51 + line * 10) * scale, paint) }
            paint.color = if (locked) Color.rgb(200, 200, 200) else Color.rgb(247, 238, 231)
            canvas.drawRoundRect(RectF(left + 8f * scale, top + 70f * scale, rect.right - 8f * scale, top + 81f * scale), 6f * scale, 6f * scale, paint)
            paint.textSize = 7f * scale
            paint.color = if (locked) Color.rgb(120, 120, 120) else Color.rgb(98, 76, 104)
            val progress = if (locked) "Bloqueado" else "$completed/${range.count()} ★"
            canvas.drawText(progress, rect.centerX(), top + 78f * scale, paint)
        }
    }
}
