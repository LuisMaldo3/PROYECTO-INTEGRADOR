import { useEffect, useState } from 'react'
import { listarActividades } from '../services/actividadesService'
import { formatearFecha, resumen } from '../services/formatoActividad'

/** Tarea 5 · Lista de actividades del administrador (la más nueva primero). `version` sube para pedirla de nuevo. */
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
          <p className="suave">{cargando ? 'Cargando…' : `${actividades.length} actividades`}</p>
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
            Actualizar
          </button>
          <button type="button" className="boton" onClick={onNueva} disabled={nuevaDeshabilitada}>
            Nueva actividad
          </button>
        </div>
      </header>

      {error && (
        <p className="aviso aviso--error" role="alert">
          {error}
        </p>
      )}

      {!cargando && !error && actividades.length === 0 && <p className="vacio">Todavía no hay actividades. Crea la primera.</p>}

      {actividades.length > 0 && (
        <div className="tabla-envoltura">
          <table className="tabla">
            <thead>
              <tr>
                <th>Nombre</th>
                <th>Categoría</th>
                <th>Descripción</th>
                <th>Creada</th>
              </tr>
            </thead>
            <tbody>
              {actividades.map((a) => (
                <tr key={a.id}>
                  <td>{a.nombre}</td>
                  <td>
                    <span className="etiqueta">{a.categoria?.nombre ?? '—'}</span>
                  </td>
                  <td title={a.descripcion}>{resumen(a.descripcion)}</td>
                  <td>{formatearFecha(a.creadaEn)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
