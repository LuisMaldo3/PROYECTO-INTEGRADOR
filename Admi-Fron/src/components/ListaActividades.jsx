import { useState } from 'react'
import Icono from './Iconos'
import FilaActividad from './FilaActividad'
import ResumenActividades from './ResumenActividades'
import BuscadorActividades from './BuscadorActividades'
import ResultadosCompetencia from './ResultadosCompetencia'

/**
 * Listado de actividades, por fecha y hora. Ya no guarda el arreglo: lo recibe
 * del padre (PantallaActividades), ya filtrado por el buscador. Cada fila
 * avisa con `onEditar` / `onCancelar`; este componente no decide nada sobre el
 * estado de la entidad, solo arma la tabla con map() usando `id` como key.
 * Conserva su propio estado para dos cosas que no son la lista de actividades:
 * la ventana de confirmación antes de cancelar, y la ventana de resultados.
 */
export default function ListaActividades({
  token, actividades, todasLasActividades, terminoBusqueda, cargando, error,
  consulta, filtro, onConsulta, onFiltro, onActualizar, onNueva, onEditar, onCancelar,
  nuevaDeshabilitada = false, onSesionExpirada,
}) {
  const [pendiente, setPendiente] = useState(null)
  const [competenciaElegida, setCompetenciaElegida] = useState(null)
  const [procesando, setProcesando] = useState(false)
  const [errorCancelar, setErrorCancelar] = useState('')
  const [exitoCancelar, setExitoCancelar] = useState('')

  async function confirmarCancelacion() {
    if (!pendiente || procesando) return
    setProcesando(true)
    setErrorCancelar('')
    try {
      const respuesta = await onCancelar(pendiente)
      setExitoCancelar(respuesta.avisosError || `Actividad «${pendiente.nombre}» cancelada. ${respuesta.avisosGenerados ?? 0} notificación(es) registradas para inscritos.`)
      setPendiente(null)
    } catch (e) {
      if (e.estado === 401) onSesionExpirada(e.detalle)
      else setErrorCancelar(e.message)
    } finally {
      setProcesando(false)
    }
  }

  return (
    <section className="lista">
      {exitoCancelar && <p className="aviso aviso--ok" role="status">{exitoCancelar}</p>}
      <header className="pagina__cabecera">
        <div>
          <h1>Actividades</h1>
          <p>Crea, edita y cancela actividades. Revisa los resultados de las competencias.</p>
        </div>
        <div className="pagina__acciones">
          <button type="button" className="boton boton--claro" onClick={onActualizar}>
            <Icono nombre="actualizar" tam={18} /> Actualizar
          </button>
          <button type="button" className="boton" onClick={onNueva} disabled={nuevaDeshabilitada}>
            <Icono nombre="mas" tam={18} /> Nueva actividad
          </button>
        </div>
      </header>

      <ResumenActividades actividades={todasLasActividades} cargando={cargando} />
      <BuscadorActividades consulta={consulta} onConsulta={onConsulta} filtro={filtro} onFiltro={onFiltro} />

      {error && (
        <p className="aviso aviso--error" role="alert">
          <Icono nombre="alerta" tam={18} /> {error}
        </p>
      )}

      {!cargando && !error && todasLasActividades.length === 0 && (
        <div className="vacio">
          <Icono nombre="calendario" tam={40} />
          <p>Todavía no hay actividades. Usa «Nueva actividad» para crear la primera.</p>
        </div>
      )}

      {cargando && <div className="cargando" role="status">Cargando actividades…</div>}
      {!cargando && !error && todasLasActividades.length > 0 && actividades.length === 0 && (
        <p className="vacio">{terminoBusqueda ? `No hay actividades que coincidan con «${terminoBusqueda}».` : 'No hay actividades con esos filtros.'}</p>
      )}
      {actividades.length > 0 && (
        <div className="tabla-envoltura">
          <table className="tabla tabla--actividades">
            <thead>
              <tr>
                <th>Actividad</th>
                <th>Tipo y área</th>
                <th>Lugar</th>
                <th>Fecha y hora</th>
                <th className="tabla__acciones-titulo">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {actividades.map((a) => (
                <FilaActividad key={a.id} actividad={a} onEditar={onEditar} onCancelar={setPendiente} onVerResultados={setCompetenciaElegida} />
              ))}
            </tbody>
          </table>
        </div>
      )}
      {competenciaElegida && <ResultadosCompetencia token={token} actividad={competenciaElegida} onCerrar={() => setCompetenciaElegida(null)} />}
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
