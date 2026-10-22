import { API_URL } from '../config.js'

async function peticion(ruta, token, { method = 'GET', body, signal } = {}) {
  if (!token) throw new Error('Inicia sesión para gestionar tu horario.')
  let response
  try {
    response = await fetch(`${API_URL}${ruta}`, {
      method, signal,
      headers: { Authorization: `Bearer ${token}`, ...(body instanceof FormData ? {} : { 'Content-Type': 'application/json' }) },
      ...(body ? { body: body instanceof FormData ? body : JSON.stringify(body) } : {}),
    })
  } catch (error) {
    if (signal?.aborted) throw error
    throw new Error('No se pudo conectar con Spring Boot. Revisa tu conexión.')
  }
  if (!response.ok) {
    if (response.status === 401) throw new Error('Tu sesión expiró. Inicia sesión nuevamente.')
    if (response.status === 413) throw new Error('La imagen supera el límite de 5 MB.')
    if (response.status === 503) throw new Error('La lectura con IA no está disponible. Configura GEMINI_API_KEY en el backend.')
    if (response.status === 404) throw new Error('Actualiza el backend para utilizar Mi disponibilidad.')
    throw new Error(`No se pudo completar la operación (HTTP ${response.status}).`)
  }
  return response.json()
}
export const obtenerDisponibilidad = (token, signal) => peticion('/api/disponibilidad', token, { signal })
export function interpretarHorario(token, imagen, signal) {
  if (!imagen || !['image/jpeg', 'image/png', 'image/webp'].includes(imagen.type)) throw new Error('Selecciona una imagen JPG, PNG o WEBP.')
  if (imagen.size > 5 * 1024 * 1024 || !imagen.size) throw new Error('La imagen debe pesar entre 1 byte y 5 MB.')
  const body = new FormData()
  body.append('imagen', imagen)
  return peticion('/api/disponibilidad/interpretar', token, { method: 'POST', body, signal })
}
export const guardarDisponibilidad = (token, bloques, signal) => peticion('/api/disponibilidad', token, { method: 'PUT', body: { bloques }, signal })
export const obtenerSugerenciasHuecos = (token, signal) => peticion('/api/agenda/huecos', token, { signal })

export function revisarBloques(bloques) {
  if (!Array.isArray(bloques) || bloques.length > 100) return 'Máximo 100 bloques por horario.'
  for (const b of bloques) {
    if (!Number.isInteger(Number(b.dia)) || Number(b.dia) < 1 || Number(b.dia) > 7) return 'Selecciona un día válido.'
    if (!/^([01]\d|2[0-3]):[0-5]\d$/.test(b.inicio) || !/^([01]\d|2[0-3]):[0-5]\d$/.test(b.fin)) return 'Revisa las horas; deben estar en formato HH:mm.'
    if (b.fin <= b.inicio) return 'La hora de fin debe ser posterior a la hora de inicio.'
  }
  return ''
}
