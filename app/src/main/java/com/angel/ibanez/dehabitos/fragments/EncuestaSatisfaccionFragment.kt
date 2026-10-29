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
import com.angel.ibanez.dehabitos.utils.TecladoUtils
import com.angel.ibanez.dehabitos.R
import com.angel.ibanez.dehabitos.databinding.FragmentEncuestaSatisfaccionBinding
import com.angel.ibanez.dehabitos.models.EncuestaSatisfaccion
import com.angel.ibanez.dehabitos.models.Nino
import com.angel.ibanez.dehabitos.repository.TutorRepository
import com.angel.ibanez.dehabitos.utils.Resultado
import com.angel.ibanez.dehabitos.viewmodels.EncuestaViewModel
import com.angel.ibanez.dehabitos.viewmodels.NinoViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.launch

class EncuestaSatisfaccionFragment : Fragment() {
    private var _binding: FragmentEncuestaSatisfaccionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EncuestaViewModel by viewModels()
    private val ninoViewModel: NinoViewModel by viewModels()
    private var ninos = emptyList<Nino>()
    private var seleccionado = ""
    private var tutorId = ""
    private val respuestas = linkedMapOf<String, Int>().apply {
        EncuestaSatisfaccion.PREGUNTAS.forEach { (clave, _) -> put(clave, 3) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEncuestaSatisfaccionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        seleccionado = arguments?.getString("ninoId").orEmpty()
        construirPreguntas()
        binding.etComentario.setHorizontallyScrolling(false)
        TecladoUtils.configurar(binding.etComentario)
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

    /** Cada pregunta es una tarjeta con 5 botones segmentados (1–5) en lugar de un slider. */
    private fun construirPreguntas() {
        val densidad = resources.displayMetrics.density
        fun dp(valor: Int) = (valor * densidad).toInt()
        EncuestaSatisfaccion.PREGUNTAS.forEachIndexed { indice, (clave, texto) ->
            val tarjeta = MaterialCardView(requireContext(), null, 0).apply {
                radius = 16 * densidad
                cardElevation = densidad
                strokeWidth = dp(1)
                strokeColor = requireContext().getColor(R.color.md_theme_outline_10)
                setCardBackgroundColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = dp(10)
                }
            }
            val contenido = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(14), dp(16), dp(14))
            }
            val numero = TextView(requireContext()).apply {
                text = "Pregunta ${indice + 1} de ${EncuestaSatisfaccion.PREGUNTAS.size}"
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelMedium)
                setTextColor(MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary))
            }
            val etiqueta = TextView(requireContext()).apply {
                text = texto
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)
                setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface))
                setPadding(0, dp(4), 0, dp(12))
            }
            val grupo = MaterialButtonToggleGroup(requireContext()).apply {
                isSingleSelection = true
                isSelectionRequired = true
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            (EncuestaSatisfaccion.VALOR_MINIMO..EncuestaSatisfaccion.VALOR_MAXIMO).forEach { valor ->
                val boton = MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                    id = View.generateViewId()
                    text = valor.toString()
                    textSize = 16f
                    tag = valor
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                grupo.addView(boton)
                if (valor == respuestas[clave]) grupo.check(boton.id)
            }
            grupo.addOnButtonCheckedListener { g, id, marcado ->
                if (marcado) respuestas[clave] = g.findViewById<MaterialButton>(id).tag as Int
            }
            val extremos = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(6), 0, 0)
                listOf("En desacuerdo" to android.view.Gravity.START, "De acuerdo" to android.view.Gravity.END).forEach { (txt, gravedad) ->
                    addView(TextView(requireContext()).apply {
                        text = txt
                        gravity = gravedad
                        setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelSmall)
                        setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant))
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    })
                }
            }
            contenido.addView(numero)
            contenido.addView(etiqueta)
            contenido.addView(grupo)
            contenido.addView(extremos)
            tarjeta.addView(contenido)
            binding.contenedorPreguntas.addView(tarjeta)
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
                respuestas = respuestas.toMap(),
                comentario = binding.etComentario.text?.toString()?.trim().orEmpty()
            )
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
