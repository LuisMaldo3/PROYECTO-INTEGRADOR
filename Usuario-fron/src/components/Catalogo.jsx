import { useEffect, useState } from 'react'
import { obtenerCatalogo } from '../services/catalogoService'
import './Catalogo.css'

export function Catalogo() {
  const [actividades, setActividades] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [version, setVersion] = useState(0)

  useEffect(() => {
    const ctrl = new AbortController()
    setCargando(true)
    setError('')
    obtenerCatalogo(ctrl.signal)
      .then(datos => { if (!ctrl.signal.aborted) setActividades(datos) })
      .catch(e => { if (!ctrl.signal.aborted) setError(e.message) })
      .finally(() => { if (!ctrl.signal.aborted) setCargando(false) })
    return () => ctrl.abort()
  }, [version])

  return <main className="vy-catalogo">
    <div className="vy-catalogo__intro">
      <div><span className="vy-catalogo__eyebrow">VYNK · EXPERIENCIAS</span><h2>Explora tu próxima actividad<span>.</span></h2><p>Actividades publicadas por el equipo organizador.</p></div>
      <button type="button" onClick={() => setVersion(v => v + 1)}>↻ Actualizar</button>
    </div>
    {cargando ? <div className="vy-catalogo__estado">Cargando actividades…</div> :
      error ? <div className="vy-catalogo__estado" role="alert"><h3>No pudimos cargar el catálogo</h3><p>{error}</p><button onClick={() => setVersion(v => v + 1)}>Reintentar</button></div> :
      !actividades.length ? <div className="vy-catalogo__estado"><span className="vy-catalogo__estrella">✦</span><h3>Pronto habrá actividades para ti</h3><p>Cuando el administrador publique actividades, aparecerán aquí automáticamente.</p></div> :
      <div className="vy-catalogo__grid">{actividades.map(a => <article className="vy-catalogo__card" key={a.id}>
        <div className="vy-catalogo__card-top"><span className="vy-catalogo__pill">ACTIVIDAD</span><span className="vy-catalogo__disponible">● Disponible</span></div>
        <h3>{a.nombre}</h3><p>{a.descripcion || 'Conoce esta actividad de VYNK.'}</p>
        <div className="vy-catalogo__detalles"><span>📅 {a.fecha || 'Fecha por confirmar'}</span><span>◷ {a.horaInicio || 'Hora por confirmar'}{a.horaFin ? ` – ${a.horaFin}` : ''}</span><span>◎ {a.cuposDisponibles == null ? 'Cupos por confirmar' : `${a.cuposDisponibles} cupos disponibles`}</span></div>
        <div className="vy-catalogo__pie">Publicada por VYNK <span>✦</span></div>
      </article>)}</div>}
  </main>
}
