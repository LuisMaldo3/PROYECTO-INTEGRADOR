import { API_URL } from '../config'

async function leer(url, token, signal) {
  const respuesta = await fetch(`${API_URL}${url}`, {
    headers: { Authorization: `Bearer ${token}` }, signal,
  })
  if (!respuesta.ok) throw new Error(respuesta.status === 401 ? 'Inicia sesión nuevamente.' : 'No se pudieron consultar los cruces.')
  return respuesta.json()
}
export const obtenerCruces = (token, signal) => leer('/api/agenda/cruces', token, signal)
export const explicarCruce = (token, id, signal) => leer(`/api/agenda/cruces/${encodeURIComponent(id)}/explicacion`, token, signal)
export { actividadesEnCruce } from './cruces.js'
