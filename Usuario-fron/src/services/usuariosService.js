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

// [Jordi] listarUsuarios(token) es de la tarea 3: se reemplaza por su archivo al integrar.
export async function listarUsuarios() {
  return []
}

/** GET /api/usuarios/<id>/ → { id, nombre, correo, rol, activo }. */
export function obtenerUsuario(token, id) {
  return pedir(`/api/usuarios/${encodeURIComponent(id)}/`, token)
}
