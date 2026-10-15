import { useCallback, useEffect, useRef, useState } from 'react'
import { leerSesion } from '../services/authService'
import { preguntarAsistente } from '../services/asistenteService'
import { comprobarServicios } from '../services/estadoServicio'
import { borrarHistorial, guardarHistorial, leerHistorial } from '../services/historialAsistente'
import { MensajesChat } from './MensajesChat'
import './Asistente.css'

const EJEMPLOS = ['¿Qué actividades hay?', '¿Qué horarios hay hoy?', '¿Qué actividades hay mañana?', '¿A qué hora empieza una actividad?']
const SALUDO = { rol: 'asistente', contenido: '¡Hola! Soy el asistente de VYNK. Puedo consultar fechas y horarios de las actividades publicadas por administración. ¿Qué te gustaría saber?' }

/** Mishel: estado de la conversación, almacenamiento por cuenta y control de carga/error. */
export function Asistente() {
  const sesion = leerSesion()
  const correo = sesion?.usuario?.correo ?? ''
  const [mensajes, setMensajes] = useState(() => leerHistorial(correo))
  const [texto, setTexto] = useState('')
  const [cargando, setCargando] = useState(false)
  const [error, setError] = useState('')
  const [estado, setEstado] = useState('Listo para consultar')
  const [diagnostico, setDiagnostico] = useState(null)
  const [diagnosticoError, setDiagnosticoError] = useState('')
  const [verificando, setVerificando] = useState(false)
  const peticion = useRef(null)

  useEffect(() => {
    setMensajes(leerHistorial(correo))
    return () => { peticion.current?.abort() }
  }, [correo])
  useEffect(() => { guardarHistorial(correo, mensajes) }, [correo, mensajes])

  const verificar = useCallback(async () => {
    const controller = new AbortController()
    setVerificando(true); setDiagnosticoError('')
    try { setDiagnostico(await comprobarServicios(leerSesion()?.token, controller.signal)) }
    catch (e) { setDiagnostico(null); setDiagnosticoError(e.message) }
    finally { setVerificando(false) }
  }, [])

  const enviar = useCallback(async (pregunta = texto) => {
    const contenido = String(pregunta).trim()
    if (!contenido || cargando) return
    const token = leerSesion()?.token
    if (!token) { setError('Inicia sesión para utilizar el asistente.'); return }
    const nuevo = { rol: 'usuario', contenido }
    const anteriores = mensajes.filter(m => m !== SALUDO).slice(-8)
    const controller = new AbortController()
    peticion.current = controller
    setMensajes(prev => [...prev, nuevo])
    setTexto(''); setCargando(true); setError(''); setEstado('Consultando las actividades publicadas…')
    const reloj = setTimeout(() => controller.abort(), 28000)
    try {
      const resultado = await preguntarAsistente({ mensaje: contenido, historial: anteriores, token, signal: controller.signal })
      setMensajes(prev => [...prev, { rol: 'asistente', contenido: resultado.respuesta, origen: resultado.origen, aviso: resultado.aviso }])
      setEstado(resultado.origen === 'gemini' ? 'Respuesta contrastada con el catálogo · Gemini' : 'Respuesta del catálogo verificado')
    } catch (e) {
      if (controller.signal.aborted) { setError('La consulta tardó demasiado. Comprueba que Spring Boot esté activo e inténtalo otra vez.') }
      else setError(e.message || 'No fue posible consultar el asistente.')
      setEstado('No se pudo completar la consulta')
    } finally { clearTimeout(reloj); setCargando(false); peticion.current = null }
  }, [texto, cargando, mensajes])

  const limpiar = () => { peticion.current?.abort(); borrarHistorial(correo); setMensajes([]); setTexto(''); setError(''); setEstado('Listo para consultar') }
  return <section className="v-asistente" aria-label="Asistente de horarios">
    <div className="v-asistente-hero">
      <div><span className="v-asistente-eyebrow"><span className="v-asistente-punto" /> VYNK INTELIGENTE · HORARIOS REALES</span><h2>Tu semana, más clara.</h2><p>Pregunta por fechas, actividades y horas del evento. Siempre consultamos la información publicada por administración.</p></div>
      <div className="v-asistente-hero__icon" aria-hidden="true">✦</div>
    </div>
    <div className="v-asistente-grid">
      <aside className="v-asistente-side">
        <div className="v-asistente-side__cab"><span>EXPLORA</span><h3>¿Qué necesitas saber?</h3><p>Empieza con una consulta rápida o escribe tu propia pregunta.</p></div>
        <div className="v-asistente-ejemplos">{EJEMPLOS.map(p => <button key={p} type="button" onClick={() => enviar(p)} disabled={cargando}>{p}<span>↗</span></button>)}</div>
        <div className="v-asistente-diagnostico"><button type="button" onClick={verificar} disabled={verificando}>{verificando ? 'Comprobando…' : '◉ Comprobar conexiones'}</button>{diagnostico && <p role="status">{diagnostico.mongodb === 'conectado' ? '✓ MongoDB conectado' : '⚠ MongoDB sin conexión'} · {diagnostico.actividadesPublicadas} actividades · Gemini {diagnostico.geminiConfigurado ? 'configurado' : 'sin configurar'}</p>}{diagnosticoError && <p role="alert">{diagnosticoError}</p>}</div><div className="v-asistente-fuente"><span aria-hidden="true">✓</span><div><strong>Información confiable</strong><small>Sin eventos inventados: el asistente consulta el catálogo real. Gemini es opcional.</small></div></div>
      </aside>
      <div className="v-asistente-chat">
        <header className="v-asistente-chat__header"><div className="v-asistente-chat__logo" aria-hidden="true">✦</div><div><h3>Asistente VYNK</h3><p><span className="v-asistente-online" />{estado}</p></div><button className="v-asistente-reset" type="button" onClick={limpiar} disabled={cargando || mensajes.length === 0} title="Borrar la conversación de este dispositivo">↻ Nueva conversación</button></header>
        <MensajesChat mensajes={mensajes.length ? mensajes : [SALUDO]} texto={texto} onTexto={setTexto} onEnviar={() => enviar()} cargando={cargando} deshabilitado={!correo} />
        {error && <div className="v-asistente-error" role="alert">⚠ {error} <button type="button" onClick={() => setError('')}>Cerrar</button></div>}
      </div>
    </div>
  </section>
}
