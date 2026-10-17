import { useEffect, useMemo, useState } from 'react'
import { obtenerAgenda } from '../services/agendaService'
import { obtenerAgendaSugerida } from '../services/agendaSugeridaService'
import { leerSesion } from '../services/authService'
import { actividadesEnCruce } from '../services/cruces'
import { obtenerCruces } from '../services/crucesService'
import { agruparPorDia, bloqueDia, cantidad, estiloCategoria, hoyISO, nombreDia } from '../services/presentacion'
import { AvisoCruces } from './AvisoCruces'
import EstadoPantalla from './EstadoPantalla'
import Icono from './Iconos'
import HorarioLibre from './HorarioLibre'
import './Actividades.css'
import './Agenda.css'

function Item({ actividad: a, conflicto, sugerida = false }) {
  const categoria = estiloCategoria({ nombre: a.categoria || a.categoriaId })
  const cancelada = a.estado === 'cancelada'
  return (
    <li className={`agenda-item${conflicto ? ' agenda-item--cruce' : ''}${cancelada ? ' agenda-item--cancelada' : ''}`}>
      <div className="agenda-item__hora">
        <strong>{a.horaInicio || '--:--'}</strong>
        {a.horaFin && <small>hasta {a.horaFin}</small>}
      </div>
      <div className="agenda-item__cuerpo">
        <div className="agenda-item__etiquetas">
          <span className={`categoria categoria--${categoria.clase}`}>
            <Icono nombre={categoria.icono} tamano={14} />
            {categoria.nombre}
          </span>
          <span className={`estado-inscripcion${cancelada ? ' estado-inscripcion--cancelada' : ''}`}>{sugerida ? 'Sugerida' : cancelada ? 'Cancelada' : 'Inscrito'}</span>
          {conflicto && (
            <span className="etiqueta-cruce">
              <Icono nombre="alerta" tamano={14} /> Cruce de horario
            </span>
          )}
        </div>
        <h3>{a.nombreActividad || a.nombre || 'Actividad sin título'}</h3>
        {sugerida && a.motivo && <p className="agenda-sugerida__motivo">{a.motivo}</p>}
        {a.descripcion && <p className="agenda-item__descripcion">{a.descripcion}</p>}
        <p className="agenda-item__lugar">
          <Icono nombre="ubicacion" tamano={16} />
          {a.ubicacion || a.ubicacionId || 'Ubicación por confirmar'}
        </p>
      </div>
    </li>
  )
}

