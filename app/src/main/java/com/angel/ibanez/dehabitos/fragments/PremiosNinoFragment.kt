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
import com.angel.ibanez.dehabitos.adapters.AppRecompensaAdapter
import com.angel.ibanez.dehabitos.databinding.FragmentPremiosNinoBinding
import com.angel.ibanez.dehabitos.models.AplicacionPermitida
import com.angel.ibanez.dehabitos.models.Nino
import com.angel.ibanez.dehabitos.models.Recompensa
import com.angel.ibanez.dehabitos.utils.AnimacionesUtils
import com.angel.ibanez.dehabitos.utils.DispositivoConfig
import com.angel.ibanez.dehabitos.utils.Resultado
import com.angel.ibanez.dehabitos.viewmodels.NinoViewModel
import com.angel.ibanez.dehabitos.viewmodels.RecompensaViewModel

class PremiosNinoFragment : Fragment() {

    private var _binding: FragmentPremiosNinoBinding? = null
    private val binding get() = _binding!!
    private val recompensaViewModel: RecompensaViewModel by viewModels()
    private val ninoViewModel: NinoViewModel by viewModels()
    private var nino: Nino? = null
    private var ninoId: String = ""
    private lateinit var adapter: AppRecompensaAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPremiosNinoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ninoId = arguments?.getString("ninoId").orEmpty().ifBlank { DispositivoConfig.obtenerNinoId() }
        AnimacionesUtils.cargarGifPausado(binding.ivMonedas, AnimacionesUtils.GIF_MONEDAS)
        AnimacionesUtils.animarPeriodicamente(binding.ivMonedas, 8000L)

        adapter = AppRecompensaAdapter(estrellas = 0) { app, minutos, precio ->
            confirmarCanje(app, minutos, precio)
        }
        binding.rvApps.layoutManager = LinearLayoutManager(requireContext())
        binding.rvApps.adapter = adapter
        AnimacionesUtils.entradaSuave(binding.rvApps)

        ninoViewModel.ninoResultado.observe(viewLifecycleOwner) { resultado ->
            when (resultado) {
                is Resultado.Cargando -> binding.progressBar.visibility = View.VISIBLE
                is Resultado.Exito -> {
                    binding.progressBar.visibility = View.GONE
                    nino = resultado.data
                    mostrarEstrellas(resultado.data.estrellas)
                    adapter = AppRecompensaAdapter(estrellas = resultado.data.estrellas) { app, minutos, precio ->
                        confirmarCanje(app, minutos, precio)
                    }
                    binding.rvApps.adapter = adapter
                    adapter.submitList(recompensaViewModel.appsResultado.value?.let { if (it is Resultado.Exito) it.data else null } ?: emptyList())
                }
                is Resultado.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_LONG).show()
                }
            }
        }

        recompensaViewModel.appsResultado.observe(viewLifecycleOwner) { resultado ->
            when (resultado) {
                is Resultado.Cargando -> binding.progressBar.visibility = View.VISIBLE
                is Resultado.Exito -> {
                    binding.progressBar.visibility = View.GONE
                    adapter.submitList(resultado.data)
                    binding.tvSinApps.visibility = if (resultado.data.isEmpty()) View.VISIBLE else View.GONE
                }
                is Resultado.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_LONG).show()
                }
            }
        }

        recompensaViewModel.canjeResultado.observe(viewLifecycleOwner) { resultado ->
            when (resultado) {
                is Resultado.Cargando -> binding.progressBar.visibility = View.VISIBLE
                is Resultado.Exito -> {
                    binding.progressBar.visibility = View.GONE
                    val duracion = "${resultado.data.minutosOtorgados} minutos"
                    AnimacionesUtils.mostrarCelebracion(
                        binding.root,
                        AnimacionesUtils.GIF_PREMIO,
                        "Premio desbloqueado",
                        "Tienes $duracion para disfrutar."
                    )
                    AnimacionesUtils.pulso(binding.tvEstrellas)
                    AnimacionesUtils.reproducirGifUnaVez(binding.ivMonedas)
                    ninoViewModel.cargarNinoPorId(ninoId)
                    recompensaViewModel.consumirCanje()
                }
                is Resultado.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_LONG).show()
                    recompensaViewModel.consumirCanje()
                }
                else -> {}
            }
        }

        ninoViewModel.escucharNino(ninoId)
        recompensaViewModel.escucharAppsPermitidas(ninoId)
    }

    private fun mostrarEstrellas(estrellas: Int) {
        binding.tvEstrellas.text = estrellas.toString()
    }

    private fun confirmarCanje(app: AplicacionPermitida, minutos: Int, precio: Int) {
        val ninoActual = nino ?: return
        val duracion = "$minutos minutos"
        AlertDialog.Builder(requireContext())
            .setTitle("Canjear $duracion")
            .setMessage("¿Gastar ${precio} estrellas en ${app.nombre}?")
            .setPositiveButton("Sí") { _, _ ->
                recompensaViewModel.canjear(ninoActual, app, minutos, precio, System.currentTimeMillis())
            }
            .setNegativeButton("No", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recompensaViewModel.detenerEscucha()
        _binding = null
    }
}
