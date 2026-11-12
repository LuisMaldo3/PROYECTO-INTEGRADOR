import { useState } from 'react'
import { generarSugerenciasOrganizadorIA } from '../services/organizadorService'
import './OrganizadorSugerencias.css'

/**
 * «Como organizador, quiero recibir sugerencias basadas en los datos recopilados para futuras
 * actividades.» Tarea 3 (Jordi): junta la tarea 1 (reunir datos) y la tarea 2 (pedir a la IA) en
 * un solo botón, porque el organizador no necesita ver un paso intermedio: pide las sugerencias y
 * ve el resultado. Gemini solo sugiere; el organizador decide qué aplicar.
 */
export function OrganizadorSugerencias({ token, actividadId }) {
  const [estado, setEstado] = useState({ cargando: false, sugerencias: [], resumen: '', origen: '', aviso: '', error: '' })

  async function pedirSugerencias() {
    if (estado.cargando) return
    setEstado((e) => ({ ...e, cargando: true, error: '' }))
    try {
      const respuesta = await generarSugerenciasOrganizadorIA(token, actividadId)
      setEstado({
        cargando: false,
        sugerencias: Array.isArray(respuesta.sugerencias) ? respuesta.sugerencias : [],
        resumen: respuesta.resumen || '',
        origen: respuesta.origen || '',
        aviso: respuesta.aviso || '',
        error: '',
      })
    } catch (error) {
      setEstado((e) => ({ ...e, cargando: false, error: error.message }))
    }
  }

  return (
    <section className="org-sugerencias org-ocupacion__panel" aria-labelledby="org-sugerencias-titulo">
      <div className="org-sugerencias__cabecera">
        <div>
          <h3 id="org-sugerencias-titulo">Sugerencias para futuras actividades</h3>
          <p>A partir de la participación, los comentarios y el resultado de esta actividad, Gemini propone mejoras para la próxima vez. Tú decides qué aplicar.</p>
        </div>
        <button className="org-boton org-boton--primario" type="button" disabled={estado.cargando} onClick={pedirSugerencias}>
          {estado.cargando ? 'Pensando sugerencias…' : estado.sugerencias.length > 0 ? 'Volver a pedir sugerencias' : 'Pedir sugerencias con IA'}
        </button>
      </div>

      <div aria-live="polite">
        {estado.cargando && <p role="status">Gemini está revisando los datos de esta actividad…</p>}
        {estado.error && <p className="org-ocupacion__alerta" role="alert">{estado.error}</p>}

        {!estado.cargando && !estado.error && estado.resumen && <p className="org-sugerencias__resumen">{estado.resumen}</p>}

        {estado.sugerencias.length > 0 && (
          <ul className="org-sugerencias__lista">
            {estado.sugerencias.map((s, i) => (
              <li key={i} className="org-sugerencias__item">
                <strong>{s.titulo}</strong>
                <p>{s.detalle}</p>
              </li>
            ))}
          </ul>
        )}

        {estado.aviso && <p className="org-ocupacion__nota" role={estado.sugerencias.length ? 'status' : 'alert'}>{estado.aviso}</p>}

        {!estado.cargando && !estado.error && !estado.aviso && estado.sugerencias.length === 0 && (
          <p>Pulsa «Pedir sugerencias con IA» para ver propuestas de mejora para la próxima vez.</p>
        )}
      </div>
    </section>
  )
}
