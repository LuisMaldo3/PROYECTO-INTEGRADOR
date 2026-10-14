import { useState } from 'react'
import { leerSesion } from '../services/authService'
import { explicarCruce } from '../services/crucesService'
import { nombreDia } from '../services/presentacion'
import Icono from './Iconos'

/** Aviso de cruces de horario de la agenda, con una explicación (IA o automática) de cada uno. */
export function AvisoCruces({ cruces = [], error = '' }) {
  const [explicaciones, setExplicaciones] = useState({})
  const [cargando, setCargando] = useState('')

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
