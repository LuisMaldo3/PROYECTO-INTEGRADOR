import { API_URL } from '../config'

const KEY_TOKEN = 'vynk.token'
const KEY_USUARIO = 'vynk.usuario'

/** Error con el código y mensaje que devuelve el backend ({ codigo, mensaje }). */
export class AuthError extends Error {
  constructor(codigo, mensaje) {
    super(mensaje)
    this.codigo = codigo
  }
}

async function pedir(ruta, { metodo = 'POST', cuerpo, token } = {}) {
  let respuesta
  try {
    respuesta = await fetch(`${API_URL}${ruta}`, {
      method: metodo,
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify(cuerpo),
    })
  } catch {
    throw new AuthError('SIN_CONEXION', 'No pudimos conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.')
  }

  if (respuesta.status === 401 && token) {
    throw new AuthError('SESION_EXPIRADA', 'Tu sesión expiró. Cierra sesión y vuelve a entrar.')
  }

  const datos = await respuesta.json().catch(() => null)
  if (!respuesta.ok) {
    throw new AuthError(
      datos?.codigo ?? 'ERROR',
      datos?.mensaje ?? 'No se pudo completar la operación. Inténtalo nuevamente.',
    )
  }
  return datos
}

/** Envía el ID token de Google (POST /auth/login). Devuelve { token, usuario }. */
export function loginConGoogle(idToken) {
  return pedir('/auth/login', { cuerpo: { idToken } })
}

/** Login con correo y contraseña (POST /auth/login-password). Devuelve { token, usuario }. */
export function loginConPassword(correo, password) {
  return pedir('/auth/login-password', { cuerpo: { correo, password } })
}

/** Crea o cambia la contraseña del usuario con sesión (PUT /auth/password). */
export function definirPassword(token, password) {
  return pedir('/auth/password', { metodo: 'PUT', cuerpo: { password }, token })
}

export function guardarSesion({ token, usuario }) {
  localStorage.setItem(KEY_TOKEN, token)
  localStorage.setItem(KEY_USUARIO, JSON.stringify(usuario))
}

/** Devuelve { token, usuario } si hay una sesión vigente; si no, null. */
export function leerSesion() {
  try {
    const token = localStorage.getItem(KEY_TOKEN)
    const usuario = JSON.parse(localStorage.getItem(KEY_USUARIO) ?? 'null')
    if (!token || !usuario || tokenExpirado(token)) {
      cerrarSesion()
      return null
    }
    return { token, usuario }
  } catch {
    return null
  }
}

export function cerrarSesion() {
  localStorage.removeItem(KEY_TOKEN)
  localStorage.removeItem(KEY_USUARIO)
  window.google?.accounts?.id?.disableAutoSelect?.()
}

function tokenExpirado(token) {
  try {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    const { exp } = JSON.parse(atob(base64))
    return typeof exp === 'number' && exp * 1000 <= Date.now()
  } catch {
    return true
  }
}
