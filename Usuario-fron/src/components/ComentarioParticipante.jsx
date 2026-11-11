import { useEffect, useState } from 'react'
import { guardarMiComentario, obtenerMiComentario } from '../services/comentariosService'
import './ComentariosActividad.css'

/** Solo una inscripción activa permite guardar o actualizar un comentario propio. */
export function ComentarioParticipante({ token, actividadId }) {
  const [estado, setEstado] = useState({ cargando: true, permitido: false, error: '' })
  const [texto, setTexto] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [aviso, setAviso] = useState('')
  useEffect(() => {
    if (!token || !actividadId) { setEstado({ cargando: false, permitido: false, error: '' }); return }
    const ctrl = new AbortController()
    setEstado({ cargando: true, permitido: false, error: '' }); setAviso('')
    obtenerMiComentario(token, actividadId, ctrl.signal)
      .then(d => { if (!ctrl.signal.aborted) { setEstado({ cargando: false, permitido: Boolean(d.puedeComentar), error: '' }); setTexto(d.comentario || '') } })
      .catch(e => { if (!ctrl.signal.aborted) setEstado({ cargando: false, permitido: false, error: e.message }) })
    return () => ctrl.abort()
  }, [token, actividadId])

  async function enviar(e) {
    e.preventDefault()
    if (enviando) return
    setEnviando(true); setAviso('')
    try {
      await guardarMiComentario(token, actividadId, texto)
      setAviso('Comentario guardado. Puedes actualizarlo si cambias de opinión.')
    } catch (e) { setAviso(e.message) } finally { setEnviando(false) }
  }
  return <section className="comentarios-actividad comentarios-actividad--estudiante" aria-labelledby="comentario-estudiante-titulo">
    <h3 id="comentario-estudiante-titulo">Tu opinión sobre la actividad</h3>
    {estado.cargando && <p role="status">Comprobando tu inscripción…</p>}
    {estado.error && <p role="alert">{estado.error}</p>}
    {!estado.cargando && !estado.error && !estado.permitido && <p>Para dejar una opinión, necesitas una inscripción activa en esta actividad.</p>}
    {estado.permitido && <form onSubmit={enviar}>
      <p>Comparte lo que funcionó y qué podría mejorar. Tu comentario contribuirá al análisis agrupado del organizador.</p>
      <label htmlFor="comentario-actividad-texto">Comentario (10 a 1000 caracteres)</label>
      <textarea id="comentario-actividad-texto" rows={4} minLength={10} maxLength={1000} value={texto} disabled={enviando}
        onChange={e => setTexto(e.target.value)} placeholder="¿Cómo fue tu experiencia?" required/>
      <div className="comentarios-actividad__acciones"><small>{texto.length}/1000</small>
        <button type="submit" className="vynk-up-btn vynk-up-btn--primary" disabled={enviando || texto.trim().length < 10}>
          {enviando ? 'Guardando…' : 'Guardar comentario'}</button></div>
      {aviso && <p role="status">{aviso}</p>}
    </form>}
  </section>
}
