package com.angel.ibanez.dehabitos.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angel.ibanez.dehabitos.models.Tarea
import com.angel.ibanez.dehabitos.models.Validacion
import com.angel.ibanez.dehabitos.firebase.FirebaseFirestoreService
import com.angel.ibanez.dehabitos.repository.TareaRepository
import com.angel.ibanez.dehabitos.utils.Resultado
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TareaViewModel(
    private val tareaRepository: TareaRepository = TareaRepository()
) : ViewModel() {

    private val _crearTareaResultado = MutableLiveData<Resultado<Tarea>>()
    val crearTareaResultado: LiveData<Resultado<Tarea>> = _crearTareaResultado

    private val _listaTareasResultado = MutableLiveData<Resultado<List<Tarea>>>()
    val listaTareasResultado: LiveData<Resultado<List<Tarea>>> = _listaTareasResultado

    private val _completarResultado = MutableLiveData<Resultado<Tarea>?>()
    val completarResultado: LiveData<Resultado<Tarea>?> = _completarResultado

    private val _validarResultado = MutableLiveData<Resultado<Validacion>?>()
    val validarResultado: LiveData<Resultado<Validacion>?> = _validarResultado

    private val _rechazarResultado = MutableLiveData<Resultado<Tarea>?>()
    val rechazarResultado: LiveData<Resultado<Tarea>?> = _rechazarResultado
    private var escuchaTareas: ListenerRegistration? = null
    private var reintentosEscucha = 0
    private var sondeoTareas: Job? = null
    private var ultimasTareas: List<Tarea> = emptyList()
    private val firestoreService = FirebaseFirestoreService()

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
        escuchaTareas = tareaRepository.escucharTareasPorNino(ninoId) { resultado ->
            when (resultado) {
                is Resultado.Exito -> {
                    reintentosEscucha = 0
                    ultimasTareas = resultado.data
                    _listaTareasResultado.postValue(resultado)
                }
                is Resultado.Error -> {
                    _listaTareasResultado.postValue(resultado)
                    if (reintentosEscucha < MAX_REINTENTOS_ESCUCHA) {
                        reintentosEscucha++
                        viewModelScope.launch {
                            delay(3000)
                            escucharTareasPorNino(ninoId)
                        }
                    }
                }
                else -> _listaTareasResultado.postValue(resultado)
            }
        }
    }

    fun escucharTareasPorTutor(tutorId: String) {
        escuchaTareas?.remove()
        _listaTareasResultado.value = Resultado.Cargando
        escuchaTareas = tareaRepository.escucharTareasPorTutor(tutorId) { resultado ->
            when (resultado) {
                is Resultado.Exito -> {
                    reintentosEscucha = 0
                    ultimasTareas = resultado.data
                    _listaTareasResultado.postValue(resultado)
                }
                is Resultado.Error -> {
                    _listaTareasResultado.postValue(resultado)
                    if (reintentosEscucha < MAX_REINTENTOS_ESCUCHA) {
                        reintentosEscucha++
                        viewModelScope.launch {
                            delay(3000)
                            escucharTareasPorTutor(tutorId)
                        }
                    }
                }
                else -> _listaTareasResultado.postValue(resultado)
            }
        }
    }

    fun iniciarSondeoTareas(ninoId: String, intervaloMs: Long = 10_000) {
        if (sondeoTareas?.isActive == true) return
        sondeoTareas = viewModelScope.launch {
            while (isActive) {
                delay(intervaloMs)
                runCatching { firestoreService.obtenerTareasPorNinoDesdeServidor(ninoId) }
                    .onSuccess { lista ->
                        if (lista != ultimasTareas) {
                            ultimasTareas = lista
                            _listaTareasResultado.postValue(Resultado.Exito(lista))
                        }
                    }
            }
        }
    }

    fun detenerSondeoTareas() {
        sondeoTareas?.cancel()
        sondeoTareas = null
    }

    fun cargarTareasPorTutor(tutorId: String) {
        _listaTareasResultado.value = Resultado.Cargando
        viewModelScope.launch {
            tareaRepository.obtenerTareasPorTutor(tutorId).collect { _listaTareasResultado.value = it }
        }
    }

    fun marcarCompletada(tarea: Tarea, evidencia: String = "") {
        _completarResultado.value = Resultado.Cargando
        viewModelScope.launch {
            tareaRepository.marcarCompletada(tarea, evidencia).collect { _completarResultado.value = it }
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

    fun consumirCompletar() { _completarResultado.value = null }
    fun consumirValidar() { _validarResultado.value = null }
    fun consumirRechazar() { _rechazarResultado.value = null }

    override fun onCleared() {
        escuchaTareas?.remove()
        detenerSondeoTareas()
        super.onCleared()
    }

    companion object {
        private const val MAX_REINTENTOS_ESCUCHA = 5
    }
}
