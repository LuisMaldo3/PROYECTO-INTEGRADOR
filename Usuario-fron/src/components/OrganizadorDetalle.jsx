import { useEffect, useState } from 'react'
import { actividadSeleccionada, obtenerDetalleActividad, actualizarDatosPermitidos, rutaOrganizador } from '../services/organizadorService'
import { nombreDia, rangoHoras } from '../services/presentacion'
import { OrganizadorComentarios } from './OrganizadorComentarios'

/** US: detalle de una actividad; solo muestra datos obtenidos del servicio autorizado. */
export function OrganizadorDetalle({ token }) {
  const actividadId = actividadSeleccionada(window.location.hash)
  const [revision, setRevision] = useState(0)
  const [modoEdicion, setModoEdicion] = useState(false)
  const [formulario, setFormulario] = useState({ descripcion: '', reglasCompetencia: '' })
  const [guardando, setGuardando] = useState(false)
  const [avisoEdicion, setAvisoEdicion] = useState({ texto: '', error: false })
  const [estado, setEstado] = useState({ cargando: true, error: '', actividad: null })

  useEffect(() => {
    const ctrl = new AbortController()
    obtenerDetalleActividad(token, actividadId, ctrl.signal)
      .then(actividad => {
        if (ctrl.signal.aborted) return
        setEstado({ cargando: false, error: '', actividad })
        setFormulario({ descripcion: actividad.descripcion || '', reglasCompetencia: actividad.reglasCompetencia || '' })
      })
      .catch(error => { if (!ctrl.signal.aborted) setEstado({ cargando: false, error: error.message, actividad: null }) })
    return () => ctrl.abort()
  }, [token, actividadId, revision])

  async function guardarCambios(e) {
    e.preventDefault()
    if (guardando || !estado.actividad?.puedeEditarOrganizador) return
    if (!window.confirm('¿Guardar los campos permitidos? No se alterarán los cupos ni el horario.')) return
    setGuardando(true)
    setAvisoEdicion({ texto: '', error: false })
    try {
      const a = estado.actividad
      const cambios = { descripcion: formulario.descripcion }
      if (a.tipoActividad === 'competencia') cambios.reglasCompetencia = formulario.reglasCompetencia
      await actualizarDatosPermitidos(token, actividadId, cambios)
      setModoEdicion(false)
      setAvisoEdicion({ texto: 'Cambios guardados correctamente.', error: false })
      setRevision(n => n + 1)
    } catch (err) {
      setAvisoEdicion({ texto: err.message, error: true })
    } finally { setGuardando(false) }
  }

  const { cargando, error, actividad: a } = estado
  return (
    <div className="org-pagina org-detalle">
      <a className="org-regresar" href="#/organizador-actividades">Mis actividades</a>
      {cargando && <div className="org-cargando" role="status">Cargando los detalles de tu actividad…</div>}
      {!cargando && error && <div className="org-alerta" role="alert"><strong>No se pudo cargar la actividad.</strong><p>{error}</p><button type="button" onClick={() => setRevision(n => n + 1)}>Reintentar</button></div>}
      {!cargando && a && <>
        <section className="org-detalle__hero">
          <div>
            
            <div className="org-detalle__estado"><span className="org-estado"><span className="org-punto" />{a.estado || 'Sin estado'}</span></div>
            <h2>{a.nombre || 'Actividad sin título'}</h2>
            <p>Consulta la información asignada a esta actividad y accede a las herramientas de gestión.</p>
          </div>
          <button type="button" className="org-boton org-boton--suave" onClick={() => setRevision(n => n + 1)}>Actualizar información</button>
        </section>
        <div className="org-detalle__layout">
          <section className="org-detalle__panel">
            <div className="org-detalle__titulo"><h3>Acerca de la actividad</h3></div>
            <p className="org-detalle__descripcion">{a.descripcion || 'Todavía no hay una descripción disponible para esta actividad.'}</p>
            {a.puedeEditarOrganizador && <div className="org-edicion-control">
              {!modoEdicion && <button type="button" className="org-boton org-boton--suave" onClick={() => { setModoEdicion(true); setAvisoEdicion({ texto: '', error: false }) }}>Editar información permitida</button>}
              {modoEdicion && <form onSubmit={guardarCambios} className="org-edicion-formulario">
                <h4>Actualizar mi actividad</h4>
                <p>Solo puedes modificar la descripción y, si es una competencia, el reglamento. El administrador conserva el control del horario, aforo, modalidad y puntajes.</p>
                <label>Descripción<textarea required minLength={15} maxLength={2500} rows={5}
                  value={formulario.descripcion} disabled={guardando}
                  onChange={e => setFormulario(v => ({ ...v, descripcion: e.target.value }))}/></label>
                {a.tipoActividad === 'competencia' && <label>Reglamento<textarea maxLength={3000} rows={4}
                  value={formulario.reglasCompetencia} disabled={guardando}
                  onChange={e => setFormulario(v => ({ ...v, reglasCompetencia: e.target.value }))}/></label>}
                <div className="org-resultados-up__acciones"><button className="org-boton org-boton--primario" type="submit" disabled={guardando}>{guardando ? 'Guardando…' : 'Guardar cambios'}</button>
                  <button type="button" className="org-boton org-boton--suave" disabled={guardando} onClick={() => setModoEdicion(false)}>Cancelar</button></div>
              </form>}
            </div>}
            {avisoEdicion.texto && <p role={avisoEdicion.error ? 'alert' : 'status'} className={avisoEdicion.error ? 'org-alerta' : 'org-resultados-up__aviso'}>{avisoEdicion.texto}</p>}
            <div className="org-detalle__datos">
              <div><span>Fecha programada</span><strong>{nombreDia(a.fecha)}</strong></div>
              <div><span>Horario</span><strong>{rangoHoras(a.horaInicio, a.horaFin)}</strong></div>
              <div><span>Ubicación</span><strong>{a.ubicacion || 'Ubicación por confirmar'}</strong></div>
              <div><span>Capacidad</span><strong>{a.maximoParticipantes != null ? `${a.maximoParticipantes} ${a.unidadCupo || 'participantes'}` : 'Por confirmar'}</strong></div>
              <div><span>Responsable principal</span><strong>{a.organizadorPrincipal || 'Por confirmar'}</strong></div>
            </div>
            {a.tipoActividad === 'competencia' && <div className="org-detalle__ubicacion">
              <strong>Reglamento de competencia</strong>
              <p>Modalidad: {a.modalidad === 'equipos' ? 'Por equipos' : 'Individual'} · Cupos: {a.unidadCupo || 'estudiantes'}{a.modalidad === 'equipos' ? ` · Hasta ${a.maxIntegrantesEquipo} integrantes por equipo` : ''}</p>
              <p>{a.reglasCompetencia || 'Aún no se publicaron las reglas.'}</p>
              {Array.isArray(a.puntosPorPuesto) && a.puntosPorPuesto.length > 0 && <p><strong>Puntos por puesto:</strong> {a.puntosPorPuesto.map((v,i) => `${i+1}.º: ${v}`).join(' · ')}</p>}
            </div>}
            {a.tipoActividad === 'competencia' && <section className="org-areas-panel" aria-label="Encargados de las cinco áreas">
              <strong>Responsables por área</strong>
              <p>El organizador principal registra los resultados. Los coordinadores apoyan a sus respectivas áreas.</p>
              <div className="org-areas-lista">{(a.coordinadores || []).map(c => <div key={c.area}>
                <span>{c.area}</span><strong>{c.asignado ? c.nombre : 'Sin asignar'}</strong>
              </div>)}</div>
            </section>}
            {a.puedeEditarOrganizador && <OrganizadorComentarios token={token} actividadId={actividadId}/>}
            {a.ubicacionDetalle?.confirmada && (a.ubicacionDetalle?.sede || a.ubicacionDetalle?.pabellon || a.ubicacionDetalle?.aula || a.ubicacionDetalle?.direccion) && <div className="org-detalle__ubicacion"><strong>Detalles del lugar</strong><p>{[a.ubicacionDetalle.sede, a.ubicacionDetalle.pabellon, a.ubicacionDetalle.aula, a.ubicacionDetalle.direccion].filter(Boolean).join(' · ')}</p></div>}
          </section>
          <aside className="org-detalle__lateral">
            <div className="org-detalle__panel">
              
              <strong className="org-detalle__cifra">{a.cuposOcupados ?? '—'}</strong>
              <p>{a.unidadCupo === 'equipos' ? 'Equipos inscritos actualmente' : 'Estudiantes inscritos actualmente'}</p>
              {a.unidadCupo === 'equipos' && <p className="org-cupos-resumen">{a.estudiantesInscritos ?? 0} estudiantes confirmados · {a.equiposConfirmados ?? 0} equipos habilitados · {a.equiposPendientes ?? 0} pendientes</p>}
              <div className="org-detalle__cupos"><span>Total de estudiantes participantes</span><strong>{a.participantesTotales ?? a.estudiantesInscritos ?? 0}</strong></div>
              <div className="org-detalle__cupos"><span>Cupos disponibles</span><strong>{a.cuposDisponibles == null ? 'Por confirmar' : a.cuposDisponibles}</strong></div>
              {a.puedeEditarOrganizador && <a className="org-boton org-boton--suave" href={rutaOrganizador('organizador-cupos', a.id)}>Ver cupos, participación e IA</a>}
              {a.puedeEditarOrganizador && <a className="org-boton org-boton--primario" href={rutaOrganizador('organizador-inscritos', a.id)}>Consultar participantes</a>}
            </div>
            <div className="org-detalle__panel">
              
              <h3>Acciones disponibles</h3>
              {a.tipoActividad === 'competencia' && <a className="org-detalle__enlace org-detalle__enlace--resultados" href={rutaOrganizador('organizador-resultados', a.id)}>{a.puedeEditarOrganizador ? 'Registrar resultados y posiciones' : 'Revisar resultados de mi área'} <span></span></a>}
              {a.puedeEditarOrganizador && <a className="org-detalle__enlace" href={rutaOrganizador('organizador-reporte', a.id)}>Reportar un problema <span></span></a>}
            </div>
          </aside>
        </div>
      </>}
    </div>
  )
}
