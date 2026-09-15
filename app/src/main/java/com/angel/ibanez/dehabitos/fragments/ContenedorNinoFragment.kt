package com.angel.ibanez.dehabitos.fragments

import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.angel.ibanez.dehabitos.R
import com.angel.ibanez.dehabitos.databinding.FragmentContenedorNinoBinding
import com.angel.ibanez.dehabitos.utils.AnimacionesUtils
import com.angel.ibanez.dehabitos.utils.AppsUtils
import com.angel.ibanez.dehabitos.utils.AvataresUtils
import com.angel.ibanez.dehabitos.utils.DispositivoConfig
import com.angel.ibanez.dehabitos.utils.Resultado
import com.angel.ibanez.dehabitos.viewmodels.AuthTutorViewModel
import com.angel.ibanez.dehabitos.viewmodels.DispositivoViewModel
import com.angel.ibanez.dehabitos.viewmodels.NinoViewModel
import com.angel.ibanez.dehabitos.viewmodels.RecompensaViewModel

class ContenedorNinoFragment : Fragment() {

    private var _binding: FragmentContenedorNinoBinding? = null
    private val binding get() = _binding!!

    private val ninoViewModel: NinoViewModel by viewModels()
    private val recompensaViewModel: RecompensaViewModel by viewModels()
    private val dispositivoViewModel: DispositivoViewModel by viewModels()
    private val authTutorViewModel: AuthTutorViewModel by viewModels()
    private lateinit var ninoId: String
    private var procesandoDesvinculacion = false
    private var catalogoHashPendiente: String? = null
    private var esperandoValidacionParental = false
    private var ultimoNivel: Int? = null
    private var ultimaCantidadInsignias: Int? = null
    private var ultimasEstrellas: Int? = null
    private var pestanaActual: Int? = null

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

        val navHostFragment = childFragmentManager.findFragmentById(R.id.nav_host_nino) as NavHostFragment
        val navController = navHostFragment.navController
        
        // Pass ninoId to the start destination
        val navGraph = navController.navInflater.inflate(R.navigation.nav_graph_nino)
        val args = Bundle().apply { putString("ninoId", ninoId) }
        navController.setGraph(navGraph, args)

        configurarNavegacionAnimada(navController, args)
        AnimacionesUtils.cargarGifPausado(binding.btnConfig, "icons8-settings-100.gif")
        AnimacionesUtils.cargarGifPausado(binding.ivMonedasHeader, AnimacionesUtils.GIF_MONEDAS)
        AnimacionesUtils.animarPeriodicamente(binding.btnConfig, INTERVALO_ANIMACION_MS)
        AnimacionesUtils.animarPeriodicamente(binding.ivMonedasHeader, INTERVALO_ANIMACION_MS, INTERVALO_ANIMACION_MS / 2)
        binding.btnConfig.setOnClickListener { solicitarPasswordTutor() }
        authTutorViewModel.validarPasswordResultado.observe(viewLifecycleOwner) { resultado ->
            if (!esperandoValidacionParental) return@observe
            when (resultado) {
                is Resultado.Cargando -> Unit
                is Resultado.Exito -> {
                    esperandoValidacionParental = false
                    navController.navigate(R.id.configuracionParentalFragment, args)
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
    }

    private fun configurarNavegacionAnimada(navController: NavController, args: Bundle) {
        AnimacionesUtils.cargarGifPausado(binding.ivNavInicio, "icons8-home-48.gif")
        AnimacionesUtils.cargarGifPausado(binding.ivNavCamino, "icons8-map100.gif")
        AnimacionesUtils.cargarGifPausado(binding.ivNavMisiones, "icons8-test-passed-100.gif")
        AnimacionesUtils.cargarGifPausado(binding.ivNavPremios, "icons8-shopping-100.gif")
        AnimacionesUtils.cargarGifPausado(binding.ivNavPerfil, "icons8-perfil.gif")

        binding.navInicio.setOnClickListener { navegarPestana(navController, R.id.dashboardNinoFragment, args) }
        binding.navCamino.setOnClickListener { navegarPestana(navController, R.id.mapaProgresoFragment, args) }
        binding.navMisiones.setOnClickListener { navegarPestana(navController, R.id.misionesNinoFragment, args) }
        binding.navPremios.setOnClickListener { navegarPestana(navController, R.id.premiosNinoFragment, args) }
        binding.navPerfil.setOnClickListener { navegarPestana(navController, R.id.perfilNinoFragment, args) }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id != R.id.configuracionParentalFragment) actualizarPestanaSeleccionada(destination.id)
        }
        actualizarPestanaSeleccionada(R.id.dashboardNinoFragment)
    }

    private fun navegarPestana(navController: NavController, destino: Int, args: Bundle) {
        if (navController.currentDestination?.id == R.id.configuracionParentalFragment) navController.popBackStack()
        if (navController.currentDestination?.id != destino) {
            navController.navigate(destino, args, navOptions {
                launchSingleTop = true
                restoreState = true
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            })
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
        elementos.forEach { (datos, icono) ->
            val (contenedor, texto, id) = datos
            val seleccionado = id == destino
            contenedor.animate().scaleX(if (seleccionado) 1.12f else 1f).scaleY(if (seleccionado) 1.12f else 1f).alpha(if (seleccionado) 1f else 0.58f).setDuration(180).start()
            texto.setTextColor(requireContext().getColor(if (seleccionado) R.color.md_theme_primary else R.color.md_theme_onSurfaceVariant))
            if (seleccionado && destino != pestanaActual) AnimacionesUtils.reproducirGifUnaVez(icono)
        }
        pestanaActual = destino
    }

    private fun solicitarPasswordTutor() {
        val campo = EditText(requireContext()).apply {
            hint = "Contraseña del tutor"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(48, 24, 48, 8)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Acceso parental")
            .setMessage("Ingresa la contraseña del tutor para administrar los permisos del dispositivo.")
            .setView(campo)
            .setPositiveButton("Continuar") { _, _ ->
                val password = campo.text.toString()
                if (password.isBlank()) {
                    Toast.makeText(requireContext(), "Ingresa la contraseña", Toast.LENGTH_SHORT).show()
                } else {
                    esperandoValidacionParental = true
                    authTutorViewModel.validarPasswordTutor(password)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
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
                    } else if (ultimaCantidadInsignias != null && nino.insignias.size > ultimaCantidadInsignias!!) {
                        AnimacionesUtils.mostrarCelebracion(
                            binding.root,
                            AnimacionesUtils.GIF_LOGRO,
                            "Nueva insignia",
                            "Desbloqueaste un logro por tu constancia."
                        )
                    }
                    ultimoNivel = nino.nivel
                    ultimaCantidadInsignias = nino.insignias.size
                    binding.tvNombreNino.text = nino.nombre
                    AvataresUtils.cargar(binding.ivAvatarHeader, nino.avatar)
                    binding.tvNivelHeader.text = "Lvl. ${nino.nivel}"
                    binding.pbXpHeader.progress = nino.experiencia % 100
                    if (ultimasEstrellas != null && ultimasEstrellas != nino.estrellas) AnimacionesUtils.reproducirGifUnaVez(binding.ivMonedasHeader)
                    ultimasEstrellas = nino.estrellas
                    binding.tvEstrellas.text = nino.estrellas.toString()
                    binding.tvGemas.text = nino.gemas.toString()
                }
                else -> { /* Handle other states if necessary */ }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val INTERVALO_ANIMACION_MS = 8000L
    }
}
