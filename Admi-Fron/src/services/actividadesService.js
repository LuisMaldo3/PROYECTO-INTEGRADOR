import { pedir } from './usuariosService'
import { armarCuerpo } from './actividades.js'
import { listaDe } from './listas.js'

/** GET /api/categorias/ → [{ id, nombre }] */
export async function listarCategorias(token) {
  const datos = await pedir('/api/categorias/', token)
  return listaDe(datos, 'categorias')
}

/** GET /api/ubicaciones/ → [{ id, nombre }] (por nombre) */
export async function listarUbicaciones(token) {
  const datos = await pedir('/api/ubicaciones/', token)
  return listaDe(datos, 'ubicaciones')
}

/** GET /api/organizadores/ → usuarios activos con rol organizador */
export async function listarOrganizadores(token) {
  const datos = await pedir('/api/organizadores/', token)
  return listaDe(datos, 'organizadores')
}

/** GET /api/actividades/ → [{ id, nombre, descripcion, categoria, ubicacion, fecha, horaInicio, horaFin, ... }] por fecha y hora; `ubicacion` es null en las actividades viejas */
export async function listarActividades(token) {
  const datos = await pedir('/api/actividades/', token)
  return listaDe(datos, 'actividades')
}

/** POST /api/actividades/ → la actividad creada. Si el servidor rechaza algo, el error trae `errores` por campo. */
export function crearActividad(token, valores) {
  return pedir('/api/actividades/', token, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(armarCuerpo(valores)),
  })
}

/** PUT /api/actividades/:id/ edita una actividad existente. */
export function modificarActividad(token, id, valores, { avisarInscritos = false } = {}) {
  return pedir(`/api/actividades/${encodeURIComponent(id)}/`, token, {
    method: 'PUT', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ ...armarCuerpo(valores), avisarInscritos }),
  })
}


/** PATCH /api/actividades/:id/cancelar/ guarda la cancelación y registra notificaciones a inscritos. */
export function cancelarActividad(token, id) {
  return pedir(`/api/actividades/${encodeURIComponent(id)}/cancelar/`, token, { method: 'PATCH' })
}
