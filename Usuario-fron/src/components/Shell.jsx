import './Shell.css'
import { Logo } from './Logo'
import { Menu } from './Menu'

/**
 * Estructura de la web después de iniciar sesión, a pantalla completa:
 * columna lateral izquierda (logo y menú), y a la derecha la cabecera con el título y el contenido.
 * Los datos de la persona y «Cerrar sesión» viven dentro de Perfil.
 * En celular la columna se convierte en una barra con el logo arriba y el menú fijo abajo.
 */
export function Shell({ rol, rutaActual, titulo, descripcion, children }) {
  return (
    <div className={`shell${rutaActual === 'asistente' ? ' shell--asistente' : ''}`}>
      <aside className="shell-lateral">
        <a className="shell-marca" href="#/inicio" aria-label="VYNK, ir al inicio">
          <Logo />
        </a>

        <Menu rol={rol} rutaActual={rutaActual} />
      </aside>

      <div className="shell-contenido">
        {titulo && (
          <div className="shell-cabecera">
            <div className="shell-cabecera-in">
              <h1>{titulo}</h1>
              {descripcion && <p>{descripcion}</p>}
            </div>
          </div>
        )}

        <main className="shell-main">{children}</main>
      </div>
    </div>
  )
}
