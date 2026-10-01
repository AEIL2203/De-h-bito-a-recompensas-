package com.angel.ibanez.dehabitos.firebase

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import com.angel.ibanez.dehabitos.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

class GoogleAuthClient(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)

    suspend fun obtenerIdToken(): String {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(context.getString(R.string.default_web_client_id))
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        val credential = try {
            credentialManager.getCredential(context, request).credential
        } catch (e: NoCredentialException) {
            throw IllegalStateException("No se encontraron cuentas de Google disponibles. Agrega una cuenta en el dispositivo.", e)
        } catch (e: Exception) {
            throw IllegalStateException("No se pudo obtener la cuenta de Google", e)
        }
        if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw IllegalStateException("No se recibió una cuenta de Google válida")
        }
        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }
}
