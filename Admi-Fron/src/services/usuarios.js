/** Lógica pura de la pantalla de usuarios (etiquetas, búsqueda y mensajes). */

const ETIQUETAS_ROL = {
  usuario: 'Usuario',
  organizador: 'Organizador',
  administrador: 'Administrador',
}

export function etiquetaRol(rol) {
  const clave = typeof rol === 'string' ? rol.trim().toLowerCase() : ''
  return ETIQUETAS_ROL[clave] ?? 'Usuario'
}

export function etiquetaEstado(activo) {
  return activo === false ? 'Inactivo' : 'Activo'
}

const sinTildes = (texto) =>
  String(texto ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()

/** Filtra por texto (nombre o correo, sin importar mayúsculas ni tildes) y por rol ('' = todos). */
export function filtrarUsuarios(usuarios, texto = '', rol = '') {
  const buscado = sinTildes(texto).trim()
  return usuarios.filter((u) => {
    if (rol && etiquetaRol(u.rol) !== etiquetaRol(rol)) return false
    if (!buscado) return true
    return sinTildes(u.nombre).includes(buscado) || sinTildes(u.correo).includes(buscado)
  })
}

/** Texto que ve la persona según el estado HTTP que devolvió el servidor. */
export function mensajeDeEstado(estado, mensajeDelServidor) {
  if (estado === 401) return 'Tu sesión expiró. Vuelve a iniciar sesión.'
  if (estado === 403) return mensajeDelServidor || 'Solo los administradores pueden ver esta información.'
  if (estado === 404) return 'No se encontró el usuario.'
  return mensajeDelServidor || 'No se pudo completar la operación. Inténtalo nuevamente.'
}
