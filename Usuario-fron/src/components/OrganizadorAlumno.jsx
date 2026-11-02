/** Fila de inscrito con sus datos de equipo y el reemplazo autorizado por el organizador. */
export function OrganizadorAlumno({ alumno, indice, onReemplazar, bloqueado = false }) {
  const iniciales = (alumno.nombre || '?').trim().split(/\s+/).slice(0, 2).map(p => p[0]?.toUpperCase()).join('') || '?'
  return (
    <div className="org-alumno">
      <span className="org-alumno__numero">{String(indice + 1).padStart(2, '0')}</span>
      <span className="org-alumno__avatar" aria-hidden="true">{iniciales}</span>
      <div className="org-alumno__datos">
        <strong>{alumno.nombre || 'Estudiante sin datos'}</strong>
        <span>{alumno.correo || 'Correo no disponible'}</span>
        {alumno.nombreEquipo && <span><strong>Equipo:</strong> {alumno.nombreEquipo} · {alumno.rolEquipo === 'capitan' ? (alumno.equipoHabilitado ? 'Equipo habilitado' : 'Pendiente de integrantes') : 'Integrante confirmado'}</span>}
        {alumno.areaTecsup && <span><strong>Área:</strong> {alumno.areaTecsup}</span>}
        {alumno.integrantes?.length > 1 && <details><summary>Ver integrantes</summary>{alumno.integrantes.map((m, i) => <div key={`${i}-${m.correo}`}>{m.nombre} · {m.correo}</div>)}</details>}
      </div>
      {onReemplazar && <button type="button" className="org-boton org-boton--suave org-boton--sustituir" disabled={bloqueado} onClick={() => onReemplazar(alumno)} aria-label={`Reemplazar a ${alumno.nombre || 'estudiante'}`}>Reemplazar</button>}
    </div>
  )
}

export function OrganizadorSinInscritos({ enlace = '#/organizador-actividades' }) {
  return (
    <div className="org-vacio" role="status">
      <div className="org-vacio__icono" aria-hidden="true">♧</div>
      <h3>Aún no hay estudiantes inscritos</h3>
      <p>Los estudiantes registrados en esta actividad aparecerán aquí. El cupo máximo lo define únicamente administración.</p>
      <a href={enlace} className="org-boton org-boton--suave">Volver a mis actividades</a>
    </div>
  )
}
