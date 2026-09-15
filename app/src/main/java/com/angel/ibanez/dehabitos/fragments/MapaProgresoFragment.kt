package com.angel.ibanez.dehabitos.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.angel.ibanez.dehabitos.R
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.angel.ibanez.dehabitos.databinding.FragmentMapaProgresoBinding
import com.angel.ibanez.dehabitos.utils.AnimacionesUtils
import com.angel.ibanez.dehabitos.utils.Resultado
import com.angel.ibanez.dehabitos.viewmodels.NinoViewModel

class MapaProgresoFragment : Fragment() {

    private var _binding: FragmentMapaProgresoBinding? = null
    private val binding get() = _binding!!
    private val ninoViewModel: NinoViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapaProgresoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AnimacionesUtils.entradaSuave(binding.gameMap, 16)

        val ninoId = arguments?.getString("ninoId") ?: ""
        if (ninoId.isEmpty()) {
            findNavController().navigateUp()
            return
        }

        ninoViewModel.ninoResultado.observe(viewLifecycleOwner) { resultado ->
            when (resultado) {
                is Resultado.Cargando -> binding.progressBar.visibility = View.VISIBLE
                is Resultado.Exito -> {
                    binding.progressBar.visibility = View.GONE
                    binding.gameMap.currentLevel = resultado.data.nivel
                }
                is Resultado.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_LONG).show()
                }
            }
        }

        ninoViewModel.escucharNino(ninoId)

        val videoUri = Uri.parse("android.resource://${requireContext().packageName}/${R.raw.camino_1}")
        binding.videoBackground.setVideoURI(videoUri)
        binding.videoBackground.setOnPreparedListener { mp ->
            mp.isLooping = true
            binding.videoBackground.start()
        }
        binding.videoBackground.setOnErrorListener { _, _, _ ->
            Toast.makeText(requireContext(), "No se pudo reproducir el video de fondo", Toast.LENGTH_LONG).show()
            true
        }
    }

    override fun onDestroyView() {
        binding.videoBackground.stopPlayback()
        _binding = null
        super.onDestroyView()
    }
}
