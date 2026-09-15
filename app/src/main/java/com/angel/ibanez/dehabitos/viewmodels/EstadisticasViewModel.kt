package com.angel.ibanez.dehabitos.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angel.ibanez.dehabitos.models.EstadisticasNino
import com.angel.ibanez.dehabitos.repository.EstadisticasRepository
import com.angel.ibanez.dehabitos.utils.Resultado
import kotlinx.coroutines.launch

class EstadisticasViewModel(private val repository: EstadisticasRepository = EstadisticasRepository()) : ViewModel() {
    private val _resultado = MutableLiveData<Resultado<EstadisticasNino>>()
    val resultado: LiveData<Resultado<EstadisticasNino>> = _resultado

    fun cargar(ninoId: String, ultimosSieteDias: Boolean) {
        _resultado.value = Resultado.Cargando
        viewModelScope.launch {
            runCatching {
                val desde = if (ultimosSieteDias) System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L else null
                repository.obtener(ninoId, desde)
            }.onSuccess { _resultado.value = Resultado.Exito(it) }
                .onFailure { _resultado.value = Resultado.Error(it.message ?: "No se pudieron cargar las estadísticas") }
        }
    }
}
