import { useEffect, useState } from 'react'
import { actividadSeleccionada, obtenerDetalleActividad, obtenerResultadosOrganizador, enviarResultadosOrganizador, rutaOrganizador } from '../services/organizadorService'
import './Organizador.css'
import './VynkUpgrade.css'

/** El organizador registra puestos; la aprobación y publicación corresponden al administrador. */
export function OrganizadorResultados({ token }) {
  const actividadId = actividadSeleccionada(window.location.hash)
  const [datos, setDatos] = useState({ cargando: true, error: '', actividad: null, resultado: null })
  const [puestos, setPuestos] = useState([])
  const [guardando, setGuardando] = useState(false)
  const [aviso, setAviso] = useState('')
  const [revision, setRevision] = useState(0)

  useEffect(() => {
    const ctrl = new AbortController()
    Promise.all([obtenerDetalleActividad(token, actividadId, ctrl.signal), obtenerResultadosOrganizador(token, actividadId, ctrl.signal)])
      .then(([actividad, resultado]) => {
        if (ctrl.signal.aborted) return
        setDatos({ cargando: false, error: '', actividad, resultado })
        setPuestos((resultado.posiciones || []).map(p => ({ inscripcionId: p.inscripcionId, puesto: p.puesto, puntos: p.puntos ?? 0 })))
      })
      .catch(e => { if (!ctrl.signal.aborted) setDatos({ cargando: false, error: e.message, actividad: null, resultado: null }) })
    return () => ctrl.abort()
  }, [token, actividadId, revision])

  const participantes = datos.resultado?.participantes || []
  const publicado = datos.resultado?.estado === 'publicado'
  const actualizarPuesto = (index, campo, valor) => setPuestos(prev => prev.map((p, i) => i === index ? { ...p, [campo]: valor } : p))
  const disponibles = participantes.filter(p => !puestos.some(x => x.inscripcionId === p.inscripcionId))
  const valido = puestos.length > 0 && puestos.every(p => p.inscripcionId && Number.isInteger(p.puesto) && p.puesto > 0 && p.puesto <= 100)
    && puestos.every(p => Number.isInteger(p.puntos) && p.puntos >= 0 && p.puntos <= 10000)
    && new Set(puestos.map(p => p.puesto)).size === puestos.length
    && new Set(puestos.map(p => p.inscripcionId)).size === puestos.length

  async function enviar(e) {
    e.preventDefault()
    if (!valido || guardando || publicado) return
    if (!window.confirm('¿Enviar estos puestos a revisión? El administrador deberá validarlos antes de publicarlos.')) return
    setGuardando(true); setAviso('')
    try {
      await enviarResultadosOrganizador(token, actividadId, puestos)
      setAviso('Propuesta registrada. Los resultados están pendientes de la aprobación del administrador.')
      setRevision(v => v + 1)
    } catch (error) { setAviso(error.message) }
    finally { setGuardando(false) }
  }

  return <div className="org-pagina org-resultados-up">
    <a href={rutaOrganizador('organizador-detalle', actividadId)} className="org-regresar">← Volver a la competencia</a>
    <section className="org-interior-hero vynk-up-hero"><span className="org-eyebrow">ORGANIZACIÓN · T&amp;C WEEK</span>
      <h2>Registrar resultados</h2><p>Asigna posiciones y propone puntos a los inscritos. El administrador debe validarlos antes de publicarlos.</p>
    </section>
    {datos.cargando && <div className="org-cargando" role="status">Cargando participantes y resultados…</div>}
    {datos.error && <div className="org-alerta" role="alert">{datos.error}<button type="button" onClick={() => setRevision(n => n + 1)}>Reintentar</button></div>}
    {!datos.cargando && !datos.error && <div className="org-resultados-up__panel">
      <div className="org-resultados-up__head"><div><span className="vynk-up-eyebrow">COMPETENCIA</span><h3>{datos.actividad?.nombre}</h3><p>Solo puedes registrar resultados de tus competencias asignadas.</p></div>
        <span className={`vynk-up-status ${publicado ? 'vynk-up-status--ok' : ''}`}>{publicado ? 'Publicado oficialmente' : datos.resultado?.estado === 'pendiente' ? 'Pendiente de validación' : 'Sin resultados'}</span>
      </div>
      <div className="org-resultados-up__meta"><span>{participantes.length} inscripciones activas</span><span>{datos.actividad?.modalidad === 'equipos' ? 'Competencia por equipos' : 'Competencia individual'}</span></div>
      {!participantes.length ? <p className="tc-vacio">Todavía no hay inscritos. Cuando existan participantes podrás registrar posiciones.</p>
        : <form onSubmit={enviar} className="org-resultados-up__form">
          {puestos.map((p, i) => <div className="org-resultados-up__fila" key={`${i}-${p.inscripcionId}`}>
            <label>Posición<input type="number" min="1" max="100" required value={p.puesto} disabled={publicado || guardando} onChange={e => actualizarPuesto(i, 'puesto', e.target.value === '' ? 0 : Number(e.target.value))}/></label>
            <label>Participante o equipo<select required disabled={publicado || guardando} value={p.inscripcionId} onChange={e => actualizarPuesto(i, 'inscripcionId', e.target.value)}>
              {participantes.map(item => <option key={item.inscripcionId} value={item.inscripcionId}>{item.nombre || 'Participante'}{item.areaTecsup ? ` · ${item.areaTecsup}` : ''}</option>)}
            </select></label>
            <label>Puntos<input type="number" min="0" max="10000" required value={p.puntos ?? 0} disabled={publicado || guardando}
              onChange={e => actualizarPuesto(i, 'puntos', e.target.value === '' ? 0 : Number(e.target.value))}/></label>
            {!publicado && <button type="button" className="org-boton org-boton--suave" onClick={() => setPuestos(prev => prev.filter((_, x) => x !== i))}>Quitar</button>}
          </div>)}
          {!publicado && <div className="org-resultados-up__acciones"><button type="button" className="org-boton org-boton--suave" disabled={!disponibles.length || guardando} onClick={() => setPuestos(prev => [...prev, { inscripcionId: disponibles[0].inscripcionId, puesto: prev.length ? Math.max(...prev.map(x => x.puesto)) + 1 : 1, puntos: datos.actividad?.puntosPorPuesto?.[prev.length] ?? 0 }])}>+ Agregar posición</button>
            <button type="submit" className="org-boton org-boton--primario" disabled={!valido || guardando}>{guardando ? 'Enviando…' : 'Enviar al administrador'}</button></div>}
        </form>}
      <p className="org-resultados-up__nota">Las posiciones no serán visibles para los estudiantes hasta que el administrador las valide y publique. El cálculo acumulado por las cinco áreas corresponde al Sprint 3.</p>
      {aviso && <p className="org-resultados-up__aviso" role="status">{aviso}</p>}
    </div>}
  </div>
}
