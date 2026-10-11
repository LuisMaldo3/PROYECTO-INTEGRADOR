import { useEffect, useState } from 'react'
import Icono from './Iconos'
import { listarActividades } from '../services/actividadesService'
import { formatearFechaActividad, formatearHorario, resumen } from '../services/formatoActividad'

/** Lista de actividades del administrador, por fecha y hora. `version` sube para pedirla de nuevo. */
export default function ListaActividades({ token, version = 0, onNueva, nuevaDeshabilitada = false, onSesionExpirada }) {
  const [estado, setEstado] = useState({ cargando: true, error: '', actividades: [] })
  const [recarga, setRecarga] = useState(0)

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
                <th>Fecha</th>
                <th>Horario</th>
              </tr>
            </thead>
            <tbody>
              {actividades.map((a) => (
                <tr key={a.id}>
                  <td>
                    <div className="celda-principal" title={a.descripcion}>
                      <strong>{a.nombre}</strong>
                      <span>{resumen(a.descripcion, 70)}</span>
                    </div>
                  </td>
                  <td>
                    <span className={`etiqueta etiqueta--${a.categoria?.id ?? 'sin'}`}>{a.categoria?.nombre ?? '—'}</span>
                  </td>
                  <td className={a.fecha ? undefined : 'sin-dato'}>{formatearFechaActividad(a.fecha)}</td>
                  <td>
                    {a.horaInicio ? (
                      <span className="hora">
                        <Icono nombre="reloj" tam={16} /> {formatearHorario(a.horaInicio, a.horaFin)}
                      </span>
                    ) : (
                      <span className="sin-dato">—</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
