package com.angel.ibanez.dehabitos.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.angel.ibanez.dehabitos.utils.TecladoUtils
import com.angel.ibanez.dehabitos.R
import com.angel.ibanez.dehabitos.databinding.FragmentAgregarNinoBinding
import com.angel.ibanez.dehabitos.utils.IconosUtils
import com.angel.ibanez.dehabitos.models.Nino
import com.angel.ibanez.dehabitos.repository.TutorRepository
import com.angel.ibanez.dehabitos.utils.Resultado
import com.angel.ibanez.dehabitos.viewmodels.NinoViewModel
import kotlinx.coroutines.launch

class AgregarNinoFragment : Fragment() {

    private var _binding: FragmentAgregarNinoBinding? = null
    private val binding get() = _binding!!
    private val ninoViewModel: NinoViewModel by viewModels()
    private val tutorRepository = TutorRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAgregarNinoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnGuardar.icon = IconosUtils.crearIcono(requireContext(), "gmr-done-all", R.color.md_theme_onPrimary)

        TecladoUtils.configurar(binding.etNombre, binding.etEdad) { binding.btnGuardar.performClick() }

        binding.btnGuardar.setOnClickListener {
            val nombre = binding.etNombre.text.toString().trim()
            val edad = binding.etEdad.text.toString().toIntOrNull() ?: 0
            binding.tilNombre.error = if (nombre.isEmpty()) "Escribe el nombre" else null
            binding.tilEdad.error = if (edad !in 1..17) "Edad entre 1 y 17" else null
            if (binding.tilNombre.error != null || binding.tilEdad.error != null) {
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                val tutor = tutorRepository.obtenerTutorActual()
                if (tutor != null) {
                    val nino = Nino(
                        nombre = nombre,
                        edad = edad,
                        tutorId = tutor.id,
                        avatar = "",
                        nivel = 1,
                        experiencia = 0
                    )
                    ninoViewModel.crearNino(nino)
                } else {
                    Toast.makeText(requireContext(), "No se encontró tutor activo", Toast.LENGTH_SHORT).show()
                }
            }
        }

        ninoViewModel.crearNinoResultado.observe(viewLifecycleOwner) { resultado ->
            when (resultado) {
                is Resultado.Cargando -> mostrarCarga(true)
                is Resultado.Exito -> {
                    mostrarCarga(false)
                    Toast.makeText(requireContext(), "Niño creado", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_agregarNinoFragment_to_dashboardTutorFragment)
                }
                is Resultado.Error -> {
                    mostrarCarga(false)
                    Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun mostrarCarga(mostrar: Boolean) {
        binding.progressBar.visibility = if (mostrar) View.VISIBLE else View.GONE
        binding.btnGuardar.isEnabled = !mostrar
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
