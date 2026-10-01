package com.angel.ibanez.dehabitos.fragments

import android.Manifest
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.angel.ibanez.dehabitos.R
import com.angel.ibanez.dehabitos.adapters.NinoAdapter
import com.angel.ibanez.dehabitos.databinding.DialogConfigNotificacionesBinding
import com.angel.ibanez.dehabitos.databinding.DialogConfigurarPinParentalBinding
import com.angel.ibanez.dehabitos.databinding.DialogHorarioSuenoBinding
import com.angel.ibanez.dehabitos.databinding.FragmentDashboardTutorBinding
import com.angel.ibanez.dehabitos.models.HorarioSueno
import com.angel.ibanez.dehabitos.models.Nino
import com.angel.ibanez.dehabitos.utils.DispositivoConfig
import com.angel.ibanez.dehabitos.utils.IconosUtils
import com.angel.ibanez.dehabitos.repository.TutorRepository
import com.angel.ibanez.dehabitos.utils.Resultado
import com.angel.ibanez.dehabitos.utils.TemaConfig
import com.angel.ibanez.dehabitos.viewmodels.DispositivoViewModel
import com.angel.ibanez.dehabitos.viewmodels.NinoViewModel
import com.angel.ibanez.dehabitos.workers.ProgramadorTrabajos
import kotlinx.coroutines.launch
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import com.angel.ibanez.dehabitos.repository.EstadisticasRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DashboardTutorFragment : Fragment() {

    private var _binding: FragmentDashboardTutorBinding? = null
    private val binding get() = _binding!!
    private val ninoViewModel: NinoViewModel by activityViewModels()
    private val dispositivoViewModel: DispositivoViewModel by viewModels()
    private val ninoAdapter = NinoAdapter { nino ->
        findNavController().navigate(R.id.action_dashboardTutorFragment_to_estadisticasNinoFragment, Bundle().apply { putString("ninoId", nino.id) })
    }
    private val tutorRepository = TutorRepository()
    private val estadisticasRepository = EstadisticasRepository()
    private val permisoNotificaciones = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private var pestañaTutorActual = 0
    private var ninos: List<Nino> = emptyList()

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
        pestañaTutorActual = savedInstanceState?.getInt(CLAVE_PESTAÑA_TUTOR)
            ?: arguments?.getInt("tutorTab")
            ?: pestañaTutorActual

        // Inicializar el adaptador de inmediato para evitar advertencias de "No adapter attached"
        binding.rvNinos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNinos.adapter = ninoAdapter

        // Los iconos de las filas del menú se asignan en el layout con app:iiv_icon
        binding.btnCerrarSesion.icon = IconosUtils.crearIcono(requireContext(), "gmr-exit-to-app", R.color.white)
        configurarIconos()

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

        binding.btnHorarioSueno.setOnClickListener { mostrarDialogoHorarioSueno() }

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

        binding.btnEncuesta.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.dashboardTutorFragment) {
                findNavController().navigate(R.id.action_dashboardTutorFragment_to_encuestaSatisfaccionFragment)
            }
        }

        configurarCerrarSesion()
        configurarModoOscuro()
        configurarMenuTutor()
        configurarInformacionApp()
        binding.btnTerminos.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.dashboardTutorFragment) {
                findNavController().navigate(R.id.action_dashboardTutorFragment_to_terminosCondicionesFragment)
            }
        }
        binding.btnPinParental.setOnClickListener { mostrarConfigurarPinParental() }
        binding.btnConfigurarNotificaciones.setOnClickListener { mostrarDialogoConfigNotificaciones() }

        ninoViewModel.listaNinosResultado.observe(viewLifecycleOwner) { resultado ->
            when (resultado) {
                is Resultado.Cargando -> binding.progressBar.visibility = View.VISIBLE
                is Resultado.Exito -> {
                    binding.progressBar.visibility = View.GONE
                    ninos = resultado.data
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

    private fun configurarIconos() {
        binding.btnAgregarNino.icon = IconosUtils.crearIcono(requireContext(), "gmr-group-add", R.color.md_theme_primary)
        binding.btnGestionarDispositivos.icon = IconosUtils.crearIcono(requireContext(), "gmr-tablet-android", R.color.md_theme_secondary)
        binding.btnHorarioSueno.icon = IconosUtils.crearIcono(requireContext(), "gmr-bedtime", R.color.md_theme_tertiary)
        binding.btnConfigurarApps.icon = IconosUtils.crearIcono(requireContext(), "gmr-apps", R.color.md_theme_tertiary)
        binding.btnAsignarTarea.icon = IconosUtils.crearIcono(requireContext(), "gmr-add-task", R.color.game_green_dark)
        binding.btnValidarTareas.icon = IconosUtils.crearIcono(requireContext(), "gmr-fact-check", R.color.md_theme_secondary)
        binding.btnEncuesta.icon = IconosUtils.crearIcono(requireContext(), "gmr-rate-review", R.color.md_theme_tertiary)
        binding.btnTerminos.icon = IconosUtils.crearIcono(requireContext(), "gmr-description", R.color.md_theme_primary)
        binding.btnConfigurarNotificaciones.icon = IconosUtils.crearIcono(requireContext(), "gmr-notifications", R.color.md_theme_secondary)
        binding.btnPinParental.icon = IconosUtils.crearIcono(requireContext(), "gmr-pin", R.color.md_theme_secondary)
        binding.navTutorInicio.icon = IconosUtils.crearIcono(requireContext(), "gmr-home", R.color.md_theme_primary)
        binding.navTutorGestion.icon = IconosUtils.crearIcono(requireContext(), "gmr-people", R.color.md_theme_primary)
        binding.navTutorControl.icon = IconosUtils.crearIcono(requireContext(), "gmr-tune", R.color.md_theme_primary)
        binding.navTutorConfiguracion.icon = IconosUtils.crearIcono(requireContext(), "gmr-settings", R.color.md_theme_primary)
    }

    private fun mostrarConfigurarPinParental(alGuardar: (() -> Unit)? = null) {
        val contenido = DialogConfigurarPinParentalBinding.inflate(layoutInflater)
        contenido.tvTitulo.text = if (DispositivoConfig.tienePinParental()) "Cambia tu PIN parental" else "Crea tu PIN parental"
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(contenido.root)
            .setPositiveButton("Guardar PIN", null)
            .setNegativeButton("Cancelar", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val pin = contenido.etPin.text.toString()
                val confirmacion = contenido.etConfirmarPin.text.toString()
                when {
                    !pin.matches(Regex("\\d{6}")) -> contenido.tilPin.error = "Debe tener exactamente 6 dígitos"
                    pin != confirmacion -> contenido.tilConfirmarPin.error = "Los PIN no coinciden"
                    else -> {
                        DispositivoConfig.guardarPinParental(pin)
                        dialog.dismiss()
                        Toast.makeText(requireContext(), "PIN parental actualizado", Toast.LENGTH_SHORT).show()
                        alGuardar?.invoke()
                    }
                }
            }
        }
        dialog.show()
    }

    private fun mostrarDialogoConfigNotificaciones() {
        val contenido = DialogConfigNotificacionesBinding.inflate(layoutInflater)
        contenido.switchHabilitadas.isChecked = DispositivoConfig.notificacionesHabilitadas()
        contenido.etHoraNino.setText(DispositivoConfig.obtenerHoraNotificacionHijo().toString())
        contenido.etHoraTutor.setText(DispositivoConfig.obtenerHoraNotificacionTutor().toString())
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(contenido.root)
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val habilitadas = contenido.switchHabilitadas.isChecked
                val horaNino = contenido.etHoraNino.text.toString().toIntOrNull() ?: 7
                val horaTutor = contenido.etHoraTutor.text.toString().toIntOrNull() ?: 17
                if (horaNino !in 0..23 || horaTutor !in 0..23) {
                    Toast.makeText(requireContext(), "Las horas deben estar entre 0 y 23", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                DispositivoConfig.guardarConfigNotificaciones(horaNino, horaTutor, habilitadas)
                ProgramadorTrabajos.programar(requireContext())
                dialog.dismiss()
                Toast.makeText(requireContext(), "Configuración guardada", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    private fun mostrarDialogoHorarioSueno() {
        if (ninos.isEmpty()) {
            Toast.makeText(requireContext(), "Agrega un niño primero", Toast.LENGTH_SHORT).show()
            return
        }
        val nino = ninos.first()
        val contenido = DialogHorarioSuenoBinding.inflate(layoutInflater)
        val horarioActual = nino.horarioSueno
        contenido.etHoraDormir.setText(horarioActual.horaDormir)
        contenido.etHoraDespertar.setText(horarioActual.horaDespertar)
        val checkBoxes = listOf(
            contenido.cbLunes, contenido.cbMartes, contenido.cbMiercoles,
            contenido.cbJueves, contenido.cbViernes, contenido.cbSabado, contenido.cbDomingo
        )
        val dias = listOf(
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
        )
        checkBoxes.forEachIndexed { i, cb -> cb.isChecked = horarioActual.diasActivos.contains(dias[i]) }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(contenido.root)
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val dormir = contenido.etHoraDormir.text.toString().trim()
                val despertar = contenido.etHoraDespertar.text.toString().trim()
                if (!validarHora(dormir) || !validarHora(despertar)) {
                    Toast.makeText(requireContext(), "Usa formato HH:MM", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val diasActivos = dias.filterIndexed { i, _ -> checkBoxes[i].isChecked }
                if (diasActivos.isEmpty()) {
                    Toast.makeText(requireContext(), "Selecciona al menos un día", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                ninoViewModel.actualizarHorarioSueno(nino.id, HorarioSueno(dormir, despertar, diasActivos))
                dialog.dismiss()
                Toast.makeText(requireContext(), "Horario guardado", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    private fun validarHora(hora: String): Boolean = hora.matches(Regex("^([01]?[0-9]|2[0-3]):[0-5][0-9]$"))

    private fun configurarCerrarSesion() {
        val esHijo = DispositivoConfig.obtenerModo() == DispositivoConfig.ModoDispositivo.HIJO

        if (esHijo) {
            binding.btnCerrarSesion.text = "Volver al modo hijo"
            binding.btnCerrarSesion.setOnClickListener { bloquearYVolverAlHijo() }
        } else {
            binding.btnCerrarSesion.text = "Cerrar sesión"
            binding.btnCerrarSesion.setOnClickListener {
                tutorRepository.cerrarSesion()
                DispositivoConfig.limpiar(preservarPinParental = true)
                if (findNavController().currentDestination?.id == R.id.dashboardTutorFragment) {
                    findNavController().navigate(R.id.loginTutorFragment)
                }
            }
        }
    }

    private fun configurarModoOscuro() {
        binding.switchModoOscuro.isChecked = TemaConfig.esOscuroActual(requireContext())
        binding.switchModoOscuro.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked == TemaConfig.esOscuroActual(requireContext())) return@setOnCheckedChangeListener
            TemaConfig.cambiar(requireContext(), isChecked)
        }
    }

    private fun configurarMenuTutor() {
        val opciones = listOf(
            Triple(binding.navTutorInicio, binding.seccionInicio, "Inicio · Mis niños"),
            Triple(binding.navTutorGestion, binding.seccionGestion, "Gestión familiar"),
            Triple(binding.navTutorControl, binding.seccionControl, "Control y recompensas"),
            Triple(binding.navTutorConfiguracion, binding.seccionConfiguracion, "Configuración")
        )

        // Configuración inicial sin animación
        val inicial = opciones[pestañaTutorActual.coerceIn(opciones.indices)]
        opciones.forEach { (boton, seccion, _) ->
            val activa = seccion === inicial.second
            seccion.visibility = if (activa) View.VISIBLE else View.GONE
            seccion.alpha = 1f
            pintarOpcionMenu(boton, activa)
        }
        var seccionMostrada: View? = inicial.second

        posicionarIndicadorTutor(inicial.first, animar = false)

        opciones.forEachIndexed { indice, (boton, seccion, _) ->
            boton.setOnClickListener {
                if (pestañaTutorActual == indice) return@setOnClickListener
                pestañaTutorActual = indice
                val anterior = seccionMostrada
                seccionMostrada = seccion
                opciones.forEach { (otroBoton, otraSeccion, _) ->
                    pintarOpcionMenu(otroBoton, otraSeccion === seccion)
                    if (otraSeccion !== anterior && otraSeccion !== seccion) {
                        otraSeccion.visibility = View.GONE
                        otraSeccion.alpha = 1f
                    }
                }
                anterior?.visibility = View.GONE
                seccion.animate().cancel()
                seccion.alpha = 0f
                seccion.visibility = View.VISIBLE
                seccion.animate()
                    .alpha(1f)
                    .setDuration(180)
                    .setInterpolator(FastOutSlowInInterpolator())
                    .start()
                posicionarIndicadorTutor(boton, animar = true)
            }
        }
    }

    private fun pintarOpcionMenu(boton: MaterialButton, seleccionada: Boolean) {
        val contenido = MaterialColors.getColor(
            boton,
            if (seleccionada) com.google.android.material.R.attr.colorOnPrimaryContainer else com.google.android.material.R.attr.colorOnSurfaceVariant,
            0
        )
        boton.setTextColor(contenido)
        boton.iconTint = ColorStateList.valueOf(contenido)
    }

    private fun posicionarIndicadorTutor(boton: MaterialButton, animar: Boolean) {
        val indicador = binding.vIndicadorTutor
        boton.post {
            if (_binding == null) return@post
            if (indicador.width == 0) {
                indicador.layoutParams = indicador.layoutParams.apply {
                    width = boton.width
                    height = boton.height
                }
                indicador.x = boton.x
                indicador.y = boton.y
                indicador.visibility = View.VISIBLE
            } else if (animar) {
                indicador.animate()
                    .withLayer()
                    .x(boton.x)
                    .y(boton.y)
                    .setDuration(250)
                    .setInterpolator(FastOutSlowInInterpolator())
                    .start()
            } else {
                indicador.x = boton.x
                indicador.y = boton.y
            }
        }
    }

    private fun configurarInformacionApp() {
        val info = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
        binding.tvVersionApp.text = "Versión ${info.versionName}"
        val fecha = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-ES"))
            .format(Date(info.lastUpdateTime))
        binding.tvUltimaActualizacion.text = "Última actualización: $fecha"
    }

    private fun bloquearYVolverAlHijo() {
        if (!DispositivoConfig.tienePinParental()) {
            mostrarConfigurarPinParental { bloquearYVolverAlHijo() }
            return
        }
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

    @Suppress("DEPRECATION")
    private fun cargarDatosTutor() {
        viewLifecycleOwner.lifecycleScope.launch {
            val tutor = tutorRepository.obtenerTutorActual()
            if (tutor != null) {
                binding.tvBienvenida.text = getString(R.string.tutor_dashboard_welcome_name, tutor.nombre)
                ninoViewModel.cargarNinosPorTutor(tutor.id)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
                runCatching { estadisticasRepository.guardarToken(tutor.id, FirebaseMessaging.getInstance().getToken().await()) }
            } else {
                binding.tvBienvenida.text = getString(R.string.tutor_dashboard_hello)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(CLAVE_PESTAÑA_TUTOR, pestañaTutorActual)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val CLAVE_PESTAÑA_TUTOR = "pestaña_tutor_actual"
    }
}
