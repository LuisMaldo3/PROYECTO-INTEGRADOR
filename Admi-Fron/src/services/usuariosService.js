import { ADMIN_URL } from '../config'
import { mensajeDeEstado } from './usuarios'

/** Error al hablar con Admin-back. `estado` es el código HTTP (0 si no hubo conexión). */
export class ApiError extends Error {
  constructor(estado, mensaje, detalle = '') {
    super(mensaje)
    this.estado = estado
    this.detalle = detalle // lo que dijo el servidor: "CODIGO: mensaje"
  }
}

async function pedir(ruta, token) {
  let respuesta
  try {
    respuesta = await fetch(`${ADMIN_URL}${ruta}`, { headers: { Authorization: `Bearer ${token}` } })
  } catch {
    throw new ApiError(0, 'No hay conexión con el servidor. Revisa que Admin-back esté encendido.')
  }
  const datos = await respuesta.json().catch(() => null)
  if (!respuesta.ok) {
    const detalle = datos?.codigo ? `${datos.codigo}: ${datos.mensaje ?? ''}`.trim() : ''
    throw new ApiError(respuesta.status, mensajeDeEstado(respuesta.status, datos?.mensaje), detalle)
  }
  return datos
}

/** Tarea 3 · GET /api/usuarios/ → arreglo de { id, nombre, correo, rol, activo }. */
export async function listarUsuarios(token) {
  const datos = await pedir('/api/usuarios/', token)
  return datos?.usuarios ?? []
}

/** GET /api/usuarios/<id>/ → { id, nombre, correo, rol, activo }. */
export function obtenerUsuario(token, id) {
  return pedir(`/api/usuarios/${encodeURIComponent(id)}/`, token)
}
