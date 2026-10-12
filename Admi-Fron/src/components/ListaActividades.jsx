import { useEffect, useState } from 'react'
import Icono from './Iconos'
import { listarActividades, cancelarActividad } from '../services/actividadesService'
import { formatearFechaActividad, formatearHorario, resumen } from '../services/formatoActividad'

/** Lista de actividades del administrador, por fecha y hora. `version` sube para pedirla de nuevo. */
export default function ListaActividades({ token, version = 0, onNueva, onEditar, nuevaDeshabilitada = false, onSesionExpirada }) {
  const [estado, setEstado] = useState({ cargando: true, error: '', actividades: [] })
  const [recarga, setRecarga] = useState(0)
  const [pendiente, setPendiente] = useState(null)
  const [procesando, setProcesando] = useState(false)
  const [errorCancelar, setErrorCancelar] = useState('')
  const [exitoCancelar, setExitoCancelar] = useState('')

  async function confirmarCancelacion() {
    if (!pendiente || procesando) return
    setProcesando(true)
    setErrorCancelar('')
    try {
      await cancelarActividad(token, pendiente.id)
      setPendiente(null)
      setExitoCancelar(`La actividad «${pendiente.nombre}» fue cancelada.`)
      setRecarga((n) => n + 1)
    } catch (e) {
      if (e.estado === 401) onSesionExpirada(e.detalle)
      else setErrorCancelar(e.message)
    } finally {
      setProcesando(false)
    }
  }

  useEffect(() => {
    let cancelado = false
    listarActividades(token)
      .then((actividades) => {
        if (!cancelado) setEstado({ cargando: false, error: '', actividades })
      })
      .catch((e) => {
        if (cancelado) return
        if (e.estado === 401) return onSesionExpirada(e.detalle)
        setEstado({ cargando: false, error: e.message, actividades: [] })
      })
    return () => {
      cancelado = true
    }
  }, [token, version, recarga, onSesionExpirada])

  const { cargando, error, actividades } = estado

  return (
    <section className="lista">
      {exitoCancelar && <p className="aviso aviso--ok" role="status">{exitoCancelar}</p>}
      <header className="lista__cabecera">
        <div>
          <h1>Actividades</h1>
          <p className="suave">{cargando ? 'Cargando…' : `${actividades.length} ${actividades.length === 1 ? 'actividad' : 'actividades'}`}</p>
        </div>
        <div className="lista__acciones">
          <button
            type="button"
            className="boton boton--claro"
            onClick={() => {
              setEstado((e) => ({ ...e, cargando: true }))
              setRecarga((n) => n + 1)
            }}
          >
            <Icono nombre="actualizar" tam={18} /> Actualizar
          </button>
          <button type="button" className="boton" onClick={onNueva} disabled={nuevaDeshabilitada}>
            <Icono nombre="mas" tam={18} /> Nueva actividad
          </button>
        </div>
      </header>

      {error && (
        <p className="aviso aviso--error" role="alert">
          <Icono nombre="alerta" tam={18} /> {error}
        </p>
      )}

      {!cargando && !error && actividades.length === 0 && (
        <div className="vacio">
          <Icono nombre="calendario" tam={40} />
          <p>Todavía no hay actividades. Crea la primera.</p>
        </div>
      )}

      {actividades.length > 0 && (
        <div className="tabla-envoltura">
          <table className="tabla">
            <thead>
              <tr>
                <th>Actividad</th>
                <th>Categoría</th>
                <th>Lugar</th>
                <th>Fecha</th>
                <th>Horario</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {actividades.map((a) => (
                <tr key={a.id}>
                  <td>
                    <div className="celda-principal" title={a.descripcion}>
                      <strong>{a.nombre}</strong>
                      {a.estado === 'cancelada' && <span className="estado-cancelada">Cancelada</span>}
                      <span>{resumen(a.descripcion, 70)}</span>
                    </div>
                  </td>
                  <td>
                    <span className={`etiqueta etiqueta--${a.categoria?.id ?? 'sin'}`}>{a.categoria?.nombre ?? '—'}</span>
                  </td>
                  <td>
                    {a.ubicacion ? (
                      <span className="hora">
                        <Icono nombre="ubicacion" tam={16} /> {a.ubicacion.nombre}
                      </span>
                    ) : (
                      <span className="sin-dato">Sin ubicación</span>
                    )}
                  </td>
                  <td className={a.fecha ? undefined : 'sin-dato'}>
                    <span className="hora">{formatearFechaActividad(a.fecha)}</span>
                  </td>
                  <td>
                    {a.horaInicio ? (
                      <span className="hora">
                        <Icono nombre="reloj" tam={16} /> {formatearHorario(a.horaInicio, a.horaFin)}
                      </span>
                    ) : (
                      <span className="sin-dato">—</span>
                    )}
                  </td>
                  <td>
                    <div className="acciones-actividad">
                      <button type="button" className="boton boton--claro" disabled={a.estado === 'cancelada'} onClick={() => onEditar(a)}>Editar</button>
                      {a.estado !== 'cancelada' && <button type="button" className="boton boton--peligro-discreto" onClick={() => { setErrorCancelar(''); setPendiente(a) }}>Cancelar</button>}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {pendiente && (
        <div className="confirmacion-fondo" role="presentation" onMouseDown={(e) => { if (e.target === e.currentTarget && !procesando) setPendiente(null) }}>
          <div className="confirmacion-panel" role="alertdialog" aria-modal="true" aria-labelledby="titulo-cancelacion" aria-describedby="detalle-cancelacion">
            <div className="confirmacion-icono" aria-hidden="true">!</div>
            <h2 id="titulo-cancelacion">Cancelar actividad</h2>
            <p id="detalle-cancelacion">¿Confirmas la cancelación de <strong>{pendiente.nombre}</strong>? La actividad quedará marcada como cancelada y se conservará el historial de inscripciones.</p>
            {errorCancelar && <p className="aviso aviso--error" role="alert">{errorCancelar}</p>}
            <div className="confirmacion-acciones">
              <button type="button" className="boton boton--claro" disabled={procesando} onClick={() => setPendiente(null)}>Volver</button>
              <button type="button" className="boton boton--peligro" disabled={procesando} onClick={confirmarCancelacion}>{procesando ? 'Cancelando…' : 'Sí, cancelar actividad'}</button>
            </div>
          </div>
        </div>
      )}
    </section>
  )
}
