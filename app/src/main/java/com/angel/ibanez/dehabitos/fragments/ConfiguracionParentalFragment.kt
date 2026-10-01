package com.angel.ibanez.dehabitos.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.angel.ibanez.dehabitos.R
import com.angel.ibanez.dehabitos.databinding.FragmentConfiguracionParentalBinding
import com.angel.ibanez.dehabitos.utils.AnimacionesUtils
import com.angel.ibanez.dehabitos.utils.AppsUtils
import com.angel.ibanez.dehabitos.utils.DispositivoConfig
import com.angel.ibanez.dehabitos.utils.PermisosUtils
import com.angel.ibanez.dehabitos.utils.Resultado
import com.angel.ibanez.dehabitos.viewmodels.DispositivoViewModel
import com.angel.ibanez.dehabitos.viewmodels.RecompensaViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ConfiguracionParentalFragment : Fragment() {
    private var _binding: FragmentConfiguracionParentalBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RecompensaViewModel by viewModels()
    private val dispositivoViewModel: DispositivoViewModel by viewModels()
    private var catalogoHashPendiente: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentConfiguracionParentalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AnimacionesUtils.entradaSuave(binding.root)
        binding.btnUso.setOnClickListener { PermisosUtils.abrirConfiguracionUso(requireContext()) }
        binding.btnAccesibilidad.setOnClickListener {
            if (!PermisosUtils.abrirConfiguracionAccesibilidad(requireContext())) {
                Toast.makeText(requireContext(), "No fue posible abrir Accesibilidad", Toast.LENGTH_LONG).show()
            }
        }
        binding.btnNotificaciones.setOnClickListener { PermisosUtils.abrirConfiguracionNotificaciones(requireContext()) }
        binding.btnActualizarApps.setOnClickListener { actualizarCatalogo() }
        binding.btnModoPadre.setOnClickListener { entrarModoPadre() }
        viewModel.sincronizarCatalogoResultado.observe(viewLifecycleOwner) { resultado ->
            binding.btnActualizarApps.isEnabled = resultado !is Resultado.Cargando
            when (resultado) {
                is Resultado.Exito -> {
                    catalogoHashPendiente?.let(DispositivoConfig::registrarCatalogoSincronizado)
                    catalogoHashPendiente = null
                    Toast.makeText(requireContext(), "Lista de apps actualizada", Toast.LENGTH_SHORT).show()
                }
                is Resultado.Error -> Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_LONG).show()
                else -> Unit
            }
        }
        actualizarEstados()
    }

    override fun onResume() {
        super.onResume()
        actualizarEstados()
    }

    private fun actualizarCatalogo() {
        val ninoId = arguments?.getString("ninoId").orEmpty().ifBlank { DispositivoConfig.obtenerNinoId() }
        val deviceId = DispositivoConfig.obtenerDeviceId()
        if (ninoId.isBlank() || deviceId.isBlank()) return
        val (apps, hash) = AppsUtils.crearCatalogo(requireContext(), ninoId, deviceId)
        catalogoHashPendiente = hash
        viewModel.sincronizarCatalogoApps(ninoId, deviceId, apps)
    }

    private fun actualizarEstados() {
        val uso = PermisosUtils.tieneAccesoUso(requireContext())
        binding.tvEstadoUso.text = if (uso) "Acceso de uso: activado" else "Acceso de uso: pendiente"
        binding.tvEstadoUso.setTextColor(requireContext().getColor(if (uso) R.color.md_theme_secondary else R.color.md_theme_error))
        val accesibilidad = PermisosUtils.servicioAccesibilidadActivo(requireContext())
        binding.tvEstadoAccesibilidad.text = if (accesibilidad) "Accesibilidad: activada" else "Accesibilidad: pendiente"
        binding.tvEstadoAccesibilidad.setTextColor(requireContext().getColor(if (accesibilidad) R.color.md_theme_secondary else R.color.md_theme_error))
        val notificaciones = PermisosUtils.notificacionesActivas(requireContext())
        binding.tvEstadoNotificaciones.text = if (notificaciones) "Notificaciones: activadas" else "Notificaciones: desactivadas"
        binding.tvEstadoNotificaciones.setTextColor(requireContext().getColor(if (notificaciones) R.color.md_theme_secondary else R.color.md_theme_error))
    }

    private fun entrarModoPadre() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Entrar al modo padre")
            .setMessage("¿Seguro que quieres entrar al modo padre?")
            .setPositiveButton("Entrar") { _, _ ->
                val deviceId = DispositivoConfig.obtenerDeviceId()
                if (deviceId.isNotBlank()) {
                    dispositivoViewModel.cambiarEstadoDesbloqueo(deviceId, true)
                }
                DispositivoConfig.guardarModo(DispositivoConfig.ModoDispositivo.PADRE)
                val contenedor = parentFragment?.parentFragment
                if (contenedor?.findNavController()?.currentDestination?.id == R.id.contenedorNinoFragment) {
                    contenedor.findNavController().navigate(R.id.action_contenedorNinoFragment_to_dashboardTutorFragment)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
