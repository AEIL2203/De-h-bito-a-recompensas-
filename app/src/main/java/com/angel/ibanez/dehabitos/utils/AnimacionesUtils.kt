package com.angel.ibanez.dehabitos.utils

import android.app.Dialog
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.angel.ibanez.dehabitos.R
import com.bumptech.glide.Glide

object AnimacionesUtils {
    const val GIF_TAREA_COMPLETADA = "icons8-check-mark-100.gif"
    const val GIF_META = "icons8-goal-100.gif"
    const val GIF_VALIDACION = "icons8-test-passed-100.gif"
    const val GIF_LOGRO = "icons8-guarantee-100.gif"
    const val GIF_PREMIO = "icons8-shopping-100.gif"
    const val GIF_MONEDAS = "icons8-stack-of-coins-96.gif"

    fun cargarGif(imageView: ImageView, gif: String) {
        Glide.with(imageView)
            .asGif()
            .load("file:///android_asset/$gif")
            .transform(FondoBlancoTransparenteTransformation())
            .into(imageView)
    }

    fun mostrarCelebracion(view: View, gif: String, titulo: String, mensaje: String, duracion: Long = 2200L) {
        val context = view.context
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val contenido = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(view, 28), dp(view, 24), dp(view, 28), dp(view, 24))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(view, 28).toFloat()
                setColor(context.getColor(R.color.white))
                setStroke(dp(view, 2), context.getColor(R.color.game_yellow))
            }
        }
        val imagen = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(view, 120), dp(view, 120))
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        val textoTitulo = TextView(context).apply {
            text = titulo
            gravity = Gravity.CENTER
            textSize = 22f
            setTextColor(context.getColor(R.color.md_theme_primary))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val textoMensaje = TextView(context).apply {
            text = mensaje
            gravity = Gravity.CENTER
            textSize = 15f
            setTextColor(context.getColor(R.color.md_theme_onSurface))
            setPadding(0, dp(view, 8), 0, 0)
        }
        contenido.addView(imagen)
        contenido.addView(textoTitulo)
        contenido.addView(textoMensaje)
        contenido.setOnClickListener { dialog.dismiss() }
        dialog.setContentView(contenido)
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            attributes = attributes.apply { dimAmount = 0.6f }
        }
        dialog.setOnShowListener {
            dialog.window?.setLayout((view.resources.displayMetrics.widthPixels * 0.82f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
            contenido.scaleX = 0.7f
            contenido.scaleY = 0.7f
            contenido.alpha = 0f
            contenido.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(320).start()
            cargarGif(imagen, gif)
        }
        dialog.show()
        Handler(Looper.getMainLooper()).postDelayed({ if (dialog.isShowing) dialog.dismiss() }, duracion)
    }

    fun entradaSuave(view: View, desplazamientoDp: Int = 28) {
        view.alpha = 0f
        view.translationY = dp(view, desplazamientoDp).toFloat()
        view.animate().alpha(1f).translationY(0f).setDuration(420).start()
    }

    fun pulso(view: View) {
        view.animate().scaleX(1.12f).scaleY(1.12f).setDuration(140).withEndAction {
            view.animate().scaleX(1f).scaleY(1f).setDuration(180).start()
        }.start()
    }

    private fun dp(view: View, valor: Int): Int = (valor * view.resources.displayMetrics.density).toInt()
}
