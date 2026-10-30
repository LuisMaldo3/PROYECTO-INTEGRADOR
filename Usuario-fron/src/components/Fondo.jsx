import './Login.css'
import campus from '../assets/campus.webp'
import { Portada } from './Portada'

const ANIO = new Date().getFullYear()

/**
 * Pantalla partida: a la izquierda la foto de Tecsup, y a la derecha un panel
 * de altura completa con la foto de la comunidad + nombre de la app arriba y,
 * debajo, todo lo de iniciar sesión.
 */
export function Fondo({ children }) {
  return (
    <div className="vynk-page" style={{ '--vynk-campus': `url(${campus})` }}>
      <div className="vynk-campus">
        <div className="vynk-campus__contenido">
          <span className="vynk-campus__sello"><span /> LA COMUNIDAD NOS CONECTA</span>
          <div className="vynk-campus__linea" />
          <h2>Un campus.<br /><em>Infinitas oportunidades.</em></h2>
          <p>Encuentra actividades, comparte experiencias y vive lo mejor de tu comunidad Tecsup.</p>
          <span className="vynk-campus__pie">VYNK <span>·</span> CAMPUS DIGITAL</span>
        </div>
      </div>

      <aside className="vynk-panel">
        <Portada />

        <main className="vynk-sheet-body">{children}</main>


        <footer className="vynk-footer">
          <span className="vynk-footer__seguridad">Acceso para cuentas institucionales</span>
          <span>© {ANIO} VYNK, Tecsup</span>
        </footer>
      </aside>
    </div>
  )
}
