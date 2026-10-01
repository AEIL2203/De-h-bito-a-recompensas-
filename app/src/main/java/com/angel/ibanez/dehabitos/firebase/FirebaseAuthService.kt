package com.angel.ibanez.dehabitos.firebase

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class FirebaseAuthService {

    private val auth = FirebaseAuth.getInstance()

    suspend fun registrarTutor(correo: String, password: String): FirebaseUser? {
        val resultado = auth.createUserWithEmailAndPassword(correo, password).await()
        return resultado.user
    }

    suspend fun iniciarSesionTutor(correo: String, password: String): FirebaseUser? {
        val resultado = auth.signInWithEmailAndPassword(correo, password).await()
        return resultado.user
    }

    suspend fun iniciarSesionGoogle(idToken: String): FirebaseUser? {
        val credencial = GoogleAuthProvider.getCredential(idToken, null)
        return auth.signInWithCredential(credencial).await().user
    }

    suspend fun validarCuentaGoogle(idToken: String, tutorIdEsperado: String) {
        val user = auth.currentUser
            ?: throw Exception("La sesión del tutor no está disponible")
        if (user.uid != tutorIdEsperado) throw Exception("La sesión no corresponde al tutor vinculado")
        val credencial = GoogleAuthProvider.getCredential(idToken, null)
        try {
            user.reauthenticate(credencial).await()
        } catch (_: Exception) {
            throw Exception("Selecciona la cuenta de Google del tutor")
        }
    }

    fun usaCuentaGoogle(): Boolean = auth.currentUser?.providerData
        ?.any { it.providerId == GoogleAuthProvider.PROVIDER_ID } == true

    suspend fun validarPasswordTutor(password: String) {
        val user = auth.currentUser ?: throw Exception("La sesión del tutor no está disponible")
        val correo = user.email ?: throw Exception("El tutor no tiene correo registrado")
        user.reauthenticate(EmailAuthProvider.getCredential(correo, password)).await()
    }

    fun cerrarSesion() {
        auth.signOut()
    }

    fun obtenerTutorActual(): FirebaseUser? {
        return auth.currentUser
    }
}
