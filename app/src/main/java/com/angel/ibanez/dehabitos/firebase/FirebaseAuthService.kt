package com.angel.ibanez.dehabitos.firebase

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
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
