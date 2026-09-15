package com.angel.ibanez.dehabitos.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angel.ibanez.dehabitos.models.Nino
import com.angel.ibanez.dehabitos.models.Tarea
import com.angel.ibanez.dehabitos.repository.TareaRepository
import com.angel.ibanez.dehabitos.utils.Resultado
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

class TareaViewModel(
    private val tareaRepository: TareaRepository = TareaRepository()
) : ViewModel() {

    private val _crearTareaResultado = MutableLiveData<Resultado<Tarea>>()
    val crearTareaResultado: LiveData<Resultado<Tarea>> = _crearTareaResultado

    private val _listaTareasResultado = MutableLiveData<Resultado<List<Tarea>>>()
    val listaTareasResultado: LiveData<Resultado<List<Tarea>>> = _listaTareasResultado

    private val _completarResultado = MutableLiveData<Resultado<Tarea>>()
    val completarResultado: LiveData<Resultado<Tarea>> = _completarResultado

    private val _validarResultado = MutableLiveData<Resultado<Nino>>()
    val validarResultado: LiveData<Resultado<Nino>> = _validarResultado

    private val _rechazarResultado = MutableLiveData<Resultado<Tarea>>()
    val rechazarResultado: LiveData<Resultado<Tarea>> = _rechazarResultado
    private var escuchaTareas: ListenerRegistration? = null

    fun crearTarea(tarea: Tarea) {
        _crearTareaResultado.value = Resultado.Cargando
        viewModelScope.launch {
            tareaRepository.crearTarea(tarea).collect { _crearTareaResultado.value = it }
        }
    }

    fun cargarTareasPorNino(ninoId: String) {
        _listaTareasResultado.value = Resultado.Cargando
        viewModelScope.launch {
            tareaRepository.obtenerTareasPorNino(ninoId).collect { _listaTareasResultado.value = it }
        }
    }

    fun escucharTareasPorNino(ninoId: String) {
        escuchaTareas?.remove()
        _listaTareasResultado.value = Resultado.Cargando
        escuchaTareas = tareaRepository.escucharTareasPorNino(ninoId) {
            _listaTareasResultado.postValue(it)
        }
    }

    fun cargarTareasPorTutor(tutorId: String) {
        _listaTareasResultado.value = Resultado.Cargando
        viewModelScope.launch {
            tareaRepository.obtenerTareasPorTutor(tutorId).collect { _listaTareasResultado.value = it }
        }
    }

    fun marcarCompletada(tarea: Tarea) {
        _completarResultado.value = Resultado.Cargando
        viewModelScope.launch {
            tareaRepository.marcarCompletada(tarea).collect { _completarResultado.value = it }
        }
    }

    fun validarTarea(tarea: Tarea) {
        _validarResultado.value = Resultado.Cargando
        viewModelScope.launch {
            tareaRepository.validarTarea(tarea).collect { _validarResultado.value = it }
        }
    }

    fun rechazarTarea(tarea: Tarea) {
        _rechazarResultado.value = Resultado.Cargando
        viewModelScope.launch {
            tareaRepository.rechazarTarea(tarea).collect { _rechazarResultado.value = it }
        }
    }

    override fun onCleared() {
        escuchaTareas?.remove()
        super.onCleared()
    }
}
