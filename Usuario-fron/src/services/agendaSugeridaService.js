import { API_URL } from '../config.js'

/** La agenda sugerida se calcula en el backend a partir de datos del alumno autenticado. */
export async function obtenerAgendaSugerida(token, signal) {
  if (!token) throw new Error('Inicia sesión para generar tu agenda sugerida.')
  let respuesta
  try {
    respuesta = await fetch(`${API_URL}/api/agenda/sugerida`, {
      headers: { Authorization: `Bearer ${token}`, Accept: 'application/json' },
      signal,
    })
  } catch (error) {
    if (signal?.aborted) throw error
    throw new Error('No se pudo conectar con el servidor. Comprueba que Spring Boot esté encendido.')
  }
  if (!respuesta.ok) {
    if (respuesta.status === 401) throw new Error('Tu sesión expiró. Vuelve a iniciar sesión.')
    if (respuesta.status === 404) throw new Error('El backend aún no dispone del servicio de agenda sugerida.')
    if (respuesta.status === 503) throw new Error('La base de datos no está disponible. Inténtalo después.')
    throw new Error(`No se pudo crear la agenda sugerida (HTTP ${respuesta.status}).`)
  }
  const datos = await respuesta.json()
  return {
    origen: ['ia', 'reglas', 'sin_actividades'].includes(datos?.origen) ? datos.origen : 'reglas',
    aviso: typeof datos?.aviso === 'string' ? datos.aviso : '',
    intereses: Array.isArray(datos?.intereses) ? datos.intereses : [],
    inscripciones: Array.isArray(datos?.inscripciones) ? datos.inscripciones : [],
    actividades: Array.isArray(datos?.actividades) ? datos.actividades : [],
  }
}
