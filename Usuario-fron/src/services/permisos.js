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
  usuario: 'Estudiante',
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
  { ruta: 'inicio', titulo: 'Inicio', descripcion: 'Tu resumen de actividades y avisos.', tituloMenu: 'Inicio', roles: ROLES_VALIDOS },
  { ruta: 'actividades', titulo: 'Actividades', descripcion: 'Competencias, talleres y eventos del campus.', tituloMenu: 'Actividades', roles: ROLES_VALIDOS },
  { ruta: 'actividad', titulo: 'Detalle de actividad', descripcion: 'Conoce los requisitos y confirma tu inscripción.', tituloMenu: 'Actividades', roles: ROLES_VALIDOS, enMenu: false, menuActivo: 'actividades' },
  { ruta: 'recomendaciones', titulo: 'Recomendaciones para ti', descripcion: 'Actividades sugeridas según tus intereses.', tituloMenu: 'Para ti', roles: ROLES_VALIDOS },
  { ruta: 'resultados', titulo: 'Resultados de T&C Week', descripcion: 'Clasificación de las cinco áreas y ganadores.', tituloMenu: 'Resultados', roles: ROLES_VALIDOS },
  { ruta: 'agenda', titulo: 'Mi agenda', descripcion: 'Tus actividades inscritas, organizadas por día.', tituloMenu: 'Agenda', roles: ROLES_VALIDOS },
  { ruta: 'asistente', titulo: 'Asistente', descripcion: 'Pregunta por horarios, lugares, cupos y requisitos.', tituloMenu: 'Asistente', roles: ROLES_VALIDOS },
  { ruta: 'perfil', titulo: 'Mi perfil', descripcion: 'Tus datos institucionales y tu sesión.', tituloMenu: 'Perfil', roles: ROLES_VALIDOS },
  // US-46 / US-51 / US-50: solo el rol organizador puede abrir estas rutas.
  { ruta: 'organizador-actividades', titulo: 'Mis actividades asignadas', descripcion: 'Gestiona tus actividades como organizador.', tituloMenu: 'Mis actividades', roles: [ROLES.ORGANIZADOR] },
  { ruta: 'organizador-competencias', titulo: 'Mis competencias', descripcion: 'Arma torneos, registra marcadores y llena hojas de puntajes de tus competencias.', tituloMenu: 'Mis competencias', roles: [ROLES.ORGANIZADOR] },
  { ruta: 'organizador-detalle', titulo: 'Detalle de actividad', descripcion: 'Información de tu actividad asignada.', tituloMenu: 'Detalle', roles: [ROLES.ORGANIZADOR], enMenu: false, menuActivo: 'organizador-actividades' },
  { ruta: 'organizador-resultados', titulo: 'Registrar resultados', descripcion: 'Envía posiciones de tus competencias para su validación.', tituloMenu: 'Resultados', roles: [ROLES.ORGANIZADOR], enMenu: false, menuActivo: 'organizador-actividades' },
  { ruta: 'organizador-inscritos', titulo: 'Estudiantes inscritos', descripcion: 'Participantes de una actividad asignada.', tituloMenu: 'Inscritos', roles: [ROLES.ORGANIZADOR], enMenu: false, menuActivo: 'organizador-actividades' },
  { ruta: 'organizador-cupos', titulo: 'Cupos y equipos', descripcion: 'Verifica ocupación y equipos de tu actividad.', tituloMenu: 'Cupos y equipos', roles: [ROLES.ORGANIZADOR], enMenu: false, menuActivo: 'organizador-actividades' },
  { ruta: 'organizador-reporte', titulo: 'Reportar un problema', descripcion: 'Informa incidencias sobre la actividad.', tituloMenu: 'Reportar', roles: [ROLES.ORGANIZADOR], enMenu: false, menuActivo: 'organizador-actividades' },
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
  const rutaOriginal = String(hash).replace(/^#\/?/, '').split(/[?/]/)[0].trim().toLowerCase()
  const ruta = rutaOriginal === 'catalogo' ? 'actividades' : rutaOriginal
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
