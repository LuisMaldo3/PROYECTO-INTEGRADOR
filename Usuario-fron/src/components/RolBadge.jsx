import { ETIQUETAS_ROL, normalizarRol } from '../services/permisos'

/** Etiqueta con el rol (el color cambia según el rol). */
export function RolBadge({ rol }) {
  const r = normalizarRol(rol)
  return <span className={`rol-badge rol-${r}`}>{ETIQUETAS_ROL[r]}</span>
}
