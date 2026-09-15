package com.angel.ibanez.dehabitos.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.angel.ibanez.dehabitos.databinding.FragmentDashboardNinoBinding
import com.angel.ibanez.dehabitos.models.Nino
import com.angel.ibanez.dehabitos.utils.AnimacionesUtils
import com.angel.ibanez.dehabitos.utils.Resultado
import com.angel.ibanez.dehabitos.viewmodels.NinoViewModel

class DashboardNinoFragment : Fragment() {

    private var _binding: FragmentDashboardNinoBinding? = null
    private val binding get() = _binding!!
    private val ninoViewModel: NinoViewModel by viewModels()

    private var ninoId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardNinoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ninoId = arguments?.getString("ninoId") ?: ""

        AnimacionesUtils.entradaSuave(binding.cardEstadisticas)
        observarNino()
        cargarDatos()
    }

    private fun cargarDatos() {
        ninoViewModel.escucharNino(ninoId)
    }

    private fun observarNino() {
        ninoViewModel.ninoResultado.observe(viewLifecycleOwner) { resultado ->
            when (resultado) {
                is Resultado.Cargando -> binding.progressBar.visibility = View.VISIBLE
                is Resultado.Exito -> {
                    binding.progressBar.visibility = View.GONE
                    pintarPerfil(resultado.data)
                }
                is Resultado.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun pintarPerfil(nino: Nino) {
        binding.tvBienvenida.text = "¡Hola, ${nino.nombre}!"
        binding.tvRacha.text = "${nino.racha} días de racha"
        binding.tvInsignias.text = "${nino.insignias.size} insignias desbloqueadas"

        binding.tvStatAcademicas.text = nino.tareasAcademicas.toString()
        binding.tvStatDomesticas.text = nino.tareasDomesticas.toString()
        binding.tvStatPersonales.text = nino.tareasPersonales.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
