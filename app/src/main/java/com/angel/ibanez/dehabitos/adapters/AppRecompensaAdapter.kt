package com.angel.ibanez.dehabitos.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.angel.ibanez.dehabitos.databinding.ItemAppRecompensaBinding
import com.angel.ibanez.dehabitos.models.AplicacionPermitida
import com.angel.ibanez.dehabitos.utils.AppsUtils

class AppRecompensaAdapter(
    private val estrellas: Int,
    private val onCanjear: (AplicacionPermitida, Int, Int) -> Unit
) : ListAdapter<AplicacionPermitida, AppRecompensaAdapter.AppViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppRecompensaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AppViewHolder(private val binding: ItemAppRecompensaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(app: AplicacionPermitida) {
            binding.tvNombre.text = app.nombre
            binding.ivIcono.setImageDrawable(AppsUtils.base64ADrawable(binding.root.context, app.iconoBase64))

            binding.btn15.text = "15m\n${app.precio15min}"
            binding.btn30.text = "30m\n${app.precio30min}"
            binding.btn60.text = "60m\n${app.precio60min}"

            binding.btn15.isEnabled = app.precio15min > 0 && estrellas >= app.precio15min
            binding.btn30.isEnabled = app.precio30min > 0 && estrellas >= app.precio30min
            binding.btn60.isEnabled = app.precio60min > 0 && estrellas >= app.precio60min

            binding.btn15.setOnClickListener { onCanjear(app, 15, app.precio15min) }
            binding.btn30.setOnClickListener { onCanjear(app, 30, app.precio30min) }
            binding.btn60.setOnClickListener { onCanjear(app, 60, app.precio60min) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AplicacionPermitida>() {
        override fun areItemsTheSame(oldItem: AplicacionPermitida, newItem: AplicacionPermitida): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: AplicacionPermitida, newItem: AplicacionPermitida): Boolean = oldItem == newItem
    }
}
