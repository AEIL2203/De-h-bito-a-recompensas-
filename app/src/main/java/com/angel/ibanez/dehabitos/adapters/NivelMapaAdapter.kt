package com.angel.ibanez.dehabitos.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.angel.ibanez.dehabitos.R
import com.angel.ibanez.dehabitos.databinding.ItemNivelMapaBinding
import com.angel.ibanez.dehabitos.utils.IconosUtils

class NivelMapaAdapter(
    private val nivelActual: Int,
    private val nivelMaximo: Int,
    private val nivelesConLogro: Set<Int> = setOf(5, 10, 15, 20)
) : RecyclerView.Adapter<NivelMapaAdapter.NivelViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NivelViewHolder {
        val binding = ItemNivelMapaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NivelViewHolder(binding)
    }

    override fun getItemCount(): Int = nivelMaximo

    override fun onBindViewHolder(holder: NivelViewHolder, position: Int) {
        // El camino se muestra de arriba (nivel máximo) hacia abajo (nivel 1)
        val nivel = nivelMaximo - position
        holder.bind(nivel, position)
    }

    inner class NivelViewHolder(private val binding: ItemNivelMapaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(nivel: Int, position: Int) {
            val context = binding.root.context
            val currentBias = getBiasForPosition(position)

            // Posicionar el nodo
            val params = binding.nodoNivel.layoutParams as ConstraintLayout.LayoutParams
            params.horizontalBias = currentBias
            binding.nodoNivel.layoutParams = params

            // Lógica del camino ondulado
            if (position == 0) {
                binding.wavyPath.visibility = View.GONE
            } else {
                binding.wavyPath.visibility = View.VISIBLE
                val prevBias = getBiasForPosition(position - 1)
                
                // Usamos el color terciario para que resalte más contra el fondo claro
                val pathColor = context.getColor(R.color.md_theme_tertiary)
                binding.wavyPath.setPathConfig(prevBias, currentBias, pathColor, isDashed = (nivel >= nivelActual))
            }

            binding.tvEtiquetaNivel.text = "Nivel $nivel"

            when {
                nivel < nivelActual -> {
                    binding.circuloNivel.setBackgroundResource(R.drawable.node_completed)
                    binding.ivEstado.visibility = View.VISIBLE
                    binding.tvNumeroNivel.visibility = View.GONE
                    binding.ivEstado.icon = IconosUtils.crearIcono(context, "gmr-done", R.color.white, 24)
                }
                nivel == nivelActual -> {
                    binding.circuloNivel.setBackgroundResource(R.drawable.node_current)
                    binding.ivEstado.visibility = View.GONE
                    binding.tvNumeroNivel.visibility = View.VISIBLE
                    binding.tvNumeroNivel.text = nivel.toString()
                }
                else -> {
                    binding.circuloNivel.setBackgroundResource(R.drawable.node_locked)
                    binding.ivEstado.visibility = View.VISIBLE
                    binding.tvNumeroNivel.visibility = View.GONE
                    binding.ivEstado.icon = IconosUtils.crearIcono(context, "gmr-lock", R.color.md_theme_onSurfaceVariant, 20)
                }
            }

            val tieneLogro = nivel in nivelesConLogro
            binding.ivLogro.visibility = if (tieneLogro) View.VISIBLE else View.GONE
            if (tieneLogro) {
                val icono = if (nivel <= nivelActual) "gmr-grade" else "gmr-star-border"
                binding.ivLogro.icon = IconosUtils.crearIcono(context, icono, R.color.game_yellow, 32)
            }
        }

        private fun getBiasForPosition(pos: Int): Float {
            return when (pos % 4) {
                0 -> 0.5f   // Centro
                1 -> 0.15f  // Izquierda
                2 -> 0.5f   // Centro
                else -> 0.85f // Derecha
            }
        }
    }
}
