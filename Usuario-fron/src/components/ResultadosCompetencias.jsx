import { useEffect, useMemo, useState } from 'react'
import { consultarClasificacion, consultarResultados } from '../services/inscripcionesService'
import './ResultadosCompetencias.css'

export function ResultadosCompetencias() {
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
  return <div className="tc-resultados">
    <section className="tc-resultados__cabecera"><div><span className="tc-resultados__kicker">T&amp;C WEEK · TU SEDE</span><h2>Resultados de competencias</h2>
      <p>Sigue las posiciones oficiales y los ganadores de cada competencia. Solo aparecen resultados que el administrador ya validó.</p>
      <div className="tc-resultados__resumen"><span><strong>{datos.cargando ? '—' : datos.resultados.length}</strong> competencias con resultados</span><span><strong>5</strong> áreas de Tecsup</span></div></div>
      <button className="tc-accion" type="button" disabled={datos.cargando} onClick={() => setRevision(v => v + 1)}>Actualizar resultados</button></section>
    {datos.cargando && <div className="tc-vacio" role="status">Consultando los resultados oficiales…</div>}
    {datos.error && <div className="tc-error" role="alert">{datos.error} <button type="button" className="tc-accion" onClick={() => setRevision(v => v + 1)}>Reintentar</button></div>}
    {!datos.cargando && !datos.error && <>
      <section className="tc-resultados__bloque"><div className="tc-resultados__bloque-titulo"><div><span className="tc-resultados__kicker">RESULTADOS VALIDOS</span><h3>Competencias finalizadas</h3></div>
        {datos.resultados.length > 0 && <label className="tc-resultados__buscar"><span className="tc-visually-hidden">Buscar competencia o participante</span><input type="search" value={busqueda} onChange={e => setBusqueda(e.target.value)} placeholder="Buscar competencia o ganador…" /></label>}</div>
        {!datos.resultados.length && <div className="tc-vacio"><strong>Aún no hay ganadores publicados</strong><p>El organizador envía las posiciones al administrador, quien debe verificarlas y publicarlas antes de que aparezcan aquí.</p></div>}
        {datos.resultados.length > 0 && !filtrados.length && <div className="tc-vacio">No encontramos resultados con ese nombre. Prueba otra búsqueda.</div>}
        <div className="tc-resultados__grid">{filtrados.map(r => <article className="tc-resultado" key={r.actividadId}>
          <div className="tc-resultado__top"><span className="tc-resultados__kicker">RESULTADO OFICIAL</span><span className="tc-resultado__publicado">Validado</span></div><h4>{r.nombre}</h4><p>{r.fecha || 'Fecha pendiente'} · {r.modalidad === 'equipos' ? 'Por equipos' : 'Individual'}</p>
          <ol>{(r.posiciones || []).slice().sort((a,b)=>a.puesto-b.puesto).map((p,i) => <li key={`${p.puesto}-${i}`} className={p.puesto === 1 ? 'tc-resultado__ganador' : undefined}>
            <span className="tc-resultado__puesto">{p.puesto}°</span><div><strong>{p.nombre}</strong><small>{p.areaTecsup || 'Área aún no verificada'}</small></div>{p.puesto === 1 && <span className="tc-resultado__primer-lugar">Ganador</span>}</li>)}</ol>
        </article>)}</div>
      </section>
      <section className="tc-resultados__bloque"><div className="tc-resultados__bloque-titulo"><div><span className="tc-resultados__kicker">CLASIFICACIÓN GENERAL</span><h3>Puntaje acumulado por áreas</h3></div><span className="tc-resultado__publicado">{datos.clasificacion?.estado === 'publicado' ? 'Oficial' : 'Aún sin resultados'}</span></div>
        {datos.clasificacion?.estado !== 'publicado' && <div className="tc-vacio">Aún no se han publicado competencias con resultados oficiales. Los cinco puntajes empiezan en cero y se actualizan cuando administración valida posiciones y puntos.</div>}
        <div className="tc-tabla"><table><thead><tr><th>Pos.</th><th>Área de Tecsup</th><th>1.er puesto</th><th>Puntos oficiales</th></tr></thead><tbody>
          {(datos.clasificacion?.areas || []).map((a, i) => <tr key={a.area}><td>{a.posicion || i + 1}.º</td><td><strong>{a.area}</strong></td><td>{a.primerosPuestos ?? 0}</td><td><strong>{a.puntos ?? 0}</strong></td></tr>)}
        </tbody></table></div>
        <div className="tc-resultados__desglose">{(datos.clasificacion?.areas || []).map(a => <details key={a.area}>
          <summary>{a.area} · {a.puntos ?? 0} puntos — ver competencias</summary>
          {(a.desglose || []).length ? <ul>{a.desglose.map((item, i) => <li key={`${item.actividadId}-${i}`}><span>{item.competencia}</span><span>{item.puesto}.º · <strong>{item.puntos} puntos</strong></span></li>)}</ul> : <p>Aún sin resultados validados.</p>}
        </details>)}</div>
      </section>
    </>}
  </div>
}
