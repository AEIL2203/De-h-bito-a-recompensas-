package com.angel.ibanez.dehabitos.adapters

import android.app.Dialog
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.angel.ibanez.dehabitos.R
import com.angel.ibanez.dehabitos.databinding.ItemTareaValidarBinding
import com.angel.ibanez.dehabitos.models.Tarea
import com.angel.ibanez.dehabitos.utils.FechasUtils
import com.angel.ibanez.dehabitos.utils.IconosUtils
import com.angel.ibanez.dehabitos.utils.ImagenesUtils
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
            val limite = if (tarea.fechaLimite.isNotBlank()) " · Límite ${FechasUtils.formatearFechaCorta(tarea.fechaLimite)}" else ""
            binding.tvDetalle.text =
                "$nombreNino · ${ProgresionUtils.nombreCategoria(tarea.categoria)} · +${tarea.puntos} XP$limite"
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

            val evidencia = if (tarea.evidencia.isNotBlank()) ImagenesUtils.decodificar(tarea.evidencia) else null
            if (evidencia != null) {
                binding.ivEvidencia.setImageBitmap(evidencia)
                binding.ivEvidencia.visibility = View.VISIBLE
                binding.ivEvidencia.setOnClickListener { mostrarEvidenciaCompleta(evidencia) }
            } else {
                binding.ivEvidencia.visibility = View.GONE
                binding.ivEvidencia.setOnClickListener(null)
            }

            binding.btnValidar.setOnClickListener { onValidar(tarea) }
            binding.btnRechazar.setOnClickListener { onRechazar(tarea) }
        }

        private fun mostrarEvidenciaCompleta(bitmap: android.graphics.Bitmap) {
            val contexto = binding.root.context
            val imagen = ImageView(contexto).apply {
                setImageBitmap(bitmap)
                scaleType = ImageView.ScaleType.FIT_CENTER
                setBackgroundColor(Color.BLACK)
            }
            val dialog = Dialog(contexto, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            dialog.setContentView(imagen)
            imagen.setOnClickListener { dialog.dismiss() }
            dialog.show()
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Tarea>() {
        override fun areItemsTheSame(oldItem: Tarea, newItem: Tarea): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Tarea, newItem: Tarea): Boolean = oldItem == newItem
    }
}
