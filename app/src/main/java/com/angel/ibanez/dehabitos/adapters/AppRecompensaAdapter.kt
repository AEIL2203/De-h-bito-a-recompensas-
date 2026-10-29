package com.angel.ibanez.dehabitos.adapters

import android.view.LayoutInflater
import android.view.View
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
            val nombre = AppsUtils.obtenerNombreApp(binding.root.context, app.packageName, app.nombre).trim()
            binding.tvNombre.text = nombre
            binding.ivIcono.setImageDrawable(AppsUtils.base64ADrawable(binding.root.context, app.iconoBase64))

            val opciones = listOf(
                Triple(binding.btn15, 15, app.precio15min),
                Triple(binding.btn30, 30, app.precio30min),
                Triple(binding.btn60, 60, app.precio60min)
            )
            opciones.forEach { (boton, minutos, precio) ->
                boton.visibility = if (precio > 0) View.VISIBLE else View.GONE
                boton.text = "$minutos min\n$precio monedas"
                boton.isEnabled = estrellas >= precio
                boton.setOnClickListener { onCanjear(app, minutos, precio) }
            }
            val minimo = opciones.map { it.third }.filter { it > 0 }.minOrNull()
            binding.tvSubtitulo.text = when {
                minimo == null -> "Sin opciones disponibles"
                estrellas >= minimo -> "Elige tu tiempo"
                else -> "Te faltan ${minimo - estrellas} monedas"
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AplicacionPermitida>() {
        override fun areItemsTheSame(oldItem: AplicacionPermitida, newItem: AplicacionPermitida): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: AplicacionPermitida, newItem: AplicacionPermitida): Boolean = oldItem == newItem
    }
}
