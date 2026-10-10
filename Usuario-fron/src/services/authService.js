import { API_URL } from '../config'
import { decidirAcceso, decidirQuienSoy, MENSAJES, tokenVigente } from './acceso'
import { normalizarUsuario } from './permisos'
import { borrarDatosDeSesion, sinTokenGuardado } from './sesionLocal'

const KEY_TOKEN = 'vynk.token'
const KEY_USUARIO = 'vynk.usuario'

/** Error con el código y mensaje que devuelve el backend ({ codigo, mensaje }). */
export class AuthError extends Error {
  constructor(codigo, mensaje) {
    super(mensaje)
    this.codigo = codigo
  }
}

/** Hace la petición y devuelve { estado, datos } o { errorDeRed: true } si no hubo conexión. */
async function enviar(ruta, { metodo = 'POST', cuerpo, token } = {}) {
  try {
    const respuesta = await fetch(`${API_URL}${ruta}`, {
      method: metodo,
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify(cuerpo),
    })
    const datos = await respuesta.json().catch(() => null)
    return { estado: respuesta.status, datos }
  } catch {
    return { errorDeRed: true }
  }
}

/** Tarea 4: si se PERMITE devuelve { token, usuario }; si se BLOQUEA lanza AuthError con el motivo. */
function resolverAcceso(respuesta) {
  const decision = decidirAcceso(respuesta)
  if (!decision.permitido) throw new AuthError(decision.codigo, decision.mensaje)
  return decision.sesion
}

/** Envía el ID token de Google (POST /auth/login). */
export async function loginConGoogle(idToken) {
  return resolverAcceso(await enviar('/auth/login', { cuerpo: { idToken } }))
}

/** Login con correo y contraseña (POST /auth/login-password). */
export async function loginConPassword(correo, password) {
  return resolverAcceso(await enviar('/auth/login-password', { cuerpo: { correo, password } }))
}

/** Crea o cambia la contraseña del usuario con sesión (PUT /auth/password). */
export async function definirPassword(token, password) {
  const r = await enviar('/auth/password', { metodo: 'PUT', cuerpo: { password }, token })
  if (r.errorDeRed) throw new AuthError('SIN_CONEXION', MENSAJES.SIN_CONEXION)
  if (r.estado === 401) throw new AuthError('SESION_EXPIRADA', 'Tu sesión expiró. Cierra sesión y vuelve a entrar.')
  if (r.estado < 200 || r.estado >= 300) {
    throw new AuthError(r.datos?.codigo ?? 'ERROR', r.datos?.mensaje ?? 'No se pudo completar la operación. Inténtalo nuevamente.')
  }
}

/**
 * «Quién soy» (GET /auth/me): datos actuales del usuario, con su rol al día.
 * Devuelve el usuario o lanza AuthError (código SESION_EXPIRADA si el token ya no sirve).
 */
export async function quienSoy(token) {
  const decision = decidirQuienSoy(await enviar('/auth/me', { metodo: 'GET', token }))
  if (!decision.valido) throw new AuthError(decision.codigo, decision.mensaje)
  return decision.usuario
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
    if (!token || !usuario || !tokenVigente(token)) {
      cerrarSesion()
      return null
    }
    return { token, usuario: normalizarUsuario(usuario) }
  } catch {
    return null
  }
}

/**
 * Avisa al servidor que se cierra la sesión (POST /auth/logout): el token deja de servir
 * aunque no haya vencido. Si no hay conexión o falla, no pasa nada: la sesión igual se cierra aquí.
 */
export async function cerrarSesionEnServidor(token) {
  await enviar('/auth/logout', { token })
}

/** true si otra pestaña cerró la sesión (ya no hay token). Solo lee: no borra nada. */
export function sesionCerradaEnOtraPestana() {
  try {
    return sinTokenGuardado(globalThis.localStorage, KEY_TOKEN)
  } catch {
    return false
  }
}

/** Almacenes del navegador (algunos bloquean el acceso: se omiten). */
function almacenesDelNavegador() {
  const lista = []
  for (const nombre of ['localStorage', 'sessionStorage']) {
    try {
      lista.push(globalThis[nombre])
    } catch {
      // bloqueado por el navegador
    }
  }
  return lista
}

/** Borra TODO lo guardado de VYNK en el navegador (token, usuario y lo que se agregue después). */
export function cerrarSesion() {
  borrarDatosDeSesion(almacenesDelNavegador())
  window.google?.accounts?.id?.disableAutoSelect?.()
}
