/** Cómo se ven los datos de una actividad en la lista (sin React ni red). */

/** Recorta el texto a `max` caracteres, con "…" si era más largo. */
export function resumen(texto, max = 80) {
  const limpio = String(texto ?? '').trim()
  return limpio.length <= max ? limpio : `${limpio.slice(0, max - 1).trimEnd()}…`
}

/** "14:30" → "2:30 p. m." (como en la programación). Devuelve "" si no es una hora HH:MM válida. */
export function formatearHora(hhmm) {
  const partes = /^([01]\d|2[0-3]):([0-5]\d)$/.exec(typeof hhmm === 'string' ? hhmm.trim() : '')
  if (!partes) return ''
  const hora = Number(partes[1])
  return `${hora % 12 || 12}:${partes[2]} ${hora < 12 ? 'a. m.' : 'p. m.'}`
}

/** "11:00 a. m. – 2:30 p. m."; solo "3:30 p. m." si no hay hora de fin; "—" si no hay hora de inicio. */
export function formatearHorario(horaInicio, horaFin) {
  const inicio = formatearHora(horaInicio)
  if (!inicio) return '—'
  const fin = formatearHora(horaFin)
  return fin ? `${inicio} – ${fin}` : inicio
}

/** "2026-10-19" → "lun, 19 oct". Se arma con el día tal cual (sin zona horaria) para que nunca se corra un día. */
export function formatearFechaActividad(fecha) {
  const partes = /^(\d{4})-(\d{2})-(\d{2})$/.exec(typeof fecha === 'string' ? fecha.trim() : '')
  if (!partes) return 'Sin fecha'
  const [anio, mes, dia] = partes.slice(1).map(Number)
  const f = new Date(anio, mes - 1, dia)
  if (f.getFullYear() !== anio || f.getMonth() !== mes - 1 || f.getDate() !== dia) return 'Sin fecha'
  return f.toLocaleDateString('es-PE', { weekday: 'short', day: 'numeric', month: 'short' })
}
