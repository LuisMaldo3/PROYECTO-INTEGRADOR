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

/**
 * Pantallas de la web y qué roles pueden verlas.
 *
 * Por ahora solo existen las del usuario (estudiante) y todas las ven todos los roles.
 * Las pantallas del organizador se agregan aquí más adelante, con `roles: [ROLES.ORGANIZADOR]`;
 * el menú, el bloqueo y «Acceso no autorizado» ya funcionan con esa lista.
 * (El administrador usa su propia web, aparte.)
 */
export const PANTALLAS = [
  { ruta: 'inicio', titulo: 'Inicio', descripcion: 'Resumen de tu participación en la T&C Week.', tituloMenu: 'Inicio', roles: ROLES_VALIDOS },
  { ruta: 'catalogo', titulo: 'Catálogo de actividades', descripcion: 'Actividades disponibles durante la T&C Week.', tituloMenu: 'Catálogo', roles: ROLES_VALIDOS },
  { ruta: 'recomendaciones', titulo: 'Recomendaciones para ti', descripcion: 'Actividades seleccionadas con ayuda de IA según tus intereses.', tituloMenu: 'Para ti', roles: ROLES_VALIDOS },
  { ruta: 'recomendaciones', titulo: 'Recomendaciones para ti', descripcion: 'Actividades seleccionadas con ayuda de IA según tus intereses.', tituloMenu: 'Para ti', roles: ROLES_VALIDOS },
  { ruta: 'agenda', titulo: 'Mi agenda', descripcion: 'Tus actividades inscritas, organizadas por día.', tituloMenu: 'Agenda', roles: ROLES_VALIDOS },
  { ruta: 'asistente', titulo: 'Asistente', descripcion: 'Consulta horarios, ubicaciones y cupos.', tituloMenu: 'Asistente', roles: ROLES_VALIDOS },
  { ruta: 'perfil', titulo: 'Mi perfil', descripcion: 'Tus datos institucionales y tu sesión.', tituloMenu: 'Perfil', roles: ROLES_VALIDOS },
  // Se abre desde Perfil: no tiene botón propio en el menú y, mientras se está en ella, se marca «Perfil».
  { ruta: 'intereses', titulo: 'Mis intereses', descripcion: 'Elige los temas que te gustan y te recomendaremos actividades.', tituloMenu: 'Intereses', roles: ROLES_VALIDOS, enMenu: false, menuActivo: 'perfil' },
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

export function pantallaDe(ruta, lista = PANTALLAS) {
  return lista.find((p) => p.ruta === ruta) ?? null
}

/** true si ese rol puede ver esa pantalla. Una pantalla que no existe no la ve nadie. */
export function puedeVer(rol, ruta, lista = PANTALLAS) {
  return pantallaDe(ruta, lista)?.roles.includes(normalizarRol(rol)) ?? false
}

/** Pantallas que le tocan a ese rol: las del menú. */
export function pantallasDelMenu(rol, lista = PANTALLAS) {
  return lista.filter((p) => p.enMenu !== false && puedeVer(rol, p.ruta, lista))
}

/** "#/agenda" -> "agenda". Sin nada (o "#/") es "inicio". */
export function rutaDeHash(hash = '') {
  const ruta = String(hash).replace(/^#\/?/, '').split(/[?/]/)[0].trim().toLowerCase()
  return ruta || 'inicio'
}

/**
 * Decide qué hacer con la dirección actual según el rol.
 * @returns {{ ruta: string, pantalla: object|null, estado: 'permitida'|'bloqueada'|'inexistente' }}
 */
export function resolverRuta(hash, rol, lista = PANTALLAS) {
  const ruta = rutaDeHash(hash)
  const pantalla = pantallaDe(ruta, lista)
  if (!pantalla) return { ruta, pantalla: null, estado: 'inexistente' }
  return { ruta, pantalla, estado: puedeVer(rol, ruta, lista) ? 'permitida' : 'bloqueada' }
}
