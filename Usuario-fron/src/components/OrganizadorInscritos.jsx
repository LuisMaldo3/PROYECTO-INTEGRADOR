import { useEffect, useState } from 'react'
import { actividadSeleccionada, obtenerInscritos, reemplazarInscrito, rutaOrganizador } from '../services/organizadorService'
import { OrganizadorAlumno, OrganizadorSinInscritos } from './OrganizadorAlumno'
import './Organizador.css'

export function OrganizadorInscritos({ token }) {
  const actividadId = actividadSeleccionada(window.location.hash)
  const [datos, setDatos] = useState({ cargando: true, error: '', actividad: null, inscritos: [] })
  const [revision, setRevision] = useState(0)
  const [busqueda, setBusqueda] = useState('')
  const [consulta, setConsulta] = useState('')
  const [seleccionado, setSeleccionado] = useState(null)
  const [correoNuevo, setCorreoNuevo] = useState('')
  const [motivo, setMotivo] = useState('')
  const [guardando, setGuardando] = useState(false)
  const [mensaje, setMensaje] = useState('')
  const [errorCambio, setErrorCambio] = useState('')
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
  function elegir(alumno) {
    setSeleccionado(alumno)
    setCorreoNuevo('')
    setMotivo('')
    setMensaje('')
    setErrorCambio('')
  }
  function cerrar() { if (!guardando) setSeleccionado(null) }
  async function confirmar(evento) {
    evento.preventDefault()
    if (!seleccionado || guardando) return
    setGuardando(true)
    setErrorCambio('')
    setMensaje('')
    try {
      await reemplazarInscrito(token, actividadId, seleccionado.inscripcionId, correoNuevo, motivo)
      setMensaje(`Se reemplazó la inscripción de ${seleccionado.nombre}. El límite de cupos no cambió.`)
      setSeleccionado(null)
      setBusqueda('')
      setRevision(n => n + 1)
    } catch (error) {
      setErrorCambio(error.message)
    } finally { setGuardando(false) }
  }
  const activa = datos.actividad?.estado === 'programada'
  return (
    <div className="org-pagina">
      <a className="org-regresar" href="#/organizador-actividades">← Volver a mis actividades</a>
      <div className="org-interior-hero"><span className="org-eyebrow">GESTIÓN · INSCRIPCIONES</span><h2>Estudiantes inscritos</h2><p>{datos.actividad?.nombre || 'Consulta y gestiona los participantes asignados.'}</p></div>
      <div className="org-seccion-titulo org-seccion-titulo--lista">
        <div><h2>Lista de participantes</h2><span className="org-contador">{consulta ? `${datos.inscritos.length} coincidencia(s) de ${datos.totalInscritos ?? '—'}` : `${datos.totalInscritos ?? datos.inscritos.length} inscritos reales`}</span></div>
        <div className="org-acciones"><button type="button" className="org-boton org-boton--suave" onClick={() => setRevision(n => n + 1)}>↻ Actualizar</button></div>
      </div>
      <div className="org-cupos-fijos" role="note">
        <strong>Límite definido por administración: {datos.actividad?.maximoParticipantes ?? 'por confirmar'} {datos.actividad?.unidadCupo === 'equipos' ? 'equipos' : 'estudiantes'}</strong>
        <span>Solo puedes sustituir un inscrito por otro; el máximo no se puede modificar desde aquí.</span>
      </div>
      {datos.actividad?.modalidad === 'equipos' && !datos.cargando && !datos.error && <p className="org-cupos-resumen" role="status">
        {datos.equiposConfirmados ?? 0} equipos habilitados de {datos.equiposRegistrados ?? 0} registrados. Cada integrante debe tener una inscripción individual.
      </p>}
      <div className="org-filtros org-inscritos__filtros">
        <label className="org-buscar"><span aria-hidden="true">⌕</span><input type="search" value={busqueda} maxLength={100} onChange={e => setBusqueda(e.target.value)} placeholder="Buscar estudiante, correo, equipo o área" aria-label="Buscar participantes por nombre, correo, equipo o área" /></label>
        {busqueda && <button className="org-boton org-boton--simple" type="button" onClick={() => setBusqueda('')}>Limpiar búsqueda</button>}
      </div>
      {mensaje && <div className="org-exito" role="status">{mensaje}</div>}
      {datos.cargando && <div className="org-cargando" role="status">Buscando estudiantes inscritos…</div>}
      {datos.error && <div className="org-alerta" role="alert">{datos.error}<p><button onClick={() => setRevision(n => n + 1)} type="button">Reintentar</button></p></div>}
      {!datos.cargando && !datos.error && datos.inscritos.length === 0 && (consulta
        ? <div className="org-vacio" role="status"><div className="org-vacio__icono">⌕</div><h3>No encontramos participantes</h3><p>No hay resultados para «{consulta}».</p><button type="button" className="org-boton org-boton--suave" onClick={() => setBusqueda('')}>Mostrar todos</button></div>
        : <OrganizadorSinInscritos />)}
      {!datos.cargando && !datos.error && datos.inscritos.length > 0 && <div className="org-lista"><div className="org-lista__encabezado"><span>ESTUDIANTE</span><span>ACCIÓN</span></div>{datos.inscritos.map((a, i) => <OrganizadorAlumno alumno={a} key={a.inscripcionId || a.estudianteId} indice={i} onReemplazar={elegir} bloqueado={!activa || guardando} />)}</div>}
      {!activa && datos.actividad && <p className="org-cupos-resumen">Los reemplazos solo están disponibles antes del cierre de la actividad.</p>}
      {seleccionado && <div className="org-reemplazo-fondo" role="presentation" onClick={cerrar}>
        <section className="org-reemplazo" role="dialog" aria-modal="true" aria-labelledby="titulo-reemplazo" onClick={e => e.stopPropagation()}>
          <div className="org-reemplazo__encabezado"><div><span className="org-eyebrow">SUSTITUCIÓN DE PARTICIPANTE</span><h3 id="titulo-reemplazo">Reemplazar inscrito</h3></div><button type="button" aria-label="Cerrar" className="org-boton org-boton--simple" disabled={guardando} onClick={cerrar}>✕</button></div>
          <p>Se retirará a <strong>{seleccionado.nombre}</strong> y se inscribirá en su lugar a otro estudiante registrado. <strong>El cupo máximo no cambiará.</strong></p>
          {seleccionado.nombreEquipo && <p className="org-reemplazo__equipo">Equipo: {seleccionado.nombreEquipo}. El reemplazo conservará su posición dentro de este equipo y deberá pertenecer al área correspondiente.</p>}
          <form onSubmit={confirmar} className="org-reemplazo__formulario">
            <label>Correo institucional del nuevo estudiante<input required type="email" autoComplete="off" maxLength={254} placeholder="estudiante@tecsup.edu.pe" value={correoNuevo} onChange={e => setCorreoNuevo(e.target.value)} /></label>
            <label>Motivo del reemplazo<textarea required minLength={5} maxLength={400} rows={3} placeholder="Explica brevemente el cambio" value={motivo} onChange={e => setMotivo(e.target.value)} /></label>
            {errorCambio && <p className="org-alerta" role="alert">{errorCambio}</p>}
            <div className="org-reemplazo__acciones"><button type="button" className="org-boton org-boton--simple" disabled={guardando} onClick={cerrar}>Cancelar</button><button type="submit" disabled={guardando || !correoNuevo.trim() || motivo.trim().length < 5} className="org-boton org-boton--primario">{guardando ? 'Reemplazando…' : 'Confirmar reemplazo'}</button></div>
          </form>
        </section>
      </div>}
    </div>
  )
}
