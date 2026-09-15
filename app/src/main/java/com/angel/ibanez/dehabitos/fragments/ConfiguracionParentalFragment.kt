package com.angel.ibanez.dehabitos.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.NavHostFragment
import com.angel.ibanez.dehabitos.R
import com.angel.ibanez.dehabitos.databinding.FragmentConfiguracionParentalBinding
import com.angel.ibanez.dehabitos.utils.AnimacionesUtils
import com.angel.ibanez.dehabitos.utils.AppsUtils
import com.angel.ibanez.dehabitos.utils.DispositivoConfig
import com.angel.ibanez.dehabitos.utils.PermisosUtils
import com.angel.ibanez.dehabitos.utils.Resultado
import com.angel.ibanez.dehabitos.viewmodels.RecompensaViewModel

class ConfiguracionParentalFragment : Fragment() {
    private var _binding: FragmentConfiguracionParentalBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RecompensaViewModel by viewModels()
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
        binding.tvEstadoUso.setTextColor(requireContext().getColor(if (uso) android.R.color.holo_green_dark else android.R.color.holo_red_dark))
        val accesibilidad = PermisosUtils.servicioAccesibilidadActivo(requireContext())
        binding.tvEstadoAccesibilidad.text = if (accesibilidad) "Accesibilidad: activada" else "Accesibilidad: pendiente"
        binding.tvEstadoAccesibilidad.setTextColor(requireContext().getColor(if (accesibilidad) android.R.color.holo_green_dark else android.R.color.holo_red_dark))
        val notificaciones = PermisosUtils.notificacionesActivas(requireContext())
        binding.tvEstadoNotificaciones.text = if (notificaciones) "Notificaciones: activadas" else "Notificaciones: desactivadas"
        binding.tvEstadoNotificaciones.setTextColor(requireContext().getColor(if (notificaciones) android.R.color.holo_green_dark else android.R.color.holo_red_dark))
    }

    private fun entrarModoPadre() {
        DispositivoConfig.guardarModo(DispositivoConfig.ModoDispositivo.PADRE)
        parentFragment?.parentFragment?.let {
            if (it is NavHostFragment) {
                it.navController.navigate(R.id.action_contenedorNinoFragment_to_dashboardTutorFragment)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
