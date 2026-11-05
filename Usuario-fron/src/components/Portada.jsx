import portada from '../assets/portada.webp'
import { Logo } from './Logo'

/** Foto de la comunidad con el nombre de la app al centro. */
export function Portada() {
  return (
    <header className="portada">
      <img src={portada} alt="Estudiantes participando en actividades del campus" className="portada-foto" width="596" height="335" />
      <div className="portada-texto">
        <h1>
          <Logo />
        </h1>
        <p>Comunidad y actividades · Tecsup</p>
      </div>
    </header>
  )
}
