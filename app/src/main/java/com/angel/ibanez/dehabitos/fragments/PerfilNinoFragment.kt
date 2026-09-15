package com.angel.ibanez.dehabitos.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.angel.ibanez.dehabitos.R
import com.angel.ibanez.dehabitos.databinding.FragmentPerfilNinoBinding
import com.angel.ibanez.dehabitos.models.Nino
import com.angel.ibanez.dehabitos.utils.AnimacionesUtils
import com.angel.ibanez.dehabitos.utils.DispositivoConfig
import com.angel.ibanez.dehabitos.utils.IconosUtils
import com.angel.ibanez.dehabitos.utils.Resultado
import com.angel.ibanez.dehabitos.viewmodels.NinoViewModel

class PerfilNinoFragment : Fragment() {
    private var _binding: FragmentPerfilNinoBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NinoViewModel by viewModels()
    private var ninoId = ""

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
        binding.btnAvatarAventurero.setOnClickListener { guardarAvatar(AVATAR_AVENTURERO) }
        binding.btnAvatarGamer.setOnClickListener { guardarAvatar(AVATAR_GAMER) }
        binding.btnAvatarEspacial.setOnClickListener { guardarAvatar(AVATAR_ESPACIAL) }
        binding.btnAvatarAnimal.setOnClickListener { guardarAvatar(AVATAR_ANIMAL) }
    }

    private fun guardarAvatar(avatar: String) {
        binding.ivAvatar.icon = IconosUtils.crearIcono(requireContext(), avatar, R.color.white, 70)
        viewModel.actualizarAvatar(ninoId, avatar)
    }

    private fun pintarPerfil(nino: Nino) {
        val avatar = nino.avatar.ifBlank { AVATAR_AVENTURERO }
        binding.ivAvatar.icon = IconosUtils.crearIcono(requireContext(), avatar, R.color.white, 70)
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

    companion object {
        private const val AVATAR_AVENTURERO = "gmr-face"
        private const val AVATAR_GAMER = "gmr-sports-esports"
        private const val AVATAR_ESPACIAL = "gmr-rocket-launch"
        private const val AVATAR_ANIMAL = "gmr-pets"
    }
}
