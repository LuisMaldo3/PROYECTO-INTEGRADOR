import { useEffect, useMemo, useState } from 'react'
import { obtenerAgenda } from '../services/agendaService'
import { leerSesion } from '../services/authService'
import { actividadesEnCruce } from '../services/cruces'
import { obtenerCruces } from '../services/crucesService'
import { agruparPorDia, bloqueDia, cantidad, estiloCategoria, hoyISO, nombreDia } from '../services/presentacion'
import { AvisoCruces } from './AvisoCruces'
import EstadoPantalla from './EstadoPantalla'
import Icono from './Iconos'
import './Actividades.css'
import './Agenda.css'

function Item({ actividad: a, conflicto }) {
  const categoria = estiloCategoria({ nombre: a.categoria })
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
          <span className={`estado-inscripcion${cancelada ? ' estado-inscripcion--cancelada' : ''}`}>{cancelada ? 'Cancelada' : 'Inscrito'}</span>
          {conflicto && (
            <span className="etiqueta-cruce">
              <Icono nombre="alerta" tamano={14} /> Cruce de horario
            </span>
          )}
        </div>
        <h3>{a.nombreActividad || 'Actividad sin título'}</h3>
        {a.descripcion && <p className="agenda-item__descripcion">{a.descripcion}</p>}
        <p className="agenda-item__lugar">
          <Icono nombre="ubicacion" tamano={16} />
          {a.ubicacion || 'Ubicación por confirmar'}
        </p>
      </div>
    </li>
  )
}

/** Agenda: las actividades en las que está inscrita la persona, por día, con los cruces de horario marcados. */
export function Agenda() {
  const [estado, setEstado] = useState({ cargando: true, error: '', actividades: [], cruces: [], errorCruces: '' })
  const [filtro, setFiltro] = useState('todas')
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

  const reintentar = () => {
    setEstado((e) => ({ ...e, cargando: true, error: '' }))
    setRecargar((n) => n + 1)
  }
  const { cargando, error, actividades, cruces, errorCruces } = estado

  const idsEnCruce = useMemo(() => actividadesEnCruce(cruces), [cruces])
  const hoy = hoyISO()
  const visibles = useMemo(() => actividades.filter((a) => a.fecha && (filtro !== 'proximas' || a.fecha >= hoy)), [actividades, filtro, hoy])
  const grupos = useMemo(() => agruparPorDia(visibles), [visibles])

  if (error) {
    return (
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
    )
  }

  return (
    <div className="agenda">
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
