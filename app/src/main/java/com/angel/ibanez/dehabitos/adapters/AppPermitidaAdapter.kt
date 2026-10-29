package com.angel.ibanez.dehabitos.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.angel.ibanez.dehabitos.databinding.ItemAppPermitidaBinding
import com.angel.ibanez.dehabitos.models.AplicacionPermitida
import com.angel.ibanez.dehabitos.utils.AppsUtils

class AppPermitidaAdapter(
    private val onEliminar: (AplicacionPermitida) -> Unit
) : ListAdapter<AplicacionPermitida, AppPermitidaAdapter.AppViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppPermitidaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AppViewHolder(private val binding: ItemAppPermitidaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(app: AplicacionPermitida) {
            binding.tvNombre.text = app.nombre
            listOf(
                Triple(binding.tvPrecio15, 15, app.precio15min),
                Triple(binding.tvPrecio30, 30, app.precio30min),
                Triple(binding.tvPrecio60, 60, app.precio60min)
            ).forEach { (pildora, minutos, precio) ->
                pildora.visibility = if (precio > 0) View.VISIBLE else View.GONE
                pildora.text = "${minutos}m · $precio 🪙"
            }
            binding.ivIcono.setImageDrawable(AppsUtils.base64ADrawable(binding.root.context, app.iconoBase64))
            binding.btnEliminar.setOnClickListener { onEliminar(app) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AplicacionPermitida>() {
        override fun areItemsTheSame(oldItem: AplicacionPermitida, newItem: AplicacionPermitida): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: AplicacionPermitida, newItem: AplicacionPermitida): Boolean = oldItem == newItem
    }
}
