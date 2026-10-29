package com.angel.ibanez.dehabitos.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
            val colorCategoria = when (tarea.categoria) {
                "academica" -> R.color.neon_cyan
                "domestica" -> R.color.neon_coral
                else -> R.color.neon_green
            }
            binding.ivCategoria.icon = IconosUtils.crearIcono(
                context,
                ProgresionUtils.iconoCategoria(tarea.categoria),
                R.color.child_on_glow,
                28
            )
            binding.ivCategoria.backgroundTintList = ContextCompat.getColorStateList(context, colorCategoria)

            if (tarea.estado == Tarea.ESTADO_PENDIENTE && tarea.fechaLimite.isNotBlank()) {
                val corta = FechasUtils.formatearFechaCorta(tarea.fechaLimite)
                when (FechasUtils.estadoVencimiento(tarea.fechaLimite)) {
                    FechasUtils.EstadoVencimiento.VENCIDA ->
                        binding.tvFechaLimite.pintarPildora("⚠ Atrasada · $corta", R.color.child_warning, R.color.white)
                    FechasUtils.EstadoVencimiento.VENCE_HOY ->
                        binding.tvFechaLimite.pintarPildora("⏰ ¡Vence hoy!", R.color.neon_gold, R.color.child_on_glow)
                    else ->
                        binding.tvFechaLimite.pintarPildora("📅 Vence $corta", R.color.child_track, R.color.child_on_surface_variant)
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
                Tarea.ESTADO_PENDIENTE ->
                    binding.tvEstado.pintarPildora("Pendiente", R.color.child_tint_violet, R.color.neon_violet)
                Tarea.ESTADO_COMPLETADA ->
                    binding.tvEstado.pintarPildora("⏳ Esperando validación", R.color.child_tint_gold, R.color.neon_gold)
                else ->
                    binding.tvEstado.pintarPildora("✓ ¡Validada!", R.color.child_tint_green, R.color.neon_green)
            }
            val vencida = tarea.estado == Tarea.ESTADO_PENDIENTE &&
                FechasUtils.estadoVencimiento(tarea.fechaLimite) == FechasUtils.EstadoVencimiento.VENCIDA
            binding.btnCompletar.visibility = if (tarea.estado == Tarea.ESTADO_PENDIENTE) View.VISIBLE else View.GONE
            binding.btnCompletar.isEnabled = !vencida
            binding.btnCompletar.alpha = if (vencida) 0.45f else 1f
            binding.btnCompletar.text = if (vencida) "⛔ Vencida" else context.getString(R.string.mission_i_did_it)
            binding.btnCompletar.setIconResource(if (vencida) 0 else R.drawable.ic_check_small)

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

    private fun TextView.pintarPildora(texto: String, fondo: Int, colorTexto: Int) {
        text = texto
        backgroundTintList = ContextCompat.getColorStateList(context, fondo)
        setTextColor(ContextCompat.getColor(context, colorTexto))
    }

    class DiffCallback : DiffUtil.ItemCallback<Tarea>() {
        override fun areItemsTheSame(oldItem: Tarea, newItem: Tarea): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Tarea, newItem: Tarea): Boolean = oldItem == newItem
    }
}
