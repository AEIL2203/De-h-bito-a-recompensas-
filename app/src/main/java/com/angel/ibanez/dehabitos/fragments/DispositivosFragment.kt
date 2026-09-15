package com.angel.ibanez.dehabitos.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.angel.ibanez.dehabitos.adapters.DispositivoAdapter
import com.angel.ibanez.dehabitos.databinding.FragmentDispositivosBinding
import com.angel.ibanez.dehabitos.utils.DispositivoConfig
import com.angel.ibanez.dehabitos.utils.Resultado
import com.angel.ibanez.dehabitos.viewmodels.DispositivoViewModel

class DispositivosFragment : Fragment() {

    private var _binding: FragmentDispositivosBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DispositivoViewModel by viewModels()
    private var dispositivoDesvinculandoId: String? = null
    private val adapter = DispositivoAdapter(
        onToggle = { dispositivo, activar ->
            viewModel.cambiarEstadoDesbloqueo(dispositivo.id, activar)
        },
        onDesvincular = { dispositivo ->
            AlertDialog.Builder(requireContext())
                .setTitle("Desvincular dispositivo")
                .setMessage("¿Desvincular el dispositivo de ${dispositivo.nombreNino}? El teléfono dejará de estar asociado y podrá configurarse para otro niño.")
                .setPositiveButton("Desvincular") { _, _ ->
                    dispositivoDesvinculandoId = dispositivo.id
                    viewModel.desvincularDispositivo(dispositivo.id)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDispositivosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvDispositivos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDispositivos.adapter = adapter

        viewModel.dispositivosResultado.observe(viewLifecycleOwner) { resultado ->
            when (resultado) {
                is Resultado.Cargando -> binding.progressBar.visibility = View.VISIBLE
                is Resultado.Exito -> {
                    binding.progressBar.visibility = View.GONE
                    adapter.submitList(resultado.data)
                }
                is Resultado.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_LONG).show()
                }
            }
        }

        viewModel.cambioEstadoResultado.observe(viewLifecycleOwner) { resultado ->
            when (resultado) {
                is Resultado.Cargando -> binding.progressBar.visibility = View.VISIBLE
                is Resultado.Exito -> binding.progressBar.visibility = View.GONE
                is Resultado.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_LONG).show()
                    viewModel.cargarDispositivos(DispositivoConfig.obtenerTutorId())
                }
            }
        }

        viewModel.desvincularResultado.observe(viewLifecycleOwner) { resultado ->
            when (resultado) {
                is Resultado.Cargando -> binding.progressBar.visibility = View.VISIBLE
                is Resultado.Exito -> {
                    binding.progressBar.visibility = View.GONE
                    val eliminadoId = dispositivoDesvinculandoId
                    adapter.submitList(adapter.currentList.filterNot { it.id == eliminadoId })
                    dispositivoDesvinculandoId = null
                    Toast.makeText(requireContext(), "Dispositivo desvinculado", Toast.LENGTH_SHORT).show()
                    viewModel.cargarDispositivos(DispositivoConfig.obtenerTutorId())
                }
                is Resultado.Error -> {
                    binding.progressBar.visibility = View.GONE
                    dispositivoDesvinculandoId = null
                    Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_LONG).show()
                }
            }
        }

        viewModel.cargarDispositivos(DispositivoConfig.obtenerTutorId())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
