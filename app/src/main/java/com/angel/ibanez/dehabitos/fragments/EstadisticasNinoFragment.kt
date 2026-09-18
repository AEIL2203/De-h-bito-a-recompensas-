package com.angel.ibanez.dehabitos.fragments

import android.content.Intent
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
import com.angel.ibanez.dehabitos.models.EstadisticasNino
import com.angel.ibanez.dehabitos.models.Nino
import com.angel.ibanez.dehabitos.models.ResumenSemanal
import com.angel.ibanez.dehabitos.repository.TutorRepository
import com.angel.ibanez.dehabitos.utils.DispositivoConfig
import com.angel.ibanez.dehabitos.utils.EstadisticasUtils
import com.angel.ibanez.dehabitos.utils.Resultado
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.angel.ibanez.dehabitos.viewmodels.EstadisticasViewModel
import com.angel.ibanez.dehabitos.viewmodels.NinoViewModel
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

class EstadisticasNinoFragment : Fragment() {
    private var _binding: FragmentEstadisticasNinoBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EstadisticasViewModel by viewModels()
    private val ninoViewModel: NinoViewModel by viewModels()
    private var ninos = emptyList<Nino>()
    private var seleccionado = ""
    private var sieteDias = true
    private var ultimas: EstadisticasNino? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentEstadisticasNinoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        seleccionado = arguments?.getString("ninoId").orEmpty()
        binding.grupoPeriodo.check(R.id.btnSieteDias)
        binding.btnExportarCsv.setOnClickListener { exportarCsv() }
        binding.btnEliminarNino.setOnClickListener { confirmarEliminar() }
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
        ninoViewModel.eliminarResultado.observe(viewLifecycleOwner) { resultado ->
            when (resultado) {
                is Resultado.Exito -> {
                    if (DispositivoConfig.obtenerNinoId() == seleccionado) DispositivoConfig.limpiarNino()
                    Toast.makeText(requireContext(), "Datos eliminados", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                is Resultado.Error -> Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_LONG).show()
                else -> Unit
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

    private fun mostrar(e: EstadisticasNino) {
        ultimas = e
        val categorias = e.tareasPorCategoria.entries.joinToString { "${it.key}: ${it.value}" }.ifBlank { "Sin tareas" }
        val satisfaccion = if (e.encuestas == 0) "sin encuestas" else String.format(Locale.getDefault(), "%.1f / 5 (%d encuestas)", e.satisfaccionPromedio, e.encuestas)
        binding.tvResumen.text = "Nivel actual: ${e.nino.nivel}\nNiveles avanzados: ${e.nivelesAvanzados}\nTareas validadas: ${e.tareasValidadas}\nPor categoría: $categorias\nCanjes totales: ${e.canjesTotales}\nEstrellas gastadas: ${e.estrellasGastadas}\nTiempo usado: ${formatoDuracion(e.tiempoUsadoMs)}\nSatisfacción parental: $satisfaccion"
        binding.tvSemanal.text = textoSemanal(e.semanas)
        binding.tvCanjes.text = "Canjes por aplicación\n" + e.canjesPorApp.joinToString("\n") { "${it.packageName}: ${it.cantidad} canjes, ${it.minutos} min" }.ifBlank { "Sin canjes" }
        binding.tvSesiones.text = "Sesiones recientes\n" + e.sesionesRecientes.joinToString("\n") { "${it.nombre}: ${formatoDuracion(it.duracionMs)} · ${DateFormat.getDateTimeInstance().format(Date(it.inicio))}" }.ifBlank { "Sin sesiones" }
    }

    private fun textoSemanal(semanas: List<ResumenSemanal>): String {
        if (semanas.isEmpty()) return "Resumen semanal\nSin datos"
        val formato = SimpleDateFormat("dd/MM", Locale.getDefault())
        val filas = semanas.joinToString("\n") {
            "${formato.format(Date(it.inicio))}–${formato.format(Date(it.fin - 1))}: ${it.tareasValidadas} tareas · ${formatoDuracion(it.tiempoUsadoMs)} · ${it.canjes} canjes"
        }
        val actual = semanas.last()
        val anterior = semanas.getOrNull(semanas.size - 2)
        val variacionTareas = anterior?.let { EstadisticasUtils.variacionPorcentual(actual.tareasValidadas.toLong(), it.tareasValidadas.toLong()) }
        val variacionTiempo = anterior?.let { EstadisticasUtils.variacionPorcentual(actual.tiempoUsadoMs, it.tiempoUsadoMs) }
        return "Resumen semanal (últimas ${semanas.size} semanas)\n$filas\n\nEsta semana vs. anterior\nTareas: ${formatoVariacion(variacionTareas)}\nTiempo de pantalla: ${formatoVariacion(variacionTiempo)}"
    }

    private fun formatoVariacion(valor: Int?): String = when {
        valor == null -> "sin datos previos"
        valor >= 0 -> "+$valor %"
        else -> "−${-valor} %"
    }

    private fun exportarCsv() {
        val e = ultimas ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "Estadísticas semanales de ${e.nino.nombre}")
            putExtra(Intent.EXTRA_TEXT, EstadisticasUtils.generarCsv(e.nino.nombre, e.semanas))
        }
        startActivity(Intent.createChooser(intent, "Exportar estadísticas"))
    }

    private fun confirmarEliminar() {
        val nino = ninos.firstOrNull { it.id == seleccionado } ?: return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("¿Eliminar a ${nino.nombre}?")
            .setMessage("Se borrará su perfil, misiones, canjes, historial de uso y dispositivos vinculados. Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ -> ninoViewModel.eliminarNino(nino.id) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun formatoDuracion(ms: Long) = "${ms / 3_600_000} h ${(ms / 60_000) % 60} min"
    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
