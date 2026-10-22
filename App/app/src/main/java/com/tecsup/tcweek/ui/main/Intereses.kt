package com.tecsup.tcweek.ui.main

/*
 * Intereses del estudiante: lógica pura (sin pantallas) para poder probarla con una prueba unitaria.
 * Las listas son de ids ("teatro", "tecnologia"…).
 */

/** Con este número de intereses o más las recomendaciones salen mejor (es una sugerencia, no una exigencia). */
const val MINIMO_SUGERIDO = 3

/** Solo se muestra el onboarding si nunca eligió intereses ni decidió omitirlos. */
fun debeSolicitarIntereses(configurados: Boolean, omitido: Boolean): Boolean = !configurados && !omitido

/** Agrega el interés si no estaba y lo quita si estaba. Devuelve una lista nueva. */
fun alternar(seleccion: List<String>, id: String): List<String> =
    if (id in seleccion) seleccion.filter { it != id } else seleccion + id

/** true si las dos listas tienen los mismos intereses (sin importar el orden). */
fun mismosIntereses(a: List<String>, b: List<String>): Boolean = a.size == b.size && a.all { it in b }

/** Se puede guardar si hay al menos uno elegido, algo cambió y no se está guardando ya. */
fun puedeGuardar(seleccion: List<String>, guardados: List<String>, guardando: Boolean = false): Boolean =
    !guardando && seleccion.isNotEmpty() && !mismosIntereses(seleccion, guardados)

/** Deja solo los ids que existen en el catálogo y los ordena como el catálogo (los desconocidos se descartan). */
fun ordenarComoCatalogo(ids: List<String>, catalogo: List<String>): List<String> = catalogo.filter { it in ids }

/** Texto del contador: "Elige al menos uno", "1 interés elegido", "4 intereses elegidos". */
fun textoContador(n: Int): String = when {
    n <= 0 -> "Elige al menos uno"
    n == 1 -> "1 interés elegido"
    else -> "$n intereses elegidos"
}

/** Frase de ayuda según cuántos hay elegidos. */
fun textoAyuda(n: Int): String = when {
    n <= 0 -> "Marca los temas que te gustan para recibir recomendaciones."
    n < MINIMO_SUGERIDO -> "Con $MINIMO_SUGERIDO o más, las recomendaciones salen mejor."
    else -> "Listo. Puedes cambiarlos cuando quieras."
}

/** Nombre de cada ícono de interés (la pantalla lo convierte en el dibujo). Uno nuevo sin ícono sale con una estrella. */
enum class IconoInteres { MASCARAS, MUSICA, DANZA, LIBROS, CHIP, DESTELLOS, IDEA, JUEGO, DEPORTE, COMIDA, PREMIO, ESTRELLA }

fun iconoDeInteres(id: String): IconoInteres = when (id) {
    "teatro" -> IconoInteres.MASCARAS
    "musica" -> IconoInteres.MUSICA
    "danza" -> IconoInteres.DANZA
    "libros" -> IconoInteres.LIBROS
    "tecnologia" -> IconoInteres.CHIP
    "ia" -> IconoInteres.DESTELLOS
    "emprendimiento" -> IconoInteres.IDEA
    "videojuegos" -> IconoInteres.JUEGO
    "deportes" -> IconoInteres.DEPORTE
    "gastronomia" -> IconoInteres.COMIDA
    "concursos" -> IconoInteres.PREMIO
    else -> IconoInteres.ESTRELLA
}
