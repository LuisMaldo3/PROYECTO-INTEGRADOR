import Icono from './Iconos'
import { urlPortada } from '../services/portadasService'
import { portadaTematica } from '../services/portadaTematica'
import { claveArea } from '../services/areas'
import { formatearFechaActividad, formatearHorario, resumen } from '../services/formatoActividad'

/**
 * Una fila de la tabla de actividades.
 * Nunca cambia el estado por sí misma: cuando se toca «Editar», «Resultados» o
 * «Cancelar», solo llama a la función que recibió por props. Quien decide qué
 * hacer con cada aviso es siempre el padre (PantallaActividades) o, para la
 * ventana de resultados, el listado que la contiene.
 */
export default function FilaActividad({ actividad: a, onEditar, onCancelar, onVerResultados }) {
  return (
    <tr className={a.estado === 'cancelada' ? 'fila--cancelada' : undefined} data-area={claveArea(a.areaTecsup)}>
      <td>
        <div className="actividad-admin__principal">
          <img className="actividad-admin__foto" src={a.portada?.url ? urlPortada(a.portada) : portadaTematica(a)} alt={`Portada de ${a.nombre}`} loading="lazy" referrerPolicy="no-referrer" />
          <div className="celda-principal" title={a.descripcion}>
            <strong>{a.nombre}</strong>
            {a.estado === 'cancelada' && <span className="estado-cancelada">Cancelada</span>}
            <span>{resumen(a.descripcion, 70)}</span>
          </div>
        </div>
      </td>
      <td>
        <span className={`etiqueta etiqueta--${a.tipoActividad === 'competencia' ? 'concurso' : a.categoria?.id ?? 'sin'}`}>{a.tipoActividad === 'competencia' ? 'Competencia' : a.categoria?.nombre ?? '—'}</span>
        {a.areaTecsup && <span className="area-punto" data-area={claveArea(a.areaTecsup)}>{a.areaTecsup}</span>}
      </td>
      <td>
        {a.ubicacion ? (
          <span className="hora">
            <Icono nombre="ubicacion" tam={16} /> {a.ubicacion.nombre}
          </span>
        ) : (
          <span className="sin-dato">Sin ubicación</span>
        )}
      </td>
      <td>
        <span className="fecha-hora">
          <strong className={a.fecha ? undefined : 'sin-dato'}>{formatearFechaActividad(a.fecha)}</strong>
          {a.horaInicio ? <small>{formatearHorario(a.horaInicio, a.horaFin)}</small> : <small className="sin-dato">Sin horario</small>}
        </span>
      </td>
      <td className="tabla__acciones-celda">
        <div className="acciones-actividad">
          <button type="button" className="boton boton--chico" disabled={a.estado === 'cancelada'} onClick={() => onEditar(a)}>Editar</button>
          {a.tipoActividad === 'competencia' && <button type="button" className="boton boton--chico" onClick={() => onVerResultados(a)}>Resultados</button>}
          {a.estado !== 'cancelada' && <button type="button" className="boton boton--chico boton--peligro-discreto" onClick={() => onCancelar(a)}>Cancelar</button>}
        </div>
      </td>
    </tr>
  )
}
