import { useEffect, useMemo, useState } from 'react'
import { obtenerCatalogo } from '../services/catalogoService'
import { agruparPorDia, cantidad, estadoCupos, estiloCategoria, nombreDia, rangoHoras, hoyISO } from '../services/presentacion'
import { esActividadTCWeek, filtrarPorEtapa } from '../services/campanas'
import { enlaceActividad } from './DetalleActividad'
import { urlImagenActividad, creditoPortada, imagenTematicaActividad } from '../services/portadaActividad'
import EstadoPantalla from './EstadoPantalla'
import Icono from './Iconos'
import './Actividades.css'
import './Catalogo.css'
import './VynkUpgrade.css'

function TarjetaActividad({ actividad: a }) {
  const categoria = estiloCategoria({ id: a.categoriaId })
  const cupos = estadoCupos(a.cuposDisponibles)
  const foto = urlImagenActividad(a) || imagenTematicaActividad(a)
  const credito = creditoPortada(a)
  return <article className="vynk-up-card">
    {foto && <a href={enlaceActividad(a.id)} className="vynk-up-card__media" aria-label={`Ver ${a.nombre}`}><img src={foto} onError={e => { e.currentTarget.onerror = null; e.currentTarget.src = imagenTematicaActividad(a) }} alt={`Portada de ${a.nombre}`} loading="lazy" /></a>}
    {credito && <a href={credito.url} className="vynk-up-card__credit" target="_blank" rel="noopener noreferrer">{credito.texto}</a>}
    <div className="vynk-up-card__top"><span className={`categoria categoria--${categoria.clase}`}>{categoria.nombre}</span>
      <span className={`cupos cupos--${cupos.tono}`}>{cupos.texto}</span></div>
    <a className="vynk-up-card__link" href={enlaceActividad(a.id)} aria-label={`Ver detalles e inscribirme en ${a.nombre}`}>
      <div className="vynk-up-card__date"><strong>{a.fecha?.slice(8, 10) || '—'}</strong><span>{a.fecha?.slice(5, 7) || '—'}</span><small>{a.tipoActividad === 'competencia' ? 'COMPETENCIA' : 'ACTIVIDAD'}</small></div>
      <h3>{a.nombre}</h3>
    </a>
    {a.areaTecsup && <span className="vynk-tarjeta-area">{a.areaTecsup}</span>}
    {esActividadTCWeek(a) && <span className="vynk-up-card__week">T&amp;C Week · evento especial</span>}
    <p className="vynk-up-card__desc">{a.descripcion || 'Actividad publicada por Tecsup. Consulta los detalles para participar.'}</p>
    <div className="vynk-up-card__info"><span><Icono nombre="reloj" tamano={16}/>{rangoHoras(a.horaInicio, a.horaFin)}</span><span><Icono nombre="ubicacion" tamano={16}/>{typeof a.ubicacion === 'string' ? a.ubicacion : a.ubicacion?.nombre || 'Ubicación por confirmar'}</span></div>
    <a className="vynk-up-card__action" href={enlaceActividad(a.id)}>Ver detalles e inscribirme <span aria-hidden="true">→</span></a>
  </article>
}

