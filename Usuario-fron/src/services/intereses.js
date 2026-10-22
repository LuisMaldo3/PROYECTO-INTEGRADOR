/**
 * Intereses del estudiante: lógica pura (sin React ni red) para probarla con `npm test`.
 * `seleccion` y `guardados` son listas de ids ("teatro", "tecnologia"…).
 */

/** Con este número de intereses o más las recomendaciones salen mejor (es una sugerencia, no una exigencia). */
export const MINIMO_SUGERIDO = 3

// Ícono de cada interés (nombres de components/Iconos.jsx). Uno nuevo sin ícono sale con una estrella.
const ICONOS = new Map([
  ['teatro', 'mascaras'],
  ['musica', 'musica'],
  ['danza', 'danza'],
  ['libros', 'libros'],
  ['tecnologia', 'chip'],
  ['ia', 'destellos'],
  ['emprendimiento', 'idea'],
  ['videojuegos', 'juego'],
  ['deportes', 'pulso'],
  ['gastronomia', 'cubiertos'],
  ['concursos', 'premio'],
])

export function iconoDeInteres(id) {
  return ICONOS.get(id) ?? 'estrella'
}

/** Agrega el interés si no estaba y lo quita si estaba. Devuelve una lista nueva. */
export function alternar(seleccion, id) {
  return seleccion.includes(id) ? seleccion.filter((x) => x !== id) : [...seleccion, id]
}

/** true si las dos listas tienen los mismos intereses (sin importar el orden). */
export function mismosIntereses(a, b) {
  return a.length === b.length && a.every((x) => b.includes(x))
}

/** Se puede guardar si hay al menos uno elegido, algo cambió y no se está guardando ya. */
export function puedeGuardar({ seleccion, guardados, guardando = false }) {
  return !guardando && seleccion.length >= 1 && !mismosIntereses(seleccion, guardados)
}

/** Deja solo los ids que existen en el catálogo y los ordena como el catálogo (los desconocidos se descartan). */
export function ordenarComoCatalogo(ids, catalogo) {
  return catalogo.filter((i) => ids.includes(i.id)).map((i) => i.id)
}

/** Texto del contador: "Elige al menos uno", "1 interés elegido", "4 intereses elegidos". */
export function textoContador(n) {
  if (n <= 0) return 'Elige al menos uno'
  return `${n} ${n === 1 ? 'interés elegido' : 'intereses elegidos'}`
}

/** Frase de ayuda según cuántos hay elegidos. */
export function textoAyuda(n) {
  if (n <= 0) return 'Marca los temas que te gustan para recibir recomendaciones.'
  if (n < MINIMO_SUGERIDO) return `Con ${MINIMO_SUGERIDO} o más, las recomendaciones salen mejor.`
  return 'Listo. Puedes cambiarlos cuando quieras.'
}

/** Solo interrumpir el primer ingreso si no hay elección guardada ni omisión. */
export function debeSolicitarIntereses({ configurados = false, omitido = false } = {}) {
  return !configurados && !omitido
}
