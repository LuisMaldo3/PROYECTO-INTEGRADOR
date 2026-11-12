import { useEffect, useState } from 'react'
import { actividadSeleccionada, obtenerOcupacionActividad, verificarEquiposActividad, rutaOrganizador } from '../services/organizadorService'
import './OrganizadorOcupacion.css'
import { OrganizadorAnalitica } from './OrganizadorAnalitica'
import { OrganizadorSugerencias } from './OrganizadorSugerencias'

/** Sprint 3 · US-58/59. Panel de consulta: nunca puede modificar el aforo del administrador. */
export function OrganizadorOcupacion({ token }) {
  const actividadId = actividadSeleccionada(window.location.hash)
  const [revision, setRevision] = useState(0)
  const [datos, setDatos] = useState({ cargando: true, error: '', ocupacion: null, verificacion: null, errorEquipos: '' })

  useEffect(() => {
    const controlador = new AbortController()
    setDatos({ cargando: true, error: '', ocupacion: null, verificacion: null, errorEquipos: '' })
    async function cargar() {
      try {
        const ocupacion = await obtenerOcupacionActividad(token, actividadId, controlador.signal)
        if (controlador.signal.aborted) return
        const esEquipos = ocupacion.unidadCupo === 'equipos'
        if (!esEquipos) {
          setDatos({ cargando: false, error: '', ocupacion, verificacion: null, errorEquipos: '' })
          return
        }
        try {
          const verificacion = await verificarEquiposActividad(token, actividadId, controlador.signal)
          if (!controlador.signal.aborted) setDatos({ cargando: false, error: '', ocupacion, verificacion, errorEquipos: '' })
        } catch (error) {
          if (!controlador.signal.aborted) setDatos({ cargando: false, error: '', ocupacion, verificacion: null, errorEquipos: error.message })
        }
      } catch (error) {
        if (!controlador.signal.aborted) setDatos({ cargando: false, error: error.message, ocupacion: null, verificacion: null, errorEquipos: '' })
      }
    }
    cargar()
    return () => controlador.abort()
  }, [token, actividadId, revision])

  const { ocupacion: o, verificacion: v } = datos
  const limite = o?.maximoParticipantes == null ? null : Number(o.maximoParticipantes)
  const tieneLimite = Number.isFinite(limite) && limite > 0
  const ocupados = Number(o?.cuposOcupados || 0)
  const porcentaje = tieneLimite ? Math.round(ocupados * 100 / limite) : null
  return <main className="org-pagina org-ocupacion">
    <div className="org-ocupacion__navegacion"><a className="org-regresar" href={rutaOrganizador('organizador-detalle', actividadId)}>Volver al detalle</a>
      <button className="org-boton org-boton--suave" type="button" onClick={() => setRevision(n => n + 1)}>Actualizar datos</button></div>
    <header className="org-ocupacion__cabecera"><span>SPRINT 3 · SEGUIMIENTO</span><h2>Ocupación y verificación de equipos</h2>
      <p>Consulta cuántas plazas están ocupadas y comprueba la inscripción individual de los integrantes.</p></header>
    {datos.cargando && <p role="status" className="org-cargando">Consultando inscripciones actuales…</p>}
    {datos.error && <div role="alert" className="org-alerta">{datos.error}</div>}
    {o && <>
      <section className="org-ocupacion__panel" aria-labelledby="org-ocupacion-titulo">
        <div className="org-ocupacion__fila"><div><h3 id="org-ocupacion-titulo">{o.nombre || 'Actividad'}</h3><p>Capacidad determinada por el administrador · {o.unidadCupo === 'equipos' ? 'cupo por equipo' : 'cupo por persona'}</p></div>
          <span className="org-ocupacion__fraccion">{ocupados} / {tieneLimite ? limite : '—'}</span></div>
        <div className="org-ocupacion__cifras">
          <div><span>Cupos ocupados</span><strong>{ocupados}</strong></div>
          <div><span>Cupos disponibles</span><strong>{o.cuposDisponibles == null ? '—' : o.cuposDisponibles}</strong></div>
          <div><span>Personas inscritas</span><strong>{o.estudiantesInscritos ?? 0}</strong></div>
          {o.unidadCupo === 'equipos' && <div><span>Equipos registrados</span><strong>{o.equiposRegistrados ?? 0}</strong></div>}
        </div>
        {tieneLimite ? <div className="org-ocupacion__progreso">
          <div className="org-ocupacion__fila"><strong>Ocupación actual</strong><span>{porcentaje}%</span></div>
          <div className="org-ocupacion__barra" role="progressbar" aria-label="Porcentaje de cupos ocupados" aria-valuenow={Math.min(100, Math.max(0, porcentaje))} aria-valuemin={0} aria-valuemax={100}>
            <span style={{ width: `${Math.min(100, Math.max(0, porcentaje))}%` }}/>
          </div>
        </div> : <p className="org-ocupacion__nota">El administrador aún no ha confirmado la capacidad máxima.</p>}
        {o.excedeLimite && <p role="alert" className="org-ocupacion__alerta">La actividad supera el límite configurado. Solicita una revisión administrativa.</p>}
        {o.unidadCupo === 'equipos' && <p className="org-ocupacion__nota">{o.equiposConfirmados ?? 0} equipos con inscripciones verificadas · {o.equiposPendientes ?? 0} pendientes. Los integrantes no consumen cupos de equipo adicionales.</p>}
      </section>
      {o.unidadCupo === 'equipos' && <section className="org-ocupacion__panel" aria-labelledby="org-equipos-titulo">
        <div className="org-ocupacion__fila"><div><h3 id="org-equipos-titulo">Verificación individual por equipo</h3>
          <p>Cada integrante necesita una inscripción propia vinculada a su equipo.</p></div>
          {v && <strong>{v.equiposCompletos} completos · {v.equiposPendientes} pendientes</strong>}</div>
        {datos.errorEquipos && <p role="alert" className="org-ocupacion__alerta">No se pudo verificar los equipos: {datos.errorEquipos}</p>}
        {v && v.equipos.length === 0 && <p className="org-ocupacion__nota">Todavía no se han registrado equipos en esta actividad.</p>}
        {v && v.equipos.map(equipo => <article key={equipo.equipoId} className="org-ocupacion__equipo">
          <div className="org-ocupacion__fila"><h4>{equipo.nombre}</h4><strong className={equipo.completo ? 'org-ocupacion__ok' : 'org-ocupacion__pendiente'}>{equipo.completo ? 'Equipo verificado' : 'Equipo pendiente'}</strong></div>
          <p>{equipo.inscripcionesConfirmadas} de {equipo.totalIntegrantes} integrantes inscritos individualmente{v.limiteIntegrantesEquipo > 0 ? ` · Máximo ${v.limiteIntegrantesEquipo} por equipo` : ''}</p>
          <ul>{equipo.integrantes.map(persona => <li key={persona.estudianteId}>
            <span><strong>{persona.nombre}</strong> <small>· {persona.rol}</small></span>
            <span className={persona.inscritoIndividualmente ? 'org-ocupacion__ok' : 'org-ocupacion__pendiente'}>{persona.inscritoIndividualmente ? 'Inscripción confirmada' : 'Inscripción pendiente'}</span>
          </li>)}</ul>
          {equipo.faltanIntegrantesMinimos > 0 && <p className="org-ocupacion__nota">Falta al menos un integrante para formar un equipo válido.</p>}
          {equipo.superaLimite && <p role="alert" className="org-ocupacion__alerta">El equipo supera el máximo de integrantes autorizado.</p>}
        </article>)}
      </section>}
      <OrganizadorAnalitica token={token} actividadId={actividadId} revision={revision} />
      <OrganizadorSugerencias token={token} actividadId={actividadId} />
      <a className="org-boton org-boton--primario" href={rutaOrganizador('organizador-inscritos', actividadId)}>Ver inscritos y reemplazos</a>
    </>}
  </main>
}
