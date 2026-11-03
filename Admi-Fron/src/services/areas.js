export const AREAS_TECSUP = Object.freeze([
  'Gestión y Producción',
  'Tecnología Digital',
  'Minería y Procesos Químicos',
  'Electricidad y Electrónica',
  'Mecánica',
])

const sinTildes = (t) => String(t ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase()

/** Clave de color de cada área (la usa el CSS con data-area). */
export function claveArea(nombre) {
  const t = sinTildes(nombre)
  if (!t) return 'general'
  if (t.includes('digital')) return 'digital'
  if (t.includes('electr')) return 'electro'
  if (t.includes('mecanica')) return 'mecanica'
  if (t.includes('quimic') || t.includes('mineria')) return 'quimica'
  if (t.includes('gestion') || t.includes('produccion')) return 'gestion'
  return 'general'
}
