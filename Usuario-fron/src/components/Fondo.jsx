import campus from '../assets/campus.webp'
import { Logo } from './Logo'
import { AREAS_TECSUP, claveArea } from '../services/areas'

const ANIO = new Date().getFullYear()

/** Pantalla de acceso: foto del campus a la izquierda y el formulario a la derecha. */
export function Fondo({ children }) {
  return (
    <div className="acceso">
      <section className="acceso-campus" style={{ '--foto-campus': `url(${campus})` }} aria-label="VYNK, actividades de Tecsup">
        <Logo className="acceso-campus__logo" />
        <div className="acceso-campus__texto">
          <h2>Actividades, competencias y vida en el campus.</h2>
          <p>Inscríbete, arma tu agenda y sigue los resultados de tu área en un solo lugar.</p>
        </div>
        <ul className="acceso-areas" aria-label="Áreas de Tecsup">
          {AREAS_TECSUP.map(area => <li key={area} data-area={claveArea(area)}>{area}</li>)}
        </ul>
      </section>

      <div className="acceso-panel">
        <div className="acceso-panel__marca"><Logo /><span className="acceso-panel__sede">Tecsup</span></div>
        <main className="vynk-sheet-body">{children}</main>
        <footer className="acceso-pie">© {ANIO} VYNK · Tecsup</footer>
      </div>
    </div>
  )
}
