import { API_URL } from '../config.js'
import { leerSesion } from './authService.js'

/** Recomendaciones reales: distingue problemas de acceso, red, MongoDB y backend desactualizado. */
export async function cargarRecomendaciones(signal) {
  const token = leerSesion()?.token
  if (!token) throw new Error('Inicia sesión para ver recomendaciones.')
  let respuesta
  try {
    respuesta = await fetch(`${API_URL}/api/recomendaciones`, {
      signal, headers: { Authorization: `Bearer ${token}`, Accept: 'application/json' },
    })
  } catch (e) {
    if (signal?.aborted || e.name === 'AbortError') throw e
    throw new Error(`No hay conexión con Spring Boot (${API_URL}). Comprueba la URL y que el backend esté encendido.`)
  }
  if (respuesta.status === 401) throw new Error('Tu sesión expiró. Vuelve a iniciar sesión.')
  if (respuesta.status === 403) throw new Error('No tienes permisos para ver recomendaciones.')
  if (respuesta.status === 404) throw new Error('No existe /api/recomendaciones. Instala y reinicia el backend actualizado.')
  if (respuesta.status === 503) throw new Error('MongoDB no responde. Revisa la conexión del backend.')
  if (!respuesta.ok) throw new Error(`No se pudieron cargar recomendaciones (HTTP ${respuesta.status}).`)
  const datos = await respuesta.json()
  if (!Array.isArray(datos?.actividades)) throw new Error('El backend devolvió recomendaciones con formato incorrecto.')
  return datos
}

/** Registra un descarte para el usuario autenticado. El servidor conserva la preferencia. */
export async function descartarRecomendacion(actividadId, signal) {
  const token = leerSesion()?.token
  if (!token) throw new Error('Inicia sesión para guardar tus preferencias.')
  if (!/^[a-zA-Z0-9_-]{1,128}$/.test(String(actividadId ?? ''))) {
    throw new Error('La actividad no tiene un identificador válido.')
  }
  let respuesta
  try {
    respuesta = await fetch(`${API_URL}/api/recomendaciones/${encodeURIComponent(actividadId)}/descartar`, {
      method: 'POST', signal,
      headers: { Authorization: `Bearer ${token}`, Accept: 'application/json' },
    })
  } catch (e) {
    if (signal?.aborted || e.name === 'AbortError') throw e
    throw new Error('No hay conexión. No se guardó tu preferencia; inténtalo nuevamente.')
  }
  if (respuesta.status === 401) throw new Error('Tu sesión expiró. Inicia sesión nuevamente.')
  if (respuesta.status === 403) throw new Error('No tienes permisos para descartar recomendaciones.')
  if (respuesta.status === 404) throw new Error('El backend aún no admite descartar recomendaciones. Actualiza Spring Boot.')
  if (!respuesta.ok) throw new Error(`No se pudo guardar el descarte (HTTP ${respuesta.status}).`)
  return true
}

/** Quita una tarjeta sin mutar los datos originales; simplifica una reversión ante fallo. */
export function retirarTarjetaDescartada(datos, actividadId) {
  if (!Array.isArray(datos?.actividades)) return datos
  return { ...datos, actividades: datos.actividades.filter(item => item.id !== actividadId) }
}
