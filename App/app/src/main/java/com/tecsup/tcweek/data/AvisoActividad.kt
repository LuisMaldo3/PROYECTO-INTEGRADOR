package com.tecsup.tcweek.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** El backend marca con este valor los mensajes que son recordatorios de una actividad inscrita. */
const val TIPO_AVISO_RECORDATORIO = "recordatorio_actividad"

/** El backend marca con este valor las alertas de cambio de horario de una actividad inscrita. */
const val TIPO_AVISO_CAMBIO_HORARIO = "cambio_horario"

/** El backend marca con este valor las alertas de cambio de ubicación de una actividad inscrita. */
const val TIPO_AVISO_CAMBIO_UBICACION = "cambio_ubicacion"

/** El backend marca con este valor la notificación de que una actividad inscrita fue cancelada. */
const val TIPO_AVISO_CANCELACION = "cancelacion_actividad"

/** El backend marca con este valor la recomendación de una actividad disponible. */
const val TIPO_AVISO_RECOMENDACION = "recomendacion_actividad"

private val TIPOS_CONOCIDOS = setOf(
    TIPO_AVISO_RECORDATORIO, TIPO_AVISO_CAMBIO_HORARIO, TIPO_AVISO_CAMBIO_UBICACION,
    TIPO_AVISO_CANCELACION, TIPO_AVISO_RECOMENDACION
)

private val ID_ACTIVIDAD = Regex("[a-zA-Z0-9_-]{1,100}")

/**
 * Aviso sobre una actividad tal como lo manda el backend. Solo contiene texto ya validado.
 * En un cambio de horario o de ubicación, [anterior] y [nuevo] traen el texto listo para mostrar.
 */
data class AvisoActividad(
    val actividadId: String,
    val titulo: String,
    val cuerpo: String,
    val tipo: String = TIPO_AVISO_RECORDATORIO,
    val anterior: String = "",
    val nuevo: String = ""
) {
    val esCambioHorario: Boolean get() = tipo == TIPO_AVISO_CAMBIO_HORARIO
    val esCambioUbicacion: Boolean get() = tipo == TIPO_AVISO_CAMBIO_UBICACION
    /** Un cambio (de horario o de lugar): se muestra con el valor anterior y el nuevo. */
    val esCambio: Boolean get() = esCambioHorario || esCambioUbicacion
    val esCancelacion: Boolean get() = tipo == TIPO_AVISO_CANCELACION
    val esRecomendacion: Boolean get() = tipo == TIPO_AVISO_RECOMENDACION

    /** La actividad cancelada ya no se puede abrir: al tocar la notificación se muestra la agenda actualizada. */
    val abreAgenda: Boolean get() = esCancelacion

    /** Avisos que cambian la agenda del estudiante: la pantalla Agenda se vuelve a cargar al recibirlos. */
    val cambiaAgenda: Boolean get() = esCambio || esCancelacion

    /** Texto que se muestra arriba del detalle de la actividad al abrirla desde el aviso. */
    val nota: String
        get() = when {
            esCambioHorario -> "El horario cambió.\nAntes: $anterior\nAhora: $nuevo"
            esCambioUbicacion -> "El lugar cambió.\nAntes: $anterior\nAhora: $nuevo"
            else -> ""
        }
}

/**
 * Convierte los datos de un mensaje de Firebase en un aviso. Devuelve null si no es un aviso conocido
 * o si no trae una actividad válida: nunca se muestra algo que no se pueda abrir.
 */
