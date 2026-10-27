package com.angel.ibanez.dehabitos.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.animation.doOnEnd
import androidx.core.view.doOnLayout
import com.angel.ibanez.dehabitos.R
import com.google.android.material.card.MaterialCardView
import kotlin.math.max
import kotlin.math.min

data class TutorialStep(
    val targetViewId: Int? = null,
    val targetProvider: (() -> View?)? = null,
    val title: String,
    val message: String,
    val tooltipPosition: TooltipPosition = TooltipPosition.AUTO,
    val targetPaddingDp: Int = 8,
    val cornerRadiusDp: Int = 16,
    val onPresent: (() -> Unit)? = null
) {
    enum class TooltipPosition { TOP, BOTTOM, AUTO }
}

class TutorialOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val steps = mutableListOf<TutorialStep>()
    private var currentIndex = 0
    private var onFinish: (() -> Unit)? = null

    private val overlayColor = 0xE6000000.toInt()
    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = overlayColor }
    private val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    private val pulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = 0xFFFFFFFF.toInt()
    }

    private val currentHole = RectF()
    private val targetHole = RectF()
    private var currentCorner = 0f
    private var targetCorner = 0f
    private var pulseScale = 1f

    private lateinit var tooltipCard: MaterialCardView
    private lateinit var tooltipView: View

    private var animator: ValueAnimator? = null
    private var pulseAnimator: ValueAnimator? = null
    private var pendingPresent: Runnable? = null

    init {
        setWillNotDraw(false)
        setLayerType(LAYER_TYPE_HARDWARE, null)
        setOnClickListener { /* consume touches */ }
    }

    companion object {
        fun start(
            activity: Activity,
            steps: List<TutorialStep>,
            onFinish: (() -> Unit)? = null
        ): TutorialOverlay? {
            if (steps.isEmpty()) return null
            val content = activity.findViewById<ViewGroup>(android.R.id.content)
            val existing = content.findViewById<TutorialOverlay?>(R.id.tutorial_overlay)
            existing?.let { content.removeView(it) }
            val overlay = TutorialOverlay(activity)
            overlay.id = R.id.tutorial_overlay
            overlay.steps.addAll(steps)
            overlay.onFinish = onFinish
            content.addView(
                overlay,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            overlay.post { overlay.initTooltip(); overlay.showStep(0, animate = false) }
            return overlay
        }

        fun isShowing(activity: Activity): Boolean =
            activity.findViewById<View?>(R.id.tutorial_overlay) != null
    }

    private fun initTooltip() {
        tooltipView = LayoutInflater.from(context).inflate(R.layout.view_tutorial_tooltip, this, false)
        tooltipCard = tooltipView.findViewById(R.id.cardTooltip)
        addView(tooltipView, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))

        tooltipView.findViewById<View>(R.id.btnTutorialSaltar).setOnClickListener { finish() }
        tooltipView.findViewById<View>(R.id.btnTutorialAnterior).setOnClickListener { previousStep() }
        tooltipView.findViewById<View>(R.id.btnTutorialSiguiente).setOnClickListener { nextStep() }
    }

    private fun showStep(index: Int, animate: Boolean) {
        if (index !in steps.indices) return
        currentIndex = index
        val step = steps[index]
        step.onPresent?.invoke()
        pendingPresent?.let { removeCallbacks(it) }
        pendingPresent = Runnable { presentStep(index, step, animate) }
        postDelayed(pendingPresent!!, 300)
    }

    private fun presentStep(index: Int, step: TutorialStep, animate: Boolean) {
        val target = step.targetProvider?.invoke()
            ?: (context as? Activity)?.findViewById<View>(step.targetViewId ?: 0)
        if (target == null) {
            if (index < steps.size - 1) showStep(index + 1, animate)
            else finish()
            return
        }

        val density = resources.displayMetrics.density
        val paddingPx = step.targetPaddingDp * density
        val cornerPx = step.cornerRadiusDp * density

        val loc = IntArray(2)
        val overlayLoc = IntArray(2)
        target.getLocationOnScreen(loc)
        getLocationOnScreen(overlayLoc)

        val left = loc[0] - overlayLoc[0] - paddingPx
        val top = loc[1] - overlayLoc[1] - paddingPx
        val right = left + target.width + 2 * paddingPx
        val bottom = top + target.height + 2 * paddingPx

        targetHole.set(
            max(0f, left),
            max(0f, top),
            min(width.toFloat(), right),
            min(height.toFloat(), bottom)
        )
        targetCorner = cornerPx

        if (animate) {
            animator?.cancel()
            animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 400
                addUpdateListener { animateHole(it.animatedValue as Float) }
                start()
            }
        } else {
            currentHole.set(targetHole)
            currentCorner = targetCorner
            invalidate()
            updateTooltip(step)
        }

        startPulseAnimation()
    }

    private fun animateHole(fraction: Float) {
        currentHole.left = currentHole.left + (targetHole.left - currentHole.left) * fraction
        currentHole.top = currentHole.top + (targetHole.top - currentHole.top) * fraction
        currentHole.right = currentHole.right + (targetHole.right - currentHole.right) * fraction
        currentHole.bottom = currentHole.bottom + (targetHole.bottom - currentHole.bottom) * fraction
        currentCorner = currentCorner + (targetCorner - currentCorner) * fraction
        invalidate()
        updateTooltip(steps[currentIndex])
    }

    private fun startPulseAnimation() {
        pulseAnimator?.cancel()
        pulseAnimator = ValueAnimator.ofFloat(1f, 1.15f).apply {
            duration = 800
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                pulseScale = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun updateTooltip(step: TutorialStep) {
        tooltipView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnTutorialAnterior)
            .visibility = if (currentIndex > 0) View.VISIBLE else View.GONE
        val siguiente = tooltipView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnTutorialSiguiente)
        siguiente.text = if (currentIndex == steps.size - 1) "Finalizar" else "Siguiente"

        tooltipView.findViewById<android.widget.TextView>(R.id.tvTutorialTitulo).text = step.title
        tooltipView.findViewById<android.widget.TextView>(R.id.tvTutorialMensaje).text = step.message
        val progress = tooltipView.findViewById<com.google.android.material.progressindicator.LinearProgressIndicator>(R.id.progressTutorial)
        progress.max = steps.size
        progress.progress = currentIndex + 1

        tooltipCard.post {
            positionTooltip(step.tooltipPosition)
        }
    }

    private fun positionTooltip(position: TutorialStep.TooltipPosition) {
        if (tooltipCard.width == 0 || width == 0) return
        val margin = (16 * resources.displayMetrics.density).toInt()
        val cardWidth = min(tooltipCard.width, width - 2 * margin)
        tooltipCard.layoutParams.width = cardWidth
        tooltipCard.requestLayout()

        val centerX = currentHole.centerX()
        val tooltipX = (centerX - cardWidth / 2f).coerceIn(margin.toFloat(), (width - margin - cardWidth).toFloat())

        val spaceTop = currentHole.top - margin
        val spaceBottom = height - currentHole.bottom - margin
        val atBottom = when (position) {
            TutorialStep.TooltipPosition.BOTTOM -> true
            TutorialStep.TooltipPosition.TOP -> false
            TutorialStep.TooltipPosition.AUTO -> spaceBottom >= spaceTop
        }

        val tooltipY = if (atBottom) {
            (currentHole.bottom + margin).coerceAtMost(height - tooltipCard.height - margin.toFloat())
        } else {
            (currentHole.top - margin - tooltipCard.height).coerceAtLeast(margin.toFloat())
        }

        tooltipCard.x = tooltipX
        tooltipCard.y = tooltipY
    }

    private fun nextStep() {
        if (currentIndex < steps.size - 1) showStep(currentIndex + 1, animate = true)
        else finish()
    }

    private fun previousStep() {
        if (currentIndex > 0) showStep(currentIndex - 1, animate = true)
    }

    private fun finish() {
        pulseAnimator?.cancel()
        animator?.cancel()
        pendingPresent?.let { removeCallbacks(it) }
        val parent = parent as? ViewGroup ?: return
        animate().alpha(0f).setDuration(250).withEndAction {
            parent.removeView(this)
            onFinish?.invoke()
        }.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPaint(overlayPaint)
        if (currentHole.width() > 0 && currentHole.height() > 0) {
            canvas.drawRoundRect(currentHole, currentCorner, currentCorner, holePaint)
            val halfStroke = pulsePaint.strokeWidth / 2f
            val pulseRect = RectF(
                currentHole.left - halfStroke * pulseScale,
                currentHole.top - halfStroke * pulseScale,
                currentHole.right + halfStroke * pulseScale,
                currentHole.bottom + halfStroke * pulseScale
            )
            val alpha = ((1.5f - pulseScale) * 255).toInt().coerceIn(0, 255)
            pulsePaint.alpha = alpha
            canvas.drawRoundRect(pulseRect, currentCorner, currentCorner, pulsePaint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pulseAnimator?.cancel()
        animator?.cancel()
        pendingPresent?.let { removeCallbacks(it) }
    }
}
