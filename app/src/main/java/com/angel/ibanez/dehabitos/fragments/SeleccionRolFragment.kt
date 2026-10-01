package com.angel.ibanez.dehabitos.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import android.widget.Toast
import com.angel.ibanez.dehabitos.R
import com.angel.ibanez.dehabitos.databinding.FragmentSeleccionRolBinding
import com.angel.ibanez.dehabitos.utils.DispositivoConfig
import com.angel.ibanez.dehabitos.utils.IconosUtils

class SeleccionRolFragment : Fragment() {

    private var _binding: FragmentSeleccionRolBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSeleccionRolBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSoyTutor.icon = IconosUtils.crearIcono(requireContext(), "gmr-play-arrow", R.color.md_theme_onPrimary)
        binding.btnSoyNino.icon = IconosUtils.crearIcono(requireContext(), "gmr-play-arrow", R.color.md_theme_onPrimary)

        binding.btnSoyTutor.setOnClickListener { usarModoPadre() }
        binding.cardTutor.setOnClickListener { usarModoPadre() }

        binding.btnSoyNino.setOnClickListener { usarModoHijo() }
        binding.cardNino.setOnClickListener { usarModoHijo() }
    }

    private fun usarModoPadre() {
        if (DispositivoConfig.obtenerTutorId().isBlank()) {
            Toast.makeText(requireContext(), "Primero inicia sesión como tutor", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_seleccionRolFragment_to_loginTutorFragment)
            return
        }
        DispositivoConfig.guardarModo(DispositivoConfig.ModoDispositivo.PADRE)
        findNavController().navigate(R.id.action_seleccionRolFragment_to_dashboardTutorFragment)
    }

    private fun usarModoHijo() {
        if (DispositivoConfig.obtenerTutorId().isBlank()) {
            Toast.makeText(requireContext(), "Primero inicia sesión como tutor", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_seleccionRolFragment_to_loginTutorFragment)
            return
        }
        DispositivoConfig.guardarModo(DispositivoConfig.ModoDispositivo.HIJO)
        findNavController().navigate(R.id.action_seleccionRolFragment_to_seleccionarNinoFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
