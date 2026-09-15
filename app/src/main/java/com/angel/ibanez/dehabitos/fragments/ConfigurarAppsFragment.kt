package com.angel.ibanez.dehabitos.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.angel.ibanez.dehabitos.adapters.AppPermitidaAdapter
import com.angel.ibanez.dehabitos.databinding.FragmentConfigurarAppsBinding
import com.angel.ibanez.dehabitos.models.AplicacionPermitida
import com.angel.ibanez.dehabitos.models.Nino
import com.angel.ibanez.dehabitos.utils.AppInfo
import com.angel.ibanez.dehabitos.utils.AppsUtils
import com.angel.ibanez.dehabitos.utils.DispositivoConfig
import com.angel.ibanez.dehabitos.utils.Resultado
import com.angel.ibanez.dehabitos.viewmodels.NinoViewModel
import com.angel.ibanez.dehabitos.viewmodels.RecompensaViewModel

class ConfigurarAppsFragment : Fragment() {

    private var _binding: FragmentConfigurarAppsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RecompensaViewModel by viewModels()
    private val ninoViewModel: NinoViewModel by viewModels()
    private val adapter = AppPermitidaAdapter { app -> viewModel.eliminarApp(app.id) }

    private var ninos: List<Nino> = emptyList()
    private var ninoSeleccionado: Nino? = null
    private var appsInstaladas: List<AppInfo> = emptyList()
    private var appSeleccionada: AppInfo? = null
    private var tutorId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfigurarAppsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { root, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()).top
            root.setPadding(root.paddingLeft, topInset, root.paddingRight, root.paddingBottom)
            insets
        }

        tutorId = DispositivoConfig.obtenerTutorId()
        if (tutorId.isBlank()) {
            Toast.makeText(requireContext(), "Tutor no identificado", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressedDispatcher.onBackPressed()
            return
        }

        binding.rvApps.layoutManager = LinearLayoutManager(requireContext())
        binding.rvApps.adapter = adapter

        actualizarSelectorApps(appsDisponibles(emptyList()))

        binding.actvApp.setOnItemClickListener { _, _, position, _ ->
            appSeleccionada = appsInstaladas.getOrNull(position)
        }

        binding.btnAgregar.setOnClickListener { agregarApp() }
        binding.btnActualizarCatalogo.setOnClickListener {
            ninoSeleccionado?.id?.let(viewModel::cargarCatalogoApps)
        }

        ninoViewModel.listaNinosResultado.observe(viewLifecycleOwner) { resultado ->
            when (resultado) {
                is Resultado.Cargando -> binding.progressBar.visibility = View.VISIBLE
                is Resultado.Exito -> {
                    binding.progressBar.visibility = View.GONE
                    ninos = resultado.data
                    binding.tilNino.visibility = if (ninos.size > 1) View.VISIBLE else View.GONE
                    val adapterNinos = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, ninos.map { it.nombre })
                    binding.actvNino.setAdapter(adapterNinos)
                    binding.actvNino.setOnItemClickListener { _, _, position, _ ->
                        ninoSeleccionado = ninos[position]
                        ninoSeleccionado?.id?.let {
                            viewModel.cargarAppsPermitidas(it)
                            viewModel.cargarCatalogoApps(it)
                        }
                    }
                    if (ninos.isNotEmpty()) {
                        ninoSeleccionado = ninos[0]
                        binding.actvNino.setText(ninos[0].nombre, false)
                        viewModel.cargarAppsPermitidas(ninos[0].id)
                        viewModel.cargarCatalogoApps(ninos[0].id)
                    }
                }
                is Resultado.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_LONG).show()
                }
            }
        }

        viewModel.catalogoResultado.observe(viewLifecycleOwner) { resultado ->
            when (resultado) {
                is Resultado.Cargando -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnActualizarCatalogo.isEnabled = false
                }
                is Resultado.Exito -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnActualizarCatalogo.isEnabled = true
                    val catalogo = resultado.data.map { app ->
                        AppInfo(app.packageName, app.nombre, AppsUtils.base64ADrawable(requireContext(), app.iconoBase64))
                    }
                    actualizarSelectorApps(appsDisponibles(catalogo))
                }
                is Resultado.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnActualizarCatalogo.isEnabled = true
                    actualizarSelectorApps(appsDisponibles(emptyList()))
                    Toast.makeText(requireContext(), "No se pudo cargar el catálogo: ${resultado.mensaje}", Toast.LENGTH_LONG).show()
                }
            }
        }

        viewModel.appsResultado.observe(viewLifecycleOwner) { resultado ->
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

        viewModel.guardarResultado.observe(viewLifecycleOwner) { resultado ->
            if (resultado is Resultado.Exito) {
                Toast.makeText(requireContext(), "App agregada", Toast.LENGTH_SHORT).show()
                limpiarCampos()
                ninoSeleccionado?.id?.let { viewModel.cargarAppsPermitidas(it) }
            } else if (resultado is Resultado.Error) {
                Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.eliminarResultado.observe(viewLifecycleOwner) { resultado ->
            if (resultado is Resultado.Exito) {
                ninoSeleccionado?.id?.let { viewModel.cargarAppsPermitidas(it) }
            } else if (resultado is Resultado.Error) {
                Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_LONG).show()
            }
        }

        ninoViewModel.cargarNinosPorTutor(tutorId)
    }

    private fun obtenerAppsLocales(): List<AppInfo> {
        return AppsUtils.listarAppsInstaladas(requireContext())
    }

    private fun appsDisponibles(catalogo: List<AppInfo>): List<AppInfo> {
        val locales = obtenerAppsLocales()
        val catalogoMap = catalogo.associateBy { it.packageName }
        return (catalogoMap.keys + locales.map { it.packageName })
            .distinct()
            .mapNotNull { packageName ->
                val catalogoItem = catalogoMap[packageName]
                val localItem = locales.find { it.packageName == packageName }
                AppInfo(
                    packageName = packageName,
                    nombre = catalogoItem?.nombre ?: localItem?.nombre ?: packageName,
                    icono = catalogoItem?.icono ?: localItem?.icono
                )
            }
            .sortedBy { it.nombre.lowercase() }
    }

    private fun actualizarSelectorApps(apps: List<AppInfo>) {
        appsInstaladas = apps
        appSeleccionada = null
        binding.actvApp.setText("", false)
        binding.actvApp.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, apps.map { it.nombre })
        )
    }

    private fun agregarApp() {
        val ninoActual = ninoSeleccionado
        if (ninoActual == null) {
            Toast.makeText(requireContext(), "Selecciona un niño", Toast.LENGTH_SHORT).show()
            return
        }

        val texto = binding.actvApp.text.toString().trim()
        val app: AppInfo? = when {
            appSeleccionada != null && appSeleccionada?.nombre == texto -> appSeleccionada
            texto.isNotBlank() -> {
                val local = obtenerAppsLocales().find { it.packageName.equals(texto, ignoreCase = true) || it.nombre.equals(texto, ignoreCase = true) }
                local ?: AppInfo(texto, texto, null)
            }
            else -> null
        }
        if (app == null) {
            Toast.makeText(requireContext(), "Selecciona una app o escribe el package", Toast.LENGTH_SHORT).show()
            return
        }

        val precio15 = binding.etPrecio15.text.toString().toIntOrNull() ?: 0
        val precio30 = binding.etPrecio30.text.toString().toIntOrNull() ?: 0
        val precio60 = binding.etPrecio60.text.toString().toIntOrNull() ?: 0

        if (precio15 <= 0 && precio30 <= 0 && precio60 <= 0) {
            Toast.makeText(requireContext(), "Define al menos un precio", Toast.LENGTH_SHORT).show()
            return
        }

        val appPermitida = AplicacionPermitida(
            ninoId = ninoActual.id,
            tutorId = tutorId,
            packageName = app.packageName,
            nombre = app.nombre,
            iconoBase64 = AppsUtils.drawableABase64(app.icono),
            precio15min = precio15,
            precio30min = precio30,
            precio60min = precio60
        )
        viewModel.guardarApp(appPermitida)
    }

    private fun limpiarCampos() {
        appSeleccionada = null
        binding.actvApp.setText("")
        binding.etPrecio15.setText("")
        binding.etPrecio30.setText("")
        binding.etPrecio60.setText("")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
