import { useEffect, useState } from 'react'
import { consultarClasificacionAdmin } from '../services/competenciasService'
import { claveArea } from '../services/areas'
import CierreSemana from './CierreSemana'

export default function ClasificacionGeneral({ token }) {
  const [estado, setEstado] = useState({ cargando: true, error: '', tabla: null })
  const [version, setVersion] = useState(0)
  useEffect(() => { let activo = true
    consultarClasificacionAdmin(token).then(tabla => { if (activo) setEstado({ cargando: false, error: '', tabla }) })
      .catch(e => { if (activo) setEstado({ cargando: false, error: e.message, tabla: null }) })
    return () => { activo = false }
  }, [token, version])
  const areas = estado.tabla?.areas || []
  const max = Math.max(1, ...areas.map(f => Number(f.puntos) || 0))
  return <div className="clasificacion-admin">
    <header className="pagina__cabecera"><div><h1>Clasificación</h1><p>Puntos por área según las competencias con resultados publicados.</p></div>
      <div className="pagina__acciones"><button className="boton boton--claro" type="button" onClick={() => { setEstado(s => ({ ...s, cargando: true })); setVersion(n => n + 1) }}>Actualizar</button></div></header>
    <CierreSemana token={token} />
    {estado.cargando && <p className="cargando" role="status">Cargando clasificación…</p>}
    {estado.error && <p className="aviso aviso--error" role="alert">{estado.error}</p>}
    {estado.tabla && <section className="tarjeta clasificacion-admin__tabla">
      <p className="clasificacion-admin__nota">{estado.tabla.totalCompetencias} {estado.tabla.totalCompetencias === 1 ? 'competencia publicada' : 'competencias publicadas'}. Desempate: {estado.tabla.criterioDesempate}.</p>
      <ol className="ranking">{areas.map(f => <li key={f.area} data-area={claveArea(f.area)}>
        <span className="ranking__pos">{f.posicion}</span>
        <div className="ranking__nombre"><strong>{f.area}</strong><small>{f.primerosPuestos} {f.primerosPuestos === 1 ? 'primer puesto' : 'primeros puestos'}</small></div>
        <div className="ranking__barra" aria-hidden="true"><span style={{ width: `${Math.round(((Number(f.puntos) || 0) / max) * 100)}%` }} /></div>
        <strong className="ranking__puntos">{f.puntos}<small> pts</small></strong>
      </li>)}</ol>
      <div className="clasificacion-admin__desglose">{areas.map(f => <details key={f.area}><summary>{f.area}: detalle de puntos</summary>
        {f.desglose.length ? <ul>{f.desglose.map((p, index) => <li key={`${p.actividadId}-${index}`}><span>{p.competencia}</span><span>{p.puesto}.º · {p.puntos} pts</span></li>)}</ul> : <p className="suave">Sin puntos aún.</p>}</details>)}</div>
    </section>}
  </div>
}
