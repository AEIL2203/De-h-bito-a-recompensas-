package com.angel.ibanez.dehabitos.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.angel.ibanez.dehabitos.adapters.AvataresAdapter
import com.angel.ibanez.dehabitos.databinding.FragmentPerfilNinoBinding
import com.angel.ibanez.dehabitos.models.Nino
import com.angel.ibanez.dehabitos.utils.AnimacionesUtils
import com.angel.ibanez.dehabitos.utils.AvataresUtils
import com.angel.ibanez.dehabitos.utils.DispositivoConfig
import com.angel.ibanez.dehabitos.utils.Resultado
import com.angel.ibanez.dehabitos.viewmodels.NinoViewModel

class PerfilNinoFragment : Fragment() {
    private var _binding: FragmentPerfilNinoBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NinoViewModel by viewModels()
    private var ninoId = ""
    private lateinit var avataresAdapter: AvataresAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPerfilNinoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ninoId = arguments?.getString("ninoId").orEmpty().ifBlank { DispositivoConfig.obtenerNinoId() }
        AnimacionesUtils.entradaSuave(binding.ivAvatar)
        configurarAvatares()
        viewModel.ninoResultado.observe(viewLifecycleOwner) { resultado ->
            if (resultado is Resultado.Exito) pintarPerfil(resultado.data)
            if (resultado is Resultado.Error) Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_LONG).show()
        }
        viewModel.avatarResultado.observe(viewLifecycleOwner) { resultado ->
            if (resultado is Resultado.Exito) Toast.makeText(requireContext(), "Avatar actualizado", Toast.LENGTH_SHORT).show()
            if (resultado is Resultado.Error) Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_LONG).show()
        }
        viewModel.escucharNino(ninoId)
    }

    private fun configurarAvatares() {
        avataresAdapter = AvataresAdapter { avatar ->
            AvataresUtils.cargar(binding.ivAvatar, avatar.id)
            AnimacionesUtils.pulso(binding.ivAvatar)
            viewModel.actualizarAvatar(ninoId, avatar.id)
        }
        binding.rvAvatares.layoutManager = GridLayoutManager(requireContext(), 4)
        binding.rvAvatares.adapter = avataresAdapter
    }

    private fun pintarPerfil(nino: Nino) {
        val avatar = AvataresUtils.obtener(nino.avatar)
        AvataresUtils.cargar(binding.ivAvatar, avatar.id)
        avataresAdapter.seleccionadoId = avatar.id
        binding.tvNombre.text = nino.nombre
        binding.tvProgreso.text = "Nivel ${nino.nivel} · ${nino.experiencia} XP"
        binding.tvEstrellas.text = "Estrellas: ${nino.estrellas}"
        binding.tvRacha.text = "Racha: ${nino.racha} días"
        binding.tvInsignias.text = "Insignias: ${nino.insignias.size}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
