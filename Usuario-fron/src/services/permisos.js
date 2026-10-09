/**
 * Roles y pantallas de la web (tareas 4, 5 y 6).
 *
 * Es lógica pura (sin red ni pantallas) para poder probarla con `npm test`.
 *
 * IMPORTANTE: esto solo decide qué se MUESTRA en la web. No es seguridad: quien manda
 * es el backend, que ya convierte el rol del token en ROLE_* (hasRole) para proteger
 * cada endpoint. Ocultar un botón aquí nunca reemplaza esa protección.
 */

export const ROLES = {
  USUARIO: 'usuario',
  ORGANIZADOR: 'organizador',
  ADMINISTRADOR: 'administrador',
}

const ROLES_VALIDOS = Object.values(ROLES)

export const ETIQUETAS_ROL = {
  usuario: 'Usuario',
  organizador: 'Organizador',
  administrador: 'Administrador',
}

export const DESCRIPCION_ROL = {
  usuario: 'Puedes ver y participar en las actividades de la T&C Week.',
  organizador: 'Puedes organizar actividades de la T&C Week.',
  administrador: 'Gestionas los usuarios y los roles de VYNK.',
}

/**
 * Pantallas de la web y qué roles pueden verlas.
 * Para sumar una pantalla nueva basta con agregarla aquí: el menú, los accesos del
 * inicio y el bloqueo salen de esta lista.
 */
export const PANTALLAS = [
  {
    ruta: 'inicio',
    titulo: 'Inicio',
    tituloMenu: 'Inicio',
    descripcion: 'Tu perfil y tus accesos.',
    roles: [ROLES.USUARIO, ROLES.ORGANIZADOR, ROLES.ADMINISTRADOR],
  },
  {
    ruta: 'organizador',
    titulo: 'Panel del organizador',
    tituloMenu: 'Organizar',
    descripcion: 'Crea y gestiona las actividades de la T&C Week.',
    roles: [ROLES.ORGANIZADOR, ROLES.ADMINISTRADOR],
  },
  {
    ruta: 'usuarios',
    titulo: 'Gestión de usuarios',
    tituloMenu: 'Usuarios',
    descripcion: 'Consulta a los usuarios y asigna roles.',
    roles: [ROLES.ADMINISTRADOR],
  },
]

/** Devuelve un rol válido: lo que no se reconoce (o falta) cuenta como "usuario" (el de menos permisos). */
export function normalizarRol(rol) {
  const limpio = typeof rol === 'string' ? rol.trim().toLowerCase() : ''
  return ROLES_VALIDOS.includes(limpio) ? limpio : ROLES.USUARIO
}

/** Copia del usuario con el rol normalizado (así lo que se guarda y se lee siempre tiene un rol válido). */
export function normalizarUsuario(usuario) {
  return usuario && typeof usuario === 'object' ? { ...usuario, rol: normalizarRol(usuario.rol) } : usuario
}

export function pantallaDe(ruta) {
  return PANTALLAS.find((p) => p.ruta === ruta) ?? null
}

/** true si ese rol puede ver esa pantalla. Una pantalla que no existe no la ve nadie. */
export function puedeVer(rol, ruta) {
  return pantallaDe(ruta)?.roles.includes(normalizarRol(rol)) ?? false
}

/** Pantallas que le tocan a ese rol: las del menú. */
export function pantallasDelMenu(rol) {
  return PANTALLAS.filter((p) => puedeVer(rol, p.ruta))
}

/** "#/usuarios" -> "usuarios". Sin nada (o "#/") es "inicio". */
export function rutaDeHash(hash = '') {
  const ruta = String(hash).replace(/^#\/?/, '').split(/[?/]/)[0].trim().toLowerCase()
  return ruta || 'inicio'
}

/**
 * Decide qué hacer con la dirección actual según el rol.
 * @returns {{ ruta: string, pantalla: object|null, estado: 'permitida'|'bloqueada'|'inexistente' }}
 */
export function resolverRuta(hash, rol) {
  const ruta = rutaDeHash(hash)
  const pantalla = pantallaDe(ruta)
  if (!pantalla) return { ruta, pantalla: null, estado: 'inexistente' }
  return { ruta, pantalla, estado: puedeVer(rol, ruta) ? 'permitida' : 'bloqueada' }
}
