import { API_URL } from '../config.js'

export async function comprobarServicios(token, signal) {
  if (!token) throw new Error('Inicia sesión antes de comprobar la conexión.')
  let res
  try { res = await fetch(`${API_URL}/api/estado`, { headers: { Authorization: `Bearer ${token}` }, signal }) }
  catch (e) { if (e.name === 'AbortError') throw e; throw new Error(`No se puede llegar a ${API_URL}. Revisa que Spring Boot esté iniciado y que CORS permita la web.`) }
  if (!res.ok) throw new Error(res.status === 404 ? 'El backend está desactualizado: falta /api/estado.' : `Error al comprobar servicios (HTTP ${res.status}).`)
  return res.json()
}
