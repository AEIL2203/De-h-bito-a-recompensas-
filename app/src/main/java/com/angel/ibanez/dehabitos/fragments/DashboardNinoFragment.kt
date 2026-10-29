package com.angel.ibanez.dehabitos.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.angel.ibanez.dehabitos.adapters.LogrosAdapter
import com.angel.ibanez.dehabitos.databinding.FragmentDashboardNinoBinding
import com.angel.ibanez.dehabitos.models.Nino
import com.angel.ibanez.dehabitos.utils.AnimacionesUtils
import com.angel.ibanez.dehabitos.utils.LogrosUtils
import com.angel.ibanez.dehabitos.utils.Resultado
import com.angel.ibanez.dehabitos.viewmodels.NinoViewModel

class DashboardNinoFragment : Fragment() {

    private var _binding: FragmentDashboardNinoBinding? = null
    private val binding get() = _binding!!
    private val ninoViewModel: NinoViewModel by viewModels()

    private var ninoId: String = ""
    private val logrosAdapter = LogrosAdapter()

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
        binding.rvLogros.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            false
        )
        binding.rvLogros.isNestedScrollingEnabled = false
        binding.rvLogros.adapter = logrosAdapter

        AnimacionesUtils.entradaSuave(binding.cardEstadisticas)
        listOf(
            binding.chipEstrellas to "Gana monedas subiendo de nivel y canjéalas por tiempo de juego",
            binding.chipRacha to "Días seguidos completando misiones. ¡No rompas la racha!",
            binding.chipInsignias to "Insignias ganadas por tus hitos"
        ).forEach { (chip, mensaje) ->
            chip.setOnClickListener { Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show() }
        }
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

        // Estadísticas de tareas
        binding.tvStatAcademicas.text = nino.tareasAcademicas.toString()
        binding.tvStatDomesticas.text = nino.tareasDomesticas.toString()
        binding.tvStatPersonales.text = nino.tareasPersonales.toString()

        // Estadísticas del niño (Estrellas / Racha / Insignias)
        binding.tvEstrellas.text = nino.estrellas.toString()
        binding.tvRacha.text = nino.racha.toString()
        binding.tvInsignias.text = nino.insignias.size.toString()

        val logros = LogrosUtils.logrosDesbloqueados(nino)
        logrosAdapter.actualizar(nino)
        binding.tvProgresoLogros.text = "${logros.size} de ${LogrosUtils.CATALOGO.size}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
