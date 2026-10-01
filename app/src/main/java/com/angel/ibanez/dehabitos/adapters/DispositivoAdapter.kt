package com.angel.ibanez.dehabitos.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.angel.ibanez.dehabitos.databinding.ItemDispositivoBinding
import com.angel.ibanez.dehabitos.models.Dispositivo

class DispositivoAdapter(
    private val onToggle: (Dispositivo, Boolean) -> Unit,
    private val onDesvincular: (Dispositivo) -> Unit
) : ListAdapter<Dispositivo, DispositivoAdapter.DispositivoViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DispositivoViewHolder {
        val binding = ItemDispositivoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DispositivoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DispositivoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DispositivoViewHolder(private val binding: ItemDispositivoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(dispositivo: Dispositivo) {
            binding.tvNombreDispositivo.text = buildString {
                append("Dispositivo de ${dispositivo.nombreNino}")
                if (dispositivo.nombreDispositivo.isNotBlank()) append(" · ${dispositivo.nombreDispositivo}")
            }
            binding.tvEstado.text = if (dispositivo.desbloqueado) "Desbloqueado" else "Bloqueado"

            binding.swDesbloquear.setOnCheckedChangeListener(null)
            binding.swDesbloquear.isChecked = dispositivo.desbloqueado
            binding.swDesbloquear.setOnCheckedChangeListener { _, isChecked ->
                onToggle(dispositivo, isChecked)
            }
            binding.btnDesvincular.setOnClickListener { onDesvincular(dispositivo) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Dispositivo>() {
        override fun areItemsTheSame(oldItem: Dispositivo, newItem: Dispositivo): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Dispositivo, newItem: Dispositivo): Boolean {
            return oldItem == newItem
        }
    }
}
