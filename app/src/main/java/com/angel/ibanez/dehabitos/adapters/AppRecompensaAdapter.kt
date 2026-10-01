package com.angel.ibanez.dehabitos.adapters

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.StyleSpan
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
            binding.tvSubtitulo.text = "Elige tu tiempo"
            binding.tvSubtitulo.visibility = View.VISIBLE
            binding.ivIcono.setImageDrawable(AppsUtils.base64ADrawable(binding.root.context, app.iconoBase64))

            setChipText(binding.btn15, "15 min", app.precio15min)
            setChipText(binding.btn30, "30 min", app.precio30min)
            setChipText(binding.btn60, "60 min", app.precio60min)

            binding.btn15.isEnabled = app.precio15min > 0 && estrellas >= app.precio15min
            binding.btn30.isEnabled = app.precio30min > 0 && estrellas >= app.precio30min
            binding.btn60.isEnabled = app.precio60min > 0 && estrellas >= app.precio60min

            binding.btn15.setOnClickListener { onCanjear(app, 15, app.precio15min) }
            binding.btn30.setOnClickListener { onCanjear(app, 30, app.precio30min) }
            binding.btn60.setOnClickListener { onCanjear(app, 60, app.precio60min) }
        }

        private fun setChipText(button: com.google.android.material.button.MaterialButton, min: String, precio: Int) {
            val unidad = if (precio == 1) "moneda" else "monedas"
            val linea2 = "$precio $unidad"
            val texto = "$min\n$linea2"
            val spannable = SpannableString(texto)
            spannable.setSpan(StyleSpan(Typeface.BOLD), 0, min.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(AbsoluteSizeSpan(12, true), min.length + 1, texto.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            button.text = spannable
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AplicacionPermitida>() {
        override fun areItemsTheSame(oldItem: AplicacionPermitida, newItem: AplicacionPermitida): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: AplicacionPermitida, newItem: AplicacionPermitida): Boolean = oldItem == newItem
    }
}
