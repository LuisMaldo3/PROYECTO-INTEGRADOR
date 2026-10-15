import { API_URL } from '../config.js'

const BASE = '/api/organizador/actividades'

/** Solo el token de la sesión actual identifica al organizador: jamás pasar IDs del dueño desde React. */
async function peticion(ruta, token, { method = 'GET', body, signal } = {}) {
  if (!token) throw new Error('Inicia sesión como organizador para continuar.')
  let respuesta
  try {
    respuesta = await fetch(`${API_URL}${ruta}`, {
      method,
      headers: {
        Authorization: `Bearer ${token}`,
        ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
      },
      ...(body === undefined ? {} : { body: JSON.stringify(body) }),
      signal,
    })
  } catch (error) {
    if (signal?.aborted || error?.name === 'AbortError') throw error
    throw new Error('Sin conexión con Spring Boot. Comprueba que funcione en el puerto 8081.')
  }
  const datos = await respuesta.json().catch(() => null)
  if (!respuesta.ok) {
    if (respuesta.status === 401) throw new Error('Tu sesión expiró. Inicia sesión nuevamente.')
    if (respuesta.status === 403) throw new Error('Solo una cuenta con rol organizador activo puede acceder.')
    if (respuesta.status === 404) throw new Error('Esta actividad no existe o no está asignada a tu cuenta.')
    if (respuesta.status === 503) throw new Error('MongoDB no está disponible. Revisa la conexión del backend.')
    const detalle = datos?.mensaje || datos?.detail || datos?.message
    throw new Error(detalle || `No se pudo completar la operación (HTTP ${respuesta.status}).`)
  }
  return datos
}

export async function obtenerMisActividades(token, signal) {
  const datos = await peticion(BASE, token, { signal })
  return Array.isArray(datos?.actividades) ? datos.actividades : []
}

export async function obtenerInscritos(token, actividadId, signal) {
  if (!actividadId) throw new Error('Selecciona primero una actividad.')
  const datos = await peticion(`${BASE}/${encodeURIComponent(actividadId)}/inscritos`, token, { signal })
  return { actividad: datos?.actividad || null, inscritos: Array.isArray(datos?.inscritos) ? datos.inscritos : [] }
}

export async function guardarAsistencia(token, actividadId, marcas) {
  if (!actividadId) throw new Error('Selecciona primero una actividad.')
  if (!Array.isArray(marcas) || marcas.length === 0) throw new Error('No hay estudiantes para registrar asistencia.')
  return peticion(`${BASE}/${encodeURIComponent(actividadId)}/asistencia`, token, {
    method: 'POST', body: { asistencias: marcas },
  })
}

export async function reportarProblema(token, actividadId, categoria, descripcion) {
  if (!actividadId) throw new Error('Selecciona primero una actividad.')
  const texto = descripcion.trim()
  if (texto.length < 15 || texto.length > 2000) throw new Error('Describe el problema entre 15 y 2000 caracteres.')
  return peticion(`${BASE}/${encodeURIComponent(actividadId)}/reportes`, token, {
    method: 'POST', body: { categoria, descripcion: texto },
  })
}

/** Navegación de subpantallas del organizador usando el hash existente del proyecto. */
export function rutaOrganizador(pantalla, actividadId) {
  return `#/${pantalla}${actividadId ? `?actividad=${encodeURIComponent(actividadId)}` : ''}`
}

export function actividadSeleccionada(hash = '') {
  const consulta = String(hash).split('?')[1] || ''
  return new URLSearchParams(consulta).get('actividad') || ''
}
