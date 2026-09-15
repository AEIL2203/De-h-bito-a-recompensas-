package com.angel.ibanez.dehabitos.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.angel.ibanez.dehabitos.R
import com.angel.ibanez.dehabitos.databinding.ItemTareaValidarBinding
import com.angel.ibanez.dehabitos.models.Tarea
import com.angel.ibanez.dehabitos.utils.IconosUtils
import com.angel.ibanez.dehabitos.utils.ProgresionUtils

class TareaValidarAdapter(
    private val nombresNinos: () -> Map<String, String>,
    private val onValidar: (Tarea) -> Unit,
    private val onRechazar: (Tarea) -> Unit
) : ListAdapter<Tarea, TareaValidarAdapter.TareaViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TareaViewHolder {
        val binding = ItemTareaValidarBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TareaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TareaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TareaViewHolder(private val binding: ItemTareaValidarBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(tarea: Tarea) {
            val nombreNino = nombresNinos()[tarea.ninoId] ?: "Niño"
            binding.tvNombreTarea.text = tarea.nombre
            binding.tvDetalle.text =
                "$nombreNino · ${ProgresionUtils.nombreCategoria(tarea.categoria)} · +${tarea.puntos} XP"
            val ctx = binding.root.context
            val colorIcono = when (tarea.categoria) {
                "academica" -> R.color.game_blue_dark
                "domestica" -> R.color.game_orange_dark
                else -> R.color.game_green_dark
            }
            binding.ivCategoria.icon = IconosUtils.crearIcono(
                ctx,
                ProgresionUtils.iconoCategoria(tarea.categoria),
                colorIcono
            )
            val bgIcono = when (tarea.categoria) {
                "academica" -> R.drawable.circle_game_blue
                "domestica" -> R.drawable.circle_game_orange
                else -> R.drawable.circle_game_green
            }
            binding.ivCategoria.setBackgroundResource(bgIcono)

            binding.btnValidar.setOnClickListener { onValidar(tarea) }
            binding.btnRechazar.setOnClickListener { onRechazar(tarea) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Tarea>() {
        override fun areItemsTheSame(oldItem: Tarea, newItem: Tarea): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Tarea, newItem: Tarea): Boolean = oldItem == newItem
    }
}
