package com.tecsup.tcweek.recordatorios

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.tecsup.tcweek.data.leerAvisoActividad
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Recibe lo que Firebase manda a este celular: recordatorios, cambios de horario o de lugar y cambios de token. */
class ServicioMensajes : FirebaseMessagingService() {
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Firebase cambió el token de este celular: se guarda y, si hay sesión, se avisa al backend. */
    override fun onNewToken(token: String) {
        RegistroDispositivo(this).guardarToken(token)
        alcance.launch { RegistroDispositivo(applicationContext).sincronizar() }
    }

    /** Llega con la app abierta, en segundo plano o cerrada, porque el backend envía solo «datos». */
    override fun onMessageReceived(message: RemoteMessage) {
        val aviso = leerAvisoActividad(message.data) ?: return
        NotificadorAvisos.mostrar(applicationContext, aviso)
    }

    override fun onDestroy() {
        alcance.cancel()
        super.onDestroy()
    }
}
