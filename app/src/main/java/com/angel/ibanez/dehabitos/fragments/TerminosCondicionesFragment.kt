package com.angel.ibanez.dehabitos.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.angel.ibanez.dehabitos.databinding.FragmentTerminosCondicionesBinding
import com.mikepenz.iconics.view.IconicsImageView

class TerminosCondicionesFragment : Fragment() {

    private var _binding: FragmentTerminosCondicionesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTerminosCondicionesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarDesplegable(binding.headerCondiciones, binding.contentCondiciones, binding.arrowCondiciones)
        configurarDesplegable(binding.headerPrivacidad, binding.contentPrivacidad, binding.arrowPrivacidad)
        configurarDesplegable(binding.headerSeguridad, binding.contentSeguridad, binding.arrowSeguridad)
        configurarDesplegable(binding.headerProyecto, binding.contentProyecto, binding.arrowProyecto)
    }

    private fun configurarDesplegable(header: View, contenido: TextView, flecha: IconicsImageView) {
        header.setOnClickListener {
            cambiarEstado(contenido, flecha, contenido.visibility != View.VISIBLE, true)
        }
    }

    private fun cambiarEstado(
        contenido: TextView,
        flecha: IconicsImageView,
        mostrar: Boolean,
        animar: Boolean
    ) {
        contenido.visibility = if (mostrar) View.VISIBLE else View.GONE
        if (animar) {
            flecha.animate().rotation(if (mostrar) 180f else 0f).setDuration(180).start()
            if (mostrar) {
                contenido.alpha = 0f
                contenido.translationY = -12f
                contenido.animate().alpha(1f).translationY(0f).setDuration(220).start()
            }
        } else {
            flecha.rotation = if (mostrar) 180f else 0f
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
