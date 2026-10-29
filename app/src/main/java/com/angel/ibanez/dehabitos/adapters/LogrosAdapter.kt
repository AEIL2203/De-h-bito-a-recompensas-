package com.angel.ibanez.dehabitos.adapters

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.angel.ibanez.dehabitos.R
import com.angel.ibanez.dehabitos.databinding.ItemLogroBinding
import com.angel.ibanez.dehabitos.models.Nino
import com.angel.ibanez.dehabitos.utils.IconosUtils
import com.angel.ibanez.dehabitos.utils.LogrosUtils

class LogrosAdapter : RecyclerView.Adapter<LogrosAdapter.LogroViewHolder>() {
    private var contexto = LogrosUtils.Contexto(Nino())

    companion object {
        private const val PAYLOAD_PROGRESO = "progreso"
    }

    fun actualizar(nino: Nino) {
        contexto = LogrosUtils.Contexto(nino)
        notifyItemRangeChanged(0, itemCount, PAYLOAD_PROGRESO)
    }

    override fun onBindViewHolder(holder: LogroViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_PROGRESO)) {
            holder.actualizarProgreso(LogrosUtils.CATALOGO[position])
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    fun actualizarTodo(nino: Nino) {
        contexto = LogrosUtils.Contexto(nino)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = LogroViewHolder(
        ItemLogroBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )
    override fun getItemCount() = LogrosUtils.CATALOGO.size
    override fun onBindViewHolder(holder: LogroViewHolder, position: Int) = holder.bind(LogrosUtils.CATALOGO[position])

    inner class LogroViewHolder(private val binding: ItemLogroBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(logro: LogrosUtils.Logro) {
            val context = binding.root.context
            val progreso = logro.progresoActual(contexto).coerceAtMost(logro.objetivo)
            val ganado = logro.cumplido(contexto)
            val colorRes = if (ganado) logro.colorRes else R.color.game_locked
            val color = ContextCompat.getColor(context, colorRes)
            binding.tvNombre.text = logro.nombre
            binding.tvDescripcion.text = logro.descripcion
            binding.tvProgreso.text = if (ganado) "Completado" else "$progreso/${logro.objetivo}"
            binding.progresoLogro.max = logro.objetivo
            binding.progresoLogro.setProgressCompat(if (ganado) logro.objetivo else progreso, false)
            binding.progresoLogro.setIndicatorColor(color)
            binding.tvProgreso.setTextColor(ColorStateList.valueOf(color))
            binding.ivIcono.icon = IconosUtils.crearIcono(context, logro.iconoGmr, colorRes, 28)
            binding.ivCandado.visibility = if (ganado) View.GONE else View.VISIBLE
            binding.ivCandado.icon = IconosUtils.crearIcono(context, "gmr-lock", R.color.child_on_primary_container, 12)
            binding.fondoIcono.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(if (ganado) 56 else 30, Color.red(color), Color.green(color), Color.blue(color)))
            }
            // Completado: borde dorado brillante. Bloqueado: aspecto esmerilado (opacidad baja).
            val dorado = ContextCompat.getColor(context, R.color.neon_gold)
            val borde = ContextCompat.getColor(context, R.color.child_stroke)
            binding.root.strokeColor = if (ganado) dorado else borde
            binding.root.strokeWidth = (context.resources.displayMetrics.density * if (ganado) 2f else 1f).toInt()
            binding.root.cardElevation = if (ganado) 6f * context.resources.displayMetrics.density else 0f
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) binding.root.outlineSpotShadowColor = dorado
            binding.contenidoLogro.alpha = if (ganado) 1f else 0.55f
        }

        fun actualizarProgreso(logro: LogrosUtils.Logro) {
            bind(logro)
        }
    }
}
