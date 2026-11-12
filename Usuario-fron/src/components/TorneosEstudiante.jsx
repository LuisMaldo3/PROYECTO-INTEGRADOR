import { useEffect, useMemo, useState } from 'react'
import Escudo from './Escudo'
import { consultarTorneos } from '../services/inscripcionesService'
import { claveDepartamento } from '../services/departamentos'
import { claveArea } from '../services/areas'
import { agruparRondas, avance, calendario, deTipo, etiquetaEstado, frasePorGanador, porDia, puntajeDeLado, equipo, normalizarTorneo } from '../services/torneos'
import { hoyISO, nombreDia } from '../services/presentacion'

function Lado({ e, puntaje, gana, descansa }) {
  return (
    <div className={`cruce__lado${gana ? ' cruce__lado--gana' : ''}${!e ? ' cruce__lado--vacio' : ''}`} data-area={e ? claveArea(e.area) || claveDepartamento(e.departamento) : undefined}>
      <Escudo id={e?.departamento} tam={24} decorativo />
      <span className="cruce__textos"><span className="cruce__nombre">{e ? e.nombre : descansa ? '—' : 'Por definir'}</span>{e?.area && e.area !== e.nombre && <small className="cruce__area">{e.area}</small>}</span>
      {puntaje != null && <span className="cruce__puntaje">{puntaje}</span>}
    </div>
  )
}

export function TarjetaPartido({ partido: p, torneo }) {
  const titulo = p.tercerPuesto ? <p className="cruce__titulo">Tercer puesto</p> : null
  if (p.descanso) {
    return (
      <article className="cruce cruce--descanso">
        <Lado e={equipo(torneo, p.a ?? p.b)} gana descansa />
        <div className="cruce__pie"><span className="cruce__estado cruce__estado--descanso">Descansa</span><small>Avanza sin jugar</small></div>
      </article>
    )
  }
  const gana = (lado) => p.ganador && p[lado] === p.ganador
  return (
    <article className="cruce" aria-label={`${p.nombreRonda}: ${p.a ? equipo(torneo, p.a).nombre : 'por definir'} contra ${p.b ? equipo(torneo, p.b).nombre : 'por definir'}`}>
      {titulo}
      <Lado e={equipo(torneo, p.a)} puntaje={puntajeDeLado(p, 'a')} gana={gana('a')} />
      <Lado e={equipo(torneo, p.b)} puntaje={puntajeDeLado(p, 'b')} gana={gana('b')} />
      <div className="cruce__pie">
        <span className={`cruce__estado cruce__estado--${p.estado}`}>{etiquetaEstado(p)}</span>
        {p.resultado && <small>{frasePorGanador(p, (id) => equipo(torneo, id)?.nombre ?? id)}</small>}
        {p.fecha && p.hora ? <small>{nombreDia(p.fecha)} · {p.hora}{p.lugar ? ` · ${p.lugar}` : ''}</small> : !p.resultado && <small>{p.a && p.b ? 'Horario por confirmar' : 'Esperando rivales'}</small>}
        {p.provisional && <span className="cruce__sello" title="Todavía no lo valida la organización">Provisional</span>}
      </div>
    </article>
  )
}

/** Llaves por ronda. Con `onElegir` (organizador) cada enfrentamiento con rivales es un botón. */
export function Llaves({ torneo, onElegir, seleccionadoId }) {
  return (
    <div className="llaves" role="group" aria-label={`Llaves de ${torneo.deporte}`}>
      {agruparRondas(torneo.partidos).map((r) => (
        <section className="llaves__ronda" key={r.ronda} aria-label={r.nombre}>
          <h3>{r.nombre}</h3>
          <div className="llaves__partidos">{r.partidos.map((p) => <div className="llaves__celda" key={p.id}>{onElegir && !p.descanso ? <button type="button" className={`cruce-boton${seleccionadoId === p.id ? ' cruce-boton--activo' : ''}`} onClick={() => onElegir(p)} aria-label={`${p.nombreRonda}: ${p.a ? equipo(torneo, p.a).nombre : 'por definir'} contra ${p.b ? equipo(torneo, p.b).nombre : 'por definir'}. Abrir`}><TarjetaPartido partido={p} torneo={torneo} /></button> : <TarjetaPartido partido={p} torneo={torneo} />}</div>)}</div>
        </section>
      ))}
    </div>
  )
}

function Calendario({ torneo }) {
  const dias = useMemo(() => porDia(calendario(torneo)), [torneo])
  if (!dias.length) return <div className="tc-vacio">Todavía no hay horarios publicados. Cuando la organización programe los enfrentamientos, aparecerán aquí.</div>
  return (
    <div className="calendario-torneo">
      {dias.map(([fecha, ps]) => (
        <section key={fecha}><h3>{nombreDia(fecha)}</h3><div className="calendario-torneo__lista">{ps.map((p) => <TarjetaPartido key={p.id} partido={p} />)}</div></section>))}
    </div>
  )
}