/** Agenda: las actividades en las que está inscrita la persona, por día, con los cruces de horario marcados. */
export function Agenda() {
  const [estado, setEstado] = useState({ cargando: true, error: '', actividades: [], cruces: [], errorCruces: '' })
  const [filtro, setFiltro] = useState('todas')
  const [vista, setVista] = useState('inscritas')
  const [sugerida, setSugerida] = useState({ cargando: false, cargada: false, error: '', actividades: [], origen: '', aviso: '', inscripciones: [] })
  const [refrescarSugerida, setRefrescarSugerida] = useState(0)
  const [recargar, setRecargar] = useState(0)

  useEffect(() => {
    const ctrl = new AbortController()
    const token = leerSesion()?.token
    if (!token) {
      queueMicrotask(() => setEstado((e) => ({ ...e, cargando: false, error: 'Inicia sesión para consultar tu agenda.' })))
      return () => ctrl.abort()
    }
    Promise.allSettled([obtenerAgenda(token, ctrl.signal), obtenerCruces(token, ctrl.signal)]).then(([agenda, conflictos]) => {
      if (ctrl.signal.aborted) return
      if (agenda.status === 'rejected') {
        setEstado({ cargando: false, error: agenda.reason.message, actividades: [], cruces: [], errorCruces: '' })
        return
      }
      const cruces = conflictos.status === 'fulfilled' ? conflictos.value : []
      setEstado({
        cargando: false,
        error: '',
        actividades: agenda.value,
        cruces,
        errorCruces: conflictos.status === 'fulfilled' ? '' : 'No se pudo comprobar los cruces de horario.',
      })
    })
    return () => ctrl.abort()
  }, [recargar])

  useEffect(() => {
    if (vista !== 'sugerida') return undefined
    const ctrl = new AbortController()
    const token = leerSesion()?.token
    if (!token) {
      queueMicrotask(() => setSugerida((s) => ({ ...s, cargando: false, cargada: true, error: 'Inicia sesión para generar tu agenda.' })))
      return () => ctrl.abort()
    }
    // Se calcula cada vez que se abre la vista o se pulsa Actualizar.
    queueMicrotask(() => { if (!ctrl.signal.aborted) setSugerida((s) => ({ ...s, cargando: true, error: '' })) })
    obtenerAgendaSugerida(token, ctrl.signal)
      .then((datos) => { if (!ctrl.signal.aborted) setSugerida({ ...datos, cargada: true, cargando: false, error: '' }) })
      .catch((e) => { if (!ctrl.signal.aborted) setSugerida((s) => ({ ...s, cargada: true, cargando: false, error: e.message })) })
    return () => ctrl.abort()
  }, [vista, refrescarSugerida])

  const reintentar = () => {
    setEstado((e) => ({ ...e, cargando: true, error: '' }))
    setRecargar((n) => n + 1)
  }
  const { cargando, error, actividades, cruces, errorCruces } = estado

  const idsEnCruce = useMemo(() => actividadesEnCruce(cruces), [cruces])
  const hoy = hoyISO()
  const visibles = useMemo(() => actividades.filter((a) => a.fecha && (filtro !== 'proximas' || a.fecha >= hoy)), [actividades, filtro, hoy])
  const grupos = useMemo(() => agruparPorDia(visibles), [visibles])

  if (vista === 'libres') {
    return <div className="agenda">
      <div className="agenda-vistas" role="group" aria-label="Tipo de agenda">
        <button type="button" aria-pressed="false" onClick={() => setVista('inscritas')}>Mi agenda</button>
        <button type="button" aria-pressed="false" onClick={() => setVista('sugerida')}>✦ Agenda sugerida</button>
        <button type="button" aria-pressed="true">Mis horas libres</button>
      </div>
      <HorarioLibre />
    </div>
  }

  if (vista === 'sugerida') {
    const lista = sugerida.actividades || []
    const gruposSugeridos = agruparPorDia(lista.filter((a) => a.fecha))
    return (
      <div className="agenda">
        <div className="agenda-vistas" role="group" aria-label="Tipo de agenda">
          <button type="button" aria-pressed="false" onClick={() => setVista('inscritas')}>Mi agenda</button>
          <button type="button" aria-pressed="true">✦ Agenda sugerida</button>
          <button type="button" aria-pressed="false" onClick={() => setVista('libres')}>Mis horas libres</button>
        </div>
        <div className="agenda-sugerida__hero">
          <div><span className="agenda-sugerida__eyebrow">PLAN PERSONALIZADO</span>
            <h2>Una agenda creada para ti</h2>
            <p>Actividades publicadas que puedes combinar con tus inscripciones, sin cruces de horarios.</p>
          </div>
          <button type="button" className="boton-secundario" disabled={sugerida.cargando} onClick={() => setRefrescarSugerida((n) => n + 1)}>
            <Icono nombre="actualizar" tamano={16} /> Actualizar propuesta
          </button>
        </div>
        {sugerida.cargando && <div role="status" className="agenda-esqueleto">Preparando tu propuesta…</div>}
        {!sugerida.cargando && sugerida.error && <EstadoPantalla icono="alerta" tono="error" titulo="No pudimos preparar tu agenda" texto={sugerida.error} accion={<button className="boton-secundario" onClick={() => setRefrescarSugerida((n) => n + 1)}>Reintentar</button>} />}
        {!sugerida.cargando && !sugerida.error && (
          <>
            <div className="agenda-sugerida__resumen">
              <span className="agenda-sugerida__dato"><strong>{lista.length}</strong> actividades sugeridas</span>
              <span className="agenda-sugerida__dato"><strong>{sugerida.inscripciones.length}</strong> inscripciones consideradas</span>
              <span className={`agenda-sugerida__origen ${sugerida.origen === 'ia' ? 'agenda-sugerida__origen--ia' : ''}`}>{sugerida.origen === 'ia' ? '✦ Priorizada con Gemini' : sugerida.origen === 'sin_actividades' ? 'Sin actividades compatibles' : 'Propuesta por reglas (sin IA)'}</span>
            </div>
            {sugerida.aviso && <p className="agenda-sugerida__aviso">{sugerida.aviso}</p>}
            {lista.length === 0 && <EstadoPantalla icono="calendario" titulo="Sin sugerencias compatibles" texto="No encontramos nuevas actividades sin cruces. Prueba de nuevo cuando el catálogo tenga más opciones." />}
            {gruposSugeridos.map((g) => (
              <section className="dia dia--agenda" key={g.fecha}>
                <div className="dia__bloque" aria-hidden="true"><strong>{bloqueDia(g.fecha)?.numero}</strong><span>{bloqueDia(g.fecha)?.mes}</span></div>
                <div className="dia__contenido"><header className="dia__cabecera"><h2>{nombreDia(g.fecha)}</h2><span>{cantidad(g.items.length, 'sugerencia', 'sugerencias')}</span></header>
                  <ul className="agenda__lista">{g.items.map((a) => <Item key={a.id} actividad={a} sugerida />)}</ul>
                </div>
              </section>
            ))}
          </>
        )}
      </div>
    )
  }

  if (error) {
    return (
      <div className="agenda">
        <div className="agenda-vistas" role="group" aria-label="Tipo de agenda">
          <button type="button" aria-pressed="true">Mi agenda</button>
          <button type="button" aria-pressed="false" onClick={() => setVista('sugerida')}>✦ Agenda sugerida</button>
          <button type="button" aria-pressed="false" onClick={() => setVista('libres')}>Mis horas libres</button>
        </div>
      <EstadoPantalla
        icono="alerta"
        tono="error"
        titulo="No pudimos cargar tu agenda"
        texto={error}
        accion={
          <button type="button" className="boton-secundario" onClick={reintentar}>
            <Icono nombre="actualizar" tamano={16} /> Reintentar
          </button>
        }
      />
      </div>
    )
  }

  return (
    <div className="agenda">
      <div className="agenda-vistas" role="group" aria-label="Tipo de agenda">
        <button type="button" aria-pressed="true">Mi agenda</button>
        <button type="button" aria-pressed="false" onClick={() => setVista('sugerida')}>✦ Agenda sugerida</button>
        <button type="button" aria-pressed="false" onClick={() => setVista('libres')}>Mis horas libres</button>
      </div>
      <div className="lista-barra">
        <p>{cargando ? 'Cargando tu agenda…' : cantidad(actividades.length, 'actividad inscrita', 'actividades inscritas')}</p>
        <div className="segmentos" role="group" aria-label="Filtrar la agenda">
          <button type="button" aria-pressed={filtro === 'todas'} onClick={() => setFiltro('todas')}>
            Todas
          </button>
          <button type="button" aria-pressed={filtro === 'proximas'} onClick={() => setFiltro('proximas')}>
            Próximas
          </button>
        </div>
      </div>

      <AvisoCruces cruces={cruces} error={errorCruces} />

      {cargando && actividades.length === 0 && <div className="agenda-esqueleto" aria-busy="true" />}

      {!cargando && visibles.length === 0 && (
        <EstadoPantalla
          icono="calendario"
          titulo="Tu agenda está lista para empezar"
          texto={filtro === 'proximas' ? 'No tienes actividades próximas.' : 'Cuando te inscribas en una actividad, aparecerá aquí.'}
        />
      )}

      {grupos.map((g) => {
        const bloque = bloqueDia(g.fecha)
        return (
          <section className="dia dia--agenda" key={g.fecha ?? 'sin-fecha'}>
            {bloque && (
              <div className="dia__bloque" aria-hidden="true">
                <strong>{bloque.numero}</strong>
                <span>
                  {bloque.diaSemana} · {bloque.mes}
                </span>
              </div>
            )}
            <div className="dia__contenido">
              <header className="dia__cabecera">
                <h2>{nombreDia(g.fecha)}</h2>
                <span>{cantidad(g.items.length, 'actividad', 'actividades')}</span>
              </header>
              <ul className="agenda__lista">
                {g.items.map((a) => (
                  <Item key={a.id || a.actividadId} actividad={a} conflicto={idsEnCruce.has(a.actividadId)} />
                ))}
              </ul>
            </div>
          </section>
        )
      })}
    </div>
  )
}
