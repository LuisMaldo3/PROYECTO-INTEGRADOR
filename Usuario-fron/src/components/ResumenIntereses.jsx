import { useIntereses } from '../hooks/useIntereses'
import { iconoDeInteres } from '../services/intereses'
import Icono from './Iconos'

/** Perfil · «Mis intereses»: los que eligió la persona y un acceso para cambiarlos. */
export function ResumenIntereses({ token }) {
  const i = useIntereses({ token })
  const elegidos = i.catalogo.filter((c) => i.guardados.includes(c.id))

  return (
    <div className="resumen-intereses">
      {i.cargando && <p className="resumen-intereses__texto">Cargando tus intereses…</p>}

      {!i.cargando && i.error && (
        <p className="resumen-intereses__texto">
          No pudimos cargar tus intereses.{' '}
          <button type="button" className="enlace-boton" onClick={i.recargar}>
            Reintentar
          </button>
        </p>
      )}

      {!i.cargando && !i.error && elegidos.length === 0 && (
        <p className="resumen-intereses__texto">Todavía no elegiste intereses. Elígelos para recibir recomendaciones.</p>
      )}

      {elegidos.length > 0 && (
        <ul className="resumen-intereses__lista" aria-label="Tus intereses">
          {elegidos.map((c) => (
            <li key={c.id} className="chip-interes">
              <Icono nombre={iconoDeInteres(c.id)} tamano={16} />
              {c.nombre}
            </li>
          ))}
        </ul>
      )}

      <a className="boton-secundario" href="#/intereses">
        <Icono nombre={elegidos.length > 0 ? 'editar' : 'flecha'} tamano={16} />
        {elegidos.length > 0 ? 'Editar intereses' : 'Elegir intereses'}
      </a>
    </div>
  )
}
