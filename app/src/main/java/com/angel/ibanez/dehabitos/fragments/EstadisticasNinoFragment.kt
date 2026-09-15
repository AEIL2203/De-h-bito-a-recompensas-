package com.angel.ibanez.dehabitos.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.angel.ibanez.dehabitos.R
import com.angel.ibanez.dehabitos.databinding.FragmentEstadisticasNinoBinding
import com.angel.ibanez.dehabitos.models.Nino
import com.angel.ibanez.dehabitos.repository.TutorRepository
import com.angel.ibanez.dehabitos.utils.Resultado
import com.angel.ibanez.dehabitos.viewmodels.EstadisticasViewModel
import com.angel.ibanez.dehabitos.viewmodels.NinoViewModel
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

class EstadisticasNinoFragment : Fragment() {
    private var _binding: FragmentEstadisticasNinoBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EstadisticasViewModel by viewModels()
    private val ninoViewModel: NinoViewModel by viewModels()
    private var ninos = emptyList<Nino>()
    private var seleccionado = ""
    private var sieteDias = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentEstadisticasNinoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        seleccionado = arguments?.getString("ninoId").orEmpty()
        binding.grupoPeriodo.check(R.id.btnSieteDias)
        binding.grupoPeriodo.addOnButtonCheckedListener { _, id, checked -> if (checked) { sieteDias = id == R.id.btnSieteDias; cargar() } }
        ninoViewModel.listaNinosResultado.observe(viewLifecycleOwner) { resultado ->
            if (resultado is Resultado.Exito) {
                ninos = resultado.data
                binding.spNino.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, ninos.map { it.nombre })
                val posicion = ninos.indexOfFirst { it.id == seleccionado }.coerceAtLeast(0)
                binding.spNino.setSelection(posicion)
                binding.spNino.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) { seleccionado = ninos[position].id; cargar() }
                    override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
                }
            }
        }
        viewModel.resultado.observe(viewLifecycleOwner) { resultado ->
            binding.progressBar.visibility = if (resultado is Resultado.Cargando) View.VISIBLE else View.GONE
            when (resultado) {
                is Resultado.Exito -> mostrar(resultado.data)
                is Resultado.Error -> Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_LONG).show()
                else -> Unit
            }
        }
        TutorRepository().let { repo -> viewLifecycleOwner.lifecycleScope.launch { repo.obtenerTutorActual()?.let { ninoViewModel.cargarNinosPorTutor(it.id) } } }
    }

    private fun cargar() { if (seleccionado.isNotBlank()) viewModel.cargar(seleccionado, sieteDias) }

    private fun mostrar(e: com.angel.ibanez.dehabitos.models.EstadisticasNino) {
        val categorias = e.tareasPorCategoria.entries.joinToString { "${it.key}: ${it.value}" }.ifBlank { "Sin tareas" }
        binding.tvResumen.text = "Nivel actual: ${e.nino.nivel}\nNiveles avanzados: ${e.nivelesAvanzados}\nTareas validadas: ${e.tareasValidadas}\nPor categoría: $categorias\nCanjes totales: ${e.canjesTotales}\nEstrellas gastadas: ${e.estrellasGastadas}\nTiempo usado: ${formatoDuracion(e.tiempoUsadoMs)}"
        binding.tvCanjes.text = "Canjes por aplicación\n" + e.canjesPorApp.joinToString("\n") { "${it.packageName}: ${it.cantidad} canjes, ${it.minutos} min" }.ifBlank { "Sin canjes" }
        binding.tvSesiones.text = "Sesiones recientes\n" + e.sesionesRecientes.joinToString("\n") { "${it.nombre}: ${formatoDuracion(it.duracionMs)} · ${DateFormat.getDateTimeInstance().format(Date(it.inicio))}" }.ifBlank { "Sin sesiones" }
    }

    private fun formatoDuracion(ms: Long) = "${ms / 3_600_000} h ${(ms / 60_000) % 60} min"
    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
