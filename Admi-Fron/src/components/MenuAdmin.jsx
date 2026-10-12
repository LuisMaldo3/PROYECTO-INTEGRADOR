import Icono from './Iconos'
import Logo from './Logo'

/** Tarea 6 · Menú lateral del panel. Para agregar una pantalla nueva, súmala a OPCIONES. */
export const OPCIONES = [
  { id: 'usuarios', titulo: 'Usuarios', icono: 'usuarios' },
  { id: 'actividades', titulo: 'Actividades', icono: 'calendario' },
  { id: 'avisos', titulo: 'Avisos', icono: 'correo' },
]

export default function MenuAdmin({ activa, onElegir, usuario, onCerrarSesion }) {
  return (
    <aside className="menu">
      <div className="menu__marca">
        <Logo />
      </div>

      <nav className="menu__lista" aria-label="Menú de administración">
        {OPCIONES.map((op) => (
          <button
            key={op.id}
            type="button"
            className={`menu__item${op.id === activa ? ' menu__item--activo' : ''}`}
            aria-current={op.id === activa ? 'page' : undefined}
            onClick={() => onElegir(op.id)}
          >
            <Icono nombre={op.icono} /> {op.titulo}
          </button>
        ))}
      </nav>

      <div className="menu__pie">
        <div className="menu__persona">
          <span className="avatar" aria-hidden="true">
            {(usuario?.nombre ?? usuario?.correo ?? 'A').trim().charAt(0).toUpperCase()}
          </span>
          <div>
            <p className="menu__nombre">{usuario?.nombre ?? 'Administrador'}</p>
            <p className="menu__correo">{usuario?.correo}</p>
          </div>
        </div>
        <button type="button" className="boton boton--fantasma" onClick={onCerrarSesion}>
          <Icono nombre="salir" tam={18} /> Cerrar sesión
        </button>
      </div>
    </aside>
  )
}
