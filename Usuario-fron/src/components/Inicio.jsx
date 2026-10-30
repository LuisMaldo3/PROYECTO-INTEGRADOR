import { useEffect, useMemo, useState } from 'react'
import { obtenerAgenda } from '../services/agendaService'
import { obtenerCatalogo } from '../services/catalogoService'
import { nombreDia, rangoHoras, hoyISO } from '../services/presentacion'
import './Inicio.css'
import campus from '../assets/campus.webp'
import Icono from './Iconos'
import { obtenerSemanaEspecial } from '../services/campanas'

/** Inicio con información real, sin cifras ni actividades simuladas. */
export function Inicio({ token, usuario }) {
  const [estado, setEstado] = useState({ cargando: true, agenda: [], catalogo: [], agendaError: false, catalogoError: false, error: '' })
  const [revision, setRevision] = useState(0)
  useEffect(() => {
    const ctrl = new AbortController()
    Promise.allSettled([obtenerAgenda(token, ctrl.signal), obtenerCatalogo(ctrl.signal)]).then(([agenda, catalogo]) => {
      if (ctrl.signal.aborted) return
      setEstado({
        cargando: false,
        agenda: agenda.status === 'fulfilled' ? agenda.value : [],
        catalogo: catalogo.status === 'fulfilled' ? catalogo.value : [],
        agendaError: agenda.status === 'rejected',
        catalogoError: catalogo.status === 'rejected',
        error: agenda.status === 'rejected' && catalogo.status === 'rejected'
          ? 'No se pudieron cargar la agenda ni el catálogo. Comprueba tu conexión y vuelve a intentarlo.'
          : agenda.status === 'rejected' ? 'No pudimos actualizar tu agenda. Las actividades del catálogo sí están disponibles.'
          : catalogo.status === 'rejected' ? 'No pudimos actualizar el catálogo. Tu agenda sí está disponible.' : '',
      })
    })
    return () => ctrl.abort()
  }, [token, revision])
  const primerNombre = String(usuario?.nombre || '').trim().split(/\s+/)[0]
  const especial = useMemo(() => estado.catalogoError ? null : obtenerSemanaEspecial(estado.catalogo, hoyISO()), [estado.catalogo, estado.catalogoError])
  const descubrir = useMemo(() => estado.catalogo.filter(a => a.fecha && a.fecha >= hoyISO())
    .sort((a, b) => `${a.fecha || ''} ${a.horaInicio || ''}`.localeCompare(`${b.fecha || ''} ${b.horaInicio || ''}`)).slice(0, 3), [estado.catalogo])
  const proximas = useMemo(() => estado.agenda.filter(a => a.estado !== 'cancelada' && a.fecha >= hoyISO())
    .sort((a, b) => `${a.fecha || ''} ${a.horaInicio || ''}`.localeCompare(`${b.fecha || ''} ${b.horaInicio || ''}`)).slice(0, 3), [estado.agenda])
  return (
    <div className="inicio-panel">
      <section className="inicio-panel__hero">
        <div className="inicio-panel__hero-texto">
          <span className="inicio-panel__eyebrow">{primerNombre ? `HOLA, ${primerNombre.toLocaleUpperCase('es-PE')}` : 'TU ESPACIO EN VYNK'}</span>
          <h2>Todo lo que pasa en tu campus, en un lugar.</h2>
          <p>Encuentra actividades, revisa tus inscripciones y descubre nuevas oportunidades durante todo el año.</p>
          <div className="inicio-panel__acciones"><a href="#/catalogo" className="inicio-panel__btn">Explorar actividades <span aria-hidden="true">→</span></a><a href="#/agenda" className="inicio-panel__link">Ver mi agenda</a></div>
        </div>
        <div className="inicio-panel__hero-foto" aria-hidden="true"><img src={campus} alt="" /><span className="inicio-panel__hero-foto-badge">TECSUP <i /> CAMPUS</span></div>
      </section>
      <section className="inicio-panel__metricas" aria-label="Tu resumen">
        <div><span className="inicio-panel__metrica-icono"><Icono nombre="calendario" tamano={20} /></span><span>Mis inscripciones</span><strong>{estado.cargando || estado.agendaError ? '—' : estado.agenda.length}</strong><small>Actividades en tu agenda</small></div>
        <div><span className="inicio-panel__metrica-icono"><Icono nombre="estrella" tamano={20} /></span><span>Publicadas</span><strong>{estado.cargando || estado.catalogoError ? '—' : estado.catalogo.length}</strong><small>En el catálogo</small></div>
        <div><span className="inicio-panel__metrica-icono"><Icono nombre="reloj" tamano={20} /></span><span>Próximos eventos</span><strong>{estado.cargando || estado.agendaError ? '—' : estado.agenda.filter(a => a.estado !== 'cancelada' && a.fecha >= hoyISO()).length}</strong><small>Desde hoy en adelante</small></div>
      </section>
      {estado.error && <div className="inicio-panel__error" role="alert">{estado.error} <button type="button" onClick={() => setRevision(n => n + 1)}>Reintentar</button></div>}
      {especial && <aside className="inicio-panel__especial" aria-label="Evento temporal vigente o próximo">
        <div><span className="inicio-panel__especial-kicker">SEMANA ESPECIAL · SOLO POR TIEMPO LIMITADO</span><h3>T&amp;C Week</h3><p>Hay {especial.cantidad} {especial.cantidad === 1 ? 'actividad publicada' : 'actividades publicadas'} de esta semana temática, desde el {nombreDia(especial.inicio)} hasta el {nombreDia(especial.fin)}. Después, VYNK continúa con el resto de actividades.</p></div>
        <a href="#/catalogo">Explorar catálogo <span aria-hidden="true">→</span></a>
      </aside>}
      <div className="inicio-panel__columnas"><section className="inicio-panel__columna"><div className="inicio-panel__seccion"><div><span className="inicio-panel__eyebrow">MIS INSCRIPCIONES</span><h2>Próximo en mi agenda</h2></div><a href="#/agenda">Ver agenda <span aria-hidden="true">→</span></a></div>
      {estado.cargando && <div className="inicio-panel__vacio" role="status">Preparando tu resumen…</div>}
      {!estado.cargando && !estado.agendaError && proximas.length === 0 && <div className="inicio-panel__vacio"><h3>Aún no tienes próximas actividades</h3><p>Explora el catálogo y encuentra actividades para añadir a tu agenda.</p><a href="#/catalogo">Ir al catálogo →</a></div>}
      {!estado.cargando && !estado.agendaError && proximas.length > 0 && <div className="inicio-panel__eventos">{proximas.map((a, i) => <article key={a.id || a.actividadId || i} className="inicio-panel__evento"><span className="inicio-panel__fecha">{a.fecha ? a.fecha.slice(8, 10) : '—'}<small>{a.fecha ? a.fecha.slice(5, 7) : ''}</small></span><div><span>{nombreDia(a.fecha)} · {rangoHoras(a.horaInicio, a.horaFin)}</span><h3>{a.nombreActividad || a.nombre || 'Actividad'}</h3><p>{a.ubicacion || 'Ubicación por confirmar'}</p></div><span className="inicio-panel__estado">Inscrito</span></article>)}</div>}
      </section><section className="inicio-panel__columna"><div className="inicio-panel__seccion"><div><span className="inicio-panel__eyebrow">DESDE EL CATÁLOGO</span><h2>Para descubrir</h2></div><a href="#/catalogo">Ver todas <span aria-hidden="true">→</span></a></div>
      {estado.cargando && <div className="inicio-panel__vacio" role="status">Buscando actividades del campus…</div>}
      {!estado.cargando && !estado.catalogoError && descubrir.length === 0 && <div className="inicio-panel__vacio"><h3>Por ahora no hay próximas actividades</h3><p>Las nuevas publicaciones del catálogo aparecerán aquí.</p><a href="#/catalogo">Consultar catálogo →</a></div>}
      {!estado.cargando && !estado.catalogoError && descubrir.length > 0 && <div className="inicio-panel__eventos">{descubrir.map((a, i) => <article key={a.id || i} className="inicio-panel__evento"><span className="inicio-panel__fecha">{a.fecha?.slice(8, 10) || '—'}<small>{a.fecha?.slice(5, 7) || ''}</small></span><div><span>{nombreDia(a.fecha)} · {rangoHoras(a.horaInicio, a.horaFin)}</span><h3>{a.nombre || 'Actividad'}</h3><p>{a.ubicacion || 'Lugar por confirmar'}</p></div></article>)}</div>}
      </section></div>
    </div>
  )
}
