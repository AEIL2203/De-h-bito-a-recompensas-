package com.angel.ibanez.dehabitos.firebase

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.angel.ibanez.dehabitos.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

class GoogleAuthClient(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)

    private companion object {
        const val TAG = "GoogleAuthClient"
    }

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
        } catch (e: GetCredentialCancellationException) {
            throw e
        } catch (e: NoCredentialException) {
            Log.e(TAG, "NoCredentialException: type=${e.type} msg=${e.message}", e)
            throw IllegalStateException("No se encontraron cuentas de Google disponibles en este dispositivo. (${e.message ?: "sin detalles"})", e)
        } catch (e: Exception) {
            Log.e(TAG, "GetCredential fallo: ${e.javaClass.name} msg=${e.message}", e)
            throw IllegalStateException("No se pudo obtener la cuenta de Google: ${e.message ?: e.javaClass.simpleName}", e)
        }
        if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw IllegalStateException("No se recibió una cuenta de Google válida")
        }
        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }
}

