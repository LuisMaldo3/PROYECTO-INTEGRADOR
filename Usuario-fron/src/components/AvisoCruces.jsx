import { useState } from 'react'
import { leerSesion } from '../services/authService'
import { explicarCruce } from '../services/crucesService'
import { nombreDia } from '../services/presentacion'
import { useAlternativasCruces } from '../hooks/useAlternativasCruces'
import Icono from './Iconos'

/** Aviso de cruces de horario de la agenda, con una explicación (IA o automática) de cada uno. */
export function AvisoCruces({ cruces = [], error = '' }) {
  const [explicaciones, setExplicaciones] = useState({})
  const [cargando, setCargando] = useState('')
  const { resultados, reintentar } = useAlternativasCruces(cruces)

  async function explicar(id) {
    const token = leerSesion()?.token
    if (!token) {
      setExplicaciones((prev) => ({ ...prev, [id]: { texto: 'Inicia sesión para consultar la explicación.', origen: 'automatico' } }))
      return
    }
    setCargando(id)
    try {
      const respuesta = await explicarCruce(token, id)
      setExplicaciones((prev) => ({ ...prev, [id]: respuesta }))
    } catch {
      setExplicaciones((prev) => ({
        ...prev,
        [id]: { texto: 'No se pudo obtener la explicación. Revisa el horario de ambas actividades.', origen: 'automatico' },
      }))
    } finally {
      setCargando('')
    }
  }

  if (error) {
    return (
      <p className="cruces cruces--nota" role="status">
        <Icono nombre="alerta" tamano={16} /> {error}
      </p>
    )
  }
  if (!cruces.length) return null

  return (
    <section className="cruces" aria-label="Cruces de horario">
      <header className="cruces__cabecera">
        <span className="cruces__icono">
          <Icono nombre="alerta" tamano={20} />
        </span>
        <div>
          <h3>{cruces.length === 1 ? '1 cruce de horario detectado' : `${cruces.length} cruces de horario detectados`}</h3>
          <p>Algunas de tus actividades inscritas coinciden. Revisa los horarios antes de asistir.</p>
        </div>
      </header>

      <ul className="cruces__lista">
        {cruces.map((c) => (
          <li key={c.id} className="cruces__item">
            <div className="cruces__texto">
              <strong>
                {c.actividadA} <span>y</span> {c.actividadB}
              </strong>
              <small>
                {nombreDia(c.fecha)} · {c.desde} – {c.hasta} · {c.minutos} min
              </small>
            </div>
            <button type="button" className="boton-secundario boton-chico" disabled={cargando === c.id} onClick={() => explicar(c.id)}>
              <Icono nombre="destellos" tamano={16} />
              {cargando === c.id ? 'Consultando…' : 'Explicar con IA'}
            </button>
            <div className="cruces__alternativas" aria-label={`Alternativas para ${c.actividadA} y ${c.actividadB}`}>
              <div className="cruces__alternativas-titulo">
                <strong><Icono nombre="calendario" tamano={17} /> Actividades que sí puedes realizar</strong>
                <span>{resultados[c.id]?.cargando ? 'Buscando…' : `${resultados[c.id]?.alternativas?.length || 0} opciones`}</span>
              </div>
              {resultados[c.id]?.cargando && <p role="status">Buscando actividades con cupos y horarios compatibles…</p>}
              {resultados[c.id]?.error && (
                <p role="alert">{resultados[c.id].error}{' '}
                  <button type="button" className="cruces__reintentar" onClick={() => reintentar(c.id)}>Reintentar</button>
                </p>
              )}
              {!resultados[c.id]?.cargando && !resultados[c.id]?.error && resultados[c.id] && (
                resultados[c.id].alternativas.length ? (
                  <ul className="cruces__opciones">
                    {resultados[c.id].alternativas.map((a) => (
                      <li key={a.id} className="cruces__opcion">
                        <div><strong>{a.nombre || 'Actividad'}</strong><small>
                          <Icono nombre="calendario" tamano={14} /> {nombreDia(a.fecha)} · {a.horaInicio} – {a.horaFin}
                        </small><small><Icono nombre="ubicacion" tamano={14} /> {a.ubicacionId || 'Ubicación por confirmar'}</small></div>
                        <span>Sin cruce</span>
                      </li>
                    ))}
                  </ul>
                ) : <p>Por ahora no hay actividades compatibles con cupos disponibles.</p>
              )}
              <small className="cruces__nota-alternativas">Son sugerencias, no nuevas inscripciones. Consulta el catálogo para inscribirte.</small>
            </div>
            {explicaciones[c.id] && (
              <div className="cruces__explicacion" role="status">
                <p>{explicaciones[c.id].texto}</p>
                <small>{explicaciones[c.id].origen === 'ia' ? 'Explicación generada con IA' : 'Explicación automática de respaldo'}</small>
              </div>
            )}
          </li>
        ))}
      </ul>
    </section>
  )
}
