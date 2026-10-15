import { API_URL } from '../config'
import { leerSesion } from './authService'
export async function cargarRecomendaciones(signal) {
  const token = leerSesion()?.token
  if (!token) throw new Error('Inicia sesión para ver recomendaciones.')
  const res = await fetch(`${API_URL}/api/recomendaciones`, { signal, headers: { Authorization: `Bearer ${token}` } })
  if (!res.ok) throw new Error(res.status === 401 ? 'Tu sesión expiró.' : 'No se pudieron consultar las recomendaciones.')
  return res.json()
}
