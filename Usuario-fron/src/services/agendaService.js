import { API_URL } from '../config'

export async function obtenerAgenda(token, signal) {
  const respuesta = await fetch(`${API_URL}/api/agenda`, {
    headers: { Authorization: `Bearer ${token}` }, signal,
  })
  if (!respuesta.ok) {
    if (respuesta.status === 401) throw new Error('Tu sesión ha expirado. Vuelve a iniciar sesión.')
    throw new Error('No se pudo cargar tu agenda. Inténtalo nuevamente.')
  }
  const datos = await respuesta.json()
  return Array.isArray(datos) ? datos : []
}
