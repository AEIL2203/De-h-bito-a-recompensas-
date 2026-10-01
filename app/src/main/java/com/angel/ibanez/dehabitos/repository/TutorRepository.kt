package com.angel.ibanez.dehabitos.repository

import com.angel.ibanez.dehabitos.firebase.FirebaseAuthService
import com.angel.ibanez.dehabitos.firebase.FirebaseFirestoreService
import com.angel.ibanez.dehabitos.models.Tutor
import com.angel.ibanez.dehabitos.utils.Resultado
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TutorRepository(
    private val authService: FirebaseAuthService = FirebaseAuthService(),
    private val firestoreService: FirebaseFirestoreService = FirebaseFirestoreService()
) {

    fun registrarTutor(
        nombre: String,
        correo: String,
        telefono: String,
        password: String
    ): Flow<Resultado<Tutor>> = flow {
        emit(Resultado.Cargando)
        try {
            val user = authService.registrarTutor(correo, password)
                ?: throw Exception("No se pudo crear el usuario")
            val fechaRegistro = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val tutor = Tutor(
                id = user.uid,
                nombre = nombre,
                correo = correo,
                telefono = telefono,
                fechaRegistro = fechaRegistro
            )
            firestoreService.guardarTutor(tutor)
            emit(Resultado.Exito(tutor))
        } catch (e: Exception) {
            Log.e("TutorRepository", "Error al registrar tutor", e)
            emit(Resultado.Error(e.message ?: "Error desconocido"))
        }
    }

    fun iniciarSesionTutor(correo: String, password: String): Flow<Resultado<Tutor>> = flow {
        emit(Resultado.Cargando)
        try {
            val user = authService.iniciarSesionTutor(correo, password)
                ?: throw Exception("Credenciales incorrectas")
            val tutor = firestoreService.obtenerTutor(user.uid)
                ?: throw Exception("No se encontraron datos del tutor")
            emit(Resultado.Exito(tutor))
        } catch (e: Exception) {
            Log.e("TutorRepository", "Error al iniciar sesión tutor", e)
            emit(Resultado.Error(e.message ?: "Error desconocido"))
        }
    }

    fun iniciarSesionGoogle(idToken: String): Flow<Resultado<Tutor>> = flow {
        emit(Resultado.Cargando)
        try {
            val user = authService.iniciarSesionGoogle(idToken)
                ?: throw Exception("No se pudo iniciar sesión con Google")
            val existente = firestoreService.obtenerTutor(user.uid)
            if (existente != null) {
                emit(Resultado.Exito(existente))
                return@flow
            }
            val correo = user.email.orEmpty()
            val tutor = Tutor(
                id = user.uid,
                nombre = user.displayName?.takeIf { it.isNotBlank() }
                    ?: correo.substringBefore('@').ifBlank { "Tutor" },
                correo = correo,
                telefono = "",
                fechaRegistro = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            )
            firestoreService.guardarTutor(tutor)
            emit(Resultado.Exito(tutor))
        } catch (e: Exception) {
            Log.e("TutorRepository", "Error al iniciar sesión con Google", e)
            emit(Resultado.Error(e.message ?: "No se pudo iniciar sesión con Google"))
        }
    }

    fun usaCuentaGoogle(): Boolean = authService.usaCuentaGoogle()

    fun validarCuentaGoogle(idToken: String, tutorId: String): Flow<Resultado<Unit>> = flow {
        emit(Resultado.Cargando)
        try {
            authService.validarCuentaGoogle(idToken, tutorId)
            emit(Resultado.Exito(Unit))
        } catch (e: Exception) {
            emit(Resultado.Error(e.message ?: "No se pudo confirmar la cuenta de Google"))
        }
    }

    fun validarPasswordTutor(password: String): Flow<Resultado<Unit>> = flow {
        emit(Resultado.Cargando)
        try {
            authService.validarPasswordTutor(password)
            emit(Resultado.Exito(Unit))
        } catch (e: Exception) {
            emit(Resultado.Error("Contraseña incorrecta"))
        }
    }

    fun cerrarSesion() {
        authService.cerrarSesion()
    }

    suspend fun obtenerTutorActual(): Tutor? {
        val user = authService.obtenerTutorActual() ?: return null
        return firestoreService.obtenerTutor(user.uid)
    }
}
