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
      const respuesta = await cancelarActividad(token, pendiente.id)
      setPendiente(null)
      setExitoCancelar(respuesta.avisosError || `Actividad «${pendiente.nombre}» cancelada. ${respuesta.avisosGenerados ?? 0} notificación(es) registradas para inscritos.`)
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
  const [filtro, setFiltro] = useState('todas')
  const [consulta, setConsulta] = useState('')
  const filtradas = actividades.filter((a) =>
    (filtro === 'todas' || (filtro === 'canceladas' ? a.estado === 'cancelada' : a.estado !== 'cancelada')) &&
    `${a.nombre} ${a.descripcion} ${a.ubicacion?.nombre || ''}`.toLocaleLowerCase('es').includes(consulta.toLocaleLowerCase('es').trim()),
  )

  return (
    <section className="lista">
      {exitoCancelar && <p className="aviso aviso--ok" role="status">{exitoCancelar}</p>}
      <header className="lista__cabecera">
        <div>
          <p className="eyebrow">PROGRAMACIÓN Y EVENTOS</p>
          <h1>Actividades</h1>
          <p className="suave">Organiza eventos, horarios, lugares y participantes.</p>
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

      <div className="metricas-actividad" aria-label="Resumen de actividades">
        <div className="metrica"><span>Total de actividades</span><strong>{actividades.length}</strong><small>Eventos registrados</small></div>
        <div className="metrica"><span>Programadas</span><strong>{actividades.filter((a) => a.estado !== 'cancelada').length}</strong><small>Disponibles en el catálogo</small></div>
        <div className="metrica"><span>Canceladas</span><strong>{actividades.filter((a) => a.estado === 'cancelada').length}</strong><small>Con historial preservado</small></div>
      </div>
      <div className="barra-filtros">
        <label className="buscador barra-filtros__busca"><Icono nombre="buscar" tam={18} />
          <input type="search" placeholder="Buscar actividad o lugar..." value={consulta} onChange={(e) => setConsulta(e.target.value)} aria-label="Buscar actividades" /></label>
        <div className="pestanas" role="group" aria-label="Filtrar actividades">
          {[['todas', 'Todas'], ['activas', 'Activas'], ['canceladas', 'Canceladas']].map(([id, nombre]) =>
            <button type="button" key={id} className={filtro === id ? 'pestana pestana--activa' : 'pestana'} onClick={() => setFiltro(id)}>{nombre}</button>)}
        </div>
      </div>
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

      {!cargando && !error && actividades.length > 0 && filtradas.length === 0 && <p className="vacio">No hay actividades que coincidan con los filtros.</p>}
      {filtradas.length > 0 && (
        <div className="tabla-envoltura">
          <table className="tabla tabla--actividades">
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
              {filtradas.map((a) => (
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
            <p id="detalle-cancelacion">¿Confirmas la cancelación de <strong>{pendiente.nombre}</strong>? Se conservará el historial y se registrará una notificación para cada inscrito activo. Esta acción no se puede deshacer desde el panel.</p>
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
