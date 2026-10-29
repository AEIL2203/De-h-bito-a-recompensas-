package com.angel.ibanez.dehabitos.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.angel.ibanez.dehabitos.utils.TecladoUtils
import com.angel.ibanez.dehabitos.R
import com.angel.ibanez.dehabitos.databinding.FragmentAsignarTareaBinding
import com.angel.ibanez.dehabitos.models.Nino
import com.angel.ibanez.dehabitos.models.Tarea
import com.angel.ibanez.dehabitos.utils.DispositivoConfig
import com.angel.ibanez.dehabitos.utils.IconosUtils
import com.angel.ibanez.dehabitos.utils.Resultado
import com.angel.ibanez.dehabitos.viewmodels.NinoViewModel
import com.angel.ibanez.dehabitos.viewmodels.TareaViewModel
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AsignarTareaFragment : Fragment() {

    private var _binding: FragmentAsignarTareaBinding? = null
    private val binding get() = _binding!!
    private val ninoViewModel: NinoViewModel by viewModels()
    private val tareaViewModel: TareaViewModel by viewModels()

    private var ninos: List<Nino> = emptyList()
    private var ninoSeleccionado: Nino? = null

    private val categorias = listOf(
        Tarea.CATEGORIA_ACADEMICA to "Académica",
        Tarea.CATEGORIA_DOMESTICA to "Doméstica",
        Tarea.CATEGORIA_PERSONAL to "Personal"
    )
    private var categoriaSeleccionada = Tarea.CATEGORIA_ACADEMICA
    private var fechaLimite = ""
    private val formatoDia = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val formatoVisible = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAsignarTareaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnAsignar.icon = IconosUtils.crearIcono(requireContext(), "gmr-flash-on", R.color.md_theme_onPrimary)

        configurarCategorias()
        configurarFechaLimite()
        observarNinos()
        observarCreacion()

        binding.btnAsignar.setOnClickListener { asignarTarea() }

        ninoViewModel.cargarNinosPorTutor(DispositivoConfig.obtenerTutorId())
    }

    private fun configurarCategorias() {
        val botones = mapOf(
            R.id.btnCatAcademica to categorias[0].first,
            R.id.btnCatDomestica to categorias[1].first,
            R.id.btnCatPersonal to categorias[2].first
        )
        binding.toggleCategoria.addOnButtonCheckedListener { _, id, marcado ->
            if (marcado) botones[id]?.let { categoriaSeleccionada = it }
        }
        // Atajos de XP: rellenan el campo de puntos
        val atajos = mapOf(
            binding.chip5 to 5, binding.chip10 to 10, binding.chip20 to 20,
            binding.chip50 to 50, binding.chip100 to 100, binding.chip150 to 150
        )
        atajos.forEach { (chip, valor) ->
            chip.setOnClickListener {
                atajos.keys.forEach { it.isChecked = it == chip }
                binding.etPuntos.setText(valor.toString())
            }
        }
        binding.chip10.isChecked = true
        binding.etPuntos.setText("10")
        TecladoUtils.configurar(binding.etNombre, binding.etPuntos)
    }

    private fun configurarFechaLimite() {
        binding.etFechaLimite.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Fecha límite")
                .setCalendarConstraints(
                    com.google.android.material.datepicker.CalendarConstraints.Builder()
                        .setValidator(DateValidatorPointForward.now())
                        .build()
                )
                .build()
            picker.addOnPositiveButtonClickListener { seleccion ->
                fechaLimite = formatoDia.format(Date(seleccion))
                binding.etFechaLimite.setText(formatoVisible.format(Date(seleccion)))
            }
            picker.show(childFragmentManager, "fecha_limite")
        }
        binding.tilFechaLimite.setEndIconOnClickListener {
            fechaLimite = ""
            binding.etFechaLimite.setText("")
        }
    }

    private fun observarNinos() {
        ninoViewModel.listaNinosResultado.observe(viewLifecycleOwner) { resultado ->
            when (resultado) {
                is Resultado.Cargando -> mostrarCarga(true)
                is Resultado.Exito -> {
                    mostrarCarga(false)
                    ninos = resultado.data
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, ninos.map { it.nombre })
                    binding.actvNino.setAdapter(adapter)
                    binding.actvNino.setOnItemClickListener { _, _, position, _ ->
                        ninoSeleccionado = ninos[position]
                    }
                    if (ninos.size == 1 && ninoSeleccionado == null) {
                        ninoSeleccionado = ninos[0]
                        binding.actvNino.setText(ninos[0].nombre, false)
                    }
                }
                is Resultado.Error -> {
                    mostrarCarga(false)
                    Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun observarCreacion() {
        tareaViewModel.crearTareaResultado.observe(viewLifecycleOwner) { resultado ->
            when (resultado) {
                is Resultado.Cargando -> mostrarCarga(true)
                is Resultado.Exito -> {
                    mostrarCarga(false)
                    Toast.makeText(requireContext(), "Misión asignada", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                is Resultado.Error -> {
                    mostrarCarga(false)
                    Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun asignarTarea() {
        val nino = ninoSeleccionado
        val nombre = binding.etNombre.text.toString().trim()
        val puntos = binding.etPuntos.text.toString().toIntOrNull() ?: 0

        when {
            nino == null -> Toast.makeText(requireContext(), "Selecciona un niño", Toast.LENGTH_SHORT).show()
            nombre.isEmpty() -> Toast.makeText(requireContext(), "Escribe el nombre de la misión", Toast.LENGTH_SHORT).show()
            puntos <= 0 -> Toast.makeText(requireContext(), "Los puntos deben ser mayores a 0", Toast.LENGTH_SHORT).show()
            else -> {
                val tarea = Tarea(
                    ninoId = nino.id,
                    tutorId = DispositivoConfig.obtenerTutorId(),
                    nombre = nombre,
                    categoria = categoriaSeleccionada,
                    puntos = puntos,
                    estado = Tarea.ESTADO_PENDIENTE,
                    fechaLimite = fechaLimite
                )
                tareaViewModel.crearTarea(tarea)
            }
        }
    }

    private fun mostrarCarga(mostrar: Boolean) {
        binding.progressBar.visibility = if (mostrar) View.VISIBLE else View.GONE
        binding.btnAsignar.isEnabled = !mostrar
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
