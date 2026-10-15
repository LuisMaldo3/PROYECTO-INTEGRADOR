import { API_URL } from '../config'
import { leerSesion } from './authService'

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
