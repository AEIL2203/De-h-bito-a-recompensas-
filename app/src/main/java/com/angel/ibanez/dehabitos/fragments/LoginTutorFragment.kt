package com.angel.ibanez.dehabitos.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.angel.ibanez.dehabitos.R
import com.angel.ibanez.dehabitos.databinding.FragmentLoginTutorBinding
import com.angel.ibanez.dehabitos.utils.DispositivoConfig
import com.angel.ibanez.dehabitos.utils.Resultado
import com.angel.ibanez.dehabitos.viewmodels.AuthTutorViewModel

class LoginTutorFragment : Fragment() {

    private var _binding: FragmentLoginTutorBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthTutorViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginTutorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnIniciarSesion.setOnClickListener {
            val correo = binding.etCorreo.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (correo.isNotEmpty() && password.isNotEmpty()) {
                viewModel.iniciarSesionTutor(correo, password)
            } else {
                Toast.makeText(requireContext(), "Ingresa correo y contraseña", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.loginResultado.observe(viewLifecycleOwner) { resultado ->
            when (resultado) {
                is Resultado.Cargando -> mostrarCarga(true)
                is Resultado.Exito -> {
                    mostrarCarga(false)
                    val tutor = resultado.data
                    DispositivoConfig.guardarTutorId(tutor.id)
                    Toast.makeText(requireContext(), "Bienvenido", Toast.LENGTH_SHORT).show()
                    navegarTrasLogin()
                }
                is Resultado.Error -> {
                    mostrarCarga(false)
                    Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_LONG).show()
                }
            }
        }

        binding.tvIrRegistro.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.loginTutorFragment) {
                findNavController().navigate(R.id.action_loginTutorFragment_to_registroTutorFragment)
            }
        }
    }

    private fun navegarTrasLogin() {
        if (findNavController().currentDestination?.id != R.id.loginTutorFragment) return

        when (DispositivoConfig.obtenerModo()) {
            DispositivoConfig.ModoDispositivo.PADRE -> {
                findNavController().navigate(R.id.action_loginTutorFragment_to_dashboardTutorFragment)
            }
            DispositivoConfig.ModoDispositivo.HIJO -> {
                val ninoId = DispositivoConfig.obtenerNinoId()
                if (ninoId.isNotBlank()) {
                    val bundle = Bundle().apply { putString("ninoId", ninoId) }
                    findNavController().navigate(R.id.loginNinoFragment, bundle)
                } else {
                    findNavController().navigate(R.id.seleccionRolFragment)
                }
            }
            DispositivoConfig.ModoDispositivo.SIN_CONFIGURAR -> {
                findNavController().navigate(R.id.seleccionRolFragment)
            }
        }
    }

    private fun mostrarCarga(mostrar: Boolean) {
        binding.progressBar.visibility = if (mostrar) View.VISIBLE else View.GONE
        binding.btnIniciarSesion.isEnabled = !mostrar
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
