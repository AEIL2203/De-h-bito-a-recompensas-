package com.angel.ibanez.dehabitos.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import com.angel.ibanez.dehabitos.databinding.DialogAccesoParentalBinding
import com.angel.ibanez.dehabitos.databinding.DialogConfigurarPinParentalBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.view.WindowInsetsCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.angel.ibanez.dehabitos.R
import com.angel.ibanez.dehabitos.databinding.FragmentContenedorNinoBinding
import com.angel.ibanez.dehabitos.firebase.GoogleAuthClient
import com.angel.ibanez.dehabitos.utils.AnimacionesUtils
import com.angel.ibanez.dehabitos.utils.AppsUtils
import com.angel.ibanez.dehabitos.utils.AudioConfig
import com.angel.ibanez.dehabitos.utils.AvataresUtils
import com.angel.ibanez.dehabitos.utils.MusicaFondoManager
import com.angel.ibanez.dehabitos.utils.PermisosUtils
import com.angel.ibanez.dehabitos.repository.EstadisticasRepository
import com.angel.ibanez.dehabitos.repository.TareaRepository
import com.angel.ibanez.dehabitos.utils.DispositivoConfig
import com.angel.ibanez.dehabitos.utils.LogrosUtils
import com.angel.ibanez.dehabitos.utils.Resultado
import com.angel.ibanez.dehabitos.utils.TutorialManager
import com.angel.ibanez.dehabitos.viewmodels.AuthTutorViewModel
import com.angel.ibanez.dehabitos.viewmodels.DispositivoViewModel
import com.angel.ibanez.dehabitos.models.Tarea
import com.angel.ibanez.dehabitos.viewmodels.NinoViewModel
import com.angel.ibanez.dehabitos.viewmodels.RecompensaViewModel
import com.angel.ibanez.dehabitos.viewmodels.TareaViewModel
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ContenedorNinoFragment : Fragment() {

    private var _binding: FragmentContenedorNinoBinding? = null
    private val binding get() = _binding!!

    private val ninoViewModel: NinoViewModel by viewModels()
    private val recompensaViewModel: RecompensaViewModel by viewModels()
    private val dispositivoViewModel: DispositivoViewModel by viewModels()
    private val authTutorViewModel: AuthTutorViewModel by viewModels()
    private val tareaViewModel: TareaViewModel by viewModels()
    private lateinit var ninoId: String
    private var procesandoDesvinculacion = false
    private var catalogoHashPendiente: String? = null
    private var esperandoValidacionParental = false
    private var ultimoNivel: Int? = null
    private var ultimaCantidadInsignias: Int? = null
    private var ultimosLogros: Set<String>? = null
    private var ultimasEstrellas: Int? = null
    private var pestanaActual: Int? = null
    private var estadosAnteriores: Map<String, String>? = null
    private val estadisticasRepository = EstadisticasRepository()
    private val tareaRepository = TareaRepository()
    private val permisoNotificaciones = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContenedorNinoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { root, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()).top
            root.setPadding(root.paddingLeft, topInset, root.paddingRight, root.paddingBottom)
            insets
        }

        ninoId = arguments?.getString("ninoId").orEmpty().ifBlank { DispositivoConfig.obtenerNinoId() }
        DispositivoConfig.guardarModo(DispositivoConfig.ModoDispositivo.HIJO)
        MusicaFondoManager.preparar(requireContext())

        val navHostFragment = childFragmentManager.findFragmentById(R.id.nav_host_nino) as NavHostFragment
        val navController = navHostFragment.navController
        
        // Pass ninoId to the start destination
        val navGraph = navController.navInflater.inflate(R.navigation.nav_graph_nino)
        val args = Bundle().apply { putString("ninoId", ninoId) }
        navController.setGraph(navGraph, args)

        configurarNavegacionAnimada(navController, args)
        binding.ivMonedasHeader.setImageResource(R.drawable.ic_reward)
        binding.ivMonedasHeader.imageTintList = ContextCompat.getColorStateList(requireContext(), R.color.game_yellow)
        binding.btnConfig.setOnClickListener { solicitarAccesoTutor() }
        binding.btnAbrirAccesibilidad.setOnClickListener {
            if (!PermisosUtils.abrirConfiguracionAccesibilidad(requireContext())) {
                Toast.makeText(requireContext(), "No fue posible abrir Accesibilidad", Toast.LENGTH_LONG).show()
            }
        }
        binding.btnVerificarAccesibilidad.setOnClickListener { verificarAccesibilidadObligatoria() }
        authTutorViewModel.validarPasswordResultado.observe(viewLifecycleOwner) { resultado ->
            if (!esperandoValidacionParental) return@observe
            when (resultado) {
                is Resultado.Cargando -> Unit
                is Resultado.Exito -> {
                    esperandoValidacionParental = false
                    if (DispositivoConfig.tienePinParental()) {
                        navController.navigate(R.id.configuracionParentalFragment, args)
                    } else {
                        mostrarConfigurarPinParental {
                            navController.navigate(R.id.configuracionParentalFragment, args)
                        }
                    }
                }
                is Resultado.Error -> {
                    esperandoValidacionParental = false
                    Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_LONG).show()
                }
            }
        }

        observarNino()
        observarSincronizacionCatalogo()
        observarVinculacionDispositivo()
        sincronizarCatalogoSiCorresponde()
        dispositivoViewModel.iniciarEscuchaDispositivo(DispositivoConfig.obtenerDeviceId())
        ninoViewModel.escucharNino(ninoId)
        observarValidaciones()
        tareaViewModel.escucharTareasPorNino(ninoId)
        registrarTokenYPermisos()
        asegurarMisionDiaria()
    }

    @Suppress("DEPRECATION")
    private fun registrarTokenYPermisos() {
        if (ninoId.isBlank()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        val tutorId = DispositivoConfig.obtenerTutorId()
        if (tutorId.isBlank()) return
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                estadisticasRepository.guardarToken(tutorId, FirebaseMessaging.getInstance().getToken().await(), ninoId, "hijo")
            }
        }
    }

    private fun asegurarMisionDiaria() {
        val tutorId = DispositivoConfig.obtenerTutorId()
        if (ninoId.isBlank() || tutorId.isBlank()) return
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching { tareaRepository.asegurarMisionDiaria(ninoId, tutorId) }
        }
    }

    private fun configurarNavegacionAnimada(navController: NavController, args: Bundle) {
        binding.navInicio.setOnClickListener { navegarPestana(navController, R.id.dashboardNinoFragment, args) }
        binding.navCamino.setOnClickListener { navegarPestana(navController, R.id.mapaProgresoFragment, args) }
        binding.navMisiones.setOnClickListener { navegarPestana(navController, R.id.misionesNinoFragment, args) }
        binding.navPremios.setOnClickListener { navegarPestana(navController, R.id.premiosNinoFragment, args) }
        binding.navPerfil.setOnClickListener { navegarPestana(navController, R.id.perfilNinoFragment, args) }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id != R.id.configuracionParentalFragment) actualizarPestanaSeleccionada(destination.id)
            binding.cardProfilePill.visibility =
                if (destination.id == R.id.perfilNinoFragment) View.GONE else View.VISIBLE
        }
        actualizarPestanaSeleccionada(R.id.dashboardNinoFragment)
    }

    private fun navegarPestana(navController: NavController, destino: Int, args: Bundle) {
        if (navController.currentDestination?.id == R.id.configuracionParentalFragment) navController.popBackStack()
        val actual = navController.currentDestination?.id ?: -1
        if (actual != destino) {
            val opciones = navOptions {
                launchSingleTop = true
                restoreState = true
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            }
            navController.navigate(destino, args, opciones)
        }
    }

    private fun actualizarPestanaSeleccionada(destino: Int) {
        val elementos = listOf(
            Triple(binding.navInicio, binding.tvNavInicio, R.id.dashboardNinoFragment) to binding.ivNavInicio,
            Triple(binding.navCamino, binding.tvNavCamino, R.id.mapaProgresoFragment) to binding.ivNavCamino,
            Triple(binding.navMisiones, binding.tvNavMisiones, R.id.misionesNinoFragment) to binding.ivNavMisiones,
            Triple(binding.navPremios, binding.tvNavPremios, R.id.premiosNinoFragment) to binding.ivNavPremios,
            Triple(binding.navPerfil, binding.tvNavPerfil, R.id.perfilNinoFragment) to binding.ivNavPerfil
        )
        val colorSeleccionado = requireContext().getColor(R.color.child_nav_selected_text)
        val colorNormal = requireContext().getColor(R.color.child_nav_icon)
        val indicador = binding.vIndicadorNav

        elementos.forEach { (datos, icono) ->
            val (contenedor, texto, id) = datos
            val seleccionado = id == destino
            texto.setTextColor(if (seleccionado) colorSeleccionado else colorNormal)
            if (seleccionado && destino != pestanaActual) AnimacionesUtils.reproducirGifUnaVez(icono)
        }

        val seleccionado = elementos.find { it.first.third == destino }?.first?.first
        seleccionado?.post {
            if (_binding == null) return@post
            if (indicador.width == 0) {
                indicador.layoutParams = indicador.layoutParams.apply {
                    width = seleccionado.width
                    height = seleccionado.height
                }
                indicador.x = seleccionado.x
                indicador.y = seleccionado.y
                indicador.visibility = View.VISIBLE
            } else {
                indicador.animate()
                    .withLayer()
                    .x(seleccionado.x)
                    .y(seleccionado.y)
                    .setDuration(250)
                    .setInterpolator(FastOutSlowInInterpolator())
                    .start()
            }
        }

        pestanaActual = destino
    }

    private fun solicitarAccesoTutor() {
        if (DispositivoConfig.tienePinParental()) {
            solicitarPinParental()
        } else {
            solicitarGoogleParaMigracion()
        }
    }

    private fun solicitarPinParental() {
        val contenido = DialogAccesoParentalBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(contenido.root)
            .setPositiveButton("Entrar", null)
            .setNegativeButton("Cancelar", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val pin = contenido.etPassword.text.toString()
                when {
                    !pin.matches(Regex("\\d{6}")) -> contenido.tilPassword.error = "Ingresa los 6 dígitos"
                    else -> when (DispositivoConfig.validarPinParental(pin)) {
                        DispositivoConfig.ResultadoPin.CORRECTO -> {
                            dialog.dismiss()
                            abrirConfiguracionParental()
                        }
                        DispositivoConfig.ResultadoPin.INCORRECTO -> contenido.tilPassword.error = "PIN parental incorrecto"
                        DispositivoConfig.ResultadoPin.BLOQUEADO -> contenido.tilPassword.error =
                            "Demasiados intentos. Espera ${DispositivoConfig.segundosBloqueoPin()} segundos"
                        DispositivoConfig.ResultadoPin.NO_CONFIGURADO -> contenido.tilPassword.error = "El PIN parental no está configurado"
                    }
                }
            }
        }
        dialog.show()
    }

    private fun solicitarGoogleParaMigracion() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                esperandoValidacionParental = true
                val idToken = GoogleAuthClient(requireActivity()).obtenerIdToken()
                val tutorId = DispositivoConfig.obtenerTutorId()
                if (tutorId.isBlank()) throw IllegalStateException("No se encontró el tutor vinculado")
                authTutorViewModel.validarCuentaGoogle(idToken, tutorId)
            } catch (_: GetCredentialCancellationException) {
                esperandoValidacionParental = false
            } catch (e: Exception) {
                esperandoValidacionParental = false
                Toast.makeText(requireContext(), e.message ?: "No se pudo confirmar la cuenta", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun mostrarConfigurarPinParental(alGuardar: () -> Unit) {
        val contenido = DialogConfigurarPinParentalBinding.inflate(layoutInflater)
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
                        alGuardar()
                    }
                }
            }
        }
        dialog.show()
    }

    private fun abrirConfiguracionParental() {
        val navHost = childFragmentManager.findFragmentById(R.id.nav_host_nino) as? NavHostFragment ?: return
        val args = Bundle().apply { putString("ninoId", ninoId) }
        if (navHost.navController.currentDestination?.id != R.id.configuracionParentalFragment) {
            navHost.navController.navigate(R.id.configuracionParentalFragment, args)
        }
    }

    private fun sincronizarCatalogoSiCorresponde() {
        if (ninoId.isBlank()) return
        val deviceId = DispositivoConfig.obtenerDeviceId()
        val (apps, hash) = AppsUtils.crearCatalogo(requireContext(), ninoId, deviceId)
        
        Log.d("ContenedorNino", "Catálogo local: ${apps.size} apps encontradas. Hash: $hash")
        
        if (!DispositivoConfig.debeSincronizarCatalogo(hash)) {
            Log.d("ContenedorNino", "El catálogo ya está actualizado.")
            return
        }
        
        Log.d("ContenedorNino", "Sincronizando catálogo con la nube...")
        catalogoHashPendiente = hash
        recompensaViewModel.sincronizarCatalogoApps(ninoId, deviceId, apps)
    }

    private fun observarSincronizacionCatalogo() {
        recompensaViewModel.sincronizarCatalogoResultado.observe(viewLifecycleOwner) { resultado ->
            when (resultado) {
                is Resultado.Exito -> {
                    Log.i("ContenedorNino", "Catálogo sincronizado exitosamente.")
                    catalogoHashPendiente?.let(DispositivoConfig::registrarCatalogoSincronizado)
                    catalogoHashPendiente = null
                }
                is Resultado.Error -> {
                    Log.e("ContenedorNino", "Error al sincronizar catálogo: ${resultado.mensaje}")
                }
                else -> {}
            }
        }
    }

    private fun observarVinculacionDispositivo() {
        dispositivoViewModel.vinculado.observe(viewLifecycleOwner) { vinculado ->
            if (vinculado || procesandoDesvinculacion) return@observe
            procesandoDesvinculacion = true
            dispositivoViewModel.detenerEscucha()
            DispositivoConfig.limpiar()
            if (findNavController().currentDestination?.id == R.id.contenedorNinoFragment) {
                findNavController().navigate(R.id.seleccionRolFragment, null, navOptions {
                    popUpTo(R.id.contenedorNinoFragment) { inclusive = true }
                })
            }
        }
    }

    private fun observarValidaciones() {
        tareaViewModel.listaTareasResultado.observe(viewLifecycleOwner) { resultado ->
            if (resultado !is Resultado.Exito) return@observe
            val tareas = resultado.data
            val anteriores = estadosAnteriores
            if (anteriores == null) {
                estadosAnteriores = tareas.associate { it.id to it.estado }
                return@observe
            }
            val recienValidadas = tareas.filter {
                it.estado == Tarea.ESTADO_VALIDADA && anteriores[it.id] != null && anteriores[it.id] != Tarea.ESTADO_VALIDADA
            }
            estadosAnteriores = tareas.associate { it.id to it.estado }
            when (recienValidadas.size) {
                0 -> Unit
                1 -> {
                    val tarea = recienValidadas.first()
                    AnimacionesUtils.mostrarCelebracion(
                        binding.root,
                        AnimacionesUtils.GIF_VALIDACION,
                        "¡Misión validada!",
                        "Tu tutor aprobó «${tarea.nombre}». +${tarea.puntos} XP"
                    )
                }
                else -> AnimacionesUtils.mostrarCelebracion(
                    binding.root,
                    AnimacionesUtils.GIF_VALIDACION,
                    "¡Misiones validadas!",
                    "Se validaron todas tus misiones (+${recienValidadas.sumOf { it.puntos }} XP)"
                )
            }
        }
    }

    private fun observarNino() {
        ninoViewModel.ninoResultado.observe(viewLifecycleOwner) { resultado ->
            when (resultado) {
                is Resultado.Exito -> {
                    val nino = resultado.data
                    if (ultimoNivel != null && nino.nivel > ultimoNivel!!) {
                        AnimacionesUtils.mostrarCelebracion(
                            binding.root,
                            AnimacionesUtils.GIF_META,
                            "Nuevo nivel",
                            "Alcanzaste el nivel ${nino.nivel} y recibiste nuevas estrellas."
                        )
                    }
                    val logrosActuales = LogrosUtils.logrosDesbloqueados(nino)
                    val idsActuales = logrosActuales.map { it.id }.toSet()
                    ultimosLogros?.let { anteriores ->
                        logrosActuales.filter { it.id !in anteriores }.forEach { logro ->
                            AnimacionesUtils.mostrarCelebracion(
                                binding.root,
                                AnimacionesUtils.GIF_LOGRO,
                                "¡Nuevo logro!",
                                logro.nombre
                            )
                        }
                    }
                    ultimoNivel = nino.nivel
                    ultimaCantidadInsignias = nino.insignias.size
                    ultimosLogros = idsActuales
                    binding.tvNombreNino.text = nino.nombre
                    AvataresUtils.cargar(binding.ivAvatarHeader, nino.avatar)
                    binding.tvNivelHeader.text = "Lvl. ${nino.nivel}"
                    binding.pbXpHeader.progress = nino.experiencia % 100
                    if (ultimasEstrellas != null && ultimasEstrellas != nino.estrellas) AnimacionesUtils.reproducirGifUnaVez(binding.ivMonedasHeader)
                    ultimasEstrellas = nino.estrellas
                    binding.tvEstrellas.text = nino.estrellas.toString()
                    binding.tvGemas.text = nino.gemas.toString()
                    if (!DispositivoConfig.tutorialVisto(DispositivoConfig.ModoDispositivo.HIJO)) {
                        binding.root.post { TutorialManager.iniciarTutorialNinoSiNecesario(requireActivity()) }
                    }
                }
                else -> { /* Handle other states if necessary */ }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        verificarAccesibilidadObligatoria()
        if (AudioConfig.musicaHabilitada(requireContext())) {
            MusicaFondoManager.reproducir()
        }
    }

    private fun verificarAccesibilidadObligatoria() {
        binding.overlayAccesibilidad.isVisible =
            !PermisosUtils.servicioAccesibilidadActivo(requireContext())
    }

    override fun onPause() {
        super.onPause()
        MusicaFondoManager.pausar()
    }

    override fun onDestroyView() {
        MusicaFondoManager.liberar()
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val INTERVALO_ANIMACION_MS = 8000L
    }
}
