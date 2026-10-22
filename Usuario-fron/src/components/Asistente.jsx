import { useCallback, useEffect, useRef, useState } from 'react'
import { leerSesion } from '../services/authService'
import { preguntarAsistente } from '../services/asistenteService'
import { comprobarServicios } from '../services/estadoServicio'
import { borrarHistorial, guardarHistorial, leerHistorial } from '../services/historialAsistente'
import { MensajesChat } from './MensajesChat'
import { PREGUNTAS_NATURALES } from '../services/preguntasNaturales.js'
import './Asistente.css'

const EJEMPLOS = [
  ...PREGUNTAS_NATURALES,
  { texto: '¿Qué actividades hay?', icono: 'calendario' },
  { texto: '¿Qué actividades hay hoy a las 15:00?', icono: 'reloj' },
  { texto: '¿Qué actividades hay mañana?', icono: 'calendario' },
  { texto: '¿A qué hora empieza una actividad?', icono: 'pregunta' },
  { texto: '¿Dónde se realizará una actividad?', icono: 'ubicacion' },
  { texto: '¿Qué requisitos tiene el taller de robótica?', icono: 'pregunta' },
]
const SALUDO = { rol: 'asistente', contenido: '¡Hola! Soy el asistente de VYNK. Puedo consultar fechas, horarios, ubicaciones y requisitos de las actividades publicadas por administración. ¿Qué te gustaría saber?' }

/** Iconografía propia, ligera: no requiere instalar bibliotecas. */
function Icono({ nombre, tamaño = 21 }) {
  const comun = { width: tamaño, height: tamaño, viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: 1.9, strokeLinecap: 'round', strokeLinejoin: 'round', 'aria-hidden': true }
  switch (nombre) {
    case 'calendario': return <svg {...comun}><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M7 3v4M17 3v4M3 10h18"/></svg>
    case 'ubicacion': return <svg {...comun}><path d="M20 10c0 5-8 12-8 12S4 15 4 10a8 8 0 1 1 16 0Z"/><circle cx="12" cy="10" r="2.5"/></svg>
    case 'reloj': return <svg {...comun}><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></svg>
    case 'pregunta': return <svg {...comun}><path d="M20 11.5a8.5 8.5 0 0 1-12.2 7.7L3 21l1.6-4.6A8.5 8.5 0 1 1 20 11.5Z"/><path d="M9.5 9a2.7 2.7 0 1 1 4.2 2.3c-.9.6-1.7 1.1-1.7 2.2M12 16.5h.01"/></svg>
    case 'conexion': return <svg {...comun}><path d="M2 9a15 15 0 0 1 20 0M5.5 12.5a10 10 0 0 1 13 0M9 16a5 5 0 0 1 6 0"/><circle cx="12" cy="19" r="1"/></svg>
    case 'escudo': return <svg {...comun}><path d="M12 2 20 6v6c0 5-3.4 8-8 10-4.6-2-8-5-8-10V6l8-4Z"/><path d="m8.5 12 2.3 2.3 4.7-5"/></svg>
    case 'mas': return <svg {...comun}><circle cx="12" cy="12" r="9"/><path d="M12 8v8M8 12h8"/></svg>
    case 'flecha': return <svg {...comun}><path d="m9 6 6 6-6 6"/></svg>
    default: return <svg {...comun}><path d="m12 2 2.6 7.4L22 12l-7.4 2.6L12 22l-2.6-7.4L2 12l7.4-2.6L12 2Z"/></svg>
  }
}

/** Solo se renueva la presentación. Conexión, historial, sesión y peticiones existentes intactos. */
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
      setMensajes(prev => [...prev, { rol: 'asistente', contenido: resultado.respuesta, origen: resultado.origen, aviso: resultado.aviso, ubicaciones: resultado.ubicaciones || [], tipoConsulta: resultado.tipoConsulta || 'general', requisitos: resultado.requisitos || [], actividades: ['momento', 'tema_horario'].includes(resultado.tipoConsulta) ? resultado.actividades || [] : [] }])
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
      <div className="v-asistente-hero__contenido">
        <div className="v-asistente-hero__simbolo" aria-hidden="true"><Icono nombre="estrella" tamaño={34} /></div>
        <div className="v-asistente-hero__texto">
          <h2>Asistente <span>VYNK</span></h2>
          <p>Consulta horarios, requisitos y ubicaciones de actividades.</p>
          <span className={`v-asistente-hero__estado${error ? ' v-asistente-hero__estado--error' : ''}`}><span className="v-asistente-punto" />{cargando ? 'Consultando el cronograma…' : error ? 'Revisa la última consulta' : 'Listo para consultar'}</span>
        </div>
      </div>
      <div className="v-asistente-hero__arte" aria-hidden="true" />
    </div>
    <div className="v-asistente-grid">
      <aside className="v-asistente-side" aria-label="Preguntas sugeridas">
        <div className="v-asistente-side__cab"><span>EXPLORA</span><h3>¿Qué necesitas saber?</h3><p>Empieza con una consulta rápida o escribe tu propia pregunta.</p></div>
        <div className="v-asistente-ejemplos">
          {EJEMPLOS.map(e => <button key={e.texto} type="button" onClick={() => enviar(e.texto)} disabled={cargando}>
            <span className="v-asistente-ejemplos__icono"><Icono nombre={e.icono} /></span>
            <span className="v-asistente-ejemplos__texto">{e.texto}</span>
            <Icono nombre="flecha" tamaño={17} />
          </button>)}
        </div>
        <div className="v-asistente-diagnostico">
          <button type="button" onClick={verificar} disabled={verificando}><Icono nombre="conexion" tamaño={17}/>{verificando ? 'Comprobando…' : 'Comprobar conexiones'}</button>
          {diagnostico && <p role="status">{diagnostico.mongodb === 'conectado' ? '✓ MongoDB conectado' : '⚠ MongoDB sin conexión'} · {diagnostico.actividadesPublicadas} actividades · Gemini {diagnostico.geminiConfigurado ? 'configurado' : 'sin configurar'}</p>}
          {diagnosticoError && <p role="alert">{diagnosticoError}</p>}
        </div>
        <div className="v-asistente-fuente"><span className="v-asistente-fuente__icono"><Icono nombre="escudo" tamaño={21}/></span><div><strong>Información confiable</strong><small>El asistente consulta las actividades publicadas por administración. No inventa actividades.</small></div></div>
      </aside>
      <div className="v-asistente-chat">
        <header className="v-asistente-chat__header"><div className="v-asistente-chat__logo" aria-hidden="true"><Icono nombre="estrella" tamaño={23}/></div><div className="v-asistente-chat__identidad"><h3>Asistente VYNK</h3><p><span className="v-asistente-online" />{estado}</p></div><button className="v-asistente-reset" type="button" onClick={limpiar} disabled={cargando || mensajes.length === 0} title="Borrar la conversación de este dispositivo"><Icono nombre="mas" tamaño={19}/> Nueva conversación</button></header>
        <MensajesChat mensajes={mensajes.length ? mensajes : [SALUDO]} texto={texto} onTexto={setTexto} onEnviar={() => enviar()} cargando={cargando} deshabilitado={!correo} />
        {error && <div className="v-asistente-error" role="alert">⚠ {error} <button type="button" onClick={() => setError('')}>Cerrar</button></div>}
      </div>
    </div>
  </section>
}
