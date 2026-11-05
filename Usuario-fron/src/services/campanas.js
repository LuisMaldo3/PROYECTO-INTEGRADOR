/** Una campaña se identifica solo cuando el backend la nombra explícitamente.
 * No atribuir todas las actividades culturales/concursos a una semana especial. */
export function esActividadTCWeek(actividad = {}) {
  const texto = [actividad.nombre, actividad.nombreActividad, actividad.categoria, actividad.descripcion, actividad.categoriaId]
    .filter(v => typeof v === 'string').join(' ').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase()
  return /t\s*&\s*c\s*week|tech\s*(?:and|&)\s*culture\s*week|tec\s*week/.test(texto)
}

/** El especial aparece únicamente con actividades reales de una misma semana (máx. 7 días). */
export function obtenerSemanaEspecial(actividades = [], hoy = '') {
  const fechaValida = /^\d{4}-\d{2}-\d{2}$/
  const lista = actividades.filter(a => esActividadTCWeek(a) && fechaValida.test(a.fecha || ''))
    .sort((a, b) => a.fecha.localeCompare(b.fecha))
  if (!lista.length) return null
  // Agrupar ediciones separadas de la campaña, sin mezclar registros históricos.
  const grupos = []
  for (const actividad of lista) {
    const grupo = grupos.at(-1)
    const dist = grupo ? (Date.parse(actividad.fecha + 'T00:00:00Z') - Date.parse(grupo.fin + 'T00:00:00Z')) / 86400000 : Infinity
    if (!grupo || dist > 45) grupos.push({ inicio: actividad.fecha, fin: actividad.fecha, cantidad: 1 })
    else { grupo.fin = actividad.fecha; grupo.cantidad += 1 }
  }
  const limite = hoy ? new Date(Date.parse(hoy + 'T00:00:00Z') + 30 * 86400000).toISOString().slice(0, 10) : '9999-12-31'
  return grupos.find(g => g.fin >= hoy && g.inicio <= limite &&
    (Date.parse(g.fin + 'T00:00:00Z') - Date.parse(g.inicio + 'T00:00:00Z')) / 86400000 <= 6) || null
}

export function filtrarPorEtapa(actividades = [], etapa = 'proximas', hoy = '') {
  if (etapa === 'todas') return actividades
  if (etapa === 'pasadas') return actividades.filter(a => a.fecha && a.fecha < hoy)
  return actividades.filter(a => !a.fecha || a.fecha >= hoy)
}
