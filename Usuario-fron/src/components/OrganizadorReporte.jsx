import { useEffect, useState } from 'react'
import { actividadSeleccionada, obtenerMisActividades, reportarProblema } from '../services/organizadorService'
import './Organizador.css'

const CATEGORIAS = [
  ['logistica', 'Logística y organización'],
  ['horario', 'Horarios y programación'],
  ['ubicacion', 'Espacios y ubicación'],
  ['materiales', 'Equipos y materiales'],
  ['otro', 'Otro problema'],
]

export function OrganizadorReporte({ token }) {
  const actividadId = actividadSeleccionada(window.location.hash)
  const [actividad, setActividad] = useState(null)
  const [errorActividad, setErrorActividad] = useState('')
  const [categoria, setCategoria] = useState('')
  const [descripcion, setDescripcion] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState('')
  const [reporte, setReporte] = useState(null)

  useEffect(() => {
    const ctrl = new AbortController()
    obtenerMisActividades(token, ctrl.signal)
      .then(items => {
        const seleccionada = items.find(a => a.id === actividadId)
        if (!ctrl.signal.aborted) {
          if (!seleccionada) setErrorActividad('La actividad no existe o no está asignada a tu cuenta.')
          else setActividad(seleccionada)
        }
      })
      .catch(e => { if (!ctrl.signal.aborted) setErrorActividad(e.message) })
    return () => ctrl.abort()
  }, [actividadId, token])

  const enviar = async e => {
    e.preventDefault()
    setError('')
    if (!categoria) return setError('Selecciona el tipo de problema.')
    if (descripcion.trim().length < 15 || descripcion.trim().length > 2000) return setError('Escribe entre 15 y 2000 caracteres.')
    setEnviando(true)
    try {
      const res = await reportarProblema(token, actividadId, categoria, descripcion)
      setReporte(res)
      setDescripcion('')
      setCategoria('')
    } catch (e) { setError(e.message) } finally { setEnviando(false) }
  }

  return <div className="org-pagina">
    <a className="org-regresar" href="#/organizador-actividades">← Volver a mis actividades</a>
    <div className="org-interior-hero org-interior-hero--reporte"><span className="org-eyebrow">GESTIÓN · INCIDENCIAS</span><h2>Reportar un problema</h2><p>Comunica una incidencia para que el equipo responsable pueda revisarla.</p></div>
    {errorActividad && <div className="org-alerta" role="alert">{errorActividad}</div>}
    {reporte ? <div className="org-confirmacion" role="status"><span className="org-confirmacion__marca">✓</span><h3>Reporte enviado correctamente</h3><p>Tu incidencia ha quedado registrada en el sistema. Puedes utilizar este identificador como referencia:</p><code>{reporte.id}</code><div><a href="#/organizador-actividades" className="org-boton org-boton--primario">Volver a mis actividades</a><button type="button" className="org-boton org-boton--suave" onClick={() => setReporte(null)}>Enviar otro reporte</button></div></div> : <div className="org-formulario-layout">
      <form className="org-formulario" onSubmit={enviar}>
        <label className="org-campo"><span>Actividad vinculada</span><input readOnly value={actividad?.nombre || 'Cargando actividad…'} aria-label="Actividad vinculada" /></label>
        <label className="org-campo"><span>Tipo de problema <b>*</b></span><select value={categoria} required onChange={e => setCategoria(e.target.value)}><option value="">Selecciona una categoría</option>{CATEGORIAS.map(([id, titulo]) => <option value={id} key={id}>{titulo}</option>)}</select></label>
        <label className="org-campo"><span>Describe lo ocurrido <b>*</b></span><textarea rows={7} minLength={15} maxLength={2000} value={descripcion} required onChange={e => setDescripcion(e.target.value)} placeholder="¿Qué ocurrió? Incluye información útil como lugar, hora y qué necesitas resolver." /><small>{descripcion.trim().length} / 2000 caracteres · mínimo 15</small></label>
        {error && <div className="org-alerta" role="alert">{error}</div>}
        <button className="org-boton org-boton--primario org-boton--grande" disabled={!actividad || enviando || !categoria || descripcion.trim().length < 15} type="submit">{enviando ? 'Enviando reporte…' : '↗ Enviar reporte'}</button>
      </form>
      <aside className="org-ayuda"><span aria-hidden="true">ⓘ</span><h3>¿Qué incluir en tu reporte?</h3><p>Explica el problema con claridad para que pueda revisarse. Si afecta el desarrollo de la actividad, incluye el horario y la ubicación.</p><p>El reporte quedará asociado a tu cuenta de organizador y a la actividad seleccionada.</p></aside>
    </div>}
  </div>
}
