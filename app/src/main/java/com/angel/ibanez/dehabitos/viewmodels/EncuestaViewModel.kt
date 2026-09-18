package com.angel.ibanez.dehabitos.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angel.ibanez.dehabitos.models.EncuestaSatisfaccion
import com.angel.ibanez.dehabitos.repository.EstadisticasRepository
import com.angel.ibanez.dehabitos.utils.Resultado
import kotlinx.coroutines.launch

class EncuestaViewModel(private val repository: EstadisticasRepository = EstadisticasRepository()) : ViewModel() {
    private val _resultado = MutableLiveData<Resultado<String>>()
    val resultado: LiveData<Resultado<String>> = _resultado

    fun enviar(encuesta: EncuestaSatisfaccion) {
        _resultado.value = Resultado.Cargando
        viewModelScope.launch {
            runCatching { repository.guardarEncuesta(encuesta) }
                .onSuccess { _resultado.value = Resultado.Exito("Encuesta guardada") }
                .onFailure { _resultado.value = Resultado.Error(it.message ?: "No se pudo guardar la encuesta") }
        }
    }
}
