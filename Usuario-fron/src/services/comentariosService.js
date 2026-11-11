import { API_URL } from '../config.js'

async function pedir(ruta, token, { method = 'GET', body, signal } = {}) {
  if (!token) throw new Error('Inicia sesión en VYNK para continuar.')
  let respuesta
  try {
    respuesta = await fetch(`${API_URL}${ruta}`, {
      method,
      headers: { Authorization: `Bearer ${token}`, ...(body === undefined ? {} : { 'Content-Type': 'application/json' }) },
      ...(body === undefined ? {} : { body: JSON.stringify(body) }), signal,
    })
  } catch (error) {
    if (signal?.aborted || error?.name === 'AbortError') throw error
    throw new Error('No se pudo conectar con el servidor de VYNK.')
  }
  const datos = await respuesta.json().catch(() => ({}))
  if (!respuesta.ok) {
    if (respuesta.status === 401) throw new Error('Tu sesión expiró. Inicia sesión de nuevo.')
    if (respuesta.status === 403) throw new Error(datos.message || 'No tienes permiso para realizar esta acción.')
    if (respuesta.status === 404) throw new Error('Actividad no encontrada o sin autorización.')
    throw new Error(datos?.message || datos?.detail || `No se pudo completar la operación (HTTP ${respuesta.status}).`)
  }
  return datos
}

const actividad = id => `/api/actividades/${encodeURIComponent(id)}/comentarios`
const organizador = id => `/api/organizador/actividades/${encodeURIComponent(id)}/comentarios`

export async function obtenerMiComentario(token, actividadId, signal) {
  if (!actividadId) throw new Error('Selecciona una actividad.')
  return pedir(`${actividad(actividadId)}/mio`, token, { signal })
}

export async function guardarMiComentario(token, actividadId, texto) {
  if (!actividadId) throw new Error('Selecciona una actividad.')
  const limpio = String(texto ?? '').trim()
  if (limpio.length < 10 || limpio.length > 1000) throw new Error('Escribe entre 10 y 1000 caracteres.')
  return pedir(actividad(actividadId), token, { method: 'POST', body: { texto: limpio } })
}

export async function obtenerEstadisticasComentarios(token, actividadId, signal) {
  if (!actividadId) throw new Error('Selecciona una actividad.')
  return pedir(`${organizador(actividadId)}/estadisticas`, token, { signal })
}

export async function generarAnalisisComentarios(token, actividadId, signal) {
  if (!actividadId) throw new Error('Selecciona una actividad.')
  return pedir(`${organizador(actividadId)}/analisis-ia`, token, { method: 'POST', signal })
}
