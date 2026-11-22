package com.tecsup.tcweek.recordatorios

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.tecsup.tcweek.data.AuthRepository
import com.tecsup.tcweek.data.DispositivosApi
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Mantiene al backend al tanto de este celular para que pueda enviarle los recordatorios.
 * El token de Firebase se guarda aquí porque puede cambiar (onNewToken) cuando no hay sesión iniciada.
 */
class RegistroDispositivo(context: Context) {
    private val contexto = context.applicationContext
    private val prefs = contexto.getSharedPreferences("vynk_push", Context.MODE_PRIVATE)

    fun guardarToken(token: String) {
        prefs.edit().putString(TOKEN, token).apply()
    }

    /** Registra este celular con la sesión actual. Sin sesión no hace nada: se repite al volver a entrar. */
    suspend fun sincronizar() {
        val sesion = AuthRepository(contexto).currentSession() ?: return
        val token = tokenActual() ?: return
        try {
            DispositivosApi.registrar(sesion.llave, token)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Sin red o servidor caído: no es grave, se reintenta en la próxima apertura de la app.
            Log.w(ETIQUETA, "No se pudo registrar el celular para recordatorios", e)
        }
    }

    /** Antes de cerrar sesión: este celular deja de recibir los recordatorios de esa cuenta. */
    suspend fun olvidar(llaveSesion: String) {
        val token = prefs.getString(TOKEN, null)?.takeIf { it.isNotBlank() } ?: return
        try {
            DispositivosApi.quitar(llaveSesion, token)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Si falla, el siguiente estudiante que entre en este celular lo reasigna a su cuenta.
            Log.w(ETIQUETA, "No se pudo dar de baja el celular", e)
        }
    }

    private suspend fun tokenActual(): String? {
        val nuevo = tokenDeFirebase()
        if (nuevo != null) guardarToken(nuevo)
        return nuevo ?: prefs.getString(TOKEN, null)?.takeIf { it.isNotBlank() }
    }

    private suspend fun tokenDeFirebase(): String? = suspendCancellableCoroutine { continuacion ->
        try {
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { if (continuacion.isActive) continuacion.resume(it) }
                .addOnFailureListener { if (continuacion.isActive) continuacion.resume(null) }
        } catch (e: Exception) {
            if (continuacion.isActive) continuacion.resume(null)
        }
    }

    private companion object {
        const val ETIQUETA = "RegistroDispositivo"
        const val TOKEN = "token_fcm"
    }
}
