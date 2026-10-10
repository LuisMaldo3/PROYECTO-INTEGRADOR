/**
 * Lógica pura de la sesión del administrador (sin red ni navegador, para poder probarla con `npm test`).
 */

export const ROL_ADMIN = 'administrador'

/** Lee el contenido (claims) de un JWT sin verificar la firma; null si no es un JWT. */
export function leerClaims(token) {
  try {
    const parte = token.split('.')[1]
    const base64 = parte.replace(/-/g, '+').replace(/_/g, '/')
    const bytes = Uint8Array.from(atob(base64), (c) => c.charCodeAt(0))
    return JSON.parse(new TextDecoder().decode(bytes))
  } catch {
    return null
  }
}

/** true si el token tiene fecha de vencimiento y todavía no pasó (el servidor igual lo vuelve a verificar). */
export function tokenVigente(token, ahoraEnSegundos = Date.now() / 1000) {
  const claims = leerClaims(token)
  return Boolean(claims && typeof claims.exp === 'number' && claims.exp > ahoraEnSegundos)
}

export function normalizarRol(rol) {
  return typeof rol === 'string' ? rol.trim().toLowerCase() : ''
}

export function esAdministrador(usuario) {
  return normalizarRol(usuario?.rol) === ROL_ADMIN
}
