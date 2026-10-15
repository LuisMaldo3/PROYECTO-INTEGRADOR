import { rutaOrganizador } from '../services/organizadorService'

const estadoTexto = { programada: 'Programada', cancelada: 'Cancelada', finalizada: 'Finalizada', inactiva: 'Inactiva' }

function formatoFecha(fecha) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(fecha || '')) return 'Fecha por confirmar'
  const [year, month, day] = fecha.split('-').map(Number)
  return new Intl.DateTimeFormat('es-PE', { day: 'numeric', month: 'short', year: 'numeric', timeZone: 'UTC' })
    .format(new Date(Date.UTC(year, month - 1, day)))
}

export function OrganizadorTarjeta({ actividad }) {
  const a = actividad
  const inscritos = Number(a.inscritosRegistrados || 0)
  const maximo = Number(a.maximoParticipantes) || 0
  const progreso = maximo ? Math.min(100, Math.round((inscritos / maximo) * 100)) : 0
  const estado = String(a.estado || 'programada').toLowerCase()
  return (
    <article className="org-tarjeta">
      <div className="org-tarjeta__arriba">
        <span className={`org-estado ${estado === 'cancelada' ? 'org-estado--cancelada' : ''}`}>
          <span className="org-punto" aria-hidden="true" />{estadoTexto[estado] || estado}
        </span>
        <span className="org-tarjeta__id">ACTIVIDAD</span>
      </div>
      <h3>{a.nombre || 'Actividad sin nombre'}</h3>
      <p className="org-tarjeta__descripcion">{a.descripcion || 'Consulta los detalles y gestiona los participantes de tu actividad.'}</p>
      <div className="org-metadatos">
        <span>◷ {formatoFecha(a.fecha)}</span>
        <span>◴ {a.horaInicio || '—'}{a.horaFin ? ` – ${a.horaFin}` : ''}</span>
        <span>⌖ {a.ubicacionId || 'Lugar por confirmar'}</span>
      </div>
      <div className="org-capacidad">
        <div><strong>{inscritos}</strong> <span>inscritos {maximo ? `/ ${maximo} cupos` : ''}</span></div>
        {maximo > 0 && <div className="org-barra"><span style={{ width: `${progreso}%` }} /></div>}
      </div>
      <div className="org-tarjeta__acciones">
        <a className="org-boton org-boton--primario" href={rutaOrganizador('organizador-inscritos', a.id)}>
          Ver estudiantes <span aria-hidden="true">↗</span>
        </a>
        <a className="org-boton org-boton--suave" href={rutaOrganizador('organizador-asistencia', a.id)}>Asistencia</a>
        <a className="org-boton org-boton--simple" href={rutaOrganizador('organizador-reporte', a.id)}>Reportar</a>
      </div>
    </article>
  )
}
