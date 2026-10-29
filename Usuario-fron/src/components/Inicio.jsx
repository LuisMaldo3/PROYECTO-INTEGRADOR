import { useEffect, useMemo, useState } from 'react'
import { obtenerAgenda } from '../services/agendaService'
import { obtenerCatalogo } from '../services/catalogoService'
import { nombreDia, rangoHoras, hoyISO } from '../services/presentacion'
import './Inicio.css'

/** Inicio con información real, sin cifras ni actividades simuladas. */
export function Inicio({ token }) {
  const [estado, setEstado] = useState({ cargando: true, agenda: [], catalogo: [], error: '' })
  const [revision, setRevision] = useState(0)
  useEffect(() => {
    const ctrl = new AbortController()
    Promise.allSettled([obtenerAgenda(token, ctrl.signal), obtenerCatalogo(ctrl.signal)]).then(([agenda, catalogo]) => {
      if (ctrl.signal.aborted) return
      setEstado({
        cargando: false,
        agenda: agenda.status === 'fulfilled' ? agenda.value : [],
        catalogo: catalogo.status === 'fulfilled' ? catalogo.value : [],
        error: agenda.status === 'rejected' && catalogo.status === 'rejected' ? 'No se pudo conectar con los servicios de actividades. Comprueba tu sesión y conexión.' : '',
      })
    })
    return () => ctrl.abort()
  }, [token, revision])
  const proximas = useMemo(() => estado.agenda.filter(a => a.estado !== 'cancelada' && a.fecha >= hoyISO())
    .sort((a, b) => `${a.fecha || ''} ${a.horaInicio || ''}`.localeCompare(`${b.fecha || ''} ${b.horaInicio || ''}`)).slice(0, 3), [estado.agenda])
  return (
    <div className="inicio-panel">
      <section className="inicio-panel__hero">
        <div className="inicio-panel__hero-texto">
          <span className="inicio-panel__eyebrow">TU ESPACIO EN VYNK</span>
          <h2>Descubre, participa y aprovecha cada oportunidad.</h2>
          <p>Tus actividades, próximos encuentros y nuevas experiencias en un solo lugar.</p>
          <div className="inicio-panel__acciones"><a href="#/catalogo" className="inicio-panel__btn">Explorar actividades <span aria-hidden="true">→</span></a><a href="#/agenda" className="inicio-panel__link">Ver mi agenda</a></div>
        </div>
        <div className="inicio-panel__hero-figura" aria-hidden="true"><span>V</span><span>✦</span></div>
      </section>
      <section className="inicio-panel__metricas" aria-label="Tu resumen">
        <div><span>Mis inscripciones</span><strong>{estado.cargando ? '—' : estado.agenda.length}</strong><small>Actividades en tu agenda</small></div>
        <div><span>Para descubrir</span><strong>{estado.cargando ? '—' : estado.catalogo.length}</strong><small>Actividades del catálogo</small></div>
        <div><span>Próximos eventos</span><strong>{estado.cargando ? '—' : estado.agenda.filter(a => a.estado !== 'cancelada' && a.fecha >= hoyISO()).length}</strong><small>Desde hoy en adelante</small></div>
      </section>
      {estado.error && <div className="inicio-panel__error" role="alert">{estado.error} <button type="button" onClick={() => setRevision(n => n + 1)}>Reintentar</button></div>}
      <div className="inicio-panel__seccion"><div><span className="inicio-panel__eyebrow">TU PROGRAMACIÓN</span><h2>Lo que viene en tu agenda</h2></div><a href="#/agenda">Ver agenda completa <span aria-hidden="true">→</span></a></div>
      {estado.cargando && <div className="inicio-panel__vacio" role="status">Preparando tu resumen…</div>}
      {!estado.cargando && !estado.error && proximas.length === 0 && <div className="inicio-panel__vacio"><h3>Aún no tienes próximas actividades</h3><p>Explora el catálogo y encuentra actividades para añadir a tu agenda.</p><a href="#/catalogo">Ir al catálogo →</a></div>}
      {!estado.cargando && proximas.length > 0 && <div className="inicio-panel__eventos">{proximas.map((a, i) => <article key={a.id || a.actividadId || i} className="inicio-panel__evento"><span className="inicio-panel__fecha">{a.fecha ? a.fecha.slice(8, 10) : '—'}<small>{a.fecha ? a.fecha.slice(5, 7) : ''}</small></span><div><span>{nombreDia(a.fecha)} · {rangoHoras(a.horaInicio, a.horaFin)}</span><h3>{a.nombreActividad || a.nombre || 'Actividad'}</h3><p>{a.ubicacion || 'Ubicación por confirmar'}</p></div><span className="inicio-panel__estado">Inscrito</span></article>)}</div>}
    </div>
  )
}
