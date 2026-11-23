package com.tecsup.tcweek.data

import java.time.Instant
import org.json.JSONObject

/** US-81: una de mis próximas actividades, con la actividad completa para mostrar su portada y abrirla. */
data class ProximaInicio(
    val actividad: ActividadCatalogo,
    val empiezaEn: Long,
    val terminaEn: Long,
    val cuando: String,
    val equipo: String = "",
    val equipoConfirmado: Boolean = true
) {
    fun enCurso(ahora: Long) = ahora in empiezaEn until terminaEn
}

/** US-81-T07: actividad o competencia destacada; sin portada publicada se usa la ilustración del tema. */
data class DestacadaInicio(
    val actividad: ActividadCatalogo,
    val tienePortada: Boolean,
    val paraTi: Boolean,
    val destacadaPor: String,
    val cuando: String
)

/** US-81-T06: aviso reciente con su imagen lista para mostrar (vacía si no tiene). */
data class AvisoInicio(
    val id: String,
    val titulo: String,
    val mensaje: String,
    val publicadoEn: String,
    val imagenUrl: String,
    val autor: String,
    val creditoUrl: String,
    val nuevo: Boolean
)

data class ResumenInicio(
    val generadoEn: Long,
    val vigenteHasta: Long,
    val nombre: String,
    val mensaje: String,
    val inscritas: Int,
    val hoy: Int,
    val semana: Int,
    val equiposPorConfirmar: Int,
    val proximas: List<ProximaInicio>,
    val destacadas: List<DestacadaInicio>,
    val avisos: List<AvisoInicio>
)

/** US-81 T2: cliente de GET /api/app/inicio. */
object InicioApi {
    const val CLAVE = "inicio"
    const val VIGENCIA_MS = 2 * 60_000L

    suspend fun cargar(token: String, ahora: Long = System.currentTimeMillis()): Pair<ResumenInicio, String> {
        val cuerpo = pedirJson("/api/app/inicio", token, "el resumen de inicio", lectura = 20_000)
        return leerResumenInicio(JSONObject(cuerpo), ahora) to cuerpo
    }
}

private fun instanteMs(texto: String?): Long? = runCatching { Instant.parse(texto).toEpochMilli() }.getOrNull()
private fun texto(o: JSONObject?, k: String) = o?.optString(k)?.takeUnless { it.isBlank() || it == "null" }.orEmpty()

/** Ruta del propio servidor → dirección completa; externas solo https; lo demás se descarta. */
fun imagenSegura(url: String): String = when {
    url.startsWith("/api/portadas/archivo/") -> Servidor.DIRECCION.trimEnd('/') + url
    url.startsWith("https://") -> url
    else -> ""
}

/** Lógica pura (probada). */
fun leerResumenInicio(json: JSONObject, ahora: Long): ResumenInicio {
    val generado = instanteMs(json.optString("generadoEn")) ?: ahora
    val vigente = instanteMs(json.optString("vigenteHasta"))?.takeIf { it > generado } ?: (generado + InicioApi.VIGENCIA_MS)
    val saludo = json.optJSONObject("saludo")
    val resumen = json.optJSONObject("resumen")
    val proximasJson = json.optJSONArray("proximas")
    val proximas = (0 until (proximasJson?.length() ?: 0)).mapNotNull { i ->
        val a = proximasJson!!.optJSONObject(i) ?: return@mapNotNull null
        val actividad = CatalogoApi.parsear(a)
        val inicio = instanteMs(a.optString("empiezaEn")) ?: return@mapNotNull null
        val fin = instanteMs(a.optString("terminaEn"))?.takeIf { it > inicio } ?: (inicio + 60 * 60_000L)
        val eq = a.optJSONObject("equipo")
        ProximaInicio(actividad, inicio, fin, texto(a, "cuando"), texto(eq, "nombre"), eq?.optBoolean("confirmado", true) ?: true)
    }
    val destacadasJson = json.optJSONArray("destacadas")
    val destacadas = (0 until (destacadasJson?.length() ?: 0)).mapNotNull { i ->
        val a = destacadasJson!!.optJSONObject(i) ?: return@mapNotNull null
        val actividad = CatalogoApi.parsear(a)
        if (actividad.id.isBlank()) return@mapNotNull null
        DestacadaInicio(actividad, a.optBoolean("tienePortada", false) && actividad.portadaUrl.isNotBlank(),
            a.optBoolean("paraTi", false), texto(a, "destacadaPor").ifBlank { "Destacada" }, texto(a, "cuando"))
    }
    val avisosJson = json.optJSONArray("avisos")
    val avisos = (0 until (avisosJson?.length() ?: 0)).mapNotNull { i ->
        val v = avisosJson!!.optJSONObject(i) ?: return@mapNotNull null
        AvisoInicio(texto(v, "id"), texto(v, "titulo").ifBlank { "Comunicado" }, texto(v, "mensaje"), texto(v, "publicadoEn"),
            imagenSegura(texto(v, "imagenUrl")), texto(v, "autor"), texto(v, "creditoUrl"), v.optBoolean("nuevo", false))
    }
    return ResumenInicio(
        generadoEn = generado, vigenteHasta = vigente,
        nombre = texto(saludo, "nombre"), mensaje = texto(saludo, "mensaje"),
        inscritas = resumen?.optInt("inscritas", proximas.size) ?: proximas.size,
        hoy = resumen?.optInt("hoy", 0) ?: 0, semana = resumen?.optInt("semana", 0) ?: 0,
        equiposPorConfirmar = resumen?.optInt("equiposPorConfirmar", 0) ?: 0,
        proximas = proximas.sortedBy { it.empiezaEn }, destacadas = destacadas, avisos = avisos
    )
}

/** Las que siguen pendientes con el reloj actual (sirve para lo guardado sin conexión). */
fun proximasVigentes(resumen: ResumenInicio, ahora: Long, omitir: String? = null): List<ProximaInicio> =
    resumen.proximas.filter { it.terminaEn > ahora && it.actividad.id != omitir }
