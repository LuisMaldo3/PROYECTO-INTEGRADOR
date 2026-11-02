import { API_URL } from '../config'
import { leerSesion } from './authService'

/** Avisos públicos: mismos documentos de MongoDB que publica Django. */
export async function listarAvisosPublicos(signal) {
  const token = leerSesion()?.token
  if (!token) throw new Error('Inicia sesión para consultar los avisos.')
  const r = await fetch(`${API_URL}/api/avisos/publicados`, { signal, headers: { Authorization: `Bearer ${token}` } })
  if (!r.ok) throw new Error('No se pudieron cargar los avisos.')
  const datos = await r.json()
  return Array.isArray(datos.avisos) ? datos.avisos.slice().sort((a,b) =>
    Date.parse(b.publicadoEn || 0) - Date.parse(a.publicadoEn || 0)) : []
}
