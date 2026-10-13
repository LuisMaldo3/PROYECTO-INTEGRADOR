
import { useState } from 'react'
import { leerSesion } from '../services/authService'
import { explicarCruce } from '../services/crucesService'

export function AvisoCruces({ cruces = [], error = '' }) {
  const [explicaciones, setExplicaciones] = useState({})
  const [cargando, setCargando] = useState('')

  async function explicar(id) {
    const token = leerSesion()?.token

    if (!token) {
      setExplicaciones(prev => ({
        ...prev,
        [id]: {
          texto: 'Inicia sesión para consultar la explicación.',
          origen: 'automatico'
        }
      }))
      return
    }

    setCargando(id)

    try {
      const respuesta = await explicarCruce(token, id)

      setExplicaciones(prev => ({
        ...prev,
        [id]: respuesta
      }))
    } catch (error) {
      console.error('Error al explicar el cruce:', error)

      setExplicaciones(prev => ({
        ...prev,
        [id]: {
          texto: 'No se pudo obtener la explicación. Revisa el horario de ambas actividades.',
          origen: 'automatico'
        }
      }))
    } finally {
      setCargando('')
    }
  }

  if (error) {
    return (
      <div className="ag-conflicts" role="status">
        {error}
      </div>
    )
  }

  if (!cruces.length) return null

  return (
    <section className="ag-conflicts" aria-label="Cruces de horario">
      <h3>
        ⚠ {cruces.length}{' '}
        {cruces.length === 1
          ? 'cruce detectado'
          : 'cruces detectados'}
      </h3>

      <p>
        Algunas de tus actividades inscritas coinciden.
        Revisa los horarios antes de asistir.
      </p>

      {cruces.map(c => (
        <div className="ag-conflict-item" key={c.id}>
          <strong>
            {c.actividadA} · {c.actividadB}
          </strong>

          <small>
            {c.fecha} · {c.desde}–{c.hasta} · {c.minutos} min
          </small>

          <button
            type="button"
            disabled={cargando === c.id}
            onClick={() => explicar(c.id)}
          >
            {cargando === c.id
              ? 'Consultando…'
              : '✦ Explicar con IA'}
          </button>

          {explicaciones[c.id] && (
            <p role="status">
              {explicaciones[c.id].texto}

              <small>
                {explicaciones[c.id].origen === 'ia'
                  ? 'Explicación generada con IA'
                  : 'Explicación automática de respaldo'}
              </small>
            </p>
          )}
        </div>
      ))}
    </section>
  )
}
