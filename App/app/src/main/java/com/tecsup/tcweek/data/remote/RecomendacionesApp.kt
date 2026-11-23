package com.tecsup.tcweek.data.remote

import com.tecsup.tcweek.data.repository.CargaProgramada
import com.tecsup.tcweek.data.repository.pedirJson
import com.tecsup.tcweek.domain.CompatibilidadHorario
import java.time.Instant
import org.json.JSONObject

/**
 * US-76: una recomendación de IA con la actividad completa del catálogo (para abrir su detalle e inscribirse, T06),
 * el motivo, etiquetas cortas de «por qué» y si se puede inscribir en un toque (no por equipos y con cupos).
 */
data class RecomendacionIa(
    val actividad: ActividadCatalogo,
    val motivo: String,
    val etiquetas: List<String> = emptyList(),
    val horario: CompatibilidadHorario = CompatibilidadHorario(),
    val inscripcionDirecta: Boolean = true
) {
    val porEquipos get() = actividad.tipoActividad == "competencia" && actividad.modalidad == "equipos"
}

data class PaqueteRecomendaciones(
    val generadoEn: Long,
    val vigenteHasta: Long,
    val origen: String,
    val conHorario: Boolean,
    val aviso: String,
    val recomendaciones: List<RecomendacionIa>
) {
    val conIa get() = origen == "ia"
}

/**
 * US-76 T2: cliente de GET /api/app/recomendaciones.
 * Descartar: [RecomendacionesApi.descartar]; inscribirse: [InscripcionApi.inscribir].
 */
object RecomendacionesAppApi {
    const val CLAVE = "recomendaciones"

    /** Devuelve el paquete y el JSON tal cual llegó (para guardarlo en el teléfono). */
    suspend fun cargar(token: String, ahora: Long = System.currentTimeMillis()): Pair<PaqueteRecomendaciones, String> {
        val cuerpo = pedirJson("/api/app/recomendaciones", token, "las recomendaciones")
        return leerRecomendacionesApp(JSONObject(cuerpo), ahora) to cuerpo
    }
}

private fun instante(texto: String?): Long? = runCatching { Instant.parse(texto).toEpochMilli() }.getOrNull()

/** Lógica pura (probada): convierte la respuesta; sin fechas válidas usa [ahora] y la vigencia por defecto. */
fun leerRecomendacionesApp(json: JSONObject, ahora: Long): PaqueteRecomendaciones {
    val generado = instante(json.optString("generadoEn")) ?: ahora
    val vigente = instante(json.optString("vigenteHasta"))?.takeIf { it > generado }
        ?: (generado + CargaProgramada.VIGENCIA_RECOMENDACIONES_MS)
    val arr = json.optJSONArray("recomendaciones")
    val lista = (0 until (arr?.length() ?: 0)).mapNotNull { i ->
        val a = arr!!.optJSONObject(i) ?: return@mapNotNull null
        val actividad = CatalogoApi.parsear(a)
        if (actividad.id.isBlank() || actividad.id == "null") return@mapNotNull null
        val etiquetas = a.optJSONArray("etiquetas")
        RecomendacionIa(
            actividad = actividad,
            motivo = a.optString("motivo").takeUnless { it.isBlank() || it == "null" }
                ?: "Actividad disponible que encaja con tu agenda.",
            etiquetas = (0 until (etiquetas?.length() ?: 0)).map { etiquetas!!.optString(it) }.filter { it.isNotBlank() },
            horario = CompatibilidadHorario(
                cabeEnHorario = a.optBoolean("cabeEnHorario", false),
                huecoInicio = a.optString("huecoInicio").takeUnless { it == "null" }.orEmpty(),
                huecoFin = a.optString("huecoFin").takeUnless { it == "null" }.orEmpty(),
                motivo = a.optString("motivoHorario").takeUnless { it == "null" }.orEmpty()
            ),
            inscripcionDirecta = a.optBoolean("inscripcionDirecta", false)
        )
    }.distinctBy { it.actividad.id }
    return PaqueteRecomendaciones(
        generadoEn = generado,
        vigenteHasta = vigente,
        origen = json.optString("origen", "reglas"),
        conHorario = json.optBoolean("conHorario", false),
        aviso = json.optString("aviso").takeUnless { it == "null" }.orEmpty(),
        recomendaciones = lista
    )
}

/** Etiqueta honesta del origen: nunca se anuncia una propuesta por reglas como si fuera de Gemini. */
fun origenRecomendaciones(origen: String): String = when (origen) {
    "ia" -> "Elegidas con IA (Gemini)"
    "reglas" -> "Por tus intereses · IA no disponible"
    else -> "Sin recomendaciones por ahora"
}
