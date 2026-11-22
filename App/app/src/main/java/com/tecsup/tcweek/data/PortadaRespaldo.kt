package com.tecsup.tcweek.data

import java.text.Normalizer

/**
 * US-71 T05: ilustración local que se usa cuando una actividad no tiene portada o la portada no se puede descargar
 * (sin conexión, enlace caído). Primero manda el área de Tecsup; si no tiene área, se deduce del nombre o categoría.
 */
enum class RespaldoPortada { GESTION, DIGITAL, QUIMICA, ELECTRONICA, MECANICA, DEPORTES, GENERAL }

fun respaldoPortada(nombre: String?, areaTecsup: String?, categoriaId: String? = null): RespaldoPortada {
    porArea(areaTecsup)?.let { return it }
    val texto = sinTildes("${nombre.orEmpty()} ${categoriaId.orEmpty()}")
    return when {
        "futbol" in texto || "deporte" in texto || "voley" in texto || "gymkana" in texto -> RespaldoPortada.DEPORTES
        "quimic" in texto || "mineria" in texto || "laboratorio" in texto -> RespaldoPortada.QUIMICA
        "electronic" in texto || "electric" in texto || "robotica" in texto -> RespaldoPortada.ELECTRONICA
        "mecanic" in texto || "taller" in texto -> RespaldoPortada.MECANICA
        "digital" in texto || "software" in texto || "programacion" in texto || "tecnologia" in texto
            || "videojuego" in texto || " ia " in " $texto " -> RespaldoPortada.DIGITAL
        "gestion" in texto || "produccion" in texto || "emprend" in texto -> RespaldoPortada.GESTION
        else -> RespaldoPortada.GENERAL
    }
}

/** Las cinco áreas de Tecsup, escritas como sea (con o sin tildes, nombre corto o completo). */
private fun porArea(area: String?): RespaldoPortada? {
    val t = sinTildes(area.orEmpty())
    return when {
        t.isBlank() || t == "null" -> null
        "digital" in t -> RespaldoPortada.DIGITAL
        "electr" in t -> RespaldoPortada.ELECTRONICA
        "mecanica" in t -> RespaldoPortada.MECANICA
        "quimic" in t || "mineria" in t -> RespaldoPortada.QUIMICA
        "gestion" in t || "produccion" in t -> RespaldoPortada.GESTION
        else -> null
    }
}

/**
 * US-71 T06: de dónde sale la portada. Solo se descargan enlaces seguros (https o del propio backend);
 * cualquier otra cosa usa directamente la ilustración local.
 */
fun portadaRemotaSegura(url: String?): Boolean {
    val u = url?.trim().orEmpty()
    return u.startsWith("https://") || (u.startsWith(Servidor.DIRECCION.trimEnd('/') + "/api/portadas/archivo/") && u.length > 30)
}

private fun sinTildes(texto: String) =
    Normalizer.normalize(texto, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase().trim()
