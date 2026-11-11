import { useEffect, useState } from 'react'
import { obtenerDetalleActividad, obtenerActividadesSimilares, obtenerResultadoOficial, ganadoresOficiales } from '../services/detalleCompetenciaService'
import { nombreDia, rangoHoras } from '../services/presentacion'
import { FormularioInscripcion } from './FormularioInscripcion'
import { urlImagenActividad, creditoPortada, imagenTematicaActividad } from '../services/portadaActividad'
import EstadoPantalla from './EstadoPantalla'
import { claveArea } from '../services/areas'
import { ComentarioParticipante } from './ComentarioParticipante'

export const enlaceActividad = id => `#/actividad?id=${encodeURIComponent(id)}`

/** Detalle de una actividad con su inscripción. */
export function DetalleActividad({ token }) {
  const id = new URLSearchParams((window.location.hash.split('?')[1] || '').split('#')[0]).get('id')
  const [estado, setEstado] = useState({ cargando: true, error: '', actividad: null })
  const [revision, setRevision] = useState(0)
  const [relacionadas, setRelacionadas] = useState({ cargando: true, actividades: [], error: '' })
  const [oficial, setOficial] = useState({ cargando: true, resultado: null, error: '' })
  useEffect(() => {
    if (!id) { setEstado({ cargando: false, error: 'No se seleccionó una actividad.', actividad: null }); return }
    const ctrl = new AbortController()
    setEstado({ cargando: true, error: '', actividad: null })
    setRelacionadas({ cargando: true, actividades: [], error: '' })
    setOficial({ cargando: true, resultado: null, error: '' })
    obtenerDetalleActividad(id, ctrl.signal)
      .then(actividad => { if (!ctrl.signal.aborted) setEstado({ cargando: false, error: '', actividad }) })
      .catch(e => { if (!ctrl.signal.aborted) setEstado({ cargando: false, error: e.message, actividad: null }) })
    obtenerActividadesSimilares(id, ctrl.signal)
      .then(datos => { if (!ctrl.signal.aborted) setRelacionadas({ cargando: false, actividades: datos.actividades || [], error: '' }) })
      .catch(e => { if (!ctrl.signal.aborted) setRelacionadas({ cargando: false, actividades: [], error: e.message }) })
    obtenerResultadoOficial(id, ctrl.signal)
      .then(datos => { if (!ctrl.signal.aborted) setOficial({ cargando: false, resultado: datos.estado === 'publicado' ? datos.resultado : null, error: '' }) })
      .catch(e => { if (!ctrl.signal.aborted) setOficial({ cargando: false, resultado: null, error: e.message }) })
    return () => ctrl.abort()
  }, [id, revision])
  const a = estado.actividad
  const finalizada = a?.estado === 'finalizada' || Boolean(oficial.resultado)
  const portadaEvento = a ? urlImagenActividad(a) || imagenTematicaActividad(a) : ''
  const ganadores = ganadoresOficiales(oficial.resultado)
  return <div className="vynk-detalle" data-area={claveArea(a?.areaTecsup)}>
    <a href="#/actividades" className="vynk-up-back">Actividades</a>
    {estado.cargando && <div role="status" className="vynk-up-loading">Cargando la actividad…</div>}
    {estado.error && <EstadoPantalla icono="alerta" tono="error" titulo="No se pudo abrir la actividad" texto={estado.error}
      accion={<button type="button" className="vynk-up-btn vynk-up-btn--outline" onClick={() => setRevision(r => r + 1)}>Volver a intentar</button>}/>} 
    {a && <>
      <section className="vynk-detalle__hero"><div className="vynk-detalle__hero-content">
        <div className="vynk-detalle__badges"><span>{a.tipoActividad === 'competencia' ? 'Competencia' : 'Actividad'}{a.areaTecsup ? ` · ${a.areaTecsup}` : ''}</span><span>{finalizada ? 'Competencia finalizada' : a.cuposDisponibles == null ? 'Cupos por confirmar' : `${a.cuposDisponibles} cupos disponibles`}</span></div>
        <h2>{a.nombre}</h2><p>{a.descripcion || 'Actividad publicada por Tecsup.'}</p>
        <div className="vynk-detalle__hero-actions">{!finalizada && <button type="button" className="vynk-up-btn vynk-up-btn--light" onClick={() => document.getElementById('inscripcion')?.scrollIntoView({ behavior: 'smooth', block: 'start' })}>Inscribirme</button>}{finalizada && <a href="#/resultados" className="vynk-up-btn vynk-up-btn--light">Ver clasificación</a>}<a className="vynk-up-btn vynk-up-btn--glass" href="#/agenda">Mi agenda</a></div>
      </div><div className="vynk-detalle__hero-imagen"><img src={urlImagenActividad(a) || imagenTematicaActividad(a)} onError={e => { e.currentTarget.onerror = null; e.currentTarget.src = imagenTematicaActividad(a) }} alt={`Portada de ${a.nombre}`} />{creditoPortada(a) && <a href={creditoPortada(a).url} target="_blank" rel="noopener noreferrer">{creditoPortada(a).texto}</a>}</div></section>
      <div className="vynk-detalle__layout"><div className="vynk-detalle__principal">
        <section className="vynk-detalle__bloque"><h3>Detalles</h3>
          <div className="vynk-detalle__ficha"><div><span>Fecha</span><strong>{nombreDia(a.fecha)}</strong></div><div><span>Horario</span><strong>{rangoHoras(a.horaInicio, a.horaFin)}</strong></div>
            <div><span>Ubicación</span><strong>{typeof a.ubicacion === 'string' ? a.ubicacion : a.ubicacion?.nombre || 'Por confirmar'}</strong></div><div><span>Organiza</span><strong>{typeof a.organizador === 'string' ? a.organizador : a.organizador?.nombre || 'Tecsup'}</strong></div>
            <div><span>Área</span><strong>{a.areaTecsup || 'Todas las áreas'}</strong></div><div><span>Modalidad</span><strong>{a.tipoActividad !== 'competencia' ? 'Actividad' : a.modalidad === 'equipos' ? 'Por equipos' : 'Individual'}</strong></div><div><span>Capacidad</span><strong>{a.maximoParticipantes == null ? 'Por confirmar' : `${a.maximoParticipantes} ${a.modalidad === 'equipos' ? 'equipos' : 'participantes'}`}</strong></div>
            {a.modalidad === 'equipos' && <div><span>Integrantes por equipo</span><strong>Entre 2 y {a.maxIntegrantesEquipo || '—'} estudiantes con inscripción personal</strong></div>}</div>
        </section>
        {a.encargadosPorArea && Object.keys(a.encargadosPorArea).length > 0 && <section className="vynk-detalle__bloque"><h3>Responsables por área</h3><div className="vynk-encargados-detalle">{Object.entries(a.encargadosPorArea).map(([area, info]) => <p key={area}><strong>{area}</strong><span>{info?.nombre || 'Sin asignar'}</span></p>)}</div></section>}
        {a.requisitos && <section className="vynk-detalle__bloque"><h3>Requisitos</h3><p className="vynk-up-richtext">{typeof a.requisitos === 'string' ? a.requisitos : Array.isArray(a.requisitos) ? a.requisitos.join(' · ') : 'Consulta los requisitos con el organizador.'}</p></section>}
                {a.tipoActividad === 'competencia' && <section className="vynk-detalle__bloque"><h3>Reglamento</h3><p className="vynk-up-richtext">{a.reglasCompetencia || 'El organizador aún no publicó un reglamento específico.'}</p>
          {Array.isArray(a.puntosPorPuesto) && a.puntosPorPuesto.length > 0 && <div className="vynk-detalle__puntos"><strong>Puntos por posición</strong><div>{a.puntosPorPuesto.map((p,i) => <span key={i}>{i+1}.º: {p} pts.</span>)}</div></div>}
          </section>}
        {a.tipoActividad === 'competencia' && <section className="vynk-detalle__bloque vynk-us22-resultados" aria-label="Resultados oficiales de la competencia">
          
          <h3>Resultados y ganadores</h3>
          {oficial.cargando && <p role="status">Cargando resultados…</p>}
          {!oficial.cargando && oficial.error && <p role="status">{oficial.error}</p>}
          {!oficial.cargando && !oficial.error && !oficial.resultado && <p className="vynk-detalle__nota">Los resultados se mostrarán cuando administración los publique.</p>}
          {oficial.resultado && <>
            <p className="vynk-us22-resultados__estado">Resultados oficiales</p>
            {ganadores.length > 0 && <div className="vynk-us22-ganadores">{ganadores.map((g, i) => <div className="vynk-us22-ganador" key={`${g.nombre}-${i}`}>
              <img src={portadaEvento} alt={`Portada de la competencia ${a.nombre}`} onError={e => { e.currentTarget.onerror = null; e.currentTarget.src = imagenTematicaActividad(a) }}/>
              <div><span>Primer puesto</span><strong>{g.nombre}</strong><small>{g.areaTecsup || 'Área por confirmar'}{g.puntos != null ? ` · ${g.puntos} puntos` : ''}</small></div>
            </div>)}</div>}
            <ol className="vynk-us22-posiciones">{(oficial.resultado.posiciones || []).slice().sort((x,y) => Number(x.puesto)-Number(y.puesto)).map((p,i) => <li key={`${p.puesto}-${i}`}><strong>{p.puesto}.º — {p.nombre}</strong><span>{p.areaTecsup || 'Área sin verificar'} · {p.puntos ?? 0} pts.</span></li>)}</ol>
          </>}
        </section>}
        <ComentarioParticipante token={token} actividadId={id}/>
        <section className="vynk-detalle__bloque vynk-us22-similares" aria-label="Actividades similares">
          <h3>Actividades similares</h3>
          {relacionadas.cargando && <p role="status">Cargando actividades similares…</p>}
          {!relacionadas.cargando && relacionadas.error && <p role="status">{relacionadas.error}</p>}
          {!relacionadas.cargando && !relacionadas.error && !relacionadas.actividades.length && <p className="vynk-detalle__nota">No hay actividades similares por ahora.</p>}
          {relacionadas.actividades.length > 0 && <div className="vynk-us22-similares__grid">{relacionadas.actividades.map(item => <a key={item.id} href={enlaceActividad(item.id)} className="vynk-us22-similar">
            <img src={urlImagenActividad(item) || imagenTematicaActividad(item)} alt={`Portada de ${item.nombre}`} onError={e => { e.currentTarget.onerror = null; e.currentTarget.src = imagenTematicaActividad(item) }}/>
            <div><strong>{item.nombre}</strong><span>{item.areaTecsup || 'Actividad de Tecsup'}</span></div>
          </a>)}</div>}
        </section>
      </div><aside className="vynk-detalle__sidebar">{finalizada ? <div className="vynk-us22-finalizada"><strong>Competencia finalizada</strong><p>Las inscripciones están cerradas. Revisa los resultados en esta página.</p></div> : <FormularioInscripcion actividad={a}/>}</aside></div>
    </>}
  </div>
}
