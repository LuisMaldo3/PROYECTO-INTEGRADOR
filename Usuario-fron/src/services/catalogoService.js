import { API_URL } from '../config'
import { leerSesion } from './authService'

export async function obtenerCatalogo(signal) {
  const token = leerSesion()?.token
  if (!token) throw new Error('Inicia sesión para consultar las actividades.')
  let respuesta
  try {
    respuesta = await fetch(`${API_URL}/api/catalogo`, {
      headers: { Authorization: `Bearer ${token}` }, signal,
    })
  } catch (error) {
    if (signal?.aborted) throw error
    throw new Error('No se pudo conectar con el servidor de actividades.')
  }
  if (respuesta.status === 401) throw new Error('Tu sesión ha expirado. Vuelve a iniciar sesión.')
  if (!respuesta.ok) throw new Error('No se pudieron cargar las actividades publicadas.')
  const datos = await respuesta.json()
  return Array.isArray(datos?.actividades) ? datos.actividades : []
}
