package com.angel.ibanez.dehabitos.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.angel.ibanez.dehabitos.R
import com.angel.ibanez.dehabitos.databinding.FragmentLoginNinoBinding
import com.angel.ibanez.dehabitos.models.Nino
import com.angel.ibanez.dehabitos.utils.DispositivoConfig
import com.angel.ibanez.dehabitos.utils.IconosUtils
import com.angel.ibanez.dehabitos.utils.Resultado
import com.angel.ibanez.dehabitos.viewmodels.NinoViewModel

class LoginNinoFragment : Fragment() {

    private var _binding: FragmentLoginNinoBinding? = null
    private val binding get() = _binding!!
    private val ninoViewModel: NinoViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginNinoBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val ninoIdEsperado: String by lazy {
        arguments?.getString("ninoId") ?: DispositivoConfig.obtenerNinoId()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnIngresar.icon = IconosUtils.crearIcono(requireContext(), "gmr-play-arrow", R.color.md_theme_onPrimary)

        binding.btnIngresar.setOnClickListener {
            val pin = binding.etPin.text.toString().trim()
            if (pin.length == 4) {
                ninoViewModel.iniciarSesionNino(pin, DispositivoConfig.obtenerTutorId())
            } else {
                Toast.makeText(requireContext(), "El PIN debe tener 4 dígitos", Toast.LENGTH_SHORT).show()
            }
        }

        ninoViewModel.loginNinoResultado.observe(viewLifecycleOwner) { resultado ->
            when (resultado) {
                is Resultado.Cargando -> mostrarCarga(true)
                is Resultado.Exito -> {
                    mostrarCarga(false)
                    val nino = resultado.data
                    if (!validarNino(nino)) {
                        Toast.makeText(requireContext(), "El PIN no corresponde a este dispositivo", Toast.LENGTH_LONG).show()
                    } else {
                        DispositivoConfig.guardarNino(nino.id, nino.nombre)
                        val bundle = Bundle().apply { putString("ninoId", nino.id) }
                        if (findNavController().currentDestination?.id == R.id.loginNinoFragment) {
                            findNavController().navigate(
                                R.id.action_loginNinoFragment_to_contenedorNinoFragment,
                                bundle
                            )
                        }
                    }
                }
                is Resultado.Error -> {
                    mostrarCarga(false)
                    Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun validarNino(nino: Nino): Boolean {
        return ninoIdEsperado.isBlank() || nino.id == ninoIdEsperado
    }

    private fun mostrarCarga(mostrar: Boolean) {
        binding.progressBar.visibility = if (mostrar) View.VISIBLE else View.GONE
        binding.btnIngresar.isEnabled = !mostrar
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
