package com.tecsup.tcweek.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** El backend marca con este valor los mensajes que son recordatorios de una actividad inscrita. */
const val TIPO_AVISO_RECORDATORIO = "recordatorio_actividad"

private val ID_ACTIVIDAD = Regex("[a-zA-Z0-9_-]{1,100}")

/** Recordatorio de una actividad tal como lo manda el backend. Solo contiene texto ya validado. */
data class AvisoActividad(val actividadId: String, val titulo: String, val cuerpo: String)

/**
 * Convierte los datos de un mensaje de Firebase en un aviso. Devuelve null si no es un recordatorio
 * o si no trae una actividad válida: nunca se muestra algo que no se pueda abrir.
 */
fun leerAvisoActividad(datos: Map<String, String>): AvisoActividad? {
    if (datos["tipo"] != TIPO_AVISO_RECORDATORIO) return null
    val actividadId = datos["actividadId"]?.trim().orEmpty()
    if (!esIdActividad(actividadId)) return null
    return AvisoActividad(
        actividadId = actividadId,
        titulo = datos["titulo"]?.trim().orEmpty().ifEmpty { "Tu actividad empieza pronto" }.take(80),
        cuerpo = datos["cuerpo"]?.trim().orEmpty().ifEmpty { "Toca para ver los detalles." }.take(200)
    )
}

fun esIdActividad(texto: String?): Boolean = texto != null && ID_ACTIVIDAD.matches(texto)

/** La misma actividad siempre usa el mismo número, así un segundo aviso reemplaza al anterior en vez de apilarse. */
fun idNotificacionAviso(actividadId: String): Int = actividadId.hashCode()

/**
 * Actividad que el estudiante pidió abrir al tocar un recordatorio. Vive fuera de las pantallas porque el toque
 * puede llegar antes de iniciar sesión o con la app cerrada: la pantalla principal la abre cuando ya puede.
 */
object AvisoPendiente {
    private val pendiente = MutableStateFlow<String?>(null)
    val actividadId: StateFlow<String?> = pendiente

    /** Recuerda la actividad a abrir; un identificador inválido se ignora. */
    fun abrir(actividadId: String?) {
        if (esIdActividad(actividadId)) pendiente.value = actividadId
    }

    /** Se llama al cerrar el detalle, para que no se vuelva a abrir. */
    fun consumir() {
        pendiente.value = null
    }
}
