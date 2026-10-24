import { ADMIN_URL } from '../config.js'
import { mensajeDeEstado } from './usuarios.js'

/** Error al hablar con Admin-back. `estado` es el código HTTP (0 si no hubo conexión). */
export class ApiError extends Error {
  constructor(estado, mensaje, detalle = '', errores = {}) {
    super(mensaje)
    this.estado = estado
    this.detalle = detalle // lo que dijo el servidor: "CODIGO: mensaje"
    this.errores = errores // { campo: mensaje } cuando el servidor rechaza datos de un formulario
  }
}

export async function pedir(ruta, token, opciones = {}) {
  let respuesta
  try {
    respuesta = await fetch(`${ADMIN_URL}${ruta}`, {
      ...opciones,
      headers: { Authorization: `Bearer ${token}`, ...opciones.headers },
    })
  } catch {
    throw new ApiError(0, 'No hay conexión con el servidor. Revisa que Admin-back esté encendido.')
  }
  const datos = await respuesta.json().catch(() => null)
  if (!respuesta.ok) {
    const detalle = datos?.codigo ? `${datos.codigo}: ${datos.mensaje ?? ''}`.trim() : ''
    throw new ApiError(respuesta.status, mensajeDeEstado(respuesta.status, datos?.mensaje), detalle, datos?.errores)
  }
  return datos
}

/** Tarea 3 · GET /api/usuarios/ → arreglo de { id, nombre, correo, rol, activo }. */
export async function listarUsuarios(token, busqueda = '') {
  const q = String(busqueda).trim().slice(0, 100)
  const ruta = q ? `/api/usuarios/?q=${encodeURIComponent(q)}` : '/api/usuarios/'
  const datos = await pedir(ruta, token)
  return datos?.usuarios ?? []
}

/** GET /api/usuarios/<id>/ → { id, nombre, correo, rol, activo }. */
export function obtenerUsuario(token, id) {
  return pedir(`/api/usuarios/${encodeURIComponent(id)}/`, token)
}

/** US-24 · Tarea 3 · PATCH /api/usuarios/<id>/rol/ → usuario actualizado { id, nombre, correo, rol, activo }. */
export function cambiarRol(token, id, rol) {
  return pedir(`/api/usuarios/${encodeURIComponent(id)}/rol/`, token, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ rol }),
  })
}


/** GET/PUT /api/usuarios/<id>/permisos/ */
export function obtenerPermisos(token, id) {
  return pedir(`/api/usuarios/${encodeURIComponent(id)}/permisos/`, token)
}

export function guardarPermisos(token, id, permisos) {
  return pedir(`/api/usuarios/${encodeURIComponent(id)}/permisos/`, token, {
    method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ permisos }),
  })
}

/** PATCH /api/usuarios/<id>/estado/ */
export function cambiarEstado(token, id, activo) {
  return pedir(`/api/usuarios/${encodeURIComponent(id)}/estado/`, token, {
    method: 'PATCH', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ activo }),
  })
}
