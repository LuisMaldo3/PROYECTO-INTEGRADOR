import { API_URL } from '../config.js'
import { leerSesion } from './authService.js'

async function solicitar(ruta, signal) {
  const token = leerSesion()?.token
  if (!token) throw new Error('Inicia sesión para consultar las actividades.')
  let respuesta
  try {
    respuesta = await fetch(`${API_URL}${ruta}`, { signal, headers: { Authorization: `Bearer ${token}` } })
  } catch (error) {
    if (signal?.aborted) throw error
    throw new Error('No se pudo conectar con VYNK. Comprueba el servidor e inténtalo nuevamente.')
  }
  if (respuesta.status === 401) throw new Error('La sesión ha expirado. Inicia sesión nuevamente.')
  if (respuesta.status === 404) throw new Error('La actividad no está publicada o ya no existe.')
  if (!respuesta.ok) throw new Error('No fue posible consultar los detalles de la actividad.')
  return respuesta.json()
}

/** Incluye competencias finalizadas, que ya no aparecen en el catálogo de inscripción. */
export const obtenerDetalleActividad = (id, signal) => solicitar(`/api/catalogo/${encodeURIComponent(id)}`, signal)
/** US-22-T01..T06: otras actividades abiertas relacionadas por categoría/área. */
export const obtenerActividadesSimilares = (id, signal) => solicitar(`/api/catalogo/${encodeURIComponent(id)}/similares`, signal)
/** US-22-T07/T08: solo resultados que la administración haya publicado. */
export const obtenerResultadoOficial = (id, signal) => solicitar(`/api/competencias/resultados/${encodeURIComponent(id)}`, signal)

export function ganadoresOficiales(resultado) {
  if (!resultado || !Array.isArray(resultado.posiciones)) return []
  return resultado.posiciones.filter(x => Number(x.puesto) === 1 && x.nombre).map(x => ({
    nombre: String(x.nombre), areaTecsup: x.areaTecsup || '', puntos: x.puntos ?? null,
  }))
}
