/**
 * Tarea 4 (web): decide si se PERMITE o se BLOQUEA el acceso según la respuesta del backend.
 *
 * Es una función pura (no usa red ni pantallas), así se puede probar fácil con `npm test`.
 * No mira el rol: todas las cuentas entran como "usuario"; el rol de organizador
 * solo lo asigna el administrador más adelante.
 */

export const MENSAJES = {
  SIN_CONEXION: 'No pudimos conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.',
  RESPUESTA_INVALIDA: 'El servidor respondió de forma inesperada. Inténtalo nuevamente.',
  ERROR_SERVIDOR: 'El servidor tuvo un problema. Inténtalo en unos minutos.',
  ERROR: 'No se pudo iniciar sesión. Inténtalo nuevamente.',
}

function bloquear(codigo, mensaje) {
  return { permitido: false, codigo, mensaje }
}

/**
 * @param {{ estado?: number, datos?: object|null, errorDeRed?: boolean }} respuesta
 * @returns {{ permitido: true, sesion: { token: string, usuario: object } }
 *         | { permitido: false, codigo: string, mensaje: string }}
 */
export function decidirAcceso({ estado, datos = null, errorDeRed = false }) {
  if (errorDeRed) return bloquear('SIN_CONEXION', MENSAJES.SIN_CONEXION)

  if (estado === 200) {
    const sesionCompleta = datos && typeof datos.token === 'string' && datos.token && datos.usuario?.correo
    return sesionCompleta
      ? { permitido: true, sesion: { token: datos.token, usuario: datos.usuario } }
      : bloquear('RESPUESTA_INVALIDA', MENSAJES.RESPUESTA_INVALIDA)
  }

  // 400, 401, 403, 5xx... : se bloquea y se muestra el mensaje que mandó el backend (si lo hay).
  const generico = estado >= 500 ? MENSAJES.ERROR_SERVIDOR : MENSAJES.ERROR
  return bloquear(datos?.codigo ?? (estado >= 500 ? 'ERROR_SERVIDOR' : 'ERROR'), datos?.mensaje ?? generico)
}

/** true si el JWT se puede leer y todavía no venció. */
export function tokenVigente(token, ahora = Date.now()) {
  try {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    const { exp } = JSON.parse(atob(base64))
    return typeof exp === 'number' && exp * 1000 > ahora
  } catch {
    return false
  }
}
