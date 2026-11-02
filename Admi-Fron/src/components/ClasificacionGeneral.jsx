import { useEffect, useState } from 'react'
import { consultarClasificacionAdmin } from '../services/competenciasService'
import './ClasificacionGeneral.css'

export default function ClasificacionGeneral({ token }) {
  const [estado, setEstado] = useState({ cargando: true, error: '', tabla: null })
  const [version, setVersion] = useState(0)
  useEffect(() => { let activo = true
    consultarClasificacionAdmin(token).then(tabla => { if (activo) setEstado({ cargando: false, error: '', tabla }) })
      .catch(e => { if (activo) setEstado({ cargando: false, error: e.message, tabla: null }) })
    return () => { activo = false }
  }, [token, version])
  return <div className="clasificacion-admin">
    <header className="clasificacion-admin__hero"><div><p>RESULTADOS OFICIALES · T&amp;C WEEK</p><h1>Clasificación por áreas</h1><span>Los puntos proceden de competencias aprobadas. No se cuentan propuestas pendientes ni asistencia.</span></div>
      <button className="clasificacion-admin__boton" type="button" onClick={() => { setEstado(s => ({ ...s, cargando: true })); setVersion(n => n + 1) }}>Actualizar</button></header>
    {estado.cargando && <p role="status">Consultando resultados oficiales…</p>}
    {estado.error && <p role="alert">{estado.error}</p>}
    {estado.tabla && <><p className="clasificacion-admin__nota">{estado.tabla.totalCompetencias} competencia(s) publicada(s). Criterio de desempate: {estado.tabla.criterioDesempate}.</p>
      <div className="clasificacion-admin__tabla"><table><thead><tr><th>Posición</th><th>Área</th><th>Puntos oficiales</th><th>1.er puesto</th></tr></thead>
      <tbody>{estado.tabla.areas.map(f => <tr key={f.area}><td><strong>{f.posicion}.º</strong></td><td>{f.area}</td><td><strong>{f.puntos}</strong></td><td>{f.primerosPuestos}</td></tr>)}</tbody></table></div>
      <div className="clasificacion-admin__desglose">{estado.tabla.areas.map(f => <details key={f.area}><summary>{f.area} · {f.puntos} puntos</summary>
        {f.desglose.length ? <ul>{f.desglose.map((p, index) => <li key={`${p.actividadId}-${index}`}>{p.competencia}: {p.puesto}.º — {p.puntos} puntos</li>)}</ul> : <p>Sin puntos oficiales aún.</p>}</details>)}</div></>}
  </div>
}
