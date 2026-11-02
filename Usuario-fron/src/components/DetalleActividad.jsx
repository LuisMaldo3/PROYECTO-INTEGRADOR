import { useEffect, useState } from 'react'
import { obtenerCatalogo } from '../services/catalogoService'
import { nombreDia, rangoHoras } from '../services/presentacion'
import { FormularioInscripcion } from './FormularioInscripcion'
import { urlImagenActividad, creditoPortada, imagenTematicaActividad } from '../services/portadaActividad'
import EstadoPantalla from './EstadoPantalla'
import './VynkUpgrade.css'

export const enlaceActividad = id => `#/actividad?id=${encodeURIComponent(id)}`

/** La tarjeta de Actividades abre aquí: ficha completa + inscripción, sin perder contexto. */
export function DetalleActividad() {
  const id = new URLSearchParams((window.location.hash.split('?')[1] || '').split('#')[0]).get('id')
  const [estado, setEstado] = useState({ cargando: true, error: '', actividad: null })
  const [revision, setRevision] = useState(0)
  useEffect(() => {
    if (!id) { setEstado({ cargando: false, error: 'No se seleccionó una actividad.', actividad: null }); return }
    const ctrl = new AbortController()
    obtenerCatalogo(ctrl.signal).then(items => {
      if (!ctrl.signal.aborted) {
        const actividad = items.find(a => String(a.id) === id)
        setEstado({ cargando: false, error: actividad ? '' : 'Esta actividad no se encuentra disponible o ya no está publicada.', actividad: actividad || null })
      }
    }).catch(e => { if (!ctrl.signal.aborted) setEstado({ cargando: false, error: e.message, actividad: null }) })
    return () => ctrl.abort()
  }, [id, revision])
  const a = estado.actividad
  return <div className="vynk-detalle">
    <a href="#/actividades" className="vynk-up-back">← Volver a Actividades</a>
    {estado.cargando && <div role="status" className="vynk-up-loading">Cargando la actividad…</div>}
    {estado.error && <EstadoPantalla icono="alerta" tono="error" titulo="No se pudo abrir la actividad" texto={estado.error}
      accion={<button type="button" className="vynk-up-btn vynk-up-btn--outline" onClick={() => setRevision(r => r + 1)}>Volver a intentar</button>}/>} 
    {a && <>
      <section className="vynk-detalle__hero"><div className="vynk-detalle__hero-content">
        <div className="vynk-detalle__badges"><span>{a.tipoActividad === 'competencia' ? 'COMPETENCIA T&C WEEK' : 'ACTIVIDAD DEL CAMPUS'}</span><span>{a.cuposDisponibles == null ? 'Cupos por confirmar' : `${a.cuposDisponibles} cupos disponibles`}</span></div>
        <h2>{a.nombre}</h2><p>{a.descripcion || 'Actividad publicada por Tecsup.'}</p>
        <div className="vynk-detalle__hero-actions"><button type="button" className="vynk-up-btn vynk-up-btn--light" onClick={() => document.getElementById('inscripcion')?.scrollIntoView({ behavior: 'smooth', block: 'start' })}>Inscribirme en esta actividad →</button><a className="vynk-up-btn vynk-up-btn--glass" href="#/agenda">Mi agenda</a></div>
      </div><div className="vynk-detalle__hero-imagen"><img src={urlImagenActividad(a) || imagenTematicaActividad(a)} onError={e => { e.currentTarget.onerror = null; e.currentTarget.src = imagenTematicaActividad(a) }} alt={`Portada de ${a.nombre}`} />{creditoPortada(a) && <a href={creditoPortada(a).url} target="_blank" rel="noopener noreferrer">{creditoPortada(a).texto}</a>}</div></section>
      <div className="vynk-detalle__layout"><div className="vynk-detalle__principal">
        <section className="vynk-detalle__bloque"><div className="vynk-up-eyebrow">DETALLES DE LA ACTIVIDAD</div><h3>Información para participar</h3>
          <div className="vynk-detalle__ficha"><div><span>Fecha</span><strong>{nombreDia(a.fecha)}</strong></div><div><span>Horario</span><strong>{rangoHoras(a.horaInicio, a.horaFin)}</strong></div>
            <div><span>Ubicación</span><strong>{typeof a.ubicacion === 'string' ? a.ubicacion : a.ubicacion?.nombre || 'Por confirmar'}</strong></div><div><span>Organiza</span><strong>{typeof a.organizador === 'string' ? a.organizador : a.organizador?.nombre || 'Tecsup'}</strong></div>
            <div><span>Área responsable</span><strong>{a.areaTecsup || 'Cinco áreas de Tecsup'}</strong></div><div><span>Modalidad</span><strong>{a.tipoActividad !== 'competencia' ? 'Actividad' : a.modalidad === 'equipos' ? 'Por equipos' : 'Individual'}</strong></div><div><span>Capacidad</span><strong>{a.maximoParticipantes == null ? 'Por confirmar' : `${a.maximoParticipantes} ${a.modalidad === 'equipos' ? 'equipos' : 'participantes'}`}</strong></div>
            {a.modalidad === 'equipos' && <div><span>Integrantes por equipo</span><strong>Entre 2 y {a.maxIntegrantesEquipo || '—'} estudiantes con inscripción personal</strong></div>}</div>
        </section>
        {a.encargadosPorArea && Object.keys(a.encargadosPorArea).length > 0 && <section className="vynk-detalle__bloque"><div className="vynk-up-eyebrow">RESPONSABLES POR ÁREA</div><h3>Encargados de departamentos</h3><div className="vynk-encargados-detalle">{Object.entries(a.encargadosPorArea).map(([area, info]) => <p key={area}><strong>{area}</strong><span>{info?.nombre || 'Sin asignar'}</span></p>)}</div></section>}
        {a.requisitos && <section className="vynk-detalle__bloque"><div className="vynk-up-eyebrow">ANTES DE INSCRIBIRTE</div><h3>Requisitos de participación</h3><p className="vynk-up-richtext">{typeof a.requisitos === 'string' ? a.requisitos : Array.isArray(a.requisitos) ? a.requisitos.join(' · ') : 'Consulta los requisitos con el organizador.'}</p></section>}
                {a.tipoActividad === 'competencia' && <section className="vynk-detalle__bloque"><div className="vynk-up-eyebrow">T&C WEEK</div><h3>Reglamento de la competencia</h3><p className="vynk-up-richtext">{a.reglasCompetencia || 'El organizador aún no publicó un reglamento específico.'}</p>
          {Array.isArray(a.puntosPorPuesto) && a.puntosPorPuesto.length > 0 && <div className="vynk-detalle__puntos"><strong>Puntos por posición</strong><div>{a.puntosPorPuesto.map((p,i) => <span key={i}>{i+1}.º: {p} pts.</span>)}</div></div>}
          <p className="vynk-up-footnote">El organizador registra los resultados y el administrador los valida antes de publicarlos.</p></section>}
      </div><aside className="vynk-detalle__sidebar"><FormularioInscripcion actividad={a}/></aside></div>
    </>}
  </div>
}
