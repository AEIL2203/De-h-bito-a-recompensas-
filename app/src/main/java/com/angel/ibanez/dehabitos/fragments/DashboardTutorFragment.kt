package com.angel.ibanez.dehabitos.fragments

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.angel.ibanez.dehabitos.R
import com.angel.ibanez.dehabitos.adapters.NinoAdapter
import com.angel.ibanez.dehabitos.databinding.FragmentDashboardTutorBinding
import com.angel.ibanez.dehabitos.utils.DispositivoConfig
import com.angel.ibanez.dehabitos.utils.IconosUtils
import com.angel.ibanez.dehabitos.repository.TutorRepository
import com.angel.ibanez.dehabitos.utils.Resultado
import com.angel.ibanez.dehabitos.viewmodels.DispositivoViewModel
import com.angel.ibanez.dehabitos.viewmodels.NinoViewModel
import kotlinx.coroutines.launch
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import com.angel.ibanez.dehabitos.repository.EstadisticasRepository

class DashboardTutorFragment : Fragment() {

    private var _binding: FragmentDashboardTutorBinding? = null
    private val binding get() = _binding!!
    private val ninoViewModel: NinoViewModel by viewModels()
    private val dispositivoViewModel: DispositivoViewModel by viewModels()
    private val ninoAdapter = NinoAdapter { nino ->
        findNavController().navigate(R.id.action_dashboardTutorFragment_to_estadisticasNinoFragment, Bundle().apply { putString("ninoId", nino.id) })
    }
    private val tutorRepository = TutorRepository()
    private val estadisticasRepository = EstadisticasRepository()
    private val permisoNotificaciones = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardTutorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar el adaptador de inmediato para evitar advertencias de "No adapter attached"
        binding.rvNinos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNinos.adapter = ninoAdapter

        binding.btnAgregarNino.icon = IconosUtils.crearIcono(requireContext(), "gmr-group-add", R.color.white)
        binding.btnGestionarDispositivos.icon = IconosUtils.crearIcono(requireContext(), "gmr-tablet-android", R.color.white)
        binding.btnCerrarSesion.icon = IconosUtils.crearIcono(requireContext(), "gmr-exit-to-app", R.color.white)

        binding.btnAgregarNino.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.dashboardTutorFragment) {
                findNavController().navigate(R.id.action_dashboardTutorFragment_to_agregarNinoFragment)
            }
        }

        binding.btnGestionarDispositivos.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.dashboardTutorFragment) {
                findNavController().navigate(R.id.action_dashboardTutorFragment_to_dispositivosFragment)
            }
        }

        binding.btnConfigurarApps.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.dashboardTutorFragment) {
                findNavController().navigate(R.id.action_dashboardTutorFragment_to_configurarAppsFragment)
            }
        }

        binding.btnAsignarTarea.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.dashboardTutorFragment) {
                findNavController().navigate(R.id.action_dashboardTutorFragment_to_asignarTareaFragment)
            }
        }

        binding.btnValidarTareas.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.dashboardTutorFragment) {
                findNavController().navigate(R.id.action_dashboardTutorFragment_to_validarTareasFragment)
            }
        }

        configurarCerrarSesion()

        ninoViewModel.listaNinosResultado.observe(viewLifecycleOwner) { resultado ->
            when (resultado) {
                is Resultado.Cargando -> binding.progressBar.visibility = View.VISIBLE
                is Resultado.Exito -> {
                    binding.progressBar.visibility = View.GONE
                    ninoAdapter.submitList(resultado.data)
                }
                is Resultado.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_LONG).show()
                }
            }
        }

        cargarDatosTutor()
    }

    private fun configurarCerrarSesion() {
        val esHijo = DispositivoConfig.obtenerModo() == DispositivoConfig.ModoDispositivo.HIJO

        if (esHijo) {
            binding.btnCerrarSesion.text = "Volver al modo hijo"
            binding.btnCerrarSesion.setOnClickListener { bloquearYVolverAlHijo() }
        } else {
            binding.btnCerrarSesion.text = "Cerrar sesión"
            binding.btnCerrarSesion.setOnClickListener {
                tutorRepository.cerrarSesion()
                DispositivoConfig.limpiar()
                if (findNavController().currentDestination?.id == R.id.dashboardTutorFragment) {
                    findNavController().navigate(R.id.action_dashboardTutorFragment_to_seleccionRolFragment)
                }
            }
        }
    }

    private fun bloquearYVolverAlHijo() {
        val deviceId = DispositivoConfig.obtenerDeviceId()
        val ninoId = DispositivoConfig.obtenerNinoId()
        if (deviceId.isNotBlank()) {
            dispositivoViewModel.cambiarEstadoDesbloqueo(deviceId, false)
        }
        val bundle = Bundle().apply { putString("ninoId", ninoId) }
        if (findNavController().currentDestination?.id == R.id.dashboardTutorFragment) {
            findNavController().navigate(R.id.action_dashboardTutorFragment_to_loginNinoFragment, bundle)
        }
    }

    private fun cargarDatosTutor() {
        viewLifecycleOwner.lifecycleScope.launch {
            val tutor = tutorRepository.obtenerTutorActual()
            if (tutor != null) {
                binding.tvBienvenida.text = "Hola, ${tutor.nombre}"
                ninoViewModel.cargarNinosPorTutor(tutor.id)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
                runCatching { estadisticasRepository.guardarToken(tutor.id, FirebaseMessaging.getInstance().token.await()) }
            } else {
                binding.tvBienvenida.text = "Hola, Tutor"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
