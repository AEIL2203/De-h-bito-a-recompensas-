package com.angel.ibanez.dehabitos.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.angel.ibanez.dehabitos.R
import com.angel.ibanez.dehabitos.databinding.FragmentSplashBinding
import com.angel.ibanez.dehabitos.utils.DispositivoConfig
import com.angel.ibanez.dehabitos.utils.Resultado
import com.angel.ibanez.dehabitos.viewmodels.NinoViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!
    private val ninoViewModel: NinoViewModel by activityViewModels()

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

        iniciarAnimaciones()

        viewLifecycleOwner.lifecycleScope.launch {
            delay(2800)
            if (!isAdded) return@launch

            when (determinarRuta()) {
                Ruta.DASHBOARD_TUTOR -> precargarDashboardYNavegar()
                else -> animarSalidaYNavegar()
            }
        }
    }

    private fun precargarDashboardYNavegar() {
        val tutorId = DispositivoConfig.obtenerTutorId()
        if (tutorId.isBlank()) {
            animarSalidaYNavegar()
            return
        }

        ninoViewModel.cargarNinosPorTutor(tutorId)

        var yaNavego = false
        ninoViewModel.listaNinosResultado.observe(viewLifecycleOwner) { resultado ->
            if (yaNavego) return@observe
            if (resultado is Resultado.Exito || resultado is Resultado.Error) {
                yaNavego = true
                animarSalidaYNavegar()
            }
        }

        // Timeout por si la carga nunca responde
        viewLifecycleOwner.lifecycleScope.launch {
            delay(6000)
            if (isAdded && !yaNavego) {
                yaNavego = true
                animarSalidaYNavegar()
            }
        }
    }

    private fun animarSalidaYNavegar() {
        val fadeOut = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_out)
        fadeOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                decidirRuta()
            }
        })
        binding.root.startAnimation(fadeOut)
    }

    private enum class Ruta {
        LOGIN_TUTOR, LOGIN_NINO, DASHBOARD_TUTOR, SELECCION_ROL
    }

    private fun determinarRuta(): Ruta {
        val tutorId = DispositivoConfig.obtenerTutorId()
        val modo = DispositivoConfig.obtenerModo()
        return when {
            tutorId.isBlank() -> Ruta.LOGIN_TUTOR
            modo == DispositivoConfig.ModoDispositivo.HIJO && DispositivoConfig.obtenerNinoId().isNotBlank() -> Ruta.LOGIN_NINO
            modo == DispositivoConfig.ModoDispositivo.PADRE -> Ruta.DASHBOARD_TUTOR
            else -> Ruta.SELECCION_ROL
        }
    }

    private fun iniciarAnimaciones() {
        val expand = AnimationUtils.loadAnimation(requireContext(), R.anim.splash_ring_expand)
        binding.ring1.startAnimation(expand)

        val expand2 = AnimationUtils.loadAnimation(requireContext(), R.anim.splash_ring_expand)
        expand2.startOffset = 600
        binding.ring2.startAnimation(expand2)

        val expand3 = AnimationUtils.loadAnimation(requireContext(), R.anim.splash_ring_expand)
        expand3.startOffset = 1200
        binding.ring3.startAnimation(expand3)

        val fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.splash_fade_in)
        fadeIn.startOffset = 300
        binding.tvTitulo.startAnimation(fadeIn)
    }

    private fun decidirRuta() {
        if (findNavController().currentDestination?.id != R.id.splashFragment) return

        val opciones = navOptions {
            popUpTo(R.id.splashFragment) { inclusive = true }
            anim {
                enter = R.anim.fade_in
                exit = R.anim.fade_out
            }
        }

        when (determinarRuta()) {
            Ruta.LOGIN_TUTOR -> {
                findNavController().navigate(R.id.action_splashFragment_to_loginTutorFragment, null, opciones)
            }
            Ruta.LOGIN_NINO -> {
                val bundle = Bundle().apply { putString("ninoId", DispositivoConfig.obtenerNinoId()) }
                findNavController().navigate(R.id.action_splashFragment_to_loginNinoFragment, bundle, opciones)
            }
            Ruta.DASHBOARD_TUTOR -> {
                findNavController().navigate(R.id.action_splashFragment_to_dashboardTutorFragment, null, opciones)
            }
            Ruta.SELECCION_ROL -> {
                findNavController().navigate(R.id.action_splashFragment_to_seleccionRolFragment, null, opciones)
            }
        }
    }

    override fun onDestroyView() {
        _binding?.ring1?.clearAnimation()
        _binding?.ring2?.clearAnimation()
        _binding?.ring3?.clearAnimation()
        _binding?.tvTitulo?.clearAnimation()
        _binding = null
        super.onDestroyView()
    }
}
