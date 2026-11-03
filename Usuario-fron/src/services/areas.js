/**
 * Las cinco áreas de Tecsup tienen un color propio en la interfaz.
 * Devuelve la clave que usa el CSS (data-area="digital", etc.).
 */
const sinTildes = (t) => String(t ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase()

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

export const AREAS_TECSUP = [
  'Gestión y Producción',
  'Tecnología Digital',
  'Minería y Procesos Químicos',
  'Electricidad y Electrónica',
  'Mecánica',
]
