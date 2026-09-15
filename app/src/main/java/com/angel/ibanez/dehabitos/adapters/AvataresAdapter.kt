package com.angel.ibanez.dehabitos.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.angel.ibanez.dehabitos.R
import com.angel.ibanez.dehabitos.databinding.ItemAvatarBinding
import com.angel.ibanez.dehabitos.utils.AvataresUtils
import com.angel.ibanez.dehabitos.utils.AvataresUtils.Avatar

class AvataresAdapter(
    private val avatares: List<Avatar> = AvataresUtils.lista,
    private val onSeleccionar: (Avatar) -> Unit
) : RecyclerView.Adapter<AvataresAdapter.AvatarViewHolder>() {

    var seleccionadoId: String = AvataresUtils.DEFAULT
        set(value) {
            if (field == value) return
            val anterior = avatares.indexOfFirst { it.id == field }
            field = value
            val nuevo = avatares.indexOfFirst { it.id == value }
            if (anterior >= 0) notifyItemChanged(anterior)
            if (nuevo >= 0) notifyItemChanged(nuevo)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AvatarViewHolder {
        val binding = ItemAvatarBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AvatarViewHolder(binding)
    }

    override fun getItemCount(): Int = avatares.size

    override fun onBindViewHolder(holder: AvatarViewHolder, position: Int) = holder.bind(avatares[position])

    inner class AvatarViewHolder(private val binding: ItemAvatarBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(avatar: Avatar) {
            val context = binding.root.context
            val seleccionado = avatar.id == seleccionadoId
            AvataresUtils.cargar(binding.ivAvatarItem, avatar.id)
            binding.tvNombreAvatar.text = avatar.nombre
            binding.cardAvatar.strokeWidth = if (seleccionado) dp(3) else 0
            binding.cardAvatar.strokeColor = context.getColor(R.color.game_yellow)
            binding.root.alpha = if (seleccionado) 1f else 0.7f
            val escala = if (seleccionado) 1.08f else 1f
            binding.cardAvatar.scaleX = escala
            binding.cardAvatar.scaleY = escala
            binding.root.setOnClickListener {
                if (seleccionadoId != avatar.id) {
                    seleccionadoId = avatar.id
                    onSeleccionar(avatar)
                }
            }
        }

        private fun dp(valor: Int): Int = (valor * binding.root.resources.displayMetrics.density).toInt()
    }
}
