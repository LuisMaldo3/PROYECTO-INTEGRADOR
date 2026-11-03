import { useEffect, useMemo, useState } from 'react'
import { obtenerCatalogo } from '../services/catalogoService'
import { agruparPorDia, bloqueDia, cantidad, estadoCupos, estiloCategoria, nombreDia, rangoHoras, hoyISO } from '../services/presentacion'
import { AREAS_TECSUP, claveArea } from '../services/areas'
import { esActividadTCWeek, filtrarPorEtapa } from '../services/campanas'
import { enlaceActividad } from './DetalleActividad'
import { urlImagenActividad, creditoPortada, imagenTematicaActividad } from '../services/portadaActividad'
import EstadoPantalla from './EstadoPantalla'
import Icono from './Iconos'

function TarjetaActividad({ actividad: a }) {
  const categoria = estiloCategoria({ id: a.categoriaId })
  const cupos = estadoCupos(a.cuposDisponibles)
  const foto = urlImagenActividad(a) || imagenTematicaActividad(a)
  const credito = creditoPortada(a)
  const lugar = typeof a.ubicacion === 'string' ? a.ubicacion : a.ubicacion?.nombre || 'Ubicación por confirmar'
  return <article className="vynk-up-card" data-area={claveArea(a.areaTecsup)}>
    <a href={enlaceActividad(a.id)} className="vynk-up-card__media" tabIndex={-1} aria-hidden="true">
      <img src={foto} onError={e => { e.currentTarget.onerror = null; e.currentTarget.src = imagenTematicaActividad(a) }} alt="" loading="lazy" />
      <span className="vynk-up-card__date"><strong>{a.fecha?.slice(8, 10) || '—'}</strong>{bloqueDia(a.fecha)?.mes || ''}</span>
    </a>
    {credito && <a href={credito.url} className="vynk-up-card__credit" target="_blank" rel="noopener noreferrer">{credito.texto}</a>}
    <div className="vynk-up-card__cuerpo">
      <div className="vynk-up-card__top">
        <span className={`categoria categoria--${a.tipoActividad === 'competencia' ? 'concurso' : categoria.clase}`}>{a.tipoActividad === 'competencia' ? 'Competencia' : categoria.nombre}</span>
        <span className={`cupos cupos--${cupos.tono}`}>{cupos.texto}</span>
      </div>
      <h3><a className="vynk-up-card__link" href={enlaceActividad(a.id)}>{a.nombre}</a></h3>
      {a.areaTecsup && <span className="vynk-tarjeta-area">{a.areaTecsup}</span>}
      {a.descripcion && <p className="vynk-up-card__desc">{a.descripcion}</p>}
      <div className="vynk-up-card__info">
        <span><Icono nombre="calendario" tamano={16}/>{nombreDia(a.fecha)} · {rangoHoras(a.horaInicio, a.horaFin)}</span>
        <span><Icono nombre="ubicacion" tamano={16}/>{lugar}</span>
      </div>
    </div>
    {esActividadTCWeek(a) && <span className="vynk-up-card__week">T&amp;C Week</span>}
  </article>
}

/** Catálogo: cada tarjeta lleva al detalle, donde está la inscripción. */
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
  const ordenadas = useMemo(() => agruparPorDia(visibles).flatMap(g => g.items), [visibles])
  const actualizar = () => { setEstado(e => ({ ...e, cargando: true, error: '' })); setRevision(v => v + 1) }
  return <div className="catalogo vynk-up-actividades">
    <div className="vynk-up-toolbar">
      <div className="vynk-up-tabs" role="group" aria-label="Filtrar actividades según la fecha">{[['proximas','Próximas'],['todas','Todas'],['pasadas','Finalizadas']].map(([valor,texto])=><button key={valor} type="button" aria-pressed={etapa===valor} onClick={()=>setEtapa(valor)}>{texto}</button>)}</div>
      <label className="vynk-up-search"><Icono nombre="buscar" tamano={18}/><input value={busqueda} type="search" placeholder="Buscar actividades" aria-label="Buscar actividades" onChange={e=>setBusqueda(e.target.value)}/></label>
      <select value={tipo} onChange={e=>setTipo(e.target.value)} aria-label="Filtrar por tipo"><option value="todas">Todos los tipos</option><option value="competencias">Competencias</option><option value="actividades">Otras actividades</option></select>
      <select value={area} onChange={e=>setArea(e.target.value)} aria-label="Filtrar por área"><option value="todas">Todas las áreas</option>{AREAS_TECSUP.map(x=><option key={x} value={x}>{x}</option>)}</select>
    </div>
    <div className="vynk-up-list-heading">
      <strong>{cargando ? 'Cargando actividades…' : cantidad(visibles.length, 'actividad', 'actividades')}</strong>
      <div className="vynk-up-list-heading__acciones">
        <a href="#/resultados" className="vynk-up-enlace">Ver resultados de T&amp;C Week</a>
        <button type="button" className="vynk-up-reload" onClick={actualizar} disabled={cargando}><Icono nombre="actualizar" tamano={16}/> Actualizar</button>
      </div>
    </div>
    {error && <EstadoPantalla icono="alerta" tono="error" titulo="No se pudieron cargar las actividades" texto={error} accion={<button type="button" className="vynk-up-btn vynk-up-btn--outline" onClick={actualizar}>Reintentar</button>}/>}
    {cargando && !error && <div className="catalogo__grid" aria-busy="true">{Array.from({length:6},(_,i)=><div className="vynk-up-card vynk-up-card--loading" key={i}/>)}</div>}
    {!cargando && !error && !actividades.length && <EstadoPantalla icono="calendario" titulo="Todavía no hay actividades" texto="Las actividades publicadas aparecerán aquí."/>}
    {!cargando && !error && actividades.length > 0 && !visibles.length && <EstadoPantalla icono="buscar" titulo="Sin resultados" texto="Prueba con otra palabra o quita los filtros." accion={<button type="button" className="vynk-up-btn vynk-up-btn--outline" onClick={()=>{setBusqueda('');setEtapa('todas');setTipo('todas');setArea('todas')}}>Quitar filtros</button>}/>}
    {!cargando && !error && visibles.length > 0 && <div className="catalogo__grid">{ordenadas.map(a=><TarjetaActividad key={a.id} actividad={a}/>)}</div>}
  </div>
}
