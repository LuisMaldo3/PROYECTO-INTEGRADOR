import { useEffect, useMemo, useState } from 'react'
import { consultarClasificacion, consultarResultados } from '../services/inscripcionesService'
import { enlaceActividad } from './DetalleActividad'
import { claveArea } from '../services/areas'
import { departamentoDeArea } from '../services/departamentos'
import Escudo from './Escudo'
import TorneosEstudiante from './TorneosEstudiante'
import { nombreDia } from '../services/presentacion'

function ResultadosGenerales() {
  const [datos, setDatos] = useState({ cargando: true, error: '', resultados: [], clasificacion: null })
  const [revision, setRevision] = useState(0)
  const [busqueda, setBusqueda] = useState('')
  useEffect(() => {
    const c = new AbortController()
    Promise.all([consultarResultados(c.signal), consultarClasificacion(c.signal)])
      .then(([r, tabla]) => { if (!c.signal.aborted) setDatos({ cargando: false, error: '', resultados: r.resultados || [], clasificacion: tabla }) })
      .catch(e => { if (!c.signal.aborted) setDatos({ cargando: false, error: e.message, resultados: [], clasificacion: null }) })
    return () => c.abort()
  }, [revision])
  const filtrados = useMemo(() => datos.resultados.filter(r => `${r.nombre} ${(r.posiciones || []).map(p => p.nombre || '').join(' ')}`.toLocaleLowerCase('es-PE').includes(busqueda.toLocaleLowerCase('es-PE').trim())), [datos.resultados, busqueda])
  const areas = datos.clasificacion?.areas || []
  const maxPuntos = Math.max(1, ...areas.map(a => Number(a.puntos) || 0))
  const publicada = datos.clasificacion?.estado === 'publicado'
  return <div className="tc-resultados">
    {datos.cargando && <div className="tc-vacio" role="status">Cargando resultados…</div>}
    {datos.error && <div className="tc-error" role="alert">{datos.error} <button type="button" className="tc-accion" onClick={() => setRevision(v => v + 1)}>Reintentar</button></div>}
    {!datos.cargando && !datos.error && <>
      <section className="tc-resultados__bloque tc-clasificacion" aria-labelledby="tc-clasificacion">
        <div className="tc-resultados__bloque-titulo">
          <div><h2 id="tc-clasificacion">Clasificación por áreas</h2><p>Puntos acumulados en competencias con resultados oficiales.</p></div>
          <div className="tc-resultados__acciones"><span className={`tc-resultado__publicado${publicada ? '' : ' tc-resultado__publicado--pendiente'}`}>{publicada ? 'Oficial' : 'Sin resultados aún'}</span>
            <button className="tc-accion" type="button" onClick={() => setRevision(v => v + 1)}>Actualizar</button></div>
        </div>
        {!publicada && <div className="tc-vacio">Todas las áreas empiezan en cero. La tabla se actualiza cuando administración publica resultados.</div>}
        <ol className="tc-areas">{areas.map((a, i) => <li key={a.area} data-area={claveArea(a.area)}>
          <span className="tc-areas__pos">{a.posicion || i + 1}</span>
          <Escudo id={departamentoDeArea(a.area)?.id} tam={32} decorativo />
          <div className="tc-areas__nombre"><strong>{departamentoDeArea(a.area)?.nombre ?? a.area}</strong><small>{departamentoDeArea(a.area) ? `${a.area} · ` : ''}{a.primerosPuestos ?? 0} {a.primerosPuestos === 1 ? 'primer puesto' : 'primeros puestos'}</small></div>
          <div className="tc-areas__barra" aria-hidden="true"><span style={{ width: `${Math.round(((Number(a.puntos) || 0) / maxPuntos) * 100)}%` }} /></div>
          <strong className="tc-areas__puntos">{a.puntos ?? 0}<small> pts</small></strong>
        </li>)}</ol>
        {areas.some(a => (a.desglose || []).length) && <div className="tc-resultados__desglose">{areas.filter(a => (a.desglose || []).length).map(a => <details key={a.area}>
          <summary>{a.area}: detalle de puntos</summary>
          <ul>{a.desglose.map((item, i) => <li key={`${item.actividadId}-${i}`}><span>{item.competencia}</span><span>{item.puesto}.º · <strong>{item.puntos} pts</strong></span></li>)}</ul>
        </details>)}</div>}
      </section>

      <section className="tc-resultados__bloque" aria-labelledby="tc-ganadores">
        <div className="tc-resultados__bloque-titulo"><div><h2 id="tc-ganadores">Ganadores por competencia</h2><p>Solo se muestran resultados validados por administración.</p></div>
          {datos.resultados.length > 0 && <label className="tc-resultados__buscar"><span className="tc-visually-hidden">Buscar competencia o participante</span><input type="search" value={busqueda} onChange={e => setBusqueda(e.target.value)} placeholder="Buscar competencia o ganador" /></label>}</div>
        {!datos.resultados.length && <div className="tc-vacio">Aún no hay ganadores publicados.</div>}
        {datos.resultados.length > 0 && !filtrados.length && <div className="tc-vacio">No hay resultados con ese nombre.</div>}
        <div className="tc-resultados__grid">{filtrados.map(r => <article className="tc-resultado" key={r.actividadId}>
          <h3>{r.nombre}</h3><p>{r.fecha ? nombreDia(r.fecha) : 'Fecha pendiente'} · {r.modalidad === 'equipos' ? 'Por equipos' : 'Individual'}</p>
          <ol>{(r.posiciones || []).slice().sort((a,b)=>a.puesto-b.puesto).map((p,i) => <li key={`${p.puesto}-${i}`} data-area={claveArea(p.areaTecsup)} className={p.puesto === 1 ? 'tc-resultado__ganador' : undefined}>
            <span className="tc-resultado__puesto">{p.puesto}.º</span><div><strong>{p.nombre}</strong><small>{p.areaTecsup || 'Área sin verificar'}</small></div></li>)}</ol>
          <a className="tc-detalle-enlace" href={enlaceActividad(r.actividadId)}>Ver competencia</a>
        </article>)}</div>
      </section>
    </>}
  </div>
}

