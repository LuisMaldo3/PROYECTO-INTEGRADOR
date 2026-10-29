import { useEffect, useState } from 'react'
import { actividadSeleccionada, obtenerInscritos, rutaOrganizador } from '../services/organizadorService'
import { OrganizadorAlumno, OrganizadorSinInscritos } from './OrganizadorAlumno'
import './Organizador.css'

export function OrganizadorInscritos({ token }) {
  const actividadId = actividadSeleccionada(window.location.hash)
  const [datos, setDatos] = useState({ cargando: true, error: '', actividad: null, inscritos: [] })
  const [revision, setRevision] = useState(0)
  const [busqueda, setBusqueda] = useState('')
  const [consulta, setConsulta] = useState('')
  useEffect(() => {
    const temporizador = window.setTimeout(() => setConsulta(busqueda.trim()), 350)
    return () => window.clearTimeout(temporizador)
  }, [busqueda])
  useEffect(() => {
    const ctrl = new AbortController()
    obtenerInscritos(token, actividadId, ctrl.signal, consulta)
      .then(res => { if (!ctrl.signal.aborted) setDatos({ cargando: false, error: '', ...res }) })
      .catch(e => { if (!ctrl.signal.aborted) setDatos({ cargando: false, error: e.message, actividad: null, inscritos: [] }) })
    return () => ctrl.abort()
  }, [token, actividadId, revision, consulta])
  return (
    <div className="org-pagina">
      <a className="org-regresar" href="#/organizador-actividades">← Volver a mis actividades</a>
      <div className="org-interior-hero"><span className="org-eyebrow">GESTIÓN · PARTICIPANTES</span><h2>Estudiantes inscritos</h2><p>{datos.actividad?.nombre || 'Consulta la lista de esta actividad y su estado de asistencia.'}</p></div>
      <div className="org-seccion-titulo org-seccion-titulo--lista"><div><h2>Lista de participantes</h2><span className="org-contador">{consulta ? `${datos.inscritos.length} coincidencia(s) de ${datos.totalInscritos ?? '—'}` : `${datos.totalInscritos ?? datos.inscritos.length} inscritos reales`}</span></div><div className="org-acciones"><button type="button" className="org-boton org-boton--suave" onClick={() => setRevision(n => n + 1)}>↻ Actualizar</button>{datos.actividad && <a className="org-boton org-boton--primario" href={rutaOrganizador('organizador-asistencia', actividadId)}>Registrar asistencia →</a>}</div></div>
      <div className="org-filtros org-inscritos__filtros">
        <label className="org-buscar"><span aria-hidden="true">⌕</span><input type="search" value={busqueda} maxLength={100} onChange={e => setBusqueda(e.target.value)} placeholder="Buscar por nombre o correo institucional" aria-label="Buscar participantes por nombre o correo" /></label>
        {busqueda && <button className="org-boton org-boton--simple" type="button" onClick={() => setBusqueda('')}>Limpiar búsqueda</button>}
      </div>
      {datos.cargando && <div className="org-cargando" role="status">Buscando estudiantes inscritos…</div>}
      {datos.error && <div className="org-alerta" role="alert">{datos.error}<p><button onClick={() => setRevision(n => n + 1)} type="button">Reintentar</button></p></div>}
      {!datos.cargando && !datos.error && datos.inscritos.length === 0 && (consulta
        ? <div className="org-vacio" role="status"><div className="org-vacio__icono">⌕</div><h3>No encontramos participantes</h3><p>No hay resultados para «{consulta}». Prueba un nombre o un correo distinto.</p><button type="button" className="org-boton org-boton--suave" onClick={() => setBusqueda('')}>Mostrar todos</button></div>
        : <OrganizadorSinInscritos />)}
      {!datos.cargando && !datos.error && datos.inscritos.length > 0 && <div className="org-lista"><div className="org-lista__encabezado"><span>ESTUDIANTE</span><span>ASISTENCIA</span></div>{datos.inscritos.map((a, i) => <OrganizadorAlumno alumno={a} key={a.inscripcionId || a.estudianteId} indice={i} />)}</div>}
    </div>
  )
}
