import { ADMIN_URL } from '../config.js'
import { normalizarTorneo } from './torneosOrganizador.js'

/** Error del servidor con el detalle por campo (`errores`) que devuelve Admin-back. */
export class ErrorTorneo extends Error {
  constructor(estado, mensaje, errores = {}) { super(mensaje); this.estado = estado; this.errores = errores }
}

async function pedir(ruta, token, { signal, metodo = 'GET', cuerpo } = {}) {
  let res
  try {
    res = await fetch(`${ADMIN_URL}${ruta}`, { method: metodo, signal, headers: { Authorization: `Bearer ${token}`, Accept: 'application/json', ...(cuerpo ? { 'Content-Type': 'application/json' } : {}) }, body: cuerpo ? JSON.stringify(cuerpo) : undefined })
  } catch (e) {
    if (e.name === 'AbortError') throw e
    throw new ErrorTorneo(0, `No se pudo conectar con el servidor de competencias (${ADMIN_URL}). Revisa que Admin-back (Django) esté encendido en esa dirección; si usa otra, cámbiala en VITE_ADMIN_URL del archivo .env.local de Usuario-fron.`)
  }
  const datos = await res.json().catch(() => null)
  if (res.ok) return datos
  if (res.status === 401) throw new ErrorTorneo(401, 'Tu sesión expiró. Inicia sesión nuevamente.')
  if (res.status === 403) throw new ErrorTorneo(403, datos?.mensaje || 'No tienes permiso para esta acción.')
  throw new ErrorTorneo(res.status, datos?.mensaje || 'No se pudo completar la acción.', datos?.errores ?? {})
}

/** Torneos asignados al organizador. */
export const listarMisTorneos = async (token, signal) => ((await pedir('/api/organizador/torneos/', token, { signal })).torneos ?? []).map(normalizarTorneo)

/** Programa, cambia el estado o registra el resultado de un enfrentamiento; devuelve el torneo actualizado. */
export async function accionPartido(token, torneoId, partidoId, accion, cuerpo) {
  const ruta = `/api/organizador/torneos/${encodeURIComponent(torneoId)}/partidos/${encodeURIComponent(partidoId)}/${accion}/`
  return normalizarTorneo(await pedir(ruta, token, { metodo: 'POST', cuerpo }))
}

/** Competencias que te asignaron, con lo que ya tienen (torneo u hoja de puntajes). */
export const listarCompetencias = async (token, signal) => (await pedir('/api/organizador/competencias/', token, { signal })).competencias ?? []
/** Arma el torneo de tu competencia (todos los inscritos confirmados, o en el orden que se indique). */
export const crearTorneo = async (token, datos) => normalizarTorneo(await pedir('/api/organizador/torneos/', token, { metodo: 'POST', cuerpo: datos }))
/** La IA propone el orden de siembra: solo propone, tú decides. */
export const proponerSorteo = (token, actividadId) => pedir('/api/organizador/torneos/propuesta-ia/', token, { metodo: 'POST', cuerpo: { actividadId } })
export const obtenerHoja = (token, actividadId) => pedir(`/api/organizador/puntajes/${encodeURIComponent(actividadId)}/`, token)
export const guardarHoja = (token, actividadId, cuerpo) => pedir(`/api/organizador/puntajes/${encodeURIComponent(actividadId)}/`, token, { metodo: 'PUT', cuerpo })
