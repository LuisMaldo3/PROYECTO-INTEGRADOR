package com.tecsup.tcweek.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** El backend marca con este valor los mensajes que son recordatorios de una actividad inscrita. */
const val TIPO_AVISO_RECORDATORIO = "recordatorio_actividad"

/** El backend marca con este valor las alertas de cambio de horario de una actividad inscrita. */
const val TIPO_AVISO_CAMBIO_HORARIO = "cambio_horario"

private val ID_ACTIVIDAD = Regex("[a-zA-Z0-9_-]{1,100}")

/**
 * Aviso sobre una actividad tal como lo manda el backend. Solo contiene texto ya validado.
 * En un cambio de horario, [horarioAnterior] y [horarioNuevo] traen el texto listo para mostrar.
 */
data class AvisoActividad(
    val actividadId: String,
    val titulo: String,
    val cuerpo: String,
    val tipo: String = TIPO_AVISO_RECORDATORIO,
    val horarioAnterior: String = "",
    val horarioNuevo: String = ""
) {
    val esCambioHorario: Boolean get() = tipo == TIPO_AVISO_CAMBIO_HORARIO

    /** Texto que se muestra arriba del detalle de la actividad al abrirla desde el aviso. */
    val nota: String
        get() = if (esCambioHorario) "El horario cambió.\nAntes: $horarioAnterior\nAhora: $horarioNuevo" else ""
}

/**
 * Convierte los datos de un mensaje de Firebase en un aviso. Devuelve null si no es un aviso conocido
 * o si no trae una actividad válida: nunca se muestra algo que no se pueda abrir.
 */
fun leerAvisoActividad(datos: Map<String, String>): AvisoActividad? {
    val tipo = datos["tipo"]
    if (tipo != TIPO_AVISO_RECORDATORIO && tipo != TIPO_AVISO_CAMBIO_HORARIO) return null
    val actividadId = datos["actividadId"]?.trim().orEmpty()
    if (!esIdActividad(actividadId)) return null
    if (tipo == TIPO_AVISO_CAMBIO_HORARIO) {
        val anterior = datos["horarioAnterior"]?.trim().orEmpty().take(60)
        val nuevo = datos["horarioNuevo"]?.trim().orEmpty().take(60)
        // Sin el horario nuevo la alerta no dice nada útil.
        if (nuevo.isEmpty()) return null
        val antes = anterior.ifEmpty { "sin horario" }
        return AvisoActividad(
            actividadId = actividadId,
            titulo = datos["titulo"]?.trim().orEmpty().ifEmpty { "Cambió el horario de tu actividad" }.take(110),
            cuerpo = "Antes: $antes\nAhora: $nuevo",
            tipo = tipo,
            horarioAnterior = antes,
            horarioNuevo = nuevo
        )
    }
    return AvisoActividad(
        actividadId = actividadId,
        titulo = datos["titulo"]?.trim().orEmpty().ifEmpty { "Tu actividad empieza pronto" }.take(80),
        cuerpo = datos["cuerpo"]?.trim().orEmpty().ifEmpty { "Toca para ver los detalles." }.take(200)
    )
}

fun esIdActividad(texto: String?): Boolean = texto != null && ID_ACTIVIDAD.matches(texto)

/**
 * Un aviso nuevo del mismo tipo y actividad reemplaza al anterior en vez de apilarse; un cambio de horario
 * no borra el recordatorio de la misma actividad (y al revés).
 */
fun idNotificacionAviso(actividadId: String, tipo: String = TIPO_AVISO_RECORDATORIO): Int =
    if (tipo == TIPO_AVISO_RECORDATORIO) actividadId.hashCode() else "$tipo:$actividadId".hashCode()

/** Actividad a abrir y, si el aviso fue un cambio de horario, el texto con el horario anterior y el nuevo. */
data class AperturaAviso(val actividadId: String, val nota: String = "")

/**
 * Actividad que el estudiante pidió abrir al tocar un aviso. Vive fuera de las pantallas porque el toque
 * puede llegar antes de iniciar sesión o con la app cerrada: la pantalla principal la abre cuando ya puede.
 */
object AvisoPendiente {
    private val pendiente = MutableStateFlow<AperturaAviso?>(null)
    val apertura: StateFlow<AperturaAviso?> = pendiente

    /** Recuerda la actividad a abrir; un identificador inválido se ignora. */
    fun abrir(actividadId: String?, nota: String? = null) {
        if (esIdActividad(actividadId)) pendiente.value = AperturaAviso(actividadId!!, nota.orEmpty().take(300))
    }

    /** Se llama al cerrar el detalle, para que no se vuelva a abrir. */
    fun consumir() {
        pendiente.value = null
    }
}
