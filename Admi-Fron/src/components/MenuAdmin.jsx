/** Tarea 6 · Menú lateral del panel. Para agregar una pantalla nueva, súmala a OPCIONES. */
export const OPCIONES = [{ id: 'usuarios', titulo: 'Usuarios', icono: '👥' }]

export default function MenuAdmin({ activa, onElegir, usuario, onCerrarSesion }) {
  return (
    <aside className="menu">
      <div className="menu__marca">
        VYNK <span>Admin</span>
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
            <span aria-hidden="true">{op.icono}</span> {op.titulo}
          </button>
        ))}
      </nav>

      <div className="menu__pie">
        <p className="menu__nombre">{usuario?.nombre ?? 'Administrador'}</p>
        <p className="menu__correo">{usuario?.correo}</p>
        <button type="button" className="boton boton--claro" onClick={onCerrarSesion}>
          Cerrar sesión
        </button>
      </div>
    </aside>
  )
}
