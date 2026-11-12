import { pantallasDelMenu } from '../services/permisos'

const iconoProps = { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: 2, strokeLinecap: 'round', strokeLinejoin: 'round', 'aria-hidden': true }

const ICONOS = {
  inicio: (
    <>
      <path d="M3 11.5 12 4l9 7.5" />
      <path d="M5 10v10h5v-6h4v6h5V10" />
    </>
  ),
  actividades: (
    <>
      <circle cx="11" cy="11" r="7" />
      <path d="m20 20-3.5-3.5" />
    </>
  ),
  recomendaciones: <><path d="m12 2 1.9 6.1L20 10l-6.1 1.9L12 18l-1.9-6.1L4 10l6.1-1.9L12 2Z"/><path d="m19 17 .7 2.3L22 20l-2.3.7L19 23l-.7-2.3L16 20l2.3-.7L19 17Z"/></>,
  resultados: (<><path d="M8 4h8v5a4 4 0 0 1-8 0V4Z"/><path d="M8 6H4v3a4 4 0 0 0 4 4M16 6h4v3a4 4 0 0 1-4 4M12 13v4m-4 3h8m-7-3h6"/></>),
  agenda: (
    <>
      <rect x="3" y="5" width="18" height="16" rx="2" />
      <path d="M3 10h18M8 3v4M16 3v4" />
    </>
  ),
  asistente: <path d="M21 12a8 8 0 0 1-11.5 7.2L4 20l1.2-4.2A8 8 0 1 1 21 12z" />,
  'organizador-competencias': (<><circle cx="12" cy="12" r="9"/><path d="m12 7 4 3-1.5 5h-5L8 10Z"/></>),
  'organizador-actividades': (<><rect x="4" y="5" width="16" height="16" rx="2"/><path d="M8 3v4M16 3v4M8 12h8M8 16h6"/></>),
  perfil: (
    <>
      <circle cx="12" cy="8" r="4" />
      <path d="M4 21a8 8 0 0 1 16 0" />
    </>
  ),
}

/** Menú principal: lateral en pantallas grandes y barra inferior en celular. */
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
