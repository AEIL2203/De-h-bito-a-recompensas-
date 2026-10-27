package com.angel.ibanez.dehabitos.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class FirebaseAuthService {

    private val auth = FirebaseAuth.getInstance()

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

    fun cerrarSesion() {
        auth.signOut()
    }

    fun obtenerTutorActual(): FirebaseUser? {
        return auth.currentUser
    }
}
