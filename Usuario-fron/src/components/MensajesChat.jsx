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
  </div>
}
/** Jordi: mensajes y caja de texto con envío por Enter, Shift+Enter para salto de línea. */
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
        <textarea id="v-chat-input" rows={2} maxLength={600} placeholder="Ej. ¿A qué hora comienza la charla de programación?" value={texto} onChange={e => onTexto(e.target.value)} onKeyDown={e => { if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing) { e.preventDefault(); if (texto.trim() && !cargando && !deshabilitado) onEnviar() } }} disabled={deshabilitado} />
        <button type="submit" disabled={deshabilitado || cargando || !texto.trim()} aria-label="Enviar pregunta">{cargando ? '…' : 'Enviar ↗'}</button>
      </div>
      <span>Enter para enviar · Shift + Enter para nueva línea · {texto.length}/600</span>
    </form>
  </>
}
