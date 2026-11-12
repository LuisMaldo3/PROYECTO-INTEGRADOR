import Escudo from './Escudo'
import { claveArea } from '../services/areas'
import { agruparRondas, etiquetaEstado, puntajeDeLado, textoResultado } from '../services/torneos'
import { formatearFechaActividad } from '../services/formatoActividad'

function Lado({ equipo, puntaje, gana, descansa }) {
  return (
    <div className={`cruce__lado${gana ? ' cruce__lado--gana' : ''}${!equipo ? ' cruce__lado--vacio' : ''}`} data-area={equipo ? claveArea(equipo.area) : undefined}>
      <Escudo id={equipo?.departamento} tam={24} decorativo />
      <span className="cruce__nombre">{equipo ? equipo.nombre : descansa ? '—' : 'Por definir'}</span>
      {puntaje != null && <strong className="cruce__puntaje">{puntaje}</strong>}
    </div>
  )
}

/** Enfrentamiento: dos departamentos, marcador, estado y horario. Si `onElegir` existe, es un botón. */
export function TarjetaPartido({ partido: p, modo, onElegir, activo }) {
  const gana = (lado) => p.ganador && p[lado]?.id === p.ganador
  const contenido = p.descanso ? (
    <>
      <Lado equipo={p.a ?? p.b} gana descansa />
      <div className="cruce__pie"><span className="cruce__estado cruce__estado--descanso">Descansa</span><small>Avanza sin jugar</small></div>
    </>
  ) : (
    <>
      <Lado equipo={p.a} puntaje={puntajeDeLado(p, 'a')} gana={gana('a')} />
      <Lado equipo={p.b} puntaje={puntajeDeLado(p, 'b')} gana={gana('b')} />
      <div className="cruce__pie">
        <span className={`cruce__estado cruce__estado--${p.estado}`}>{etiquetaEstado(p)}</span>
        {p.resultado && modo === 'ganador' && <small>{textoResultado(p, modo)}</small>}
        {p.resultado?.ganadorPenales && <small>Ganó en el desempate</small>}
        {p.fecha && p.hora ? <small>{formatearFechaActividad(p.fecha)} · {p.hora}{p.lugar ? ` · ${p.lugar}` : ''}</small> : !p.resultado && <small>{p.a && p.b ? 'Sin horario' : 'Esperando rivales'}</small>}
        {p.estado === 'finalizado' && !p.validado && <span className="cruce__sello cruce__sello--aviso">Por validar</span>}
        {p.validado && <span className="cruce__sello cruce__sello--ok">Validado</span>}
      </div>
    </>
  )
  const titulo = p.tercerPuesto ? <p className="cruce__titulo">Tercer puesto</p> : null
  const clase = `cruce${activo ? ' cruce--activo' : ''}${p.descanso ? ' cruce--descanso' : ''}`
  if (!onElegir || p.descanso) return <article className={clase}>{titulo}{contenido}</article>
  return <button type="button" className={`${clase} cruce--boton`} onClick={() => onElegir(p)} aria-label={`${p.nombreRonda}: ${p.a?.nombre ?? 'por definir'} contra ${p.b?.nombre ?? 'por definir'}. Abrir`}>{titulo}{contenido}</button>
}

/** Llaves de eliminación directa: una columna por ronda, de la primera a la final. */
export default function LlavesTorneo({ torneo, onElegir, seleccionadoId }) {
  const rondas = agruparRondas(torneo.partidos)
  return (
    <div className="llaves" role="group" aria-label={`Llaves de ${torneo.deporte}`}>
      {rondas.map((r) => (
        <section className="llaves__ronda" key={r.ronda} aria-label={r.nombre}>
          <h3>{r.nombre}</h3>
          <div className="llaves__partidos">
            {r.partidos.map((p) => (
              <div className="llaves__celda" key={p.id}>
                <TarjetaPartido partido={p} modo={torneo.modoResultado} onElegir={onElegir} activo={seleccionadoId === p.id} />
              </div>
            ))}
          </div>
        </section>
      ))}
    </div>
  )
}
