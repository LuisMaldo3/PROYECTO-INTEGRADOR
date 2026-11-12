import { useCallback, useEffect, useState } from 'react'
import Icono from './Iconos'
import { estadoIA } from '../services/puntajesService'

const USOS = [
  ['T&C ARENA', 'Propone el sorteo de los equipos (tú decides).'],
  ['Demanda', 'Explica qué actividades tienen más demanda.'],
  ['Intereses', 'Resume los intereses de los estudiantes.'],
  ['Resumen del evento', 'Redacta el resumen de la T&C Week con los resultados oficiales.'],
  ['Web de estudiantes', 'Asistente de horarios, lectura del horario de clases y comentarios (usa la clave de Usuario-back/.env).'],
]

/** Estado de Gemini y prueba real, con la causa exacta si algo falla. */
export default function PantallaIA({ token, onSesionExpirada }) {
  const [e, setE] = useState(null)
  const [probando, setProbando] = useState(false)
  const [error, setError] = useState('')
  const consultar = useCallback((probar) => {
    setProbando(probar); setError('')
    return estadoIA(token, probar).then(setE)
      .catch((x) => { if (x?.estado === 401) onSesionExpirada?.(x.detalle); else setError(x?.message || 'No se pudo consultar la IA.') })
      .finally(() => setProbando(false))
  }, [token, onSesionExpirada])
  useEffect(() => {
    let activo = true
    estadoIA(token, false).then((r) => { if (activo) setE(r) })
      .catch((x) => { if (activo) { if (x?.estado === 401) onSesionExpirada?.(x.detalle); else setError(x?.message || 'No se pudo consultar la IA.') } })
    return () => { activo = false }
  }, [token, onSesionExpirada])

  return (
    <section className="pantalla-ia">
      <header className="pagina__cabecera"><div><h1>Inteligencia artificial</h1><p>Gemini propone, explica y resume; nunca decide resultados ni cambia puntos.</p></div></header>
      {error && <p className="aviso aviso--error" role="alert"><Icono nombre="alerta" tam={18} />{error}</p>}
      {e && (
        <div className="tarjeta ia-estado">
          <dl className="ia-estado__datos">
            <div><dt>Clave de Gemini</dt><dd className={e.configurada ? 'ia-ok' : 'ia-mal'}>{e.configurada ? `Configurada${e.origen ? ` (${e.origen})` : ''}` : 'No configurada'}</dd></div>
            <div><dt>Modelo</dt><dd>{e.modelo} <small>· si ya no existe, se prueba {e.respaldo.join(' y luego ')}</small></dd></div>
            <div><dt>Clave para la web de estudiantes</dt><dd className={e.claveEnSpring ? 'ia-ok' : 'ia-mal'}>{e.claveEnSpring ? 'Encontrada en Usuario-back/.env' : 'No está en Usuario-back/.env: el asistente de estudiantes no podrá usar la IA'}</dd></div>
          </dl>
          {e.probada && (
            <p className={`aviso ${e.ok ? 'aviso--ok' : 'aviso--error'}`} role="status"><Icono nombre={e.ok ? 'check' : 'alerta'} tam={18} />
              {e.ok ? <>{e.mensaje} Respuesta: «{e.respuesta}» ({e.modeloUsado}).</> : <span>{e.mensaje}{e.detalleGoogle && <small className="ia-detalle">Respuesta de Google: «{e.detalleGoogle}»</small>}</span>}</p>)}
          {!e.configurada && <p className="aviso aviso--atencion"><Icono nombre="alerta" tam={18} />{e.mensaje} Consigue una clave en Google AI Studio (aistudio.google.com) y escribe <code>GEMINI_API_KEY=tu_clave</code> en ese archivo.</p>}
          <div className="modal__acciones"><button type="button" className="boton" onClick={() => consultar(true)} disabled={probando || !e.configurada}><Icono nombre="chispa" tam={18} /> {probando ? 'Probando con Google…' : 'Probar la IA ahora'}</button></div>
        </div>)}
      <div className="tarjeta ia-usos"><h2>Dónde se usa</h2><ul>{USOS.map(([d, t]) => <li key={d}><strong>{d}</strong><span>{t}</span></li>)}</ul></div>
    </section>
  )
}
