/** Roles que el administrador puede asignar (mismos que Admin-back: usuarios/roles.py). */
export const ROLES = [
  { valor: 'usuario', etiqueta: 'Usuario' },
  { valor: 'organizador', etiqueta: 'Organizador' },
  { valor: 'administrador', etiqueta: 'Administrador' },
]

const normalizar = (rol) => (typeof rol === 'string' ? rol.trim().toLowerCase() : '')

/** true si `nuevo` es un rol de la lista y es distinto del rol que ya tiene el usuario. */
export function hayCambio(actual, nuevo) {
  const elegido = normalizar(nuevo)
  return ROLES.some((r) => r.valor === elegido) && elegido !== normalizar(actual)
}
