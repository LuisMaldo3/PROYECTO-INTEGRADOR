import { API_URL } from '../config'
import { leerSesion } from './authService'

async function consultar(ruta, { method = 'GET', body, signal } = {}) {
  const token = leerSesion()?.token
  if (!token) throw new Error('Inicia sesión con tu cuenta institucional.')
  let respuesta
  try { respuesta = await fetch(`${API_URL}${ruta}`, {
    method, signal,
    headers: { Authorization: `Bearer ${token}`, ...(body ? { 'Content-Type': 'application/json' } : {}) },
    ...(body ? { body: JSON.stringify(body) } : {}),
  }) }
  catch (e) { if (e.name === 'AbortError') throw e; throw new Error('No hay conexión con el servidor de inscripciones.') }
  const datos = await respuesta.json().catch(() => null)
  if (!respuesta.ok) {
    if (respuesta.status === 401) throw new Error('Tu sesión expiró. Inicia sesión otra vez.')
    throw new Error(datos?.message || datos?.mensaje || datos?.detail || `Error HTTP ${respuesta.status}. Revisa cupos y requisitos.`)
  }
  return datos
}
const base = (id) => `/api/actividades/${encodeURIComponent(id)}`
export const verMiInscripcion = (id, signal) => consultar(`${base(id)}/mi-inscripcion`, { signal })
export const inscribirme = (id, nombreEquipo = '') => consultar(`${base(id)}/inscripcion`, { method: 'POST', body: { nombreEquipo } })
export const unirmeEquipo = (id, codigoEquipo) => consultar(`${base(id)}/unirse-equipo`, { method: 'POST', body: { codigoEquipo } })
export const cancelarInscripcion = (id) => consultar(`${base(id)}/inscripcion`, { method: 'DELETE' })
export const consultarResultados = (signal) => consultar('/api/competencias/resultados', { signal })
export const consultarClasificacion = (signal) => consultar('/api/competencias/clasificacion', { signal })
