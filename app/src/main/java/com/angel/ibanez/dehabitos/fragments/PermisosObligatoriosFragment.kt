package com.angel.ibanez.dehabitos.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.angel.ibanez.dehabitos.R
import com.angel.ibanez.dehabitos.databinding.FragmentPermisosObligatoriosBinding
import com.angel.ibanez.dehabitos.utils.PermisosUtils

class PermisosObligatoriosFragment : Fragment() {

    private var _binding: FragmentPermisosObligatoriosBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPermisosObligatoriosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnAbrirAccesibilidad.setOnClickListener {
            if (!PermisosUtils.abrirConfiguracionAccesibilidad(requireContext())) {
                Toast.makeText(requireContext(), "No fue posible abrir Accesibilidad", Toast.LENGTH_LONG).show()
            }
        }
        binding.btnVerificarAccesibilidad.setOnClickListener {
            if (!continuarSiEstaActivo()) {
                Toast.makeText(requireContext(), "El servicio aún no está activo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        continuarSiEstaActivo()
    }

    private fun continuarSiEstaActivo(): Boolean {
        if (!PermisosUtils.servicioAccesibilidadActivo(requireContext())) return false
        if (findNavController().currentDestination?.id != R.id.permisosObligatoriosFragment) return true
        findNavController().navigate(
            R.id.splashFragment, null,
            navOptions { popUpTo(R.id.permisosObligatoriosFragment) { inclusive = true } }
        )
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
