package com.angel.ibanez.dehabitos.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.angel.ibanez.dehabitos.R
import com.angel.ibanez.dehabitos.databinding.FragmentSplashBinding
import com.angel.ibanez.dehabitos.utils.DispositivoConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewLifecycleOwner.lifecycleScope.launch {
            // Espera breve para asegurar que la UI se renderice y tome foco
            delay(1200)
            if (isAdded) {
                decidirRuta()
            }
        }
    }

    private fun decidirRuta() {
        if (findNavController().currentDestination?.id != R.id.splashFragment) return

        val tutorId = DispositivoConfig.obtenerTutorId()
        val modo = DispositivoConfig.obtenerModo()

        when {
            tutorId.isBlank() -> {
                findNavController().navigate(R.id.action_splashFragment_to_loginTutorFragment)
            }
            modo == DispositivoConfig.ModoDispositivo.HIJO && DispositivoConfig.obtenerNinoId().isNotBlank() -> {
                val bundle = Bundle().apply { putString("ninoId", DispositivoConfig.obtenerNinoId()) }
                findNavController().navigate(R.id.action_splashFragment_to_loginNinoFragment, bundle)
            }
            modo == DispositivoConfig.ModoDispositivo.PADRE -> {
                findNavController().navigate(R.id.action_splashFragment_to_dashboardTutorFragment)
            }
            else -> {
                findNavController().navigate(R.id.action_splashFragment_to_seleccionRolFragment)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
