import { useEffect, useMemo, useState } from 'react'
import { actividadSeleccionada, guardarAsistencia, obtenerInscritos, rutaOrganizador } from '../services/organizadorService'
import { OrganizadorAlumno, OrganizadorSinInscritos } from './OrganizadorAlumno'
import './Organizador.css'

export function OrganizadorAsistencia({ token }) {
  const actividadId = actividadSeleccionada(window.location.hash)
  const [datos, setDatos] = useState({ cargando: true, error: '', actividad: null, inscritos: [] })
  const [marcas, setMarcas] = useState({})
  const [guardando, setGuardando] = useState(false)
  const [resultado, setResultado] = useState('')
  const [errorGuardar, setErrorGuardar] = useState('')
  const [revision, setRevision] = useState(0)

  useEffect(() => {
    const ctrl = new AbortController()
    obtenerInscritos(token, actividadId, ctrl.signal)
      .then(res => {
        if (ctrl.signal.aborted) return
        setDatos({ cargando: false, error: '', ...res })
        setMarcas(Object.fromEntries(res.inscritos.filter(a => a.asistenciaRegistrada).map(a => [a.estudianteId, Boolean(a.presente)])))
        setResultado('')
      })
      .catch(e => { if (!ctrl.signal.aborted) setDatos({ cargando: false, error: e.message, actividad: null, inscritos: [] }) })
    return () => ctrl.abort()
  }, [token, actividadId, revision])

  const total = datos.inscritos.length
  const marcados = useMemo(() => datos.inscritos.filter(a => typeof marcas[a.estudianteId] === 'boolean').length, [datos.inscritos, marcas])
  const presentes = useMemo(() => datos.inscritos.filter(a => marcas[a.estudianteId] === true).length, [datos.inscritos, marcas])
  const completo = total > 0 && marcados === total
  const cancelada = String(datos.actividad?.estado || '').toLowerCase() === 'cancelada'
  const cambiar = (id, presente) => { setMarcas(actual => ({ ...actual, [id]: presente })); setResultado(''); setErrorGuardar('') }

  const guardar = async () => {
    if (!completo || guardando || cancelada) return
    setGuardando(true)
    setErrorGuardar('')
    setResultado('')
    try {
      const guardado = await guardarAsistencia(token, actividadId, datos.inscritos.map(a => ({ estudianteId: a.estudianteId, presente: marcas[a.estudianteId] })))
      setResultado(`${guardado.guardados} registros de asistencia guardados correctamente.`)
    } catch (error) {
      setErrorGuardar(error.message)
    } finally {
      setGuardando(false)
    }
  }

  return <div className="org-pagina">
    <a className="org-regresar" href={rutaOrganizador('organizador-inscritos', actividadId)}>← Volver a inscritos</a>
    <div className="org-interior-hero org-interior-hero--asistencia"><span className="org-eyebrow">CONTROL · CONTROL DE PARTICIPACIÓN</span><h2>Registrar asistencia</h2><p>{datos.actividad?.nombre || 'Marca a cada estudiante como presente o ausente y guarda los cambios.'}</p></div>
    {datos.cargando && <div className="org-cargando" role="status">Cargando estudiantes y marcas existentes…</div>}
    {datos.error && <div className="org-alerta" role="alert">{datos.error}<p><button type="button" onClick={() => setRevision(n => n + 1)}>Reintentar</button></p></div>}
    {!datos.cargando && !datos.error && total === 0 && <OrganizadorSinInscritos />}
    {!datos.cargando && !datos.error && total > 0 && <>
      <div className="org-asistencia-resumen"><div><span>Inscritos</span><strong>{total}</strong></div><div><span>Marcados</span><strong>{marcados}</strong></div><div><span>Presentes</span><strong>{presentes}</strong></div><div><span>Pendientes</span><strong>{total - marcados}</strong></div></div>
      <div className="org-asistencia-intro"><p>Selecciona <strong>Presente</strong> o <strong>Ausente</strong> para cada estudiante. Debes completar toda la lista antes de guardar.</p><button type="button" onClick={() => { setMarcas(Object.fromEntries(datos.inscritos.map(a => [a.estudianteId, true]))); setResultado('') }}>Marcar todos presentes</button></div>
      <div className="org-lista">{datos.inscritos.map((a, i) => <OrganizadorAlumno key={a.inscripcionId || a.estudianteId} alumno={a} indice={i} seleccion={marcas[a.estudianteId]} onCambiar={cambiar} />)}</div>
      <div className="org-pie-guardar"><div><strong>{marcados} de {total} estudiantes marcados</strong><span>{completo ? 'Lista lista para guardar' : 'Completa los registros pendientes'}</span></div><button type="button" className="org-boton org-boton--primario org-boton--grande" disabled={!completo || guardando || cancelada} onClick={guardar}>{guardando ? 'Guardando…' : '✓ Guardar asistencia'}</button></div>
      {cancelada && <div className="org-alerta" role="alert">No se puede registrar asistencia para una actividad cancelada.</div>}
      {errorGuardar && <div className="org-alerta" role="alert">{errorGuardar}</div>}
      {resultado && <div className="org-exito" role="status">✓ {resultado}</div>}
    </>}
  </div>
}
