/** Presentación reutilizable para inscritos y marcado de asistencia. */
export function OrganizadorAlumno({ alumno, indice, seleccion, onCambiar }) {
  const iniciales = (alumno.nombre || '?').trim().split(/\s+/).slice(0, 2).map(p => p[0]?.toUpperCase()).join('') || '?'
  return (
    <div className="org-alumno">
      <span className="org-alumno__numero">{String(indice + 1).padStart(2, '0')}</span>
      <span className="org-alumno__avatar" aria-hidden="true">{iniciales}</span>
      <div className="org-alumno__datos">
        <strong>{alumno.nombre || 'Estudiante sin datos'}</strong>
        <span>{alumno.correo || 'Correo no disponible'}</span>
      </div>
      {onCambiar ? (
        <div className="org-alumno__control" role="group" aria-label={`Asistencia de ${alumno.nombre}`}>
          <button type="button" className={seleccion === true ? 'org-seleccion org-seleccion--presente' : 'org-seleccion'} aria-pressed={seleccion === true} onClick={() => onCambiar(alumno.estudianteId, true)}>Presente</button>
          <button type="button" className={seleccion === false ? 'org-seleccion org-seleccion--ausente' : 'org-seleccion'} aria-pressed={seleccion === false} onClick={() => onCambiar(alumno.estudianteId, false)}>Ausente</button>
        </div>
      ) : (
        <span className={`org-asistencia-tag ${!alumno.asistenciaRegistrada ? 'org-asistencia-tag--sin' : alumno.presente ? 'org-asistencia-tag--si' : 'org-asistencia-tag--no'}`}>
          {!alumno.asistenciaRegistrada ? 'Sin marcar' : alumno.presente ? 'Presente' : 'Ausente'}
        </span>
      )}
    </div>
  )
}

export function OrganizadorSinInscritos({ enlace = '#/organizador-actividades' }) {
  return (
    <div className="org-vacio" role="status">
      <div className="org-vacio__icono" aria-hidden="true">♧</div>
      <h3>Aún no hay estudiantes inscritos</h3>
      <p>Cuando los estudiantes se registren en esta actividad, sus datos aparecerán aquí. No se muestran participantes de ejemplo.</p>
      <a href={enlace} className="org-boton org-boton--suave">Volver a mis actividades</a>
    </div>
  )
}
