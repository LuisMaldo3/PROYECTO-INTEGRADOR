import { pantallasDelMenu } from '../services/permisos'

const iconoProps = { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: 2, strokeLinecap: 'round', strokeLinejoin: 'round', 'aria-hidden': true }

const ICONOS = {
  inicio: (
    <>
      <path d="M3 11.5 12 4l9 7.5" />
      <path d="M5 10v10h5v-6h4v6h5V10" />
    </>
  ),
  catalogo: (
    <>
      <circle cx="11" cy="11" r="7" />
      <path d="m20 20-3.5-3.5" />
    </>
  ),
  recomendaciones: <><path d="m12 2 1.9 6.1L20 10l-6.1 1.9L12 18l-1.9-6.1L4 10l6.1-1.9L12 2Z"/><path d="m19 17 .7 2.3L22 20l-2.3.7L19 23l-.7-2.3L16 20l2.3-.7L19 17Z"/></>,
  agenda: (
    <>
      <rect x="3" y="5" width="18" height="16" rx="2" />
      <path d="M3 10h18M8 3v4M16 3v4" />
    </>
  ),
  asistente: <path d="M21 12a8 8 0 0 1-11.5 7.2L4 20l1.2-4.2A8 8 0 1 1 21 12z" />,
  'organizador-actividades': (<><rect x="4" y="5" width="16" height="16" rx="2"/><path d="M8 3v4M16 3v4M8 12h8M8 16h6"/></>),
  perfil: (
    <>
      <circle cx="12" cy="8" r="4" />
      <path d="M4 21a8 8 0 0 1 16 0" />
    </>
  ),
}

/** Menú principal: arriba en pantallas grandes y abajo en celular (con íconos). La pantalla actual va resaltada. */
export function Menu({ rol, rutaActual }) {
  return (
    <nav className="shell-menu" aria-label="Menú principal">
      <ul>
        {pantallasDelMenu(rol).map((p) => (
          <li key={p.ruta}>
            <a href={`#/${p.ruta}`} aria-current={p.ruta === rutaActual ? 'page' : undefined}>
              <svg {...iconoProps}>{ICONOS[p.ruta]}</svg>
              <span>{p.tituloMenu}</span>
            </a>
          </li>
        ))}
      </ul>
    </nav>
  )
}
