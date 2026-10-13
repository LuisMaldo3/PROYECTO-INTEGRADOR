import { useEffect, useMemo, useState } from 'react'
import { leerSesion } from '../services/authService'
import { obtenerAgenda } from '../services/agendaService'
import { obtenerCruces, actividadesEnCruce } from '../services/crucesService'
import { AvisoCruces } from './AvisoCruces'
import './Agenda.css'

const formatoFecha = (fecha) => new Date(`${fecha}T12:00:00`).toLocaleDateString('es-PE', { weekday: 'long', day: 'numeric', month: 'long' })
const corta = (fecha) => new Date(`${fecha}T12:00:00`).toLocaleDateString('es-PE', { day: '2-digit', month: 'short' })

function Tarjeta({ actividad, conflicto }) {
  return <article className={`ag-card ${conflicto ? "ag-card--conflict" : ""}`}>
    <div className="ag-time"><span className="ag-dot" />{actividad.horaInicio || 'Por confirmar'}<small>{actividad.horaFin ? `hasta ${actividad.horaFin}` : ''}</small></div>
    <div className="ag-card-body">{conflicto && <span className="ag-conflict-label">⚠ Cruce de horario</span>}
      <div className="ag-card-head"><span className="ag-category">{actividad.categoria || 'Actividad'}</span><span className={`ag-status ${actividad.estado === 'cancelada' ? 'ag-status--cancel' : ''}`}>{actividad.estado === 'cancelada' ? 'Cancelada' : 'Inscrito'}</span></div>
      <h3>{actividad.nombreActividad || 'Actividad sin título'}</h3>
      {actividad.descripcion && <p>{actividad.descripcion}</p>}
      <div className="ag-location">⌖ &nbsp;{actividad.ubicacion || 'Ubicación por confirmar'}</div>
    </div>
  </article>
}

export function Agenda() {
  const [actividades, setActividades] = useState([])
  const [cruces, setCruces] = useState([])
  const [errorCruces, setErrorCruces] = useState('')
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [filtro, setFiltro] = useState('todas')
  const [recargar, setRecargar] = useState(0)
  useEffect(() => {
    const ctrl = new AbortController()
    const token = leerSesion()?.token
    if (!token) { setError('Inicia sesión para consultar tu agenda.'); setCargando(false); return () => ctrl.abort() }
    setCargando(true); setError('')
    Promise.allSettled([obtenerAgenda(token, ctrl.signal), obtenerCruces(token, ctrl.signal)]).then(([agenda, conflictos]) => {
      if (agenda.status === 'rejected') throw agenda.reason
      setActividades(agenda.value)
      if (conflictos.status === 'fulfilled') { setCruces(conflictos.value); setErrorCruces('') }
      else { setCruces([]); setErrorCruces('No se pudo comprobar los cruces de horario.') }
    }).catch(e => { if (!ctrl.signal.aborted) setError(e.message) }).finally(() => { if (!ctrl.signal.aborted) setCargando(false) })
    return () => ctrl.abort()
  }, [recargar])
  const idsEnCruce = useMemo(() => actividadesEnCruce(cruces), [cruces])
  const hoy = new Date().toLocaleDateString('en-CA')
  const visibles = useMemo(() => actividades.filter(a => a.fecha && (filtro !== 'proximas' || a.fecha >= hoy)), [actividades, filtro, hoy])
  const grupos = Object.groupBy ? Object.groupBy(visibles, a => a.fecha) : visibles.reduce((acc, a) => { (acc[a.fecha] ||= []).push(a); return acc }, {})
  return <div className="ag-page">
    <header className="ag-hero"><div><span className="ag-eyebrow">TU ESPACIO · VYNK</span><h1>Mi agenda<span>.</span></h1><p>Todo lo que viene, en un solo lugar. Organiza tu semana y no te pierdas nada.</p></div><div className="ag-hero-count"><strong>{actividades.length}</strong><span>actividades<br/>inscritas</span></div></header>
    <div className="ag-toolbar"><div><h2>Mis actividades</h2><p>Tu calendario personal</p></div><div className="ag-filters"><button className={filtro === 'todas' ? 'active' : ''} onClick={() => setFiltro('todas')}>Todas</button><button className={filtro === 'proximas' ? 'active' : ''} onClick={() => setFiltro('proximas')}>Próximas</button></div></div>
    <AvisoCruces cruces={cruces} error={errorCruces} />
    {cargando ? <div className="ag-empty">Cargando tu agenda…</div> : error ? <div className="ag-empty" role="alert">{error}<br/><button onClick={() => setRecargar(n => n + 1)}>Reintentar</button></div> : !visibles.length ? <div className="ag-empty"><span>✦</span><h3>Tu agenda está lista para empezar</h3><p>{filtro === 'proximas' ? 'No tienes actividades próximas.' : 'Cuando te inscribas en una actividad, aparecerá aquí.'}</p></div> : <div className="ag-days">{Object.entries(grupos).sort(([a],[b]) => a.localeCompare(b)).map(([fecha, items]) => <section className="ag-day" key={fecha}><div className="ag-date"><strong>{corta(fecha)}</strong><span>{formatoFecha(fecha)}</span></div><div className="ag-events">{items.map(a => <Tarjeta key={a.id || a.actividadId} actividad={a} conflicto={idsEnCruce.has(a.actividadId)}/>)}</div></section>)}</div>}
  </div>
}