/** Departamento ganador, cuando el administrador cierra la T&C Week. */
function GanadorGeneral() {
  const [g, setG] = useState(null)
  useEffect(() => {
    const c = new AbortController()
    consultarClasificacion(c.signal).then((t) => { if (!c.signal.aborted) setG(t?.ganadorGeneral ?? null) }).catch(() => {})
    return () => c.abort()
  }, [])
  if (!g) return null
  return (
    <section className="ganador-general" data-area={claveArea(g.area)} aria-label="Ganador de la T&C Week">
      <Escudo id={departamentoDeArea(g.area)?.id} tam={72} decorativo />
      <div><p className="ganador-general__etiqueta">Ganador de la T&amp;C Week</p><h2>{g.area}</h2><p>{g.puntos} puntos en la tabla acumulada</p></div>
    </section>
  )
}

const SECCIONES_RESULTADOS = [['general', 'Clasificación y ganadores'], ['arena', 'T&C ARENA']]

/** Resultados: clasificación general y ganadores (lo de siempre) más los torneos de T&C ARENA y Karadúo. */
export function ResultadosCompetencias() {
  const [seccion, setSeccion] = useState('general')
  return (
    <div className="tc-seccion">
      <GanadorGeneral />
      <div className="segmentos tc-seccion__pestanas" role="group" aria-label="Qué quieres ver">
        {SECCIONES_RESULTADOS.map(([id, texto]) => <button key={id} type="button" aria-pressed={seccion === id} onClick={() => setSeccion(id)}>{texto}</button>)}
      </div>
      {seccion === 'general' ? <ResultadosGenerales /> : <TorneosEstudiante key={seccion} tipo={seccion} />}
    </div>
  )
}
