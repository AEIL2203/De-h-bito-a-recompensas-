package com.angel.ibanez.dehabitos.fragments

import android.content.Intent
import android.os.Bundle
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.res.ColorStateList
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.angel.ibanez.dehabitos.R
import com.angel.ibanez.dehabitos.databinding.FragmentEstadisticasNinoBinding
import com.angel.ibanez.dehabitos.databinding.ItemBarraCategoriaBinding
import com.angel.ibanez.dehabitos.models.EstadisticasNino
import com.angel.ibanez.dehabitos.models.Nino
import com.angel.ibanez.dehabitos.models.ResumenSemanal
import com.angel.ibanez.dehabitos.repository.TutorRepository
import com.angel.ibanez.dehabitos.utils.AppsUtils
import com.angel.ibanez.dehabitos.utils.AvataresUtils
import com.angel.ibanez.dehabitos.utils.DispositivoConfig
import com.angel.ibanez.dehabitos.utils.EstadisticasUtils
import com.angel.ibanez.dehabitos.utils.Resultado
import androidx.navigation.fragment.findNavController
import com.google.android.material.color.MaterialColors
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
        val satisfaccion = if (e.encuestas == 0) "sin encuestas" else String.format(Locale.getDefault(), "%.1f / 5 (%d encuestas)", e.satisfaccionPromedio, e.encuestas)
        binding.tvNombreNino.text = e.nino.nombre
        AvataresUtils.cargar(binding.ivAvatar, e.nino.avatar)
        binding.tvKpiNivel.text = "Nivel\n${e.nino.nivel}"
        binding.tvKpiTareas.text = "Tareas\n${e.tareasValidadas}"
        binding.tvKpiTiempo.text = "Tiempo\n${formatoDuracionCorta(e.tiempoUsadoMs)}"
        binding.tvKpiCanjes.text = "Canjes\n${e.canjesTotales}"
        mostrarDashboardCategorias(e.tareasPorCategoria)
        binding.tvResumenExtra.text = "Estrellas gastadas: ${e.estrellasGastadas}  ·  Satisfacción: $satisfaccion"
        mostrarSemanas(e.semanas)
        binding.tvCanjes.text = e.canjesPorApp.joinToString("\n\n") {
            val nombreApp = AppsUtils.obtenerNombreApp(requireContext(), it.packageName)
            "$nombreApp\n${it.cantidad} canjes  ·  ${it.minutos} minutos"
        }.ifBlank { "Todavía no hay canjes\nLos canjes por tiempo aparecerán aquí." }
        binding.tvSesiones.text = e.sesionesRecientes.joinToString("\n\n") {
            val nombreApp = AppsUtils.obtenerNombreApp(requireContext(), it.packageName, it.nombre)
            "$nombreApp\n${formatoDuracion(it.duracionMs)}  ·  ${DateFormat.getDateTimeInstance().format(Date(it.inicio))}"
        }.ifBlank { "Todavía no hay sesiones\nEl uso reciente de aplicaciones aparecerá aquí." }
    }

    private fun mostrarDashboardCategorias(categorias: Map<String, Int>) {
        binding.containerCategorias.removeAllViews()
        if (categorias.isEmpty()) {
            val vacio = TextView(requireContext()).apply {
                text = "Sin tareas validadas"
                setTextColor(MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOnSurfaceVariant, 0))
                textSize = 14f
                setPadding(0, dp(12), 0, dp(12))
            }
            binding.containerCategorias.addView(vacio)
            return
        }
        val total = categorias.values.sum().coerceAtLeast(1)
        val orden = listOf("academica", "domestica", "personal")
        val colores = mapOf(
            "academica" to R.color.md_theme_primary,
            "domestica" to R.color.game_green_dark,
            "personal" to R.color.md_theme_tertiary
        )
        val nombres = mapOf(
            "academica" to "Académica",
            "domestica" to "Doméstica",
            "personal" to "Personal"
        )
        val restantes = categorias.keys - orden.toSet()
        (orden.filter { it in categorias } + restantes).forEach { clave ->
            val valor = categorias[clave] ?: 0
            val porcentaje = valor * 100 / total
            val item = ItemBarraCategoriaBinding.inflate(layoutInflater, binding.containerCategorias, false)
            item.tvCategoria.text = nombres[clave] ?: clave.replaceFirstChar { it.uppercase(Locale.getDefault()) }
            item.tvValor.text = valor.toString()
            item.pbProgreso.progress = porcentaje
            val colorRes = colores[clave] ?: R.color.md_theme_primary
            item.pbProgreso.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), colorRes))
            binding.containerCategorias.addView(item.root)
        }
    }

    private fun mostrarSemanas(semanas: List<ResumenSemanal>) {
        binding.containerSemanas.removeAllViews()
        binding.tvCantidadSemanas.text = if (semanas.isEmpty()) "Sin actividad registrada" else "Últimas ${semanas.size} semanas"
        binding.tvSinSemanas.visibility = if (semanas.isEmpty()) View.VISIBLE else View.GONE
        val formato = SimpleDateFormat("dd MMM", Locale.getDefault())
        semanas.forEachIndexed { indice, semana ->
            val fila = TextView(requireContext()).apply {
                text = "${formato.format(Date(semana.inicio))} – ${formato.format(Date(semana.fin - 1))}\n${semana.tareasValidadas} tareas   ·   ${formatoDuracion(semana.tiempoUsadoMs)}   ·   ${semana.canjes} canjes"
                setTextColor(MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOnSurface, 0))
                textSize = 14f
                setTypeface(typeface, Typeface.BOLD)
                setPadding(dp(14), dp(12), dp(14), dp(12))
                background = GradientDrawable().apply {
                    cornerRadius = dp(16).toFloat()
                    setColor(MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorSurfaceVariant, 0))
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    if (indice > 0) topMargin = dp(8)
                }
            }
            binding.containerSemanas.addView(fila)
        }
        val actual = semanas.lastOrNull()
        val anterior = semanas.getOrNull(semanas.size - 2)
        val variacionTareas = if (actual != null && anterior != null) EstadisticasUtils.variacionPorcentual(actual.tareasValidadas.toLong(), anterior.tareasValidadas.toLong()) else null
        val variacionTiempo = if (actual != null && anterior != null) EstadisticasUtils.variacionPorcentual(actual.tiempoUsadoMs, anterior.tiempoUsadoMs) else null
        pintarComparacion(binding.tvComparacionTareas, "Tareas", variacionTareas)
        pintarComparacion(binding.tvComparacionTiempo, "Pantalla", variacionTiempo)
    }

    private fun pintarComparacion(vista: TextView, titulo: String, valor: Int?) {
        vista.text = "$titulo\n${formatoVariacion(valor)}"
        val color = when {
            valor == null -> MaterialColors.getColor(vista, com.google.android.material.R.attr.colorOnSurfaceVariant, 0)
            valor >= 0 -> requireContext().getColor(R.color.game_green_dark)
            else -> requireContext().getColor(R.color.game_coral)
        }
        vista.setTextColor(color)
    }

    private fun dp(valor: Int): Int = (valor * resources.displayMetrics.density).toInt()

    private fun formatoVariacion(valor: Int?): String = when {
        valor == null -> "sin datos previos"
        valor >= 0 -> "▲ $valor %"
        else -> "▼ ${-valor} %"
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
    private fun formatoDuracionCorta(ms: Long) = if (ms < 3_600_000) "${ms / 60_000}m" else "${ms / 3_600_000}h"
    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
