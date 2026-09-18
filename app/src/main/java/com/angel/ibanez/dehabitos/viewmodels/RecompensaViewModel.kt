package com.angel.ibanez.dehabitos.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angel.ibanez.dehabitos.models.AplicacionCatalogo
import com.angel.ibanez.dehabitos.models.AplicacionPermitida
import com.angel.ibanez.dehabitos.models.Nino
import com.angel.ibanez.dehabitos.models.Recompensa
import com.angel.ibanez.dehabitos.repository.RecompensaRepository
import com.angel.ibanez.dehabitos.utils.Resultado
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

class RecompensaViewModel(
    private val repository: RecompensaRepository = RecompensaRepository()
) : ViewModel() {

    private val _catalogoResultado = MutableLiveData<Resultado<List<AplicacionCatalogo>>>()
    val catalogoResultado: LiveData<Resultado<List<AplicacionCatalogo>>> = _catalogoResultado

    private val _sincronizarCatalogoResultado = MutableLiveData<Resultado<Unit>>()
    val sincronizarCatalogoResultado: LiveData<Resultado<Unit>> = _sincronizarCatalogoResultado

    private val _appsResultado = MutableLiveData<Resultado<List<AplicacionPermitida>>>()
    val appsResultado: LiveData<Resultado<List<AplicacionPermitida>>> = _appsResultado

    private val _guardarResultado = MutableLiveData<Resultado<AplicacionPermitida>>()
    val guardarResultado: LiveData<Resultado<AplicacionPermitida>> = _guardarResultado

    private val _eliminarResultado = MutableLiveData<Resultado<Unit>>()
    val eliminarResultado: LiveData<Resultado<Unit>> = _eliminarResultado

    private val _canjeResultado = MutableLiveData<Resultado<Recompensa>?>()
    val canjeResultado: LiveData<Resultado<Recompensa>?> = _canjeResultado

    private var listener: ListenerRegistration? = null

    fun cargarCatalogoApps(ninoId: String) {
        _catalogoResultado.value = Resultado.Cargando
        viewModelScope.launch {
            repository.obtenerCatalogoAppsPorNino(ninoId).collect { _catalogoResultado.value = it }
        }
    }

    fun sincronizarCatalogoApps(ninoId: String, deviceId: String, apps: List<AplicacionCatalogo>) {
        _sincronizarCatalogoResultado.value = Resultado.Cargando
        viewModelScope.launch {
            repository.sincronizarCatalogoApps(ninoId, deviceId, apps)
                .collect { _sincronizarCatalogoResultado.value = it }
        }
    }

    fun cargarAppsPermitidas(ninoId: String) {
        _appsResultado.value = Resultado.Cargando
        viewModelScope.launch {
            repository.obtenerAppsPermitidasPorNino(ninoId).collect { _appsResultado.value = it }
        }
    }

    fun guardarApp(app: AplicacionPermitida) {
        _guardarResultado.value = Resultado.Cargando
        viewModelScope.launch {
            repository.guardarAplicacionPermitida(app).collect { _guardarResultado.value = it }
        }
    }

    fun eliminarApp(appId: String) {
        _eliminarResultado.value = Resultado.Cargando
        viewModelScope.launch {
            repository.eliminarAplicacionPermitida(appId).collect { _eliminarResultado.value = it }
        }
    }

    fun canjear(nino: Nino, app: AplicacionPermitida, minutos: Int, estrellasCosto: Int, fechaCanje: Long) {
        _canjeResultado.value = Resultado.Cargando
        viewModelScope.launch {
            repository.canjearRecompensa(nino, app, minutos, estrellasCosto, fechaCanje).collect { _canjeResultado.value = it }
        }
    }

    fun consumirCanje() { _canjeResultado.value = null }

    fun escucharAppsPermitidas(ninoId: String) {
        listener?.remove()
        listener = repository.escucharAppsPermitidasPorNino(
            ninoId,
            onUpdate = { _appsResultado.postValue(Resultado.Exito(it)) },
            onError = { _appsResultado.postValue(Resultado.Error(it.message ?: "Error al actualizar las apps")) }
        )
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
