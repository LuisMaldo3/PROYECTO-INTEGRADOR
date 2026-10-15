import { useEffect, useState } from 'react'
import { actividadSeleccionada, obtenerInscritos, rutaOrganizador } from '../services/organizadorService'
import { OrganizadorAlumno, OrganizadorSinInscritos } from './OrganizadorAlumno'
import './Organizador.css'

export function OrganizadorInscritos({ token }) {
  const actividadId = actividadSeleccionada(window.location.hash)
  const [datos, setDatos] = useState({ cargando: true, error: '', actividad: null, inscritos: [] })
  const [revision, setRevision] = useState(0)
  useEffect(() => {
    const ctrl = new AbortController()
    obtenerInscritos(token, actividadId, ctrl.signal)
      .then(res => { if (!ctrl.signal.aborted) setDatos({ cargando: false, error: '', ...res }) })
      .catch(e => { if (!ctrl.signal.aborted) setDatos({ cargando: false, error: e.message, actividad: null, inscritos: [] }) })
    return () => ctrl.abort()
  }, [token, actividadId, revision])
  return (
    <div className="org-pagina">
      <a className="org-regresar" href="#/organizador-actividades">← Volver a mis actividades</a>
      <div className="org-interior-hero"><span className="org-eyebrow">US-51 · PARTICIPANTES</span><h2>Estudiantes inscritos</h2><p>{datos.actividad?.nombre || 'Consulta la lista de esta actividad y su estado de asistencia.'}</p></div>
      <div className="org-seccion-titulo org-seccion-titulo--lista"><div><h2>Lista de participantes</h2><span className="org-contador">{datos.inscritos.length} inscritos reales</span></div><div className="org-acciones"><button type="button" className="org-boton org-boton--suave" onClick={() => setRevision(n => n + 1)}>↻ Actualizar</button>{datos.actividad && <a className="org-boton org-boton--primario" href={rutaOrganizador('organizador-asistencia', actividadId)}>Registrar asistencia →</a>}</div></div>
      {datos.cargando && <div className="org-cargando" role="status">Buscando estudiantes inscritos…</div>}
      {datos.error && <div className="org-alerta" role="alert">{datos.error}<p><button onClick={() => setRevision(n => n + 1)} type="button">Reintentar</button></p></div>}
      {!datos.cargando && !datos.error && datos.inscritos.length === 0 && <OrganizadorSinInscritos />}
      {!datos.cargando && !datos.error && datos.inscritos.length > 0 && <div className="org-lista"><div className="org-lista__encabezado"><span>ESTUDIANTE</span><span>ASISTENCIA</span></div>{datos.inscritos.map((a, i) => <OrganizadorAlumno alumno={a} key={a.inscripcionId || a.estudianteId} indice={i} />)}</div>}
    </div>
  )
}
