package com.angel.ibanez.dehabitos.fragments

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import com.angel.ibanez.dehabitos.databinding.FragmentMisionesNinoBinding
import com.angel.ibanez.dehabitos.models.Tarea
import com.angel.ibanez.dehabitos.utils.AnimacionesUtils
import com.angel.ibanez.dehabitos.utils.ImagenesUtils
import com.angel.ibanez.dehabitos.utils.Resultado
import com.angel.ibanez.dehabitos.viewmodels.TareaViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MisionesNinoFragment : Fragment() {

    private var _binding: FragmentMisionesNinoBinding? = null
    private val binding get() = _binding!!

    private val tareaViewModel: TareaViewModel by viewModels()
    private var ninoId: String = ""
    private lateinit var misionAdapter: MisionAdapter
    private var tareaPendiente: Tarea? = null

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
        binding.rvMisiones.adapter = misionAdapter

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
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(requireContext().contentResolver, uri))
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
        }
    }.getOrNull()

    private fun completarConBitmap(bitmap: Bitmap) {
        val tarea = tareaPendiente ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val evidencia = withContext(Dispatchers.Default) { ImagenesUtils.comprimirABase64(bitmap) }
            tareaViewModel.marcarCompletada(tarea, evidencia)
        }
    }

    private fun observarMisiones() {
        tareaViewModel.listaTareasResultado.observe(viewLifecycleOwner) { resultado ->
            when (resultado) {
                is Resultado.Exito -> {
                    val misiones = resultado.data.sortedWith(
                        compareBy<Tarea> { it.estado != Tarea.ESTADO_PENDIENTE }
                            .thenBy { if (it.estado == Tarea.ESTADO_PENDIENTE) it.fechaLimite.ifBlank { "9999-99-99" } else "" }
                    )
                    misionAdapter.submitList(misiones)
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
}
