package com.angel.ibanez.dehabitos.utils

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
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

    /** Nivel global del niño (1..60). Cada mundo tiene 15 niveles. */
    var currentLevel = 5
        set(value) {
            field = value.coerceIn(1, NIVELES_POR_MUNDO * TOTAL_MUNDOS)
            if (!mundoElegidoManualmente) mundoVisible = mundoDe(field)
            invalidate()
        }

    /** Progreso 0-100 dentro del nivel actual (anillo alrededor del nodo). */
    var progresoNivel = 0
        set(value) {
            field = value.coerceIn(0, 100)
            invalidate()
        }

    /** Mundo que se está mostrando (0..3). Cambia al tocar una píldora de mundo desbloqueada. */
    var mundoVisible = 0
        private set(value) {
            if (field == value) return
            field = value
            particles.clear()
            invalidate()
        }

    /** Se invoca con el número de mundo (1..4) cuando se toca uno todavía bloqueado. */
    var onMundoBloqueado: ((Int) -> Unit)? = null

    private var mundoElegidoManualmente = false
    private val fondos = arrayOfNulls<Bitmap>(TOTAL_MUNDOS)
    private val worldRects = Array(TOTAL_MUNDOS) { RectF() }
    private var pressedWorld = -1

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
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
    // Posiciones (fracción del ancho/alto) de los 15 nodos sobre el camino de cada ilustración
    private val puntosCamino1 = listOf(
        .292f to .846f, .432f to .800f, .598f to .786f,
        .513f to .727f, .690f to .664f, .707f to .584f,
        .615f to .548f, .440f to .532f, .334f to .521f,
        .330f to .468f, .420f to .393f, .378f to .329f,
        .510f to .290f, .615f to .244f, .504f to .163f
    )

    // camino_2, camino_3 y camino_4 comparten el mismo trazado de isla
    private val puntosIsla = listOf(
        .310f to .807f, .470f to .789f, .590f to .763f,
        .520f to .720f, .600f to .672f, .750f to .676f,
        .710f to .629f, .650f to .578f, .520f to .549f,
        .340f to .501f, .320f to .458f, .440f to .403f,
        .550f to .342f, .590f to .291f, .490f to .193f
    )

    private val points get() = if (mundoVisible == 0) puntosCamino1 else puntosIsla

    private fun mundoDe(nivel: Int) = ((nivel - 1) / NIVELES_POR_MUNDO).coerceIn(0, TOTAL_MUNDOS - 1)

    /** Carga la ilustración sin reescalar por densidad y libera las de los otros mundos. */
    private fun fondoDe(mundo: Int): Bitmap = fondos[mundo] ?: BitmapFactory.decodeResource(
        resources,
        listOf(R.drawable.camino_1, R.drawable.camino_2, R.drawable.camino_3, R.drawable.camino_4)[mundo],
        BitmapFactory.Options().apply { inScaled = false }
    ).also { nuevo ->
        fondos.indices.filter { it != mundo }.forEach { fondos[it] = null }
        fondos[mundo] = nuevo
    }

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
        // Primero las píldoras de mundo
        val mundoTocado = worldRects.indexOfFirst { it.contains(event.x, event.y) }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> if (mundoTocado >= 0) {
                pressedWorld = mundoTocado
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> if (pressedWorld >= 0) {
                val elegido = pressedWorld
                pressedWorld = -1
                if (elegido == mundoTocado) {
                    if (elegido <= mundoDe(currentLevel)) {
                        mundoElegidoManualmente = true
                        mundoVisible = elegido
                        performClick()
                    } else {
                        onMundoBloqueado?.invoke(elegido + 1)
                    }
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_CANCEL -> pressedWorld = -1
        }

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
        val base = mundoVisible * NIVELES_POR_MUNDO
        points.forEachIndexed { index, point -> drawLevel(canvas, base + index + 1, point.first * w, point.second * h, scale) }
        if (mundoDe(currentLevel) == mundoVisible) drawPlayer(canvas, w, h, scale)
    }

    private fun drawIllustratedWorld(canvas: Canvas, w: Float, h: Float) {
        paint.alpha = 255
        canvas.drawBitmap(fondoDe(mundoVisible), null, RectF(0f, 0f, w, h), paint)
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
        // Ondas y salpicaduras solo encajan con la cascada del mundo 1
        if (mundoVisible != 0) return
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

    /** Botón de nivel estilo juego 3D: base de profundidad, cara con degradado y brillo, anillo de progreso. */
    private fun drawLevel(canvas: Canvas, level: Int, x: Float, y: Float, scale: Float) {
        val pressed = pressedLevel == level - 1
        val radius = 19f * scale * if (pressed) .92f else 1f
        val depth = (if (pressed) 1.5f else 4.5f) * scale
        val faceY = y - depth / 2f
        val base = when {
            level < currentLevel -> COLOR_COMPLETED
            level == currentLevel -> COLOR_CURRENT
            else -> COLOR_LOCKED
        }

        if (level == currentLevel) {
            paint.color = Color.argb((60 + animationPhase * 80).toInt(), 0, 229, 255)
            canvas.drawCircle(x, faceY, radius + (9f + animationPhase * 6f) * scale, paint)
        }

        // Base inferior (da el volumen del botón)
        paint.setShadowLayer(6f * scale, 0f, 4f * scale, Color.argb(120, 15, 10, 28))
        paint.color = shade(base, .62f)
        canvas.drawCircle(x, faceY + depth, radius, paint)
        paint.clearShadowLayer()

        // Cara superior con degradado vertical
        paint.shader = LinearGradient(
            x, faceY - radius, x, faceY + radius,
            shade(base, 1.25f), base, Shader.TileMode.CLAMP
        )
        canvas.drawCircle(x, faceY, radius, paint)
        paint.shader = null

        // Brillo especular
        paint.color = Color.argb(70, 255, 255, 255)
        canvas.drawOval(
            RectF(x - radius * .62f, faceY - radius * .82f, x + radius * .62f, faceY - radius * .12f),
            paint
        )

        // Anillo: progreso del nivel actual / dorado completo en niveles superados
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = 3.5f * scale
        val ring = RectF(x - radius - 5f * scale, faceY - radius - 5f * scale, x + radius + 5f * scale, faceY + radius + 5f * scale)
        when {
            level == currentLevel -> {
                paint.color = Color.argb(70, 255, 255, 255)
                canvas.drawOval(ring, paint)
                paint.color = COLOR_GOLD
                canvas.drawArc(ring, -90f, 360f * progresoNivel / 100f, false, paint)
            }
            level < currentLevel -> {
                paint.color = Color.argb(200, Color.red(COLOR_GOLD), Color.green(COLOR_GOLD), Color.blue(COLOR_GOLD))
                paint.strokeWidth = 2f * scale
                canvas.drawOval(ring, paint)
            }
        }
        paint.style = Paint.Style.FILL

        paint.typeface = Typeface.create("sans-serif-rounded", Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 17f * scale
        paint.setShadowLayer(2f * scale, 0f, 1.5f * scale, Color.argb(140, 0, 0, 0))
        paint.color = Color.WHITE
        canvas.drawText(level.toString(), x, faceY + 6f * scale, paint)
        paint.clearShadowLayer()

        if (level > currentLevel + 3) {
            paint.color = Color.argb(130, 15, 10, 28)
            canvas.drawCircle(x, faceY, radius, paint)
        }
    }

    /**
     * Selectores de mundo: píldoras verticales flotantes de cristal en el margen izquierdo
     * (Mundo 4 arriba, Mundo 1 abajo). Tocar uno desbloqueado cambia la ilustración del mapa.
     */
    private fun drawWorlds(canvas: Canvas, h: Float, scale: Float) {
        val nombres = listOf("Comienzo", "Responsable", "Aprendizaje", "Constancia")
        val colores = listOf(0xFFFFD700.toInt(), 0xFF00E676.toInt(), 0xFF00E5FF.toInt(), 0xFFB57BFF.toInt())
        val ys = listOf(.74f, .55f, .36f, .17f)
        val mundoActual = mundoDe(currentLevel)
        for (mundo in 0 until TOTAL_MUNDOS) {
            val inicio = mundo * NIVELES_POR_MUNDO + 1
            val locked = mundo > mundoActual
            val completados = (currentLevel - inicio).coerceIn(0, NIVELES_POR_MUNDO)
            val seleccionado = mundo == mundoVisible
            val accent = if (locked) Color.rgb(140, 132, 160) else colores[mundo]
            val left = 8f * scale
            val top = ys[mundo] * h
            val pulsado = pressedWorld == mundo
            val rect = RectF(left, top, left + 60f * scale, top + 92f * scale).apply {
                if (pulsado) inset(3f * scale, 3f * scale)
            }
            worldRects[mundo].set(rect)
            val corner = rect.width() / 2f

            // Cristal esmerilado: relleno translúcido oscuro + borde + sombra suave
            paint.setShadowLayer(10f * scale, 0f, 4f * scale, Color.argb(90, 0, 0, 0))
            paint.color = Color.argb(if (locked) 150 else 190, 26, 16, 47)
            canvas.drawRoundRect(rect, corner, corner, paint)
            paint.clearShadowLayer()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = (if (seleccionado) 2.5f else 1f) * scale
            paint.color = if (seleccionado) accent else Color.argb(40, 255, 255, 255)
            canvas.drawRoundRect(rect, corner, corner, paint)
            paint.style = Paint.Style.FILL

            // Indicador con el color del mundo
            val cx = rect.centerX()
            paint.color = Color.argb(60, Color.red(accent), Color.green(accent), Color.blue(accent))
            canvas.drawCircle(cx, top + 24f * scale, 17f * scale, paint)
            paint.color = accent
            canvas.drawCircle(cx, top + 24f * scale, 11f * scale, paint)
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.create("sans-serif-rounded", Typeface.BOLD)
            paint.textSize = 10f * scale
            paint.color = Color.rgb(26, 16, 48)
            canvas.drawText(if (locked) "🔒" else (mundo + 1).toString(), cx, top + 27.5f * scale, paint)

            paint.textSize = 8f * scale
            paint.color = Color.WHITE
            canvas.drawText("Mundo ${mundo + 1}", cx, top + 54f * scale, paint)
            paint.color = Color.argb(190, 220, 210, 240)
            canvas.drawText(nombres[mundo], cx, top + 64f * scale, paint)

            // Mini barra de progreso del mundo
            val barTop = top + 72f * scale
            val bar = RectF(left + 10f * scale, barTop, left + 50f * scale, barTop + 5f * scale)
            paint.color = Color.argb(50, 255, 255, 255)
            canvas.drawRoundRect(bar, 3f * scale, 3f * scale, paint)
            paint.color = accent
            canvas.drawRoundRect(RectF(bar.left, bar.top, bar.left + bar.width() * completados / NIVELES_POR_MUNDO, bar.bottom), 3f * scale, 3f * scale, paint)
            paint.textSize = 7f * scale
            paint.color = Color.argb(210, 230, 220, 250)
            canvas.drawText(if (locked) "Nivel $inicio" else "$completados/$NIVELES_POR_MUNDO", cx, top + 86f * scale, paint)
        }
    }

    private fun shade(color: Int, factor: Float): Int = Color.rgb(
        (Color.red(color) * factor).toInt().coerceIn(0, 255),
        (Color.green(color) * factor).toInt().coerceIn(0, 255),
        (Color.blue(color) * factor).toInt().coerceIn(0, 255)
    )

    companion object {
        val COLOR_COMPLETED = Color.rgb(0, 200, 100)
        val COLOR_CURRENT = Color.rgb(0, 176, 220)
        val COLOR_LOCKED = Color.rgb(125, 72, 190)
        val COLOR_GOLD = Color.rgb(255, 215, 0)
        const val NIVELES_POR_MUNDO = 15
        const val TOTAL_MUNDOS = 4
    }
}
