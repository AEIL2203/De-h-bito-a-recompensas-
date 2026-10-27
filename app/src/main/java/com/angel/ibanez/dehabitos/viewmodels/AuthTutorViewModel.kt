package com.angel.ibanez.dehabitos.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angel.ibanez.dehabitos.models.Tutor
import com.angel.ibanez.dehabitos.repository.TutorRepository
import com.angel.ibanez.dehabitos.utils.Resultado
import kotlinx.coroutines.launch

class AuthTutorViewModel(
    private val tutorRepository: TutorRepository = TutorRepository()
) : ViewModel() {

    private val _loginResultado = MutableLiveData<Resultado<Tutor>>()
    val loginResultado: LiveData<Resultado<Tutor>> = _loginResultado

    private val _validarPasswordResultado = MutableLiveData<Resultado<Unit>>()
    val validarPasswordResultado: LiveData<Resultado<Unit>> = _validarPasswordResultado

    fun iniciarSesionGoogle(idToken: String) {
        _loginResultado.value = Resultado.Cargando
        viewModelScope.launch {
            tutorRepository.iniciarSesionGoogle(idToken).collect {
                _loginResultado.value = it
            }
        }
    }

    fun validarCuentaGoogle(idToken: String, tutorId: String) {
        _validarPasswordResultado.value = Resultado.Cargando
        viewModelScope.launch {
            tutorRepository.validarCuentaGoogle(idToken, tutorId).collect {
                _validarPasswordResultado.value = it
            }
        }
    }
}
