package com.tecsup.tcweek.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** El backend marca con este valor los mensajes que son recordatorios de una actividad inscrita. */
const val TIPO_AVISO_RECORDATORIO = "recordatorio_actividad"

/** El backend marca con este valor las alertas de cambio de horario de una actividad inscrita. */
const val TIPO_AVISO_CAMBIO_HORARIO = "cambio_horario"

/** El backend marca con este valor las alertas de cambio de ubicación de una actividad inscrita. */
const val TIPO_AVISO_CAMBIO_UBICACION = "cambio_ubicacion"

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
    if (tipo != TIPO_AVISO_RECORDATORIO && tipo != TIPO_AVISO_CAMBIO_HORARIO && tipo != TIPO_AVISO_CAMBIO_UBICACION) return null
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
 * (recordatorio, cambio de horario, cambio de lugar) de la misma actividad no se borran entre sí.
 */
fun idNotificacionAviso(actividadId: String, tipo: String = TIPO_AVISO_RECORDATORIO): Int =
    if (tipo == TIPO_AVISO_RECORDATORIO) actividadId.hashCode() else "$tipo:$actividadId".hashCode()

/** Actividad a abrir y, si el aviso fue un cambio, el texto con el valor anterior y el nuevo. */
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
