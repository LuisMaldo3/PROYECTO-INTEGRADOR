import { useEffect, useState } from 'react'
import { generarResumenOrganizadorIA, obtenerParticipacionOrganizador } from '../services/organizadorService'
import './OrganizadorAnalitica.css'

/** US-60/US-61: análisis basado en inscripciones reales, sin registrar asistencia. */
export function OrganizadorAnalitica({ token, actividadId, revision = 0 }) {
  const [indicadores, setIndicadores] = useState({ cargando: true, error: '', datos: null })
  const [ia, setIa] = useState({ cargando: false, texto: '', origen: '', aviso: '', error: '' })

  useEffect(() => {
    const control = new AbortController()
    setIndicadores({ cargando: true, error: '', datos: null })
    setIa({ cargando: false, texto: '', origen: '', aviso: '', error: '' })
    obtenerParticipacionOrganizador(token, actividadId, control.signal)
      .then(datos => {
        if (!control.signal.aborted) setIndicadores({ cargando: false, error: '', datos })
      })
      .catch(error => {
        if (!control.signal.aborted) setIndicadores({ cargando: false, error: error.message, datos: null })
      })
    return () => control.abort()
  }, [token, actividadId, revision])

  async function generar() {
    if (ia.cargando) return
    setIa({ cargando: true, texto: '', origen: '', aviso: '', error: '' })
    try {
      const respuesta = await generarResumenOrganizadorIA(token, actividadId)
      setIa({ cargando: false, texto: respuesta.resumen || '', origen: respuesta.origen || '',
        aviso: respuesta.aviso || '', error: '' })
      if (respuesta.datos) setIndicadores({ cargando: false, error: '', datos: respuesta.datos })
    } catch (error) {
      setIa({ cargando: false, texto: '', origen: '', aviso: '', error: error.message })
    }
  }

  const d = indicadores.datos
  const porcentaje = d?.porcentajeParticipacion == null ? null : Number(d.porcentajeParticipacion)
  const seguro = Number.isFinite(porcentaje) ? Math.max(0, Math.min(100, porcentaje)) : 0
  const equipos = d?.unidadCupo === 'equipos'

  return <section className="org-analitica org-ocupacion__panel" aria-labelledby="org-analitica-titulo">
    <div className="org-analitica__cabecera">
      <div><span className="org-analitica__sobretitulo">US-60 · US-61</span>
        <h3 id="org-analitica-titulo">Participación y resumen con IA</h3>
        <p>Los indicadores representan inscripciones activas, no asistencia presencial.</p></div>
      <button className="org-boton org-boton--primario" type="button" disabled={!d || ia.cargando}
        onClick={generar}>{ia.cargando ? 'Generando resumen…' : 'Generar con IA'}</button>
    </div>
    {indicadores.cargando && <p role="status">Consultando estadísticas actuales…</p>}
    {indicadores.error && <p className="org-ocupacion__alerta" role="alert">{indicadores.error}</p>}
    {d && <>
      <div className="org-analitica__estadisticas">
        <div><span>Nivel de participación</span><strong>{porcentaje == null ? '—' : `${porcentaje}%`}</strong>
          <small>{porcentaje == null ? 'Sin capacidad definida' : d.nivel === 'alto' ? 'Ocupación alta' : d.nivel === 'medio' ? 'Ocupación media' : 'Ocupación baja'}</small></div>
        <div><span>Estudiantes inscritos</span><strong>{d.estudiantesInscritos ?? 0}</strong></div>
        <div><span>{equipos ? 'Equipos registrados' : 'Cupos ocupados'}</span><strong>{equipos ? d.equiposRegistrados : d.cuposOcupados}</strong></div>
        <div><span>Reemplazos registrados</span><strong>{d.reemplazosRegistrados ?? 0}</strong></div>
      </div>
      {porcentaje != null ? <div className="org-analitica__barra-wrap">
        <div className="org-analitica__barra-texto"><strong>Ocupación de {equipos ? 'cupos por equipo' : 'cupos por persona'}</strong><span>{d.cuposOcupados} de {d.maximoParticipantes}</span></div>
        <div className="org-analitica__barra" role="progressbar" aria-label="Nivel de participación por ocupación" aria-valuemin={0} aria-valuemax={100} aria-valuenow={seguro}><span style={{ width: `${seguro}%` }}/></div>
      </div> : <p className="org-ocupacion__nota">Administración aún no ha definido un cupo máximo válido. No se muestra un porcentaje inventado.</p>}
      {equipos && <p className="org-analitica__nota">Equipos con inscripciones completas: <strong>{d.equiposConfirmados ?? 0}</strong> · Equipos pendientes: <strong>{d.equiposPendientes ?? 0}</strong>. Los integrantes no aumentan el cupo de equipos.</p>}
      {d.excedeLimite && <p className="org-ocupacion__alerta" role="alert">La actividad supera el límite establecido. Comunícalo a administración.</p>}
    </>}
    <div className="org-analitica__respuesta" aria-live="polite">
      <h4>Resumen de inscripciones, equipos y reemplazos</h4>
      {ia.cargando && <p role="status">Gemini está analizando las estadísticas agrupadas…</p>}
      {ia.error && <p className="org-ocupacion__alerta" role="alert">{ia.error}</p>}
      {ia.texto && ia.origen === 'gemini' && <p className="org-analitica__texto">{ia.texto}</p>}
      {ia.aviso && <p className="org-analitica__nota" role={ia.texto ? 'status' : 'alert'}>{ia.aviso}</p>}
      {!ia.cargando && !ia.texto && !ia.error && !ia.aviso && <p>Pulsa «Generar con IA» para obtener un resumen basado en los datos actuales de la actividad.</p>}
    </div>
  </section>
}
