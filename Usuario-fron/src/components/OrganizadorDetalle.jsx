import { useEffect, useState } from 'react'
import { actividadSeleccionada, obtenerDetalleActividad, rutaOrganizador } from '../services/organizadorService'
import { nombreDia, rangoHoras } from '../services/presentacion'
import './Organizador.css'

/** US: detalle de una actividad; solo muestra datos obtenidos del servicio autorizado. */
export function OrganizadorDetalle({ token }) {
  const actividadId = actividadSeleccionada(window.location.hash)
  const [revision, setRevision] = useState(0)
  const [estado, setEstado] = useState({ cargando: true, error: '', actividad: null })

  useEffect(() => {
    const ctrl = new AbortController()
    obtenerDetalleActividad(token, actividadId, ctrl.signal)
      .then(actividad => { if (!ctrl.signal.aborted) setEstado({ cargando: false, error: '', actividad }) })
      .catch(error => { if (!ctrl.signal.aborted) setEstado({ cargando: false, error: error.message, actividad: null }) })
    return () => ctrl.abort()
  }, [token, actividadId, revision])

  const { cargando, error, actividad: a } = estado
  return (
    <div className="org-pagina org-detalle">
      <a className="org-regresar" href="#/organizador-actividades">← Mis actividades</a>
      {cargando && <div className="org-cargando" role="status">Cargando los detalles de tu actividad…</div>}
      {!cargando && error && <div className="org-alerta" role="alert"><strong>No se pudo cargar la actividad.</strong><p>{error}</p><button type="button" onClick={() => setRevision(n => n + 1)}>Reintentar</button></div>}
      {!cargando && a && <>
        <section className="org-detalle__hero">
          <div>
            <span className="org-eyebrow">CENTRO DE ACTIVIDAD · ORGANIZADOR</span>
            <div className="org-detalle__estado"><span className="org-estado"><span className="org-punto" />{a.estado || 'Sin estado'}</span></div>
            <h2>{a.nombre || 'Actividad sin título'}</h2>
            <p>Consulta la información asignada a esta actividad y accede a las herramientas de gestión.</p>
          </div>
          <button type="button" className="org-boton org-boton--suave" onClick={() => setRevision(n => n + 1)}>Actualizar información</button>
        </section>
        <div className="org-detalle__layout">
          <section className="org-detalle__panel">
            <div className="org-detalle__titulo"><span className="org-eyebrow">INFORMACIÓN GENERAL</span><h3>Acerca de la actividad</h3></div>
            <p className="org-detalle__descripcion">{a.descripcion || 'Todavía no hay una descripción disponible para esta actividad.'}</p>
            <div className="org-detalle__datos">
              <div><span>Fecha programada</span><strong>{nombreDia(a.fecha)}</strong></div>
              <div><span>Horario</span><strong>{rangoHoras(a.horaInicio, a.horaFin)}</strong></div>
              <div><span>Ubicación</span><strong>{a.ubicacion || 'Ubicación por confirmar'}</strong></div>
              <div><span>Capacidad</span><strong>{a.maximoParticipantes != null ? `${a.maximoParticipantes} participantes` : 'Por confirmar'}</strong></div>
            </div>
            {a.ubicacionDetalle?.confirmada && (a.ubicacionDetalle?.sede || a.ubicacionDetalle?.pabellon || a.ubicacionDetalle?.aula || a.ubicacionDetalle?.direccion) && <div className="org-detalle__ubicacion"><strong>Detalles del lugar</strong><p>{[a.ubicacionDetalle.sede, a.ubicacionDetalle.pabellon, a.ubicacionDetalle.aula, a.ubicacionDetalle.direccion].filter(Boolean).join(' · ')}</p></div>}
          </section>
          <aside className="org-detalle__lateral">
            <div className="org-detalle__panel">
              <span className="org-eyebrow">PARTICIPACIÓN</span>
              <strong className="org-detalle__cifra">{a.cuposOcupados ?? a.inscritosRegistrados ?? '—'}</strong>
              <p>Estudiantes inscritos actualmente</p>
              <div className="org-detalle__cupos"><span>Cupos disponibles</span><strong>{a.cuposDisponibles == null ? 'Por confirmar' : a.cuposDisponibles}</strong></div>
              <a className="org-boton org-boton--primario" href={rutaOrganizador('organizador-inscritos', a.id)}>Consultar participantes →</a>
            </div>
            <div className="org-detalle__panel">
              <span className="org-eyebrow">GESTIÓN</span>
              <h3>Acciones disponibles</h3>
              <a className="org-detalle__enlace" href={rutaOrganizador('organizador-asistencia', a.id)}>Registrar asistencia <span>→</span></a>
              <a className="org-detalle__enlace" href={rutaOrganizador('organizador-reporte', a.id)}>Reportar un problema <span>→</span></a>
            </div>
          </aside>
        </div>
      </>}
    </div>
  )
}
