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
import com.tecsup.tcweek.data.TIPO_AVISO_CAMBIO_HORARIO
import com.tecsup.tcweek.data.idNotificacionAviso

/** Muestra recordatorios y cambios de horario en la barra de notificaciones; al tocarlos abre la actividad. */
object NotificadorAvisos {
    private const val CANAL = "recordatorios_actividad"
    private const val CANAL_HORARIO = "cambios_horario"

    /** Datos que viajan en el Intent de la notificación hacia MainActivity. */
    const val EXTRA_ACTIVIDAD_ID = "com.tecsup.tcweek.ACTIVIDAD_ID"
    const val EXTRA_NOTA = "com.tecsup.tcweek.NOTA_AVISO"

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
        }
        // El código de la petición cambia por actividad: dos recordatorios no deben compartir el mismo Intent.
        val alTocar = PendingIntent.getActivity(
            context, numero, abrir, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // Cambio de horario: el horario anterior y el nuevo, cada uno en su línea.
        val estilo = if (aviso.esCambioHorario) NotificationCompat.InboxStyle()
            .addLine("Antes: ${aviso.horarioAnterior}")
            .addLine("Ahora: ${aviso.horarioNuevo}")
            .setBigContentTitle(aviso.titulo)
        else NotificationCompat.BigTextStyle().bigText(aviso.cuerpo)
        val notificacion = NotificationCompat.Builder(context, if (aviso.esCambioHorario) CANAL_HORARIO else CANAL)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setColor(0xFF0B2A5B.toInt())
            .setContentTitle(aviso.titulo)
            .setContentText(if (aviso.esCambioHorario) "Ahora: ${aviso.horarioNuevo}" else aviso.cuerpo)
            .setStyle(estilo)
            .setCategory(if (aviso.esCambioHorario) NotificationCompat.CATEGORY_EVENT else NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
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
        context.getSystemService(NotificationManager::class.java).createNotificationChannels(listOf(canal, horario))
    }
}
