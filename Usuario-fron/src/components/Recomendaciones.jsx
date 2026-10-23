import { useEffect, useState } from 'react'
import { cargarRecomendaciones } from '../services/recomendacionesService'
import { TarjetaRecomendada } from './TarjetaRecomendada'
import './Recomendaciones.css'
export function Recomendaciones() {
  const [datos, setDatos] = useState(null)
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(true)
  const [version, setVersion] = useState(0)
  useEffect(() => {
    const controller = new AbortController()
    setCargando(true); setError('')
    cargarRecomendaciones(controller.signal).then(setDatos).catch(e => { if (!controller.signal.aborted) setError(e.message) }).finally(() => { if (!controller.signal.aborted) setCargando(false) })
    return () => controller.abort()
  }, [version])
  return <main className="rec-pagina">
    <header className="rec-hero"><span className="rec-etiqueta">VYNK · DESCUBRE</span><h2>Actividades que conectan contigo.</h2><p>Explora experiencias elegidas según tus intereses y las actividades publicadas.</p><div className="rec-hero__acciones"><span>{datos?.origen === 'ia' ? '✦ Recomendaciones con Gemini' : 'Sugerencias personalizadas'}</span><button onClick={() => setVersion(v => v+1)} disabled={cargando}>↻ Actualizar</button></div></header>
    {cargando && <p className="rec-estado" role="status">Analizando tus intereses y actividades…</p>}
    {error && <div className="rec-estado" role="alert">{error}<button onClick={() => setVersion(v=>v+1)}>Reintentar</button></div>}
    {!cargando && !error && datos && <><p className="rec-aviso" role="status">{datos.aviso} {datos.origen === 'ia_no_disponible' && 'Estas sugerencias NO fueron generadas por IA.'}</p>
      {datos.origen === 'sin_intereses' && <a className="rec-enlace" href="#/intereses">Elegir mis intereses →</a>}
      {datos.origen === 'sin_horario' && <div className="rec-sin-horario"><strong>Primero confirma tu horario semanal</strong><p>Sube una imagen de tus clases, corrige las horas detectadas y guárdalas en Mi agenda → Mis horas libres.</p><a className="rec-enlace" href="#/agenda">Configurar mi disponibilidad →</a></div>}
      {datos.origen !== 'sin_horario' && !datos.actividades?.length && <p className="rec-estado">Todavía no hay actividades compatibles con tus horarios confirmados. Consulta el catálogo cuando se publiquen nuevas actividades.</p>}
      <div className="rec-grid">{datos.actividades?.map((a,i) => <TarjetaRecomendada key={a.id} actividad={a} posicion={i+1}/>)}</div>
    </>}
  </main>
}
