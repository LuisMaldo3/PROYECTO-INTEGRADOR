import { ETIQUETAS_ROL, normalizarRol } from '../services/permisos'

/** Se muestra cuando la dirección es de una pantalla que el rol no puede ver (o que no existe). */
export function PantallaBloqueada({ estado, pantalla, rol }) {
  const noExiste = estado === 'inexistente'
  return (
    <div className="alerta-denegado alerta-bloqueo" role="alert">
      <div className="alerta-contenido">
        <span className="alerta-titulo">{noExiste ? 'Pantalla no encontrada' : 'Acceso no autorizado'}</span>
        <p className="alerta-mensaje">
          {noExiste
            ? 'Esa pantalla no existe.'
            : `Tu rol (${ETIQUETAS_ROL[normalizarRol(rol)]}) no tiene acceso a «${pantalla.titulo}».`}
        </p>
        <a className="btn-blue-submit btn-enlace" href="#/inicio">
          Volver al inicio
        </a>
      </div>
    </div>
  )
}
