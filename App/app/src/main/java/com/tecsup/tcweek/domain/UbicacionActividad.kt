package com.tecsup.tcweek.domain

import org.json.JSONObject

/**
 * US-71: ficha del lugar de una actividad, tal como la arma el backend. Lo que no está cargado queda vacío:
 * la app nunca inventa una dirección ni coordenadas.
 */
data class UbicacionActividad(
    val lugar: String = "",
    val sede: String = "",
    val pabellon: String = "",
    val aula: String = "",
    val direccion: String = "",
    val referencia: String = "",
    val confirmada: Boolean = false,
    val latitud: Double? = null,
    val longitud: Double? = null
) {
    /** Hay coordenadas reales: se puede ofrecer «Abrir en el mapa». */
    val tieneMapa: Boolean get() = latitud != null && longitud != null

    /** Filas para la pantalla de ubicación, solo con los datos que existen. */
    fun filas(): List<Pair<String, String>> = listOf(
        "Sede" to sede, "Pabellón" to pabellon, "Aula" to aula, "Dirección" to direccion, "Cómo llegar" to referencia
    ).filter { it.second.isNotBlank() }

    /** Enlace para abrir el mapa del celular en las coordenadas, con el nombre del lugar como etiqueta. */
    fun enlaceMapa(): String? {
        if (!tieneMapa) return null
        val etiqueta = java.net.URLEncoder.encode(lugar.ifBlank { "Actividad" }, "UTF-8")
        return "geo:$latitud,$longitud?q=$latitud,$longitud($etiqueta)"
    }

    companion object {
        const val POR_CONFIRMAR = "Ubicación por confirmar"

        fun leer(json: JSONObject?, lugarDeRespaldo: String = ""): UbicacionActividad {
            fun texto(clave: String) = json?.optString(clave).orEmpty().takeUnless { it == "null" }.orEmpty().trim()
            fun numero(clave: String, limite: Double): Double? {
                if (json == null || !json.has(clave) || json.isNull(clave)) return null
                val n = json.optDouble(clave)
                return n.takeIf { !it.isNaN() && kotlin.math.abs(it) <= limite && it != 0.0 }
            }
            val lat = numero("latitud", 90.0)
            val lng = numero("longitud", 180.0)
            return UbicacionActividad(
                lugar = texto("lugar").ifBlank { lugarDeRespaldo.ifBlank { POR_CONFIRMAR } },
                sede = texto("sede"),
                pabellon = texto("pabellon"),
                aula = texto("aula"),
                direccion = texto("direccion"),
                referencia = texto("referencia"),
                confirmada = json?.optBoolean("confirmada", lugarDeRespaldo.isNotBlank() && lugarDeRespaldo != POR_CONFIRMAR)
                    ?: (lugarDeRespaldo.isNotBlank() && lugarDeRespaldo != POR_CONFIRMAR),
                // Una sola coordenada no sirve para ubicar nada.
                latitud = if (lat != null && lng != null) lat else null,
                longitud = if (lat != null && lng != null) lng else null
            )
        }
    }
}
