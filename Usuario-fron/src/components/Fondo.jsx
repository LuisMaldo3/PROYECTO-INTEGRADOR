import './Login.css'
import campus from '../assets/campus.webp'
import { Portada } from './Portada'

const ANIO = new Date().getFullYear()

/**
 * Pantalla partida: a la izquierda la foto de Tecsup, y a la derecha un panel
 * de altura completa con la foto de la comunidad + nombre de la app arriba y,
 * debajo, todo lo de iniciar sesión.
 */
export function Fondo({ children, menu = null }) {
  return (
    <div className="vynk-page" style={{ '--vynk-campus': `url(${campus})` }}>
      <div className="vynk-campus" aria-hidden="true" />

      <aside className="vynk-panel">
        <Portada />
        {menu}

        <main className="vynk-sheet-body">{children}</main>

        <footer className="vynk-footer">
          <nav className="footer-links" aria-label="Enlaces legales">
            <a href="#">Política de privacidad</a>
            <span className="dot-sep" aria-hidden="true">·</span>
            <a href="#">Términos de servicio</a>
          </nav>
          <span>© {ANIO} VYNK, Tecsup</span>
        </footer>
      </aside>
    </div>
  )
}
