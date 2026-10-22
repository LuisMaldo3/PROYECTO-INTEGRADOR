import { useEffect, useRef } from 'react'
import './MensajesChat.css'

function Burbuja({ mensaje }) {
  const usuario = mensaje.rol === 'usuario'
  return <div className={`v-chat-msg ${usuario ? 'v-chat-msg--user' : 'v-chat-msg--bot'}`}>
    {!usuario && <div className="v-chat-avatar" aria-hidden="true">✦</div>}
    <div className="v-chat-msg__content">
      <span className="v-chat-autor">{usuario ? 'Tú' : 'VYNK · Asistente de horarios'}</span>
      <p>{mensaje.contenido}</p>
      {!usuario && mensaje.origen && <small>{mensaje.origen === 'gemini' ? '✦ Selección con Gemini · horarios verificados' : '✓ Información del cronograma real'}{mensaje.aviso ? ` · ${mensaje.aviso}` : ''}</small>}
    </div>
    {usuario && <div className="v-chat-avatar v-chat-avatar--user" aria-hidden="true"><svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/></svg></div>}
  </div>
}

/** Las preguntas y respuestas son las reales del servicio; solo se modifica el diseño. */
export function MensajesChat({ mensajes, texto, onTexto, onEnviar, cargando, deshabilitado }) {
  const finalRef = useRef(null)
  useEffect(() => { finalRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' }) }, [mensajes.length, cargando])
  const enviar = (e) => { e.preventDefault(); if (!cargando && !deshabilitado && texto.trim()) onEnviar() }
  return <>
    <div className="v-chat-timeline" role="log" aria-label="Conversación con VYNK" aria-live="polite" aria-relevant="additions text">
      {mensajes.map((m, i) => <Burbuja key={`${i}-${m.rol}`} mensaje={m} />)}
      {cargando && <div className="v-chat-msg v-chat-msg--bot"><div className="v-chat-avatar" aria-hidden="true">✦</div><div className="v-chat-msg__content"><span className="v-chat-autor">VYNK está consultando el cronograma</span><div className="v-chat-typing" role="status" aria-label="Cargando respuesta"><i /><i /><i /></div></div></div>}
      <div ref={finalRef} />
    </div>
    <form className="v-chat-composer" onSubmit={enviar}>
      <label htmlFor="v-chat-input">Escribe tu pregunta</label>
      <div className="v-chat-composer__campo">
        <span className="v-chat-composer__sparkle" aria-hidden="true">✦</span>
        <textarea id="v-chat-input" rows={1} maxLength={600} placeholder="Ej. ¿A qué hora comienza la charla de programación?" value={texto} onChange={e => onTexto(e.target.value)} onKeyDown={e => { if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing) { e.preventDefault(); if (texto.trim() && !cargando && !deshabilitado) onEnviar() } }} disabled={deshabilitado} />
        <button type="submit" disabled={deshabilitado || cargando || !texto.trim()} aria-label="Enviar pregunta"><svg width="21" height="21" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="m22 2-7 20-4-9-9-4 20-7Z"/><path d="M22 2 11 13"/></svg>{cargando ? 'Enviando…' : 'Enviar'}</button>
      </div>
      <span>Presiona Enter para enviar · Shift + Enter para nueva línea · {texto.length}/600</span>
    </form>
  </>
}
