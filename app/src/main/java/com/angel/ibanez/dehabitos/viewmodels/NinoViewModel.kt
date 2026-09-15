package com.angel.ibanez.dehabitos.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angel.ibanez.dehabitos.models.Nino
import com.angel.ibanez.dehabitos.repository.NinoRepository
import com.angel.ibanez.dehabitos.utils.Resultado
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

class NinoViewModel(
    private val ninoRepository: NinoRepository = NinoRepository()
) : ViewModel() {

    private val _crearNinoResultado = MutableLiveData<Resultado<Nino>>()
    val crearNinoResultado: LiveData<Resultado<Nino>> = _crearNinoResultado

    private val _listaNinosResultado = MutableLiveData<Resultado<List<Nino>>>()
    val listaNinosResultado: LiveData<Resultado<List<Nino>>> = _listaNinosResultado

    private val _loginNinoResultado = MutableLiveData<Resultado<Nino>>()
    val loginNinoResultado: LiveData<Resultado<Nino>> = _loginNinoResultado

    private val _ninoResultado = MutableLiveData<Resultado<Nino>>()
    val ninoResultado: LiveData<Resultado<Nino>> = _ninoResultado

    private val _avatarResultado = MutableLiveData<Resultado<String>>()
    val avatarResultado: LiveData<Resultado<String>> = _avatarResultado
    private var escuchaNino: ListenerRegistration? = null

    fun crearNino(nino: Nino) {
        _crearNinoResultado.value = Resultado.Cargando
        viewModelScope.launch {
            ninoRepository.crearNino(nino).collect {
                _crearNinoResultado.value = it
            }
        }
    }

    fun actualizarAvatar(ninoId: String, avatar: String) {
        viewModelScope.launch {
            ninoRepository.actualizarAvatar(ninoId, avatar).collect {
                _avatarResultado.value = it
            }
        }
    }

    fun cargarNinosPorTutor(tutorId: String) {
        _listaNinosResultado.value = Resultado.Cargando
        viewModelScope.launch {
            ninoRepository.obtenerNinosPorTutor(tutorId).collect {
                _listaNinosResultado.value = it
            }
        }
    }

    fun iniciarSesionNino(pin: String) {
        _loginNinoResultado.value = Resultado.Cargando
        viewModelScope.launch {
            ninoRepository.iniciarSesionNino(pin).collect {
                _loginNinoResultado.value = it
            }
        }
    }

    fun cargarNinoPorId(ninoId: String) {
        _ninoResultado.value = Resultado.Cargando
        viewModelScope.launch {
            ninoRepository.obtenerNinoPorId(ninoId).collect {
                _ninoResultado.value = it
            }
        }
    }

    fun escucharNino(ninoId: String) {
        escuchaNino?.remove()
        _ninoResultado.value = Resultado.Cargando
        escuchaNino = ninoRepository.escucharNino(ninoId) {
            _ninoResultado.postValue(it)
        }
    }

    override fun onCleared() {
        escuchaNino?.remove()
        super.onCleared()
    }
}
