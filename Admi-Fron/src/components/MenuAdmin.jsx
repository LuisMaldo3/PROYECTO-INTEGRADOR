/** PLACEHOLDER · Tarea 6 es de Mishel. Se reemplaza por su archivo al integrar. */
export const OPCIONES = [{ id: 'usuarios', titulo: 'Usuarios', icono: '👥' }]

export default function MenuAdmin({ onCerrarSesion }) {
  return (
    <aside className="menu">
      <div className="menu__marca">
        VYNK <span>Admin</span>
      </div>
      <div className="menu__pie">
        <button type="button" className="boton boton--claro" onClick={onCerrarSesion}>
          Cerrar sesión
        </button>
      </div>
    </aside>
  )
}
