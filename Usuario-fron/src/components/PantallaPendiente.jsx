import { RolBadge } from './RolBadge'

/**
 * Pantalla de un rol que todavía no tiene su contenido real.
 * Sirve para probar el menú y el bloqueo; se reemplaza cuando exista la función.
 */
export function PantallaPendiente({ pantalla, rol }) {
  return (
    <>
      <h2 className="card-title">{pantalla.titulo}</h2>
      <p className="card-subtitle">{pantalla.descripcion}</p>
      <div className="pantalla-vacia">
        <RolBadge rol={rol} />
        <p>Esta pantalla estará disponible pronto.</p>
      </div>
      <a className="btn-secundario btn-enlace" href="#/inicio">
        Volver al inicio
      </a>
    </>
  )
}
