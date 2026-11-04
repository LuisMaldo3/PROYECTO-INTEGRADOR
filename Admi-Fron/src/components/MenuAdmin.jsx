import Icono from './Iconos'
import Logo from './Logo'

/** Menú lateral del panel. Para agregar una pantalla nueva, súmala a OPCIONES. */
export const OPCIONES = [
  { id: 'actividades', titulo: 'Actividades', icono: 'calendario' },
  { id: 'inscritos', titulo: 'Inscritos', icono: 'grafico' },
  { id: 'reemplazos', titulo: 'Reemplazos y cupos', icono: 'actualizar' },
  { id: 'demanda', titulo: 'Demanda', icono: 'grafico' },
  { id: 'intereses', titulo: 'Intereses', icono: 'grafico' },
  { id: 'avisos', titulo: 'Avisos', icono: 'megafono' },
  { id: 'clasificacion', titulo: 'Clasificación', icono: 'trofeo' },
  { id: 'usuarios', titulo: 'Usuarios', icono: 'usuarios' },
]

export default function MenuAdmin({ activa, onElegir, usuario, onCerrarSesion }) {
  const nombre = (usuario?.nombre ?? usuario?.correo ?? 'Administrador').trim()
  return (
    <aside className="menu">
      <div className="menu__marca"><Logo /></div>

      <nav className="menu__lista" aria-label="Menú de administración">
        {OPCIONES.map((op) => (
          <button
            key={op.id}
            type="button"
            className="menu__item"
            aria-current={op.id === activa ? 'page' : undefined}
            onClick={() => onElegir(op.id)}
          >
            <Icono nombre={op.icono} /> <span>{op.titulo}</span>
          </button>
        ))}
      </nav>

      <div className="menu__pie">
        <div className="menu__persona">
          <span className="avatar" aria-hidden="true">{nombre.charAt(0).toUpperCase()}</span>
          <div>
            <p className="menu__nombre">{nombre}</p>
            <p className="menu__correo">{usuario?.correo}</p>
          </div>
        </div>
        <button type="button" className="menu__salir" onClick={onCerrarSesion}>
          <Icono nombre="salir" tam={18} /> <span>Cerrar sesión</span>
        </button>
      </div>
    </aside>
  )
}
