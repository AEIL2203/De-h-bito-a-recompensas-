package com.angel.ibanez.dehabitos.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.angel.ibanez.dehabitos.R
import com.angel.ibanez.dehabitos.databinding.ItemMisionBinding
import com.angel.ibanez.dehabitos.models.Tarea
import com.angel.ibanez.dehabitos.utils.FechasUtils
import com.angel.ibanez.dehabitos.utils.IconosUtils
import com.angel.ibanez.dehabitos.utils.ImagenesUtils
import com.angel.ibanez.dehabitos.utils.ProgresionUtils

class MisionAdapter(
    private val onCompletar: (Tarea) -> Unit
) : ListAdapter<Tarea, MisionAdapter.MisionViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MisionViewHolder {
        val binding = ItemMisionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MisionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MisionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MisionViewHolder(private val binding: ItemMisionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(tarea: Tarea) {
            val context = binding.root.context

            binding.tvNombreMision.text = tarea.nombre
            binding.tvCategoria.text = (if (tarea.esDiaria) "☀️ Misión diaria · " else "") + ProgresionUtils.nombreCategoria(tarea.categoria)
            binding.tvXp.text = "+${tarea.puntos} XP"
            val colorIcono = when (tarea.categoria) {
                "academica" -> R.color.game_blue_dark
                "domestica" -> R.color.game_orange_dark
                else -> R.color.game_green_dark
            }
            binding.ivCategoria.icon = IconosUtils.crearIcono(
                context,
                ProgresionUtils.iconoCategoria(tarea.categoria),
                colorIcono
            )
            val bgIcono = when (tarea.categoria) {
                "academica" -> R.drawable.circle_game_blue
                "domestica" -> R.drawable.circle_game_orange
                else -> R.drawable.circle_game_green
            }
            binding.ivCategoria.setBackgroundResource(bgIcono)

            if (tarea.estado == Tarea.ESTADO_PENDIENTE && tarea.fechaLimite.isNotBlank()) {
                val corta = FechasUtils.formatearFechaCorta(tarea.fechaLimite)
                when (FechasUtils.estadoVencimiento(tarea.fechaLimite)) {
                    FechasUtils.EstadoVencimiento.VENCIDA -> {
                        binding.tvFechaLimite.text = "⚠️ Atrasada ($corta)"
                        binding.tvFechaLimite.setTextColor(ContextCompat.getColor(context, R.color.game_coral))
                    }
                    FechasUtils.EstadoVencimiento.VENCE_HOY -> {
                        binding.tvFechaLimite.text = "⏰ ¡Vence hoy!"
                        binding.tvFechaLimite.setTextColor(ContextCompat.getColor(context, R.color.game_yellow_dark))
                    }
                    else -> {
                        binding.tvFechaLimite.text = "📅 Vence $corta"
                        binding.tvFechaLimite.setTextColor(ContextCompat.getColor(context, R.color.game_text_medium))
                    }
                }
                binding.tvFechaLimite.visibility = View.VISIBLE
            } else {
                binding.tvFechaLimite.visibility = View.GONE
            }

            if (tarea.evidencia.isNotBlank() && tarea.estado != Tarea.ESTADO_PENDIENTE) {
                val bitmap = ImagenesUtils.decodificar(tarea.evidencia)
                if (bitmap != null) {
                    binding.ivEvidencia.setImageBitmap(bitmap)
                    binding.ivEvidencia.visibility = View.VISIBLE
                } else {
                    binding.ivEvidencia.visibility = View.GONE
                }
            } else {
                binding.ivEvidencia.visibility = View.GONE
            }

            when (tarea.estado) {
                Tarea.ESTADO_PENDIENTE -> {
                    binding.tvEstado.text = "🟠 Pendiente"
                    binding.tvEstado.setTextColor(ContextCompat.getColor(context, R.color.game_coral))
                    binding.btnCompletar.visibility = View.VISIBLE
                }
                Tarea.ESTADO_COMPLETADA -> {
                    binding.tvEstado.text = "⏳ Esperando validación"
                    binding.tvEstado.setTextColor(ContextCompat.getColor(context, R.color.game_yellow_dark))
                    binding.btnCompletar.visibility = View.GONE
                }
                else -> {
                    binding.tvEstado.text = "✅ ¡Validada!"
                    binding.tvEstado.setTextColor(ContextCompat.getColor(context, R.color.game_green_dark))
                    binding.btnCompletar.visibility = View.GONE
                }
            }

            binding.btnCompletar.setOnClickListener {
                binding.root.animate()
                    .scaleX(1.05f).scaleY(1.05f)
                    .setDuration(120)
                    .withEndAction {
                        binding.root.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                        onCompletar(tarea)
                    }
                    .start()
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Tarea>() {
        override fun areItemsTheSame(oldItem: Tarea, newItem: Tarea): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Tarea, newItem: Tarea): Boolean = oldItem == newItem
    }
}
