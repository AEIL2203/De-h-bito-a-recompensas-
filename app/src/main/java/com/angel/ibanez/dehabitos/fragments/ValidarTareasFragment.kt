package com.angel.ibanez.dehabitos.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.angel.ibanez.dehabitos.adapters.TareaValidarAdapter
import com.angel.ibanez.dehabitos.databinding.FragmentValidarTareasBinding
import com.angel.ibanez.dehabitos.models.Tarea
import com.angel.ibanez.dehabitos.models.Validacion
import com.angel.ibanez.dehabitos.utils.AnimacionesUtils
import com.angel.ibanez.dehabitos.utils.DispositivoConfig
import com.angel.ibanez.dehabitos.utils.Resultado
import com.angel.ibanez.dehabitos.viewmodels.NinoViewModel
import com.angel.ibanez.dehabitos.viewmodels.TareaViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ValidarTareasFragment : Fragment() {

    private var _binding: FragmentValidarTareasBinding? = null
    private val binding get() = _binding!!
    private val tareaViewModel: TareaViewModel by viewModels()
    private val ninoViewModel: NinoViewModel by viewModels()

    private var nombresNinos: Map<String, String> = emptyMap()
    private val validacionesPendientes = mutableListOf<Validacion>()
    private var jobCelebracion: Job? = null
    private val adapter = TareaValidarAdapter(
        nombresNinos = { nombresNinos },
        onValidar = { tarea -> tareaViewModel.validarTarea(tarea) },
        onRechazar = { tarea -> tareaViewModel.rechazarTarea(tarea) }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentValidarTareasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar el adaptador de inmediato para evitar advertencias de "No adapter attached"
        binding.rvTareas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTareas.adapter = adapter
        AnimacionesUtils.entradaSuave(binding.rvTareas)

        ninoViewModel.listaNinosResultado.observe(viewLifecycleOwner) { resultado ->
            if (resultado is Resultado.Exito) {
                nombresNinos = resultado.data.associate { it.id to it.nombre }
                adapter.notifyDataSetChanged()
            }
        }

        tareaViewModel.listaTareasResultado.observe(viewLifecycleOwner) { resultado ->
            when (resultado) {
                is Resultado.Cargando -> binding.progressBar.visibility = View.VISIBLE
                is Resultado.Exito -> {
                    binding.progressBar.visibility = View.GONE
                    val porValidar = resultado.data.filter { it.estado == Tarea.ESTADO_COMPLETADA }
                    adapter.submitList(porValidar)
                    binding.tvVacio.visibility = if (porValidar.isEmpty()) View.VISIBLE else View.GONE
                }
                is Resultado.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_LONG).show()
                }
            }
        }

        tareaViewModel.validarResultado.observe(viewLifecycleOwner) { resultado ->
            when (resultado) {
                is Resultado.Exito -> {
                    binding.progressBar.visibility = View.GONE
                    validacionesPendientes.add(resultado.data)
                    tareaViewModel.consumirValidar()
                    jobCelebracion?.cancel()
                    jobCelebracion = viewLifecycleOwner.lifecycleScope.launch {
                        delay(1200)
                        mostrarCelebracionValidaciones()
                    }
                }
                is Resultado.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_LONG).show()
                    tareaViewModel.consumirValidar()
                }
                else -> {}
            }
        }

        tareaViewModel.rechazarResultado.observe(viewLifecycleOwner) { resultado ->
            when (resultado) {
                is Resultado.Exito -> {
                    Toast.makeText(requireContext(), "Misión devuelta a pendiente", Toast.LENGTH_SHORT).show()
                    tareaViewModel.consumirRechazar()
                }
                is Resultado.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_LONG).show()
                    tareaViewModel.consumirRechazar()
                }
                else -> {}
            }
        }

        tareaViewModel.escucharTareasPorTutor(DispositivoConfig.obtenerTutorId())
        ninoViewModel.cargarNinosPorTutor(DispositivoConfig.obtenerTutorId())
    }

    /** Muestra una única celebración para todas las validaciones acumuladas en el lote. */
    private fun mostrarCelebracionValidaciones() {
        val vista = _binding?.root ?: return
        if (validacionesPendientes.isEmpty()) return
        val ultima = validacionesPendientes.last()
        if (validacionesPendientes.size == 1) {
            AnimacionesUtils.mostrarCelebracion(
                vista,
                AnimacionesUtils.GIF_VALIDACION,
                "Misión validada",
                "\"${ultima.nombreTarea}\" fue aprobada: ${ultima.nino.nombre} ahora tiene ${ultima.nino.experiencia} XP y está en el nivel ${ultima.nino.nivel}."
            )
        } else {
            AnimacionesUtils.mostrarCelebracion(
                vista,
                AnimacionesUtils.GIF_VALIDACION,
                "Se validaron todas tus misiones",
                "${validacionesPendientes.size} misiones aprobadas para ${ultima.nino.nombre}."
            )
        }
        validacionesPendientes.clear()
    }

    override fun onDestroyView() {
        jobCelebracion?.cancel()
        jobCelebracion = null
        validacionesPendientes.clear()
        super.onDestroyView()
        _binding = null
    }
}