export function Campeon({ torneo }) {
  if (!torneo.campeon) return null
  return (
    <div className="campeon" data-area={claveArea(equipo(torneo, torneo.campeon)?.area) || claveDepartamento(equipo(torneo, torneo.campeon)?.departamento)}>
      <Escudo id={equipo(torneo, torneo.campeon)?.departamento} tam={64} decorativo />
      <div><p className="campeon__etiqueta">Campeón</p><h2>{equipo(torneo, torneo.campeon)?.nombre}</h2>
        {torneo.subcampeon && <p className="campeon__sub"><Escudo id={equipo(torneo, torneo.subcampeon)?.departamento} tam={20} decorativo /> <span>{equipo(torneo, torneo.subcampeon)?.nombre}, subcampeón</span></p>}</div>
    </div>
  )
}

const TEXTOS = {
  arena: { vacio: 'Todavía no hay torneos deportivos publicados.', intro: 'Eliminación directa entre los equipos inscritos: los ganadores avanzan hasta la final.' },
  karaduo: { vacio: 'Todavía no se publicó el torneo de Karadúo.', intro: 'Enfrentamientos entre los participantes inscritos hasta la final.' },
}

/** T&C ARENA (tipo «arena») o Karadúo (tipo «karaduo») para estudiantes. */
export default function TorneosEstudiante({ tipo }) {
  const [estado, setEstado] = useState({ cargando: true, error: '', torneos: [] })
  const [version, setVersion] = useState(0)
  const [elegido, setElegido] = useState(null)
  const [vista, setVista] = useState('llaves')

  useEffect(() => {
    const control = new AbortController()
    consultarTorneos(control.signal)
      .then((r) => { if (!control.signal.aborted) setEstado({ cargando: false, error: '', torneos: (r.torneos ?? []).map(normalizarTorneo) }) })
      .catch((e) => { if (!control.signal.aborted) setEstado({ cargando: false, error: e.message || 'No se pudieron cargar los torneos.', torneos: [] }) })
    return () => control.abort()
  }, [version])

  const lista = deTipo(estado.torneos, tipo)
  const torneo = lista.find((t) => t.id === elegido) ?? lista[0] ?? null
  const av = torneo ? avance(torneo) : null
  const proximo = torneo ? calendario(torneo, { hoyISO: hoyISO(), soloProximos: true })[0] : null

  if (estado.cargando) return <div className="tc-vacio" role="status">Cargando…</div>
  if (estado.error) return <div className="tc-error" role="alert">{estado.error} <button className="tc-accion" type="button" onClick={() => { setEstado((e) => ({ ...e, cargando: true, error: '' })); setVersion((v) => v + 1) }}>Reintentar</button></div>
  if (!torneo) return <div className="tc-vacio">{TEXTOS[tipo].vacio}</div>

  return (
    <div className="torneo-est">
      <p className="torneo-est__intro">{TEXTOS[tipo].intro}</p>
      {lista.length > 1 && (
        <div className="segmentos" role="group" aria-label="Deporte">
          {lista.map((t) => <button key={t.id} type="button" aria-pressed={t.id === torneo.id} onClick={() => setElegido(t.id)}>{t.deporte}</button>)}
        </div>)}

      <div className="torneo-est__resumen">
        <div><h2>{torneo.deporte}</h2><p>{torneo.participantes.length} participantes · {av.jugados} de {av.total} enfrentamientos jugados</p></div>
        <ul className="torneo-est__escudos" aria-label="Participantes">{torneo.participantes.map((id) => <li key={id} title={equipo(torneo, id)?.nombre}><Escudo id={equipo(torneo, id)?.departamento} tam={34} /></li>)}</ul>
        {proximo && <p className="torneo-est__proximo"><strong>Próximo:</strong> {equipo(torneo, proximo.a)?.nombre} vs {equipo(torneo, proximo.b)?.nombre} · {nombreDia(proximo.fecha)}, {proximo.hora}</p>}
      </div>

      <Campeon torneo={torneo} />

      <div className="segmentos" role="group" aria-label="Vista">
        <button type="button" aria-pressed={vista === 'llaves'} onClick={() => setVista('llaves')}>Llaves</button>
        <button type="button" aria-pressed={vista === 'calendario'} onClick={() => setVista('calendario')}>Calendario</button>
      </div>
      <div className="torneo-est__cuerpo">{vista === 'llaves' ? <Llaves torneo={torneo} /> : <Calendario torneo={torneo} />}</div>
    </div>
  )
}
