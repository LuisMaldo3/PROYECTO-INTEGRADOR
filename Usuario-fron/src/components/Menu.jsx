import { pantallasDelMenu } from '../services/permisos'
import { RolBadge } from './RolBadge'

/** Menú principal: cada rol ve solo las pantallas que le tocan. */
export function Menu({ rol, rutaActual }) {
  return (
    <div className="vynk-menu">
      <nav aria-label="Menú principal">
        <ul>
          {pantallasDelMenu(rol).map((p) => (
            <li key={p.ruta}>
              <a href={`#/${p.ruta}`} aria-current={p.ruta === rutaActual ? 'page' : undefined}>
                {p.tituloMenu}
              </a>
            </li>
          ))}
        </ul>
      </nav>
      <RolBadge rol={rol} />
    </div>
  )
}
