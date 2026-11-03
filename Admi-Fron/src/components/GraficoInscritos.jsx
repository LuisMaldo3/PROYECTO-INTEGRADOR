import { claveArea } from '../services/areas'
import { marcasEje, maximoEje } from '../services/inscritos'

const ancho = (valor, maximo) => `${maximo ? Math.min(100, (valor * 100) / maximo) : 0}%`

/**
 * US-35 · Tarea 3 · Gráfico de barras horizontales de inscritos por actividad.
 * - Todas las barras miden PERSONAS inscritas en la misma escala, para poder compararlas.
 * - La línea roja marca el límite de cupos cuando el cupo es por persona.
 * - En competencias por equipos (T05) se añade un indicador de equipos: completos e incompletos
 *   sobre el total de cupos de equipo, porque ahí el cupo se cuenta por equipo y no por persona.
 */
export default function GraficoInscritos({ actividades }) {
  const maximo = maximoEje(actividades.flatMap((a) => [a.personasInscritas, a.porEquipos ? 0 : a.capacidad ?? 0]))
  const marcas = marcasEje(maximo)

  return (
    <figure className="grafico-inscritos" aria-labelledby="grafico-inscritos-titulo">
      <figcaption className="grafico-inscritos__leyenda">
        <span id="grafico-inscritos-titulo" className="sr-only">Gráfico de personas inscritas por actividad</span>
        <span><i className="leyenda leyenda--personas" />Personas inscritas</span>
        <span><i className="leyenda leyenda--cupo" />Límite de cupos</span>
        <span><i className="leyenda leyenda--completos" />Equipos completos</span>
        <span><i className="leyenda leyenda--incompletos" />Equipos incompletos</span>
      </figcaption>

      <div className="grafico-inscritos__eje" aria-hidden="true">
        <span />
        <div className="grafico-inscritos__marcas">{marcas.map((m, i) => <span key={i} style={{ left: `${(i * 100) / (marcas.length - 1)}%` }}>{m}</span>)}</div>
        <span />
      </div>

      <ol className="grafico-inscritos__filas">
        {actividades.map((a) => {
          const cupoPersonas = !a.porEquipos && a.capacidad
          const totalEquipos = Math.max(a.capacidad ?? 0, a.equiposRegistrados)
          const resumen = `${a.personasInscritas} ${a.personasInscritas === 1 ? 'persona inscrita' : 'personas inscritas'}`
            + (cupoPersonas ? ` de ${a.capacidad} cupos` : '')
            + (a.porEquipos ? `; ${a.equiposCompletos} de ${a.equiposRegistrados} equipos completos${a.capacidad ? `, ${a.capacidad} cupos de equipo` : ''}` : '')
          return (
            <li key={a.id} className="grafico-inscritos__fila" data-area={claveArea(a.areaTecsup)}>
              <div className="grafico-inscritos__nombre">
                <strong title={a.nombre}>{a.nombre}</strong>
                <small>{a.tipoActividad === 'competencia' ? (a.porEquipos ? 'Por equipos' : 'Competencia individual') : 'Actividad'}</small>
              </div>
              <div className="grafico-inscritos__pista" role="img" aria-label={`${a.nombre}: ${resumen}`}>
                <span className="barra barra--personas" style={{ width: ancho(a.personasInscritas, maximo) }} />
                {cupoPersonas && <span className="grafico-inscritos__cupo" style={{ left: ancho(a.capacidad, maximo) }} />}
              </div>
              <div className="grafico-inscritos__valor">
                <strong>{a.personasInscritas}</strong>
                {cupoPersonas ? <small>de {a.capacidad}</small> : !a.porEquipos && <small>sin límite</small>}
                {a.porEquipos && <span className="equipos-medidor" title={`${a.equiposCompletos} completos, ${a.equiposIncompletos} incompletos`}>
                  <span className="equipos-medidor__barra" aria-hidden="true">
                    <span className="barra--completos" style={{ width: ancho(a.equiposCompletos, totalEquipos) }} />
                    <span className="barra--incompletos" style={{ width: ancho(a.equiposIncompletos, totalEquipos) }} />
                  </span>
                  <small>{a.equiposCompletos}/{a.equiposRegistrados} equipos{a.capacidad ? ` · cupo ${a.capacidad}` : ''}</small>
                </span>}
              </div>
            </li>
          )
        })}
      </ol>
    </figure>
  )
}
