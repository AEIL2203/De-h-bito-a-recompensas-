package com.angel.ibanez.dehabitos.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.angel.ibanez.dehabitos.databinding.ItemNinoBinding
import com.angel.ibanez.dehabitos.models.Nino

class NinoAdapter(
    private val onItemClick: ((Nino) -> Unit)? = null
) : ListAdapter<Nino, NinoAdapter.NinoViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NinoViewHolder {
        val binding = ItemNinoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NinoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NinoViewHolder, position: Int) {
        val nino = getItem(position)
        holder.bind(nino)
        holder.itemView.setOnClickListener { onItemClick?.invoke(nino) }
    }

    class NinoViewHolder(private val binding: ItemNinoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(nino: Nino) {
            binding.tvNombre.text = nino.nombre
            binding.tvEdad.text = "${nino.edad} años"
            binding.tvNivel.text = "⭐ Nivel ${nino.nivel}"
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Nino>() {
        override fun areItemsTheSame(oldItem: Nino, newItem: Nino): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Nino, newItem: Nino): Boolean {
            return oldItem == newItem
        }
    }
}
