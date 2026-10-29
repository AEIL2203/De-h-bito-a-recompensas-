package com.angel.ibanez.dehabitos.fragments

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.angel.ibanez.dehabitos.utils.TecladoUtils
import com.angel.ibanez.dehabitos.R
import com.angel.ibanez.dehabitos.adapters.NinoAdapter
import com.angel.ibanez.dehabitos.databinding.DialogConfigurarPinParentalBinding
import com.angel.ibanez.dehabitos.databinding.FragmentSeleccionarNinoBinding
import com.angel.ibanez.dehabitos.models.Dispositivo
import com.angel.ibanez.dehabitos.models.Nino
import com.angel.ibanez.dehabitos.repository.DispositivoRepository
import com.angel.ibanez.dehabitos.utils.DispositivoConfig
import com.angel.ibanez.dehabitos.utils.Resultado
import com.angel.ibanez.dehabitos.viewmodels.NinoViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SeleccionarNinoFragment : Fragment() {

    private var _binding: FragmentSeleccionarNinoBinding? = null
    private val binding get() = _binding!!
    private val ninoViewModel: NinoViewModel by viewModels()
    private val adapter = NinoAdapter { nino -> prepararVinculacion(nino) }
    private val dispositivoRepository = DispositivoRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSeleccionarNinoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvNinos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNinos.setHasFixedSize(true)
        binding.rvNinos.adapter = adapter

        ninoViewModel.listaNinosResultado.observe(viewLifecycleOwner) { resultado ->
            when (resultado) {
                is Resultado.Cargando -> binding.progressBar.visibility = View.VISIBLE
                is Resultado.Exito -> {
                    binding.progressBar.visibility = View.GONE
                    adapter.submitList(resultado.data)
                    binding.tvMensaje.visibility = if (resultado.data.isEmpty()) View.VISIBLE else View.GONE
                }
                is Resultado.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_LONG).show()
                }
            }
        }

        ninoViewModel.cargarNinosPorTutor(DispositivoConfig.obtenerTutorId())
    }

    private fun prepararVinculacion(nino: Nino) {
        if (DispositivoConfig.tienePinParental()) {
            vincularNino(nino)
            return
        }
        val dialogBinding = DialogConfigurarPinParentalBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton("Guardar PIN", null)
            .setNegativeButton("Cancelar", null)
            .create()
        dialog.setOnShowListener {
            TecladoUtils.configurar(dialogBinding.etPin, dialogBinding.etConfirmarPin) { dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).performClick() }
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val pin = dialogBinding.etPin.text.toString()
                val confirmacion = dialogBinding.etConfirmarPin.text.toString()
                when {
                    !pin.matches(Regex("\\d{6}")) -> dialogBinding.tilPin.error = "Debe tener exactamente 6 dígitos"
                    pin != confirmacion -> dialogBinding.tilConfirmarPin.error = "Los PIN no coinciden"
                    else -> {
                        DispositivoConfig.guardarPinParental(pin)
                        dialog.dismiss()
                        vincularNino(nino)
                    }
                }
            }
        }
        dialog.show()
    }

    private fun vincularNino(nino: Nino) {
        val deviceId = DispositivoConfig.obtenerDeviceId()
        val tutorId = DispositivoConfig.obtenerTutorId()
        val fecha = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val dispositivo = Dispositivo(
            id = deviceId,
            tutorId = tutorId,
            ninoId = nino.id,
            nombreNino = nino.nombre,
            nombreDispositivo = listOf(Build.MANUFACTURER, Build.MODEL)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .trim(),
            desbloqueado = false,
            fechaVinculacion = fecha
        )

        DispositivoConfig.guardarNino(nino.id, nino.nombre)

        viewLifecycleOwner.lifecycleScope.launch {
            dispositivoRepository.vincularDispositivo(dispositivo).collect { resultado ->
                when (resultado) {
                    is Resultado.Cargando -> binding.progressBar.visibility = View.VISIBLE
                    is Resultado.Exito -> {
                        binding.progressBar.visibility = View.GONE
                        val bundle = Bundle().apply { putString("ninoId", nino.id) }
                        findNavController().navigate(
                            R.id.action_seleccionarNinoFragment_to_loginNinoFragment,
                            bundle
                        )
                    }
                    is Resultado.Error -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
