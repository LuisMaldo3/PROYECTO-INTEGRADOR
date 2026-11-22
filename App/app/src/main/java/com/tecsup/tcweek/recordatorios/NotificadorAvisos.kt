package com.tecsup.tcweek.recordatorios

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.tecsup.tcweek.MainActivity
import com.tecsup.tcweek.R
import com.tecsup.tcweek.data.AvisoActividad
import com.tecsup.tcweek.data.idNotificacionAviso

/**
 * Muestra en la barra de notificaciones los recordatorios, cambios, cancelaciones y recomendaciones.
 * Al tocarlos abre la actividad; en una cancelación abre la agenda.
 */
object NotificadorAvisos {
    private const val CANAL = "recordatorios_actividad"
    private const val CANAL_HORARIO = "cambios_horario"
    private const val CANAL_UBICACION = "cambios_ubicacion"
    private const val CANAL_CANCELACION = "cancelaciones"
    private const val CANAL_RECOMENDACION = "recomendaciones"

    /** Datos que viajan en el Intent de la notificación hacia MainActivity. */
    const val EXTRA_ACTIVIDAD_ID = "com.tecsup.tcweek.ACTIVIDAD_ID"
    const val EXTRA_NOTA = "com.tecsup.tcweek.NOTA_AVISO"
    const val EXTRA_ABRIR_AGENDA = "com.tecsup.tcweek.ABRIR_AGENDA"

    /** Las notificaciones de Android 13+ necesitan el permiso POST_NOTIFICATIONS, que se pide en MainActivity. */
    fun puedeNotificar(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** Devuelve false si no se pudo mostrar (permiso negado o notificaciones apagadas). */
    fun mostrar(context: Context, aviso: AvisoActividad): Boolean {
        if (!puedeNotificar(context)) return false
        crearCanal(context)
        val numero = idNotificacionAviso(aviso.actividadId, aviso.tipo)
        val abrir = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ACTIVIDAD_ID, aviso.actividadId)
            if (aviso.nota.isNotEmpty()) putExtra(EXTRA_NOTA, aviso.nota)
            if (aviso.abreAgenda) putExtra(EXTRA_ABRIR_AGENDA, true)
        }
        // El código de la petición cambia por actividad: dos recordatorios no deben compartir el mismo Intent.
        val alTocar = PendingIntent.getActivity(
            context, numero, abrir, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // Cambio de horario o de lugar: el valor anterior y el nuevo, cada uno en su línea.
        val estilo = if (aviso.esCambio) NotificationCompat.InboxStyle()
            .addLine("Antes: ${aviso.anterior}")
            .addLine("Ahora: ${aviso.nuevo}")
            .setBigContentTitle(aviso.titulo)
        else NotificationCompat.BigTextStyle().bigText(aviso.cuerpo)
        val canal = when {
            aviso.esCambioHorario -> CANAL_HORARIO
            aviso.esCambioUbicacion -> CANAL_UBICACION
            aviso.esCancelacion -> CANAL_CANCELACION
            aviso.esRecomendacion -> CANAL_RECOMENDACION
            else -> CANAL
        }
        val notificacion = NotificationCompat.Builder(context, canal)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setColor(0xFF0B2A5B.toInt())
            .setContentTitle(aviso.titulo)
            .setContentText(if (aviso.esCambio) "Ahora: ${aviso.nuevo}" else aviso.cuerpo)
            .setStyle(estilo)
            .setCategory(when {
                aviso.esRecomendacion -> NotificationCompat.CATEGORY_RECOMMENDATION
                aviso.esCambio || aviso.esCancelacion -> NotificationCompat.CATEGORY_EVENT
                else -> NotificationCompat.CATEGORY_REMINDER
            })
            // Una recomendación no es urgente: no interrumpe como una cancelación o un cambio.
            .setPriority(if (aviso.esRecomendacion) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(alTocar)
            .build()
        return try {
            NotificationManagerCompat.from(context).notify(numero, notificacion)
            true
        } catch (e: SecurityException) {
            false // el permiso se retiró justo ahora
        }
    }

    /** Android 8+: toda notificación pertenece a un canal; el estudiante puede ajustarlo en Ajustes. */
    fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val canal = NotificationChannel(CANAL, "Recordatorios de actividades", NotificationManager.IMPORTANCE_HIGH)
        canal.description = "Te avisa antes de que empiece una actividad en la que te inscribiste."
        val horario = NotificationChannel(CANAL_HORARIO, "Cambios de horario", NotificationManager.IMPORTANCE_HIGH)
        horario.description = "Te avisa cuando cambia la fecha u hora de una actividad en la que te inscribiste."
        val ubicacion = NotificationChannel(CANAL_UBICACION, "Cambios de ubicación", NotificationManager.IMPORTANCE_HIGH)
        ubicacion.description = "Te avisa cuando cambia el lugar de una actividad en la que te inscribiste."
        val cancelacion = NotificationChannel(CANAL_CANCELACION, "Actividades canceladas", NotificationManager.IMPORTANCE_HIGH)
        cancelacion.description = "Te avisa cuando se cancela una actividad en la que te inscribiste."
        val recomendacion = NotificationChannel(CANAL_RECOMENDACION, "Recomendaciones", NotificationManager.IMPORTANCE_DEFAULT)
        recomendacion.description = "Actividades disponibles que te pueden interesar (como máximo una al día)."
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannels(listOf(canal, horario, ubicacion, cancelacion, recomendacion))
    }
}