fun leerAvisoActividad(datos: Map<String, String>): AvisoActividad? {
    val tipo = datos["tipo"]
    if (tipo !in TIPOS_CONOCIDOS) return null
    val actividadId = datos["actividadId"]?.trim().orEmpty()
    if (!esIdActividad(actividadId)) return null
    if (tipo == TIPO_AVISO_CAMBIO_HORARIO) return leerCambio(
        datos, actividadId, tipo, "horarioAnterior", "horarioNuevo",
        sinAnterior = "sin horario", tituloPorDefecto = "Cambió el horario de tu actividad"
    )
    if (tipo == TIPO_AVISO_CAMBIO_UBICACION) return leerCambio(
        datos, actividadId, tipo, "ubicacionAnterior", "ubicacionNueva",
        sinAnterior = "Ubicación por confirmar", tituloPorDefecto = "Cambió el lugar de tu actividad"
    )
    if (tipo == TIPO_AVISO_CANCELACION) return AvisoActividad(
        actividadId = actividadId,
        titulo = datos["titulo"]?.trim().orEmpty().ifEmpty { "Se canceló una de tus actividades" }.take(110),
        cuerpo = datos["cuerpo"]?.trim().orEmpty().ifEmpty { "La actividad fue cancelada. Ya no está en tu agenda." }.take(250),
        tipo = tipo
    )
    if (tipo == TIPO_AVISO_RECOMENDACION) return AvisoActividad(
        actividadId = actividadId,
        titulo = datos["titulo"]?.trim().orEmpty().ifEmpty { "Te puede interesar una actividad" }.take(110),
        cuerpo = datos["cuerpo"]?.trim().orEmpty().ifEmpty { "Toca para ver los detalles." }.take(250),
        tipo = tipo
    )
    return AvisoActividad(
        actividadId = actividadId,
        titulo = datos["titulo"]?.trim().orEmpty().ifEmpty { "Tu actividad empieza pronto" }.take(80),
        cuerpo = datos["cuerpo"]?.trim().orEmpty().ifEmpty { "Toca para ver los detalles." }.take(200)
    )
}

/** Alerta de cambio: sin el valor nuevo no dice nada útil y se ignora. */
private fun leerCambio(
    datos: Map<String, String>, actividadId: String, tipo: String, claveAnterior: String, claveNuevo: String,
    sinAnterior: String, tituloPorDefecto: String
): AvisoActividad? {
    val nuevo = datos[claveNuevo]?.trim().orEmpty().take(60)
    if (nuevo.isEmpty()) return null
    val anterior = datos[claveAnterior]?.trim().orEmpty().take(60).ifEmpty { sinAnterior }
    return AvisoActividad(
        actividadId = actividadId,
        titulo = datos["titulo"]?.trim().orEmpty().ifEmpty { tituloPorDefecto }.take(110),
        cuerpo = "Antes: $anterior\nAhora: $nuevo",
        tipo = tipo,
        anterior = anterior,
        nuevo = nuevo
    )
}

fun esIdActividad(texto: String?): Boolean = texto != null && ID_ACTIVIDAD.matches(texto)

/**
 * Un aviso nuevo del mismo tipo y actividad reemplaza al anterior en vez de apilarse; avisos de distinto tipo
 * (recordatorio, cambios, cancelación, recomendación) de la misma actividad no se borran entre sí.
 */
fun idNotificacionAviso(actividadId: String, tipo: String = TIPO_AVISO_RECORDATORIO): Int =
    if (tipo == TIPO_AVISO_RECORDATORIO) actividadId.hashCode() else "$tipo:$actividadId".hashCode()

/**
 * Qué abrir al tocar un aviso: la actividad (con el texto del cambio, si lo hubo) o, si [agenda], la agenda
 * (la actividad cancelada ya no se puede inscribir ni abrir).
 */
data class AperturaAviso(val actividadId: String, val nota: String = "", val agenda: Boolean = false)

/**
 * Actividad que el estudiante pidió abrir al tocar un aviso. Vive fuera de las pantallas porque el toque
 * puede llegar antes de iniciar sesión o con la app cerrada: la pantalla principal la abre cuando ya puede.
 */
object AvisoPendiente {
    private val pendiente = MutableStateFlow<AperturaAviso?>(null)
    val apertura: StateFlow<AperturaAviso?> = pendiente

    /** Recuerda la actividad a abrir (o la agenda); un identificador inválido se ignora. */
    fun abrir(actividadId: String?, nota: String? = null, agenda: Boolean = false) {
        if (esIdActividad(actividadId)) pendiente.value = AperturaAviso(actividadId!!, nota.orEmpty().take(300), agenda)
    }

    /** Se llama al cerrar el detalle, para que no se vuelva a abrir. */
    fun consumir() {
        pendiente.value = null
    }
}

/**
 * Avisa a la pantalla Agenda que debe volver a cargarse: llegó una cancelación o un cambio de horario o de lugar.
 * Así la actividad cancelada desaparece sin que el estudiante tenga que actualizar a mano.
 */
object CambiosAgenda {
    private val contador = MutableStateFlow(0)
    val version: StateFlow<Int> = contador

    fun avisar() {
        contador.value = contador.value + 1
    }
}
