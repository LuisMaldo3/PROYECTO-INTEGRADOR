import { API_URL } from '../config.js'

/** Recupera alternativas publicadas y compatibles para un cruce real del estudiante. */
export async function obtenerAlternativasCruce(token, cruceId, signal) {
  if (!token) throw new Error('Inicia sesión para consultar alternativas.')
  if (!cruceId) throw new Error('No se identificó el cruce de horario.')
  let respuesta
  try {
    respuesta = await fetch(`${API_URL}/api/agenda/cruces/${encodeURIComponent(cruceId)}/alternativas`, {
      headers: { Authorization: `Bearer ${token}`, Accept: 'application/json' }, signal,
    })
  } catch (e) {
    if (signal?.aborted) throw e
    throw new Error('No se pudo conectar con el servidor para consultar alternativas.')
  }
  if (!respuesta.ok) {
    if (respuesta.status === 401) throw new Error('Tu sesión expiró. Inicia sesión otra vez.')
    if (respuesta.status === 404) throw new Error('Este cruce ya no está disponible. Actualiza tu agenda.')
    throw new Error(`No pudimos cargar alternativas (HTTP ${respuesta.status}).`)
  }
  const datos = await respuesta.json()
  return { alternativas: Array.isArray(datos?.alternativas) ? datos.alternativas : [],
    aviso: typeof datos?.aviso === 'string' ? datos.aviso : '' }
}
