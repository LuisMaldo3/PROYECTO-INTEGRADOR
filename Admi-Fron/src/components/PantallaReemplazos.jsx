import { useEffect, useMemo, useState } from 'react'
import Icono from './Iconos'
import { consultarReemplazos } from '../services/reemplazosService'
import { diagnosticoCupo } from '../services/reemplazos'
import './PantallaReemplazos.css'

const inicial = { datos: null, error: '', cargando: true }
const fechaLocal = (fecha) => {
  if (!fecha) return 'Sin fecha registrada'
  const parsed = new Date(fecha)
  return Number.isNaN(parsed.getTime()) ? 'Fecha no disponible' : parsed.toLocaleString('es-PE', { dateStyle: 'medium', timeStyle: 'short' })
}

/** US-36: informa; nunca cambia, aumenta ni reasigna cupos. */
export default function PantallaReemplazos({ token, onSesionExpirada }) {
  const [estado, setEstado] = useState(inicial)
  const [version, setVersion] = useState(0)
  const [buscar, setBuscar] = useState('')
  const [filtro, setFiltro] = useState('vigentes')
  const [seleccionado, setSeleccionado] = useState('')

  useEffect(() => {
    const controlador = new AbortController()
    consultarReemplazos(token, controlador.signal)
      .then((datos) => { if (!controlador.signal.aborted) setEstado({ datos, error: '', cargando: false }) })
      .catch((error) => {
        if (controlador.signal.aborted) return
        if (error?.estado === 401) { onSesionExpirada?.(error.detalle); return }
        setEstado((antes) => ({ ...antes, cargando: false, error: error.message || 'No se pudieron consultar los reemplazos.' }))
      })
    return () => controlador.abort()
  }, [token, version, onSesionExpirada])

  const actividades = estado.datos?.actividades ?? []
  const visibles = useMemo(() => actividades.filter((a) => {
    if (buscar && !a.nombre.toLocaleLowerCase('es-PE').includes(buscar.trim().toLocaleLowerCase('es-PE'))) return false
    if (filtro === 'vigentes') return a.estado !== 'cancelada'
    if (filtro === 'exceso') return a.capacidad !== null && a.ocupados > a.capacidad
    if (filtro === 'historial') return a.historial.length > 0
    return true
  }), [actividades, buscar, filtro])
  const detalle = visibles.find((a) => a.id === seleccionado) ?? visibles[0] ?? null
  const totales = estado.datos?.totales

  return <section className="reemplazos" aria-label="Seguimiento de reemplazos y cupos">
    <header className="pagina__cabecera">
      <div>
        <h1>Reemplazos y control de cupos</h1>
        <p>Supervisión administrativa de inscripciones y sustituciones confirmadas. Los cupos solo los establece el administrador al gestionar actividades.</p>
      </div>
      <button type="button" className="boton boton--claro" disabled={estado.cargando} onClick={() => { setEstado((e) => ({ ...e, cargando: true, error: '' })); setVersion((v) => v + 1) }}>
        <Icono nombre="actualizar" tam={17}/>{estado.cargando ? 'Consultando…' : 'Actualizar'}
      </button>
    </header>
    {totales && <dl className="cifras reemplazos__resumen">
      <div><dt>Actividades vigentes</dt><dd>{totales.actividades}</dd></div>
      <div><dt>Reemplazos registrados</dt><dd>{totales.reemplazosConfirmados}</dd></div>
      <div><dt>Actividades con exceso</dt><dd>{totales.actividadesConExceso}</dd></div>
      <div><dt>Equipos pendientes</dt><dd>{totales.equiposIncompletos}</dd></div>
    </dl>}
    {estado.cargando && !totales && <p className="reemplazos__estado" role="status">Consultando inscripciones y registros de reemplazos…</p>}
    {estado.error && <p className="aviso aviso--error" role="alert"><Icono nombre="alerta"/>{estado.error}</p>}
    {estado.datos && !estado.datos.historialDisponible && <p className="reemplazos__nota">No hay reemplazos confirmados registrados en el historial. El control de cupos se calcula directamente desde las inscripciones activas. Los reemplazos solo aparecerán si el sistema del organizador registra cada sustitución.</p>}
    {estado.datos && <>
      <div className="reemplazos__filtros">
        <label className="reemplazos__busqueda">Buscar actividad <input type="search" value={buscar} onChange={(e) => setBuscar(e.target.value)} placeholder="Nombre de la actividad"/></label>
        <label className="reemplazos__selector">Mostrar <select value={filtro} onChange={(e) => setFiltro(e.target.value)}>
          <option value="vigentes">Actividades vigentes</option><option value="exceso">Solo exceso de cupos</option><option value="historial">Con reemplazos</option><option value="todas">Todas, incluidas canceladas</option>
        </select></label>
      </div>
      {visibles.length === 0 ? <p className="reemplazos__estado">No hay actividades que coincidan con el filtro.</p> : <div className="reemplazos__layout">
        <div className="reemplazos__lista" aria-label="Actividades con control de cupos">
          {visibles.map((a) => {
            const cupo = diagnosticoCupo(a)
            return <button type="button" key={a.id} className={`reemplazos__item${detalle?.id === a.id ? ' reemplazos__item--activo' : ''}`} onClick={() => setSeleccionado(a.id)} aria-pressed={detalle?.id === a.id}>
              <strong>{a.nombre}</strong><span>{a.ocupados}{a.capacidad === null ? '' : ` / ${a.capacidad}`} {a.unidadCupo} · {cupo.etiqueta}</span>
              <small>{a.reemplazosConfirmados} reemplazos confirmados</small>
            </button>
          })}
        </div>
        {detalle && <article className="reemplazos__detalle">
          <h2>{detalle.nombre}</h2>
          <p className="reemplazos__leyenda">Cupo definido por administración · unidad: {detalle.unidadCupo}.</p>
          <div className="reemplazos__indicadores">
            <div><span>Capacidad</span><strong>{detalle.capacidad ?? 'Sin definir'}</strong></div>
            <div><span>Cupos ocupados</span><strong>{detalle.ocupados}</strong></div>
            <div><span>Disponibles</span><strong>{diagnosticoCupo(detalle).disponibles ?? '—'}</strong></div>
            <div><span>Personas inscritas</span><strong>{detalle.personasInscritas}</strong></div>
            {detalle.unidadCupo === 'equipos' && <div><span>Equipos pendientes</span><strong>{detalle.equiposIncompletos}</strong></div>}
          </div>
          {diagnosticoCupo(detalle).estado === 'exceso' && <p className="aviso aviso--error" role="alert">Se detectaron más {detalle.unidadCupo} inscritos que el límite permitido. Revisar inscripciones desde el flujo correspondiente; este panel no cambia cupos.</p>}
          <h3>Historial de reemplazos confirmados ({detalle.reemplazosConfirmados})</h3>
          {detalle.historial.length === 0 ? <p className="reemplazos__vacio">Sin sustituciones confirmadas registradas para esta actividad.</p> : <ol className="reemplazos__historial">
            {detalle.historial.map((evento, indice) => <li key={evento.id || `${indice}-${evento.fecha}`}>
              <strong>{evento.nombreSaliente || evento.estudianteSalienteId || 'Estudiante saliente'} → {evento.nombreEntrante || evento.estudianteEntranteId || 'Estudiante entrante'}</strong>
              <span>{fechaLocal(evento.fecha)}{evento.organizadorId ? ` · Organizador: ${evento.organizadorId}` : ''}</span>
            </li>)}
          </ol>}
        </article>}
      </div>}
    </>}
  </section>
}
