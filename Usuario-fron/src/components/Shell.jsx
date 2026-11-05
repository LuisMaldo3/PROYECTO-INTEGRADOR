import './Shell.css'
import { Logo } from './Logo'
import { Menu } from './Menu'

const nombreCorto = (usuario) => String(usuario?.nombre || usuario?.correo?.split('@')[0] || 'Mi cuenta').trim()
const iniciales = (nombre) => nombre.split(/\s+/).filter(Boolean).slice(0, 2).map(parte => parte.charAt(0).toUpperCase()).join('') || 'V'
const etiquetaRol = { usuario: 'Estudiante', organizador: 'Organizador', administrador: 'Administrador' }

/** El nombre, correo y rol salen de la sesión verificada con el backend, no de datos de demostración. */
export function Shell({ usuario, rol, rutaActual, titulo, descripcion, children }) {
  const nombre = nombreCorto(usuario)
  return (
    <div className={`shell${rutaActual === 'asistente' ? ' shell--asistente' : ''}`}>
      <aside className="shell-lateral">
        <div className="shell-identidad">
          <a className="shell-marca" href="#/inicio" aria-label="VYNK, ir al inicio"><Logo /></a>
          <span className="shell-identidad__linea">PORTAL ESTUDIANTIL <i /></span>
          <span className="shell-identidad__descripcion">Actividades · Tecsup</span>
        </div>
        <div className="shell-nav-label">NAVEGACIÓN PRINCIPAL</div>
        <Menu rol={rol} rutaActual={rutaActual} />
        <div className="shell-ayuda">
          <span className="shell-ayuda__icono" aria-hidden="true">?</span>
          <div><strong>Tu experiencia en VYNK</strong><span>Explora las actividades y organiza tu participación.</span></div>
        </div>
        <div className="shell-pie"><span className="shell-pie__punto" /> Plataforma institucional<br/>© {new Date().getFullYear()} VYNK · Tecsup</div>
      </aside>
      <div className="shell-contenido">
        <div className="shell-topbar">
          <span className="shell-topbar__camino"><span className="shell-topbar__separador">VYNK</span><span aria-hidden="true">/</span>{titulo || 'Mi espacio'}</span>
          <a className="shell-topbar__usuario" href="#/perfil" aria-label={`Abrir perfil de ${nombre}`}>
            <span className="shell-topbar__info"><strong>{nombre}</strong><small>{etiquetaRol[rol] || 'Mi cuenta'}</small></span>
            <span className="shell-topbar__avatar" aria-hidden="true">{iniciales(nombre)}</span>
          </a>
        </div>
        {titulo && <header className="shell-cabecera"><div className="shell-cabecera-in">
          <div className="shell-cabecera__intro"><span className="shell-cabecera__etiqueta"><span /> TU ESPACIO EN TECSUP</span><h1>{titulo}</h1>{descripcion && <p>{descripcion}</p>}</div>
          <div className="shell-cabecera__decoracion" aria-hidden="true"><span /><span /><span /></div>
        </div></header>}
        <main className="shell-main" id="contenido-principal">{children}</main>
      </div>
    </div>
  )
}
