/** Cómo se ven los datos de una actividad en la lista (sin React ni red). */

/** Recorta el texto a `max` caracteres, con "…" si era más largo. */
export function resumen(texto, max = 80) {
  const limpio = String(texto ?? '').trim()
  return limpio.length <= max ? limpio : `${limpio.slice(0, max - 1).trimEnd()}…`
}

/** "14 oct 2026"; "—" si la fecha falta o no es válida. */
export function formatearFecha(iso) {
  if (!iso) return '—'
  const fecha = new Date(iso)
  if (Number.isNaN(fecha.getTime())) return '—'
  return fecha.toLocaleDateString('es-PE', { day: '2-digit', month: 'short', year: 'numeric' })
}
