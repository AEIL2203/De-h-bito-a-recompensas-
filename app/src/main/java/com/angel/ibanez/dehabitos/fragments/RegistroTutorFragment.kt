package com.angel.ibanez.dehabitos.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.angel.ibanez.dehabitos.R
import com.angel.ibanez.dehabitos.databinding.FragmentRegistroTutorBinding
import com.angel.ibanez.dehabitos.firebase.GoogleAuthClient
import com.angel.ibanez.dehabitos.utils.DispositivoConfig
import com.angel.ibanez.dehabitos.utils.Resultado
import com.angel.ibanez.dehabitos.viewmodels.AuthTutorViewModel
import kotlinx.coroutines.launch

class RegistroTutorFragment : Fragment() {

    private var _binding: FragmentRegistroTutorBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthTutorViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegistroTutorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnRegistrar.setOnClickListener {
            val nombre = binding.etNombre.text.toString().trim()
            val correo = binding.etCorreo.text.toString().trim()
            val telefono = binding.etTelefono.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (validarCampos(nombre, correo, telefono, password)) {
                viewModel.registrarTutor(nombre, correo, telefono, password)
            }
        }

        binding.btnGoogle.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    mostrarCarga(true)
                    val idToken = GoogleAuthClient(requireActivity()).obtenerIdToken()
                    viewModel.iniciarSesionGoogle(idToken)
                } catch (_: GetCredentialCancellationException) {
                    mostrarCarga(false)
                } catch (e: Exception) {
                    mostrarCarga(false)
                    Toast.makeText(requireContext(), e.message ?: "No se pudo abrir Google", Toast.LENGTH_LONG).show()
                }
            }
        }

        viewModel.registroResultado.observe(viewLifecycleOwner) { resultado ->
            when (resultado) {
                is Resultado.Cargando -> mostrarCarga(true)
                is Resultado.Exito -> {
                    mostrarCarga(false)
                    Toast.makeText(requireContext(), "Registro exitoso", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_registroTutorFragment_to_loginTutorFragment)
                }
                is Resultado.Error -> {
                    mostrarCarga(false)
                    Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_LONG).show()
                }
            }
        }

        viewModel.loginResultado.observe(viewLifecycleOwner) { resultado ->
            when (resultado) {
                is Resultado.Cargando -> mostrarCarga(true)
                is Resultado.Exito -> {
                    mostrarCarga(false)
                    DispositivoConfig.guardarTutorId(resultado.data.id)
                    Toast.makeText(requireContext(), "Cuenta de Google vinculada", Toast.LENGTH_SHORT).show()
                    if (findNavController().currentDestination?.id == R.id.registroTutorFragment) {
                        findNavController().navigate(R.id.action_registroTutorFragment_to_dashboardTutorFragment)
                    }
                }
                is Resultado.Error -> {
                    mostrarCarga(false)
                    Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_LONG).show()
                }
            }
        }

        binding.tvIrLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registroTutorFragment_to_loginTutorFragment)
        }
    }

    private fun validarCampos(nombre: String, correo: String, telefono: String, password: String): Boolean {
        if (nombre.isEmpty() || correo.isEmpty() || telefono.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.length < 6) {
            Toast.makeText(requireContext(), "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun mostrarCarga(mostrar: Boolean) {
        binding.progressBar.visibility = if (mostrar) View.VISIBLE else View.GONE
        binding.btnRegistrar.isEnabled = !mostrar
        binding.btnGoogle.isEnabled = !mostrar
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
