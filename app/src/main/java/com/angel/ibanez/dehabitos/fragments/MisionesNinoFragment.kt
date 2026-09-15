package com.angel.ibanez.dehabitos.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.angel.ibanez.dehabitos.R
import com.angel.ibanez.dehabitos.adapters.MisionAdapter
import com.angel.ibanez.dehabitos.databinding.FragmentMisionesNinoBinding
import com.angel.ibanez.dehabitos.utils.AnimacionesUtils
import com.angel.ibanez.dehabitos.utils.Resultado
import com.angel.ibanez.dehabitos.viewmodels.TareaViewModel

class MisionesNinoFragment : Fragment() {

    private var _binding: FragmentMisionesNinoBinding? = null
    private val binding get() = _binding!!

    private val tareaViewModel: TareaViewModel by viewModels()
    private var ninoId: String = ""
    private lateinit var misionAdapter: MisionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMisionesNinoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ninoId = arguments?.getString("ninoId") ?: ""
        
        misionAdapter = MisionAdapter { tarea -> tareaViewModel.marcarCompletada(tarea) }
        binding.rvMisiones.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMisiones.adapter = misionAdapter

        AnimacionesUtils.entradaSuave(binding.rvMisiones)
        observarMisiones()
        tareaViewModel.escucharTareasPorNino(ninoId)
    }

    private fun observarMisiones() {
        tareaViewModel.listaTareasResultado.observe(viewLifecycleOwner) { resultado ->
            when (resultado) {
                is Resultado.Exito -> {
                    val misiones = resultado.data.sortedBy { it.estado != "pendiente" }
                    misionAdapter.submitList(misiones)
                    binding.tvSinMisiones.visibility = if (misiones.isEmpty()) View.VISIBLE else View.GONE
                }
                is Resultado.Error -> {
                    Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }

        tareaViewModel.completarResultado.observe(viewLifecycleOwner) { resultado ->
            if (resultado is Resultado.Exito) {
                AnimacionesUtils.mostrarCelebracion(
                    binding.root,
                    AnimacionesUtils.GIF_TAREA_COMPLETADA,
                    "Misión completada",
                    "Tu tutor la revisará para entregarte la recompensa."
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
