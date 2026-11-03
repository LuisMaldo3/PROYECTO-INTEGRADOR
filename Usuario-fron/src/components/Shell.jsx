import { Logo } from './Logo'
import { Menu } from './Menu'

const nombreCorto = (usuario) => String(usuario?.nombre || usuario?.correo?.split('@')[0] || 'Mi cuenta').trim()
const iniciales = (nombre) => nombre.split(/\s+/).filter(Boolean).slice(0, 2).map(parte => parte.charAt(0).toUpperCase()).join('') || 'V'
const etiquetaRol = { usuario: 'Estudiante', organizador: 'Organizador', administrador: 'Administrador' }

/** Estructura común: menú lateral, cabecera de la página y contenido. */
export function Shell({ usuario, rol, rutaActual, titulo, descripcion, children }) {
  const nombre = nombreCorto(usuario)
  return (
    <div className={`shell${rutaActual === 'asistente' ? ' shell--asistente' : ''}`}>
      <a className="shell-saltar" href="#contenido-principal">Saltar al contenido</a>
      <aside className="shell-lateral">
        <a className="shell-marca" href="#/inicio" aria-label="VYNK, ir al inicio">
          <Logo />
          <span className="shell-marca__sede">Tecsup</span>
        </a>
        <Menu rol={rol} rutaActual={rutaActual} />
      </aside>
      <div className="shell-contenido">
        <header className="shell-cabecera">
          <div className="shell-cabecera-in">
            <div className="shell-cabecera__intro">
              <h1>{titulo || 'Mi espacio'}</h1>
              {descripcion && <p>{descripcion}</p>}
            </div>
            <a className="shell-usuario" href="#/perfil" aria-label={`Abrir perfil de ${nombre}`}>
              <span className="shell-usuario__info"><strong>{nombre}</strong><small>{etiquetaRol[rol] || 'Mi cuenta'}</small></span>
              <span className="shell-usuario__avatar" aria-hidden="true">{iniciales(nombre)}</span>
            </a>
          </div>
        </header>
        <main className="shell-main" id="contenido-principal" tabIndex={-1}>{children}</main>
      </div>
    </div>
  )
}
