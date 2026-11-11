import { useEffect, useState } from 'react'
import { generarAnalisisComentarios, obtenerEstadisticasComentarios } from '../services/comentariosService'
import './ComentariosActividad.css'

/** US-62/63: resumen y temas frecuentes devueltos por Spring Boot; no se muestra un resultado ficticio. */
export function OrganizadorComentarios({ token, actividadId }) {
  const [estadisticas, setEstadisticas] = useState({ cargando: true, total: 0, error: '' })
  const [analisis, setAnalisis] = useState({ cargando: false, resumen: '', temas: [], origen: '', aviso: '', error: '', analizados: 0 })

  useEffect(() => {
    const ctrl = new AbortController()
    setEstadisticas({ cargando: true, total: 0, error: '' })
    setAnalisis({ cargando: false, resumen: '', temas: [], origen: '', aviso: '', error: '', analizados: 0 })
    obtenerEstadisticasComentarios(token, actividadId, ctrl.signal)
      .then(d => { if (!ctrl.signal.aborted) setEstadisticas({ cargando: false, total: Number(d.totalComentarios) || 0, error: '' }) })
      .catch(e => { if (!ctrl.signal.aborted) setEstadisticas({ cargando: false, total: 0, error: e.message }) })
    return () => ctrl.abort()
  }, [token, actividadId])

  async function generar() {
    if (analisis.cargando) return
    setAnalisis(a => ({ ...a, cargando: true, error: '', aviso: '', resumen: '', temas: [] }))
    try {
      const d = await generarAnalisisComentarios(token, actividadId)
      setEstadisticas({ cargando: false, total: Number(d.totalComentarios) || 0, error: '' })
      setAnalisis({ cargando: false, resumen: d.resumen || '', temas: Array.isArray(d.temas) ? d.temas : [],
        origen: d.origen || '', aviso: d.aviso || '', error: '', analizados: Number(d.comentariosAnalizados) || 0 })
    } catch (e) {
      setAnalisis({ cargando: false, resumen: '', temas: [], origen: '', aviso: '', error: e.message, analizados: 0 })
    }
  }

  return <section className="comentarios-actividad comentarios-actividad--organizador" aria-labelledby="comentarios-resumen-titulo">
    <div className="comentarios-actividad__cabecera">
      <div><span className="comentarios-actividad__eyebrow">US-62 · US-63</span>
        <h3 id="comentarios-resumen-titulo">Comentarios de participantes</h3>
        <p>Conoce las opiniones recibidas y los temas más mencionados.</p></div>
      <button type="button" className="org-boton org-boton--primario" disabled={estadisticas.cargando || analisis.cargando || !estadisticas.total}
        onClick={generar}>{analisis.cargando ? 'Analizando…' : 'Generar con IA'}</button>
    </div>
    {estadisticas.cargando ? <p role="status">Consultando comentarios…</p> : <p className="comentarios-actividad__indicador"><strong>{estadisticas.total}</strong> comentarios recibidos</p>}
    {estadisticas.error && <p role="alert" className="comentarios-actividad__error">{estadisticas.error}</p>}
    {!estadisticas.cargando && !estadisticas.total && !estadisticas.error && <p>Cuando los estudiantes inscritos comenten, podrás generar el resumen.</p>}
    <div aria-live="polite">
      {analisis.cargando && <p role="status">Gemini está analizando los comentarios sin datos de identificación…</p>}
      {analisis.error && <p role="alert" className="comentarios-actividad__error">{analisis.error}</p>}
      {analisis.origen === 'gemini' && <>
        <h4>Resumen de comentarios</h4><p className="comentarios-actividad__resumen">{analisis.resumen}</p>
        <h4>Temas frecuentes</h4>
        {analisis.temas.length ? <div className="comentarios-actividad__etiquetas">{analisis.temas.map((t, i) =>
          <span className="comentarios-actividad__etiqueta" key={`${t.tema}-${i}`}>{t.tema} <strong>{t.cantidad}</strong></span>)}</div>
          : <p>No se identificaron temas repetidos en los comentarios analizados.</p>}
      </>}
      {analisis.aviso && <p role={analisis.origen === 'gemini' ? 'status' : 'alert'} className="comentarios-actividad__nota">{analisis.aviso}</p>}
    </div>
  </section>
}
