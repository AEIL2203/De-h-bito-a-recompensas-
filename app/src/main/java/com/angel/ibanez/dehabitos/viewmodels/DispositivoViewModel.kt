package com.angel.ibanez.dehabitos.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angel.ibanez.dehabitos.models.Dispositivo
import com.angel.ibanez.dehabitos.repository.DispositivoRepository
import com.angel.ibanez.dehabitos.utils.Resultado
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

class DispositivoViewModel(
    private val repository: DispositivoRepository = DispositivoRepository()
) : ViewModel() {

    private val _dispositivosResultado = MutableLiveData<Resultado<List<Dispositivo>>>()
    val dispositivosResultado: LiveData<Resultado<List<Dispositivo>>> = _dispositivosResultado

    private val _cambioEstadoResultado = MutableLiveData<Resultado<Unit>>()
    val cambioEstadoResultado: LiveData<Resultado<Unit>> = _cambioEstadoResultado

    private val _desvincularResultado = MutableLiveData<Resultado<Unit>>()
    val desvincularResultado: LiveData<Resultado<Unit>> = _desvincularResultado

    private val _desbloqueado = MutableLiveData<Boolean>()
    val desbloqueado: LiveData<Boolean> = _desbloqueado

    private val _vinculado = MutableLiveData<Boolean>()
    val vinculado: LiveData<Boolean> = _vinculado

    private var listener: ListenerRegistration? = null

    fun cargarDispositivos(tutorId: String) {
        _dispositivosResultado.value = Resultado.Cargando
        viewModelScope.launch {
            repository.obtenerDispositivosPorTutor(tutorId).collect { _dispositivosResultado.value = it }
        }
    }

    fun cambiarEstadoDesbloqueo(dispositivoId: String, desbloqueado: Boolean) {
        _cambioEstadoResultado.value = Resultado.Cargando
        viewModelScope.launch {
            repository.cambiarEstadoDesbloqueo(dispositivoId, desbloqueado).collect { _cambioEstadoResultado.value = it }
        }
    }

    fun desvincularDispositivo(dispositivoId: String) {
        _desvincularResultado.value = Resultado.Cargando
        viewModelScope.launch {
            repository.desvincularDispositivo(dispositivoId).collect { _desvincularResultado.value = it }
        }
    }

    fun iniciarEscuchaDispositivo(dispositivoId: String) {
        listener?.remove()
        listener = repository.escucharDispositivo(dispositivoId) { dispositivo ->
            _vinculado.postValue(dispositivo != null)
            _desbloqueado.postValue(dispositivo?.desbloqueado ?: false)
        }
    }

    fun detenerEscucha() {
        listener?.remove()
        listener = null
    }

    override fun onCleared() {
        super.onCleared()
        detenerEscucha()
    }
}
