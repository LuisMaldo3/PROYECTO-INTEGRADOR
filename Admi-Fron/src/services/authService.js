import { AUTH_URL } from '../config'
import { esAdministrador, tokenVigente } from './sesion'

const KEY_TOKEN = 'vynkAdmin.token'
const KEY_USUARIO = 'vynkAdmin.usuario'

/** Error de inicio de sesión con un mensaje listo para mostrar. */
export class AuthError extends Error {}

/**
 * Inicia sesión contra Usuario-back (POST /auth/login-password).
 * Devuelve { token, usuario } SOLO si la cuenta es administrador; si no, lanza AuthError.
 */
export async function loginAdmin(correo, password) {
  let respuesta
  try {
    respuesta = await fetch(`${AUTH_URL}/auth/login-password`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ correo: correo.trim(), password }),
    })
  } catch {
    throw new AuthError('No hay conexión con el servidor. Revisa que Usuario-back esté encendido.')
  }
  const datos = await respuesta.json().catch(() => null)
  if (!respuesta.ok) {
    throw new AuthError(datos?.mensaje ?? 'No se pudo iniciar sesión. Inténtalo nuevamente.')
  }
  if (!esAdministrador(datos?.usuario)) {
    // No se guarda nada: esta web es solo para administradores.
    throw new AuthError('Esta cuenta no tiene permiso de administrador.')
  }
  return { token: datos.token, usuario: datos.usuario }
}

export function guardarSesion({ token, usuario }) {
  localStorage.setItem(KEY_TOKEN, token)
  localStorage.setItem(KEY_USUARIO, JSON.stringify(usuario))
}

/** { token, usuario } si hay una sesión vigente de administrador; si no, null. */
export function leerSesion() {
  try {
    const token = localStorage.getItem(KEY_TOKEN)
    const usuario = JSON.parse(localStorage.getItem(KEY_USUARIO) ?? 'null')
    if (!token || !usuario || !tokenVigente(token) || !esAdministrador(usuario)) {
      borrarSesionLocal()
      return null
    }
    return { token, usuario }
  } catch {
    return null
  }
}

function borrarSesionLocal() {
  try {
    localStorage.removeItem(KEY_TOKEN)
    localStorage.removeItem(KEY_USUARIO)
  } catch {
    // el navegador bloquea el almacenamiento: no hay nada que borrar
  }
}

/** Cierra la sesión en el servidor (el token deja de servir) y borra los datos locales. */
export async function cerrarSesion(token) {
  try {
    if (token) {
      await fetch(`${AUTH_URL}/auth/logout`, { method: 'POST', headers: { Authorization: `Bearer ${token}` } })
    }
  } catch {
    // sin conexión: igual se cierra aquí
  }
  borrarSesionLocal()
}
