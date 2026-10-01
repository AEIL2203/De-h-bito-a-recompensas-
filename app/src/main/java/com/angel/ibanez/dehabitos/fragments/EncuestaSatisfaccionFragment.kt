package com.angel.ibanez.dehabitos.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.angel.ibanez.dehabitos.R
import com.angel.ibanez.dehabitos.databinding.FragmentEncuestaSatisfaccionBinding
import com.angel.ibanez.dehabitos.models.EncuestaSatisfaccion
import com.angel.ibanez.dehabitos.models.Nino
import com.angel.ibanez.dehabitos.repository.TutorRepository
import com.angel.ibanez.dehabitos.utils.Resultado
import com.angel.ibanez.dehabitos.viewmodels.EncuestaViewModel
import com.angel.ibanez.dehabitos.viewmodels.NinoViewModel
import com.google.android.material.card.MaterialCardView
import com.google.android.material.slider.Slider
import kotlinx.coroutines.launch

class EncuestaSatisfaccionFragment : Fragment() {
    private var _binding: FragmentEncuestaSatisfaccionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EncuestaViewModel by viewModels()
    private val ninoViewModel: NinoViewModel by viewModels()
    private var ninos = emptyList<Nino>()
    private var seleccionado = ""
    private var tutorId = ""
    private val sliders = linkedMapOf<String, Slider>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEncuestaSatisfaccionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        seleccionado = arguments?.getString("ninoId").orEmpty()
        construirPreguntas()
        binding.btnEnviar.setOnClickListener { enviar() }

        ninoViewModel.listaNinosResultado.observe(viewLifecycleOwner) { resultado ->
            if (resultado is Resultado.Exito) {
                ninos = resultado.data
                binding.spNino.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, ninos.map { it.nombre })
                binding.spNino.setSelection(ninos.indexOfFirst { it.id == seleccionado }.coerceAtLeast(0))
                binding.spNino.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { seleccionado = ninos[position].id }
                    override fun onNothingSelected(parent: AdapterView<*>?) = Unit
                }
            }
        }
        viewModel.resultado.observe(viewLifecycleOwner) { resultado ->
            binding.progressBar.visibility = if (resultado is Resultado.Cargando) View.VISIBLE else View.GONE
            binding.btnEnviar.isEnabled = resultado !is Resultado.Cargando
            when (resultado) {
                is Resultado.Exito -> {
                    Toast.makeText(requireContext(), "Gracias por tu respuesta", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
                is Resultado.Error -> Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_LONG).show()
                else -> Unit
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            TutorRepository().obtenerTutorActual()?.let {
                tutorId = it.id
                ninoViewModel.cargarNinosPorTutor(it.id)
            }
        }
    }

    private fun construirPreguntas() {
        val densidad = resources.displayMetrics.density
        EncuestaSatisfaccion.PREGUNTAS.forEach { (clave, texto) ->
            val tarjeta = MaterialCardView(requireContext()).apply {
                radius = 24 * densidad
                cardElevation = 0f
                strokeWidth = (densidad).toInt()
                strokeColor = requireContext().getColor(R.color.md_theme_outline)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = (12 * densidad).toInt()
                }
            }
            val contenido = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding((16 * densidad).toInt(), (14 * densidad).toInt(), (16 * densidad).toInt(), (10 * densidad).toInt())
            }
            val etiqueta = TextView(requireContext()).apply {
                this.text = texto
                textSize = 15f
                setTextColor(requireContext().getColor(R.color.md_theme_onSurface))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
            val slider = Slider(requireContext()).apply {
                valueFrom = EncuestaSatisfaccion.VALOR_MINIMO.toFloat()
                valueTo = EncuestaSatisfaccion.VALOR_MAXIMO.toFloat()
                stepSize = 1f
                value = 3f
                labelBehavior = com.google.android.material.slider.LabelFormatter.LABEL_VISIBLE
            }
            contenido.addView(etiqueta)
            contenido.addView(slider)
            tarjeta.addView(contenido)
            binding.contenedorPreguntas.addView(tarjeta)
            sliders[clave] = slider
        }
    }

    private fun enviar() {
        if (seleccionado.isBlank() || tutorId.isBlank()) {
            Toast.makeText(requireContext(), "Selecciona un niño", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.enviar(
            EncuestaSatisfaccion(
                tutorId = tutorId,
                ninoId = seleccionado,
                fecha = System.currentTimeMillis(),
                respuestas = sliders.mapValues { it.value.value.toInt() },
                comentario = binding.etComentario.text?.toString()?.trim().orEmpty()
            )
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
