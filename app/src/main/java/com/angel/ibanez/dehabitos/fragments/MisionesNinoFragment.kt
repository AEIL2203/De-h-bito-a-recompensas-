package com.angel.ibanez.dehabitos.fragments

import android.animation.ObjectAnimator
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.angel.ibanez.dehabitos.R
import com.angel.ibanez.dehabitos.adapters.MisionAdapter
import com.angel.ibanez.dehabitos.databinding.DialogEscaneandoImagenBinding
import com.angel.ibanez.dehabitos.databinding.FragmentMisionesNinoBinding
import com.angel.ibanez.dehabitos.models.Tarea
import com.angel.ibanez.dehabitos.utils.AnimacionesUtils
import com.angel.ibanez.dehabitos.utils.FechasUtils
import com.angel.ibanez.dehabitos.utils.ImagenesUtils
import com.angel.ibanez.dehabitos.utils.NsfwCheckResult
import com.angel.ibanez.dehabitos.utils.NsfwDetector
import com.angel.ibanez.dehabitos.utils.Resultado
import com.angel.ibanez.dehabitos.viewmodels.TareaViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MisionesNinoFragment : Fragment() {

    private var _binding: FragmentMisionesNinoBinding? = null
    private val binding get() = _binding!!

    private val tareaViewModel: TareaViewModel by viewModels()
    private var ninoId: String = ""
    private lateinit var misionAdapter: MisionAdapter
    private var tareaPendiente: Tarea? = null
    private var misionesOrdenadas = emptyList<Tarea>()
    private var idsAnteriores = emptySet<String>()
    private var paginaActual = 0

    private val tomarFoto = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) completarConBitmap(bitmap)
    }

    private val elegirImagen = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            cargarBitmap(uri)?.let(::completarConBitmap)
                ?: Toast.makeText(requireContext(), "No se pudo cargar la imagen", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMisionesNinoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ninoId = arguments?.getString("ninoId") ?: ""

        misionAdapter = MisionAdapter { tarea -> preguntarEvidencia(tarea) }
        binding.rvMisiones.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMisiones.setHasFixedSize(true)
        binding.rvMisiones.adapter = misionAdapter
        binding.btnPaginaAnterior.setOnClickListener {
            if (paginaActual > 0) {
                paginaActual--
                actualizarPagina()
            }
        }
        binding.btnPaginaSiguiente.setOnClickListener {
            if ((paginaActual + 1) * TAMANO_PAGINA < misionesOrdenadas.size) {
                paginaActual++
                actualizarPagina()
            }
        }

        AnimacionesUtils.entradaSuave(binding.rvMisiones)
        observarMisiones()
    }

    override fun onResume() {
        super.onResume()
        tareaViewModel.escucharTareasPorNino(ninoId)
        tareaViewModel.iniciarSondeoTareas(ninoId)
    }

    override fun onPause() {
        tareaViewModel.detenerSondeoTareas()
        super.onPause()
    }

    private fun actualizarPagina() {
        val totalPaginas = ((misionesOrdenadas.size + TAMANO_PAGINA - 1) / TAMANO_PAGINA).coerceAtLeast(1)
        paginaActual = paginaActual.coerceIn(0, totalPaginas - 1)
        val inicio = paginaActual * TAMANO_PAGINA
        val fin = (inicio + TAMANO_PAGINA).coerceAtMost(misionesOrdenadas.size)
        val pagina = if (inicio < fin) misionesOrdenadas.subList(inicio, fin) else emptyList()
        misionAdapter.submitList(pagina) {
            _binding?.rvMisiones?.scrollToPosition(0)
        }
        binding.containerPaginacion.visibility = if (misionesOrdenadas.size > TAMANO_PAGINA) View.VISIBLE else View.GONE
        binding.tvPagina.text = getString(R.string.missions_page_indicator, paginaActual + 1, totalPaginas)
        binding.btnPaginaAnterior.isEnabled = paginaActual > 0
        binding.btnPaginaSiguiente.isEnabled = paginaActual < totalPaginas - 1
    }

    private fun preguntarEvidencia(tarea: Tarea) {
        tareaPendiente = tarea
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("¿Quieres adjuntar una foto?")
            .setItems(arrayOf("Tomar foto", "Elegir de la galería", "Completar sin foto")) { _, opcion ->
                val pendiente = tareaPendiente ?: return@setItems
                when (opcion) {
                    0 -> tomarFoto.launch(null)
                    1 -> elegirImagen.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                    else -> tareaViewModel.marcarCompletada(pendiente)
                }
            }
            .setOnCancelListener { tareaPendiente = null }
            .show()
    }

    private fun cargarBitmap(uri: Uri): Bitmap? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(requireContext().contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE)
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
        }
    }.getOrNull()

    private fun completarConBitmap(bitmap: Bitmap) {
        val tarea = tareaPendiente ?: return
        val contenido = DialogEscaneandoImagenBinding.inflate(layoutInflater)
        val dialogo = MaterialAlertDialogBuilder(requireContext())
            .setView(contenido.root)
            .setCancelable(false)
            .create()
        val pulso = ObjectAnimator.ofFloat(contenido.tvEscaneando, View.ALPHA, 1f, 0.45f, 1f).apply {
            duration = 1100
            repeatCount = android.animation.ValueAnimator.INFINITE
        }
        dialogo.setOnShowListener { pulso.start() }
        dialogo.setOnDismissListener { pulso.cancel() }
        dialogo.show()
        viewLifecycleOwner.lifecycleScope.launch {
            val inicio = SystemClock.elapsedRealtime()
            val resultado = NsfwDetector.analyzeBitmap(requireContext(), bitmap)
            val restante = DURACION_MINIMA_ESCANEO_MS - (SystemClock.elapsedRealtime() - inicio)
            if (restante > 0) delay(restante)
            when (resultado) {
                NsfwCheckResult.Safe -> {
                    val evidencia = withContext(Dispatchers.Default) { ImagenesUtils.comprimirABase64(bitmap) }
                    dialogo.dismiss()
                    tareaPendiente = null
                    tareaViewModel.marcarCompletada(tarea, evidencia)
                }
                is NsfwCheckResult.Unsafe -> {
                    dialogo.dismiss()
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Imagen no válida")
                        .setMessage("Esta imagen no cumple con las normas. Por favor, elige otra.")
                        .setPositiveButton("Entendido", null)
                        .show()
                }
                is NsfwCheckResult.Error -> {
                    dialogo.dismiss()
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("No se pudo verificar la imagen")
                        .setMessage("Por seguridad, la foto no se enviará. Intenta nuevamente.")
                        .setPositiveButton("Entendido", null)
                        .show()
                }
            }
        }
    }

    private fun observarMisiones() {
        tareaViewModel.listaTareasResultado.observe(viewLifecycleOwner) { resultado ->
            when (resultado) {
                is Resultado.Exito -> {
                    val misiones = resultado.data.sortedWith(
                        compareBy<Tarea> { it.estado != Tarea.ESTADO_PENDIENTE }
                            .thenBy {
                                if (it.estado == Tarea.ESTADO_PENDIENTE &&
                                    FechasUtils.estadoVencimiento(it.fechaLimite) == FechasUtils.EstadoVencimiento.VENCIDA
                                ) 1 else 0
                            }
                            .thenBy { if (it.estado == Tarea.ESTADO_PENDIENTE) it.fechaLimite.ifBlank { "9999-99-99" } else "" }
                    )
                    val idsNuevos = misiones.map { it.id }.toSet()
                    if (idsAnteriores.isNotEmpty() && (idsNuevos - idsAnteriores).isNotEmpty()) paginaActual = 0
                    idsAnteriores = idsNuevos
                    misionesOrdenadas = misiones
                    actualizarPagina()
                    binding.tvSinMisiones.visibility = if (misiones.isEmpty()) View.VISIBLE else View.GONE
                }
                is Resultado.Error -> {
                    Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }

        tareaViewModel.completarResultado.observe(viewLifecycleOwner) { resultado ->
            when (resultado) {
                is Resultado.Exito -> {
                    tareaPendiente = null
                    AnimacionesUtils.mostrarCelebracion(
                        binding.root,
                        AnimacionesUtils.GIF_TAREA_COMPLETADA,
                        "Misión completada",
                        "Tu tutor la revisará para entregarte la recompensa."
                    )
                    tareaViewModel.consumirCompletar()
                }
                is Resultado.Error -> {
                    Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_LONG).show()
                    tareaViewModel.consumirCompletar()
                }
                else -> {}
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAMANO_PAGINA = 5
        private const val DURACION_MINIMA_ESCANEO_MS = 1800L
    }
}
