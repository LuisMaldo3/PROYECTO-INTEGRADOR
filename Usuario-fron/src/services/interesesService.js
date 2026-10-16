import { API_URL } from '../config.js'

/** Error de la API de intereses. `estado` es el código HTTP (0 si no hubo conexión). */
export class ErrorIntereses extends Error {
  constructor(mensaje, estado) {
    super(mensaje)
    this.estado = estado
  }
}

async function pedir(ruta, token, { metodo = 'GET', cuerpo, signal } = {}) {
  let respuesta
  try {
    respuesta = await fetch(`${API_URL}${ruta}`, {
      method: metodo,
      headers: { Authorization: `Bearer ${token}`, ...(cuerpo ? { 'Content-Type': 'application/json' } : {}) },
      body: cuerpo ? JSON.stringify(cuerpo) : undefined,
      signal,
    })
  } catch (error) {
    if (signal?.aborted) throw error
    throw new ErrorIntereses('No se pudo conectar con el servidor. Revisa tu conexión.', 0)
  }
  const datos = await respuesta.json().catch(() => null)
  if (!respuesta.ok) {
    const mensaje =
      respuesta.status === 401
        ? 'Tu sesión expiró. Vuelve a iniciar sesión.'
        : (datos?.mensaje ?? 'No se pudo completar la operación. Inténtalo nuevamente.')
    throw new ErrorIntereses(mensaje, respuesta.status)
  }
  return datos
}

const lista = (datos) => (Array.isArray(datos?.intereses) ? datos.intereses : [])

/** GET /api/intereses → [{ id, nombre, descripcion }] los disponibles */
export async function listarIntereses(token, signal) {
  return lista(await pedir('/api/intereses', token, { signal }))
}

/** GET /api/intereses/mios → ["teatro", …] lo que eligió la persona ([] si todavía no eligió) */
export async function obtenerMisIntereses(token, signal) {
  return lista(await pedir('/api/intereses/mios', token, { signal }))
}

/** PUT /api/intereses/mios → ["teatro", …] lo que quedó guardado. Si el servidor rechaza algo, lanza ErrorIntereses con su mensaje. */
export async function guardarMisIntereses(token, ids) {
  return lista(await pedir('/api/intereses/mios', token, { metodo: 'PUT', cuerpo: { intereses: ids } }))
}

/** Lee el estado de onboarding sin interpretar «no guardó» como «omitió». */
export async function consultarEstadoIntereses(token, signal) {
  const datos = await pedir('/api/intereses/mios', token, { signal })
  return { configurados: datos?.configurados === true, omitido: datos?.omitido === true }
}

/** POST /api/intereses/omitir: el servidor recuerda que desea elegir después. */
export async function omitirIntereses(token) {
  return pedir('/api/intereses/omitir', token, { metodo: 'POST' })
}