/** Lista navegable: el detalle contiene la inscripción, evitando formularios diminutos en las tarjetas. */
export function Catalogo() {
  const [estado, setEstado] = useState({ cargando: true, error: '', actividades: [] })
  const [revision, setRevision] = useState(0)
  const [busqueda, setBusqueda] = useState('')
  const [etapa, setEtapa] = useState('proximas')
  const [tipo, setTipo] = useState('todas')
  const [area, setArea] = useState('todas')
  useEffect(() => {
    const ctrl = new AbortController()
    obtenerCatalogo(ctrl.signal).then(actividades => {
      if (!ctrl.signal.aborted) setEstado({ cargando: false, error: '', actividades })
    }).catch(e => { if (!ctrl.signal.aborted) setEstado({ cargando: false, error: e.message, actividades: [] }) })
    return () => ctrl.abort()
  }, [revision])
  const { cargando, error, actividades } = estado
  const visibles = useMemo(() => {
    const termino = busqueda.trim().toLocaleLowerCase('es-PE')
    return filtrarPorEtapa(actividades, etapa, hoyISO()).filter(a =>
      (area === 'todas' || a.areaTecsup === area) &&
      (tipo === 'todas' || (tipo === 'competencias' ? a.tipoActividad === 'competencia' : a.tipoActividad !== 'competencia')) &&
      (!termino || `${a.nombre || ''} ${a.descripcion || ''} ${a.categoriaId || ''} ${a.organizador || ''}`.toLocaleLowerCase('es-PE').includes(termino)))
  }, [actividades, etapa, busqueda, tipo, area])
  const grupos = useMemo(() => agruparPorDia(visibles), [visibles])
  const actualizar = () => { setEstado(e => ({ ...e, cargando: true, error: '' })); setRevision(v => v + 1) }
  return <div className="catalogo vynk-up-actividades">
    <section className="vynk-up-actividades__hero"><div><span className="vynk-up-eyebrow">VYNK · VIDA EN EL CAMPUS</span><h2>Actividades para vivir Tecsup</h2><p>Explora competencias de T&amp;C Week y actividades del ciclo. Entra a cada una, revisa los requisitos e inscríbete con tu cuenta.</p>
      <div className="vynk-up-hero-stats"><span><strong>{cargando ? '—' : actividades.length}</strong> publicadas</span><span><strong>{cargando ? '—' : actividades.filter(a => a.tipoActividad === 'competencia').length}</strong> competencias</span></div></div>
      <a href="#/resultados" className="vynk-up-btn vynk-up-btn--light">Ver resultados →</a></section>
    <div className="vynk-up-toolbar"><div className="vynk-up-tabs" role="group" aria-label="Filtrar actividades según la fecha">{[['proximas','Próximas'],['todas','Todas'],['pasadas','Finalizadas']].map(([valor,texto])=><button key={valor} type="button" aria-pressed={etapa===valor} onClick={()=>setEtapa(valor)}>{texto}</button>)}</div>
      <div className="vynk-up-search"><label><Icono nombre="buscar" tamano={17}/><input value={busqueda} type="search" placeholder="Buscar actividades o competencias…" aria-label="Buscar actividades" onChange={e=>setBusqueda(e.target.value)}/></label>
        <select value={tipo} onChange={e=>setTipo(e.target.value)} aria-label="Filtrar por tipo de actividad"><option value="todas">Todos los tipos</option><option value="competencias">Competencias</option><option value="actividades">Otras actividades</option></select><select value={area} onChange={e=>setArea(e.target.value)} aria-label="Filtrar por área"><option value="todas">Todas las áreas</option>{['Gestión y Producción','Tecnología Digital','Minería y Procesos Químicos','Electricidad y Electrónica','Mecánica'].map(x=><option key={x} value={x}>{x}</option>)}</select></div></div>
    <div className="vynk-up-list-heading"><strong>{cargando ? 'Cargando actividades…' : cantidad(visibles.length, 'actividad disponible', 'actividades disponibles')}</strong><button type="button" className="vynk-up-reload" onClick={actualizar} disabled={cargando}><Icono nombre="actualizar" tamano={17}/> Actualizar</button></div>
    {error && <EstadoPantalla icono="alerta" tono="error" titulo="No se pudieron cargar las actividades" texto={error} accion={<button type="button" className="vynk-up-btn vynk-up-btn--outline" onClick={actualizar}>Reintentar</button>}/>}
    {cargando && !error && <div className="catalogo__grid" aria-busy="true">{Array.from({length:6},(_,i)=><div className="vynk-up-card vynk-up-card--loading" key={i}/>)}</div>}
    {!cargando && !error && !actividades.length && <EstadoPantalla icono="calendario" titulo="Pronto habrá actividades" texto="Cuando el administrador publique actividades, aparecerán aquí automáticamente."/>}
    {!cargando && !error && actividades.length > 0 && !visibles.length && <div className="catalogo__sin-resultados"><h3>No encontramos actividades con esos filtros</h3><p>Prueba otro término o consulta las actividades de todas las fechas.</p><button className="vynk-up-btn vynk-up-btn--outline" onClick={()=>{setBusqueda('');setEtapa('todas');setTipo('todas');setArea('todas')}}>Mostrar todas</button></div>}
    {!cargando && !error && grupos.map(g=><section className="dia vynk-up-dia" key={g.fecha??'sin-fecha'}><header className="dia__cabecera"><h2>{nombreDia(g.fecha)}</h2><span>{cantidad(g.items.length,'actividad','actividades')}</span></header><div className="catalogo__grid">{g.items.map(a=><TarjetaActividad key={a.id} actividad={a}/>)}</div></section>)}
  </div>
}
