import { useEffect, useMemo, useState } from 'react'
import { obtenerMisActividades } from '../services/organizadorService'
import { OrganizadorTarjeta } from './OrganizadorTarjeta'

export function OrganizadorActividades({ token }) {
  const [estado, setEstado] = useState({ cargando: true, items: [], error: '' })
  const [busqueda, setBusqueda] = useState('')
  const [filtro, setFiltro] = useState('todas')
  const [revision, setRevision] = useState(0)
  useEffect(() => {
    const ctrl = new AbortController()
    obtenerMisActividades(token, ctrl.signal)
      .then(items => { if (!ctrl.signal.aborted) setEstado({ cargando: false, items, error: '' }) })
      .catch(error => { if (!ctrl.signal.aborted) setEstado({ cargando: false, items: [], error: error.message }) })
    return () => ctrl.abort()
  }, [token, revision])

  // Actualizar cupos mientras la pantalla está visible, sin conservar intervalos al salir.
  useEffect(() => {
    const actualizar = () => { if (document.visibilityState === 'visible') setRevision(n => n + 1) }
    const intervalo = window.setInterval(actualizar, 30000)
    window.addEventListener('focus', actualizar)
    return () => { window.clearInterval(intervalo); window.removeEventListener('focus', actualizar) }
  }, [])

  const { items, cargando, error } = estado
  const visibles = useMemo(() => items.filter(a => {
    const texto = `${a.nombre || ''} ${a.descripcion || ''}`.toLocaleLowerCase('es-PE')
    return texto.includes(busqueda.toLocaleLowerCase('es-PE')) && (filtro === 'todas' || a.estado === filtro)
  }), [items, busqueda, filtro])
  const inscritos = items.reduce((s, a) => s + (Number(a.participantesTotales ?? a.estudiantesInscritos) || 0), 0)
  return (
    <div className="org-pagina">
      <div className="org-indicadores">
        <div><span>Actividades asignadas</span><strong>{cargando ? '—' : items.length}</strong><small>En tu cuenta</small></div>
        <div><span>Total de participantes</span><strong aria-live="polite">{cargando ? '—' : inscritos}</strong><small>Estudiantes con inscripción activa en tus actividades</small></div>
        <div><span>Próximas / programadas</span><strong>{cargando ? '—' : items.filter(a => a.estado === 'programada').length}</strong><small>Por gestionar</small></div>
      </div>
      <div className="org-seccion-titulo">
        <div><h2>Mis actividades</h2></div>
        <button className="org-boton org-boton--suave" type="button" onClick={() => { setEstado(e => ({ ...e, cargando: true, error: '' })); setRevision(n => n + 1) }}>Actualizar</button>
      </div>
      <div className="org-filtros">
        <label className="org-buscar"><span aria-hidden="true">⌕</span><input value={busqueda} onChange={e => setBusqueda(e.target.value)} placeholder="Buscar por nombre o descripción" aria-label="Buscar actividades" /></label>
        <div className="org-chips" role="group" aria-label="Filtrar por estado">
          {[['todas','Todas'],['programada','Programadas'],['cancelada','Canceladas']].map(([id,nombre]) => <button type="button" key={id} aria-pressed={filtro === id} onClick={() => setFiltro(id)}>{nombre}</button>)}
        </div>
      </div>
      {error && <div className="org-alerta" role="alert"><strong>No fue posible cargar tus actividades.</strong><p>{error}</p><button type="button" onClick={() => setRevision(n => n + 1)}>Reintentar</button></div>}
      {cargando && <div className="org-cargando" role="status">Consultando tus actividades asignadas…</div>}
      {!cargando && !error && visibles.length === 0 && <div className="org-vacio"><div className="org-vacio__icono">▤</div><h3>{items.length ? 'Sin resultados con estos filtros' : 'Todavía no tienes actividades asignadas'}</h3><p>{items.length ? 'Prueba otra búsqueda o cambia el filtro.' : 'Un administrador debe asignarte como organizador desde el panel de actividades.'}</p></div>}
      {!cargando && !error && visibles.length > 0 && <div className="org-tarjetas">{visibles.map(a => <OrganizadorTarjeta key={a.id} actividad={a} />)}</div>}
    </div>
  )
}
