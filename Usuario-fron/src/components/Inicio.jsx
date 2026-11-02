import { useEffect, useMemo, useState } from 'react'
import { obtenerAgenda } from '../services/agendaService'
import { obtenerCatalogo } from '../services/catalogoService'
import { listarAvisosPublicos } from '../services/avisosPublicos'
import { urlImagenActividad, imagenTematicaActividad } from '../services/portadaActividad'
import { nombreDia, rangoHoras, hoyISO } from '../services/presentacion'
import './Inicio.css'
import campus from '../assets/campus.webp'
import Icono from './Iconos'
import { obtenerSemanaEspecial } from '../services/campanas'

/** Inicio con información real, sin cifras ni actividades simuladas. */
export function Inicio({ token, usuario }) {
  const [estado, setEstado] = useState({ cargando: true, agenda: [], catalogo: [], avisos: [], agendaError: false, catalogoError: false, error: '' })
  const [revision, setRevision] = useState(0)
  useEffect(() => {
    const ctrl = new AbortController()
    Promise.allSettled([obtenerAgenda(token, ctrl.signal), obtenerCatalogo(ctrl.signal), listarAvisosPublicos(ctrl.signal)]).then(([agenda, catalogo, avisos]) => {
      if (ctrl.signal.aborted) return
      setEstado({
        cargando: false,
        agenda: agenda.status === 'fulfilled' ? agenda.value : [],
        catalogo: catalogo.status === 'fulfilled' ? catalogo.value : [],
        avisos: avisos.status === 'fulfilled' ? avisos.value : [],
        agendaError: agenda.status === 'rejected',
        catalogoError: catalogo.status === 'rejected',
        error: agenda.status === 'rejected' && catalogo.status === 'rejected'
          ? 'No se pudieron cargar la agenda ni las actividades. Comprueba tu conexión y vuelve a intentarlo.'
          : agenda.status === 'rejected' ? 'No pudimos actualizar tu agenda. Las actividades publicadas sí están disponibles.'
          : catalogo.status === 'rejected' ? 'No pudimos actualizar las actividades. Tu agenda sí está disponible.' : '',
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
          <div className="inicio-panel__acciones"><a href="#/actividades" className="inicio-panel__btn">Explorar actividades <span aria-hidden="true">→</span></a><a href="#/agenda" className="inicio-panel__link">Ver mi agenda</a></div>
        </div>
        <div className="inicio-panel__hero-foto" aria-hidden="true"><img src={campus} alt="" /><span className="inicio-panel__hero-foto-badge">TECSUP <i /> CAMPUS</span></div>
      </section>
      <section className="inicio-panel__metricas" aria-label="Tu resumen">
        <div><span className="inicio-panel__metrica-icono"><Icono nombre="calendario" tamano={20} /></span><span>Mis inscripciones</span><strong>{estado.cargando || estado.agendaError ? '—' : estado.agenda.length}</strong><small>Actividades en tu agenda</small></div>
        <div><span className="inicio-panel__metrica-icono"><Icono nombre="estrella" tamano={20} /></span><span>Publicadas</span><strong>{estado.cargando || estado.catalogoError ? '—' : estado.catalogo.length}</strong><small>En VYNK</small></div>
        <div><span className="inicio-panel__metrica-icono"><Icono nombre="reloj" tamano={20} /></span><span>Próximos eventos</span><strong>{estado.cargando || estado.agendaError ? '—' : estado.agenda.filter(a => a.estado !== 'cancelada' && a.fecha >= hoyISO()).length}</strong><small>Desde hoy en adelante</small></div>
      </section>
      {estado.error && <div className="inicio-panel__error" role="alert">{estado.error} <button type="button" onClick={() => setRevision(n => n + 1)}>Reintentar</button></div>}
      {especial && <aside className="inicio-panel__especial" aria-label="Evento temporal vigente o próximo">
        <div><span className="inicio-panel__especial-kicker">SEMANA ESPECIAL · SOLO POR TIEMPO LIMITADO</span><h3>T&amp;C Week</h3><p>Hay {especial.cantidad} {especial.cantidad === 1 ? 'actividad publicada' : 'actividades publicadas'} de esta semana temática, desde el {nombreDia(especial.inicio)} hasta el {nombreDia(especial.fin)}. Después, VYNK continúa con el resto de actividades.</p></div>
        <a href="#/actividades">Explorar actividades <span aria-hidden="true">→</span></a>
      </aside>}
      <section className="vynk-inicio-avisos" aria-labelledby="avisos-recientes-titulo">
        <div className="inicio-panel__seccion"><div><span className="inicio-panel__eyebrow">COMUNICACIONES DE TECSUP</span><h2 id="avisos-recientes-titulo">Avisos recientes</h2></div><span className="vynk-avisos-descripcion">Publicados más recientes primero</span></div>
        {!estado.cargando && estado.avisos.length === 0 && <p className="vynk-avisos-vacio">Por ahora no hay avisos publicados.</p>}
        {estado.avisos.length > 0 && <div className="vynk-inicio-avisos__grid">{estado.avisos.slice(0, 5).map(aviso => <article className="vynk-inicio-aviso" key={aviso.id}>
          {aviso.portada?.url && <img src={urlImagenActividad({ portada: aviso.portada })} onError={e => { e.currentTarget.onerror = null; e.currentTarget.src = imagenTematicaActividad({ nombre: aviso.titulo }) }} alt={`Portada: ${aviso.titulo}`} loading="lazy" referrerPolicy="no-referrer" />}
          <div className="vynk-inicio-aviso__body"><time dateTime={aviso.publicadoEn}>{aviso.publicadoEn ? new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(aviso.publicadoEn)) : 'Aviso publicado'}</time>
          <h3>{aviso.titulo}</h3><p>{aviso.mensaje}</p>{aviso.portada?.tipo === 'commons' && aviso.portada?.creditoUrl && <a className="vynk-credito-aviso" href={aviso.portada.creditoUrl} target="_blank" rel="noopener noreferrer">Foto: {aviso.portada.autor || 'Wikimedia Commons'} · {aviso.portada.licencia || 'Licencia abierta'}</a>}</div></article>)}</div>}
      </section>
      <div className="inicio-panel__columnas"><section className="inicio-panel__columna"><div className="inicio-panel__seccion"><div><span className="inicio-panel__eyebrow">MIS INSCRIPCIONES</span><h2>Próximo en mi agenda</h2></div><a href="#/agenda">Ver agenda <span aria-hidden="true">→</span></a></div>
      {estado.cargando && <div className="inicio-panel__vacio" role="status">Preparando tu resumen…</div>}
      {!estado.cargando && !estado.agendaError && proximas.length === 0 && <div className="inicio-panel__vacio"><h3>Aún no tienes próximas actividades</h3><p>Explora las actividades disponibles y encuentra una para añadir a tu agenda.</p><a href="#/actividades">Ver actividades →</a></div>}
      {!estado.cargando && !estado.agendaError && proximas.length > 0 && <div className="inicio-panel__eventos">{proximas.map((a, i) => <article key={a.id || a.actividadId || i} className="inicio-panel__evento"><span className="inicio-panel__fecha">{a.fecha ? a.fecha.slice(8, 10) : '—'}<small>{a.fecha ? a.fecha.slice(5, 7) : ''}</small></span><div><span>{nombreDia(a.fecha)} · {rangoHoras(a.horaInicio, a.horaFin)}</span><h3>{a.nombreActividad || a.nombre || 'Actividad'}</h3><p>{a.ubicacion || 'Ubicación por confirmar'}</p></div><span className="inicio-panel__estado">Inscrito</span></article>)}</div>}
      </section><section className="inicio-panel__columna"><div className="inicio-panel__seccion"><div><span className="inicio-panel__eyebrow">ACTIVIDADES PUBLICADAS</span><h2>Para descubrir</h2></div><a href="#/actividades">Ver todas <span aria-hidden="true">→</span></a></div>
      {estado.cargando && <div className="inicio-panel__vacio" role="status">Buscando actividades del campus…</div>}
      {!estado.cargando && !estado.catalogoError && descubrir.length === 0 && <div className="inicio-panel__vacio"><h3>Por ahora no hay próximas actividades</h3><p>Las nuevas actividades publicadas aparecerán aquí.</p><a href="#/actividades">Consultar actividades →</a></div>}
      {!estado.cargando && !estado.catalogoError && descubrir.length > 0 && <div className="inicio-panel__eventos">{descubrir.map((a, i) => <a key={a.id || i} href={`#/actividad?id=${encodeURIComponent(a.id)}`} className="inicio-panel__evento inicio-panel__evento--enlace"><img className="vynk-inicio-actividad-foto" src={urlImagenActividad(a) || imagenTematicaActividad(a)} onError={e => { e.currentTarget.onerror = null; e.currentTarget.src = imagenTematicaActividad(a) }} alt={`Imagen de ${a.nombre}`} loading="lazy" /><div><span>{nombreDia(a.fecha)} · {rangoHoras(a.horaInicio, a.horaFin)}</span><h3>{a.nombre || 'Actividad'}</h3><p>{a.ubicacion || 'Lugar por confirmar'}</p></div><span className="inicio-panel__evento-ver">Ver →</span></a>)}</div>}
      </section></div>
    </div>
  )
}
