import { useEffect, useMemo, useState } from 'react'
import Icono from './Iconos'
import { consultarComparacionCategorias } from '../services/comparacionCategoriasService'
import { categoriasComparadas, clasificacionComparada, CINCO_AREAS } from '../services/comparacionCategorias'
import './PantallaComparacionCategorias.css'

const formato = n => Number(n || 0).toLocaleString('es-PE')

export default function PantallaComparacionCategorias({ token, onSesionExpirada }) {
  const [datos, setDatos] = useState(null)
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(true)
  const [revision, setRevision] = useState(0)
  const [categorias, setCategorias] = useState([])
  const [area, setArea] = useState('')

  useEffect(() => {
    const ctrl = new AbortController()
    setCargando(true)
    setError('')
    consultarComparacionCategorias(token, { signal: ctrl.signal })
      .then(d => { if (!ctrl.signal.aborted) setDatos(d) })
      .catch(e => {
        if (ctrl.signal.aborted) return
        if (e.estado === 401) { onSesionExpirada?.(e.detalle); return }
        setError(e.message || 'No se pudieron cargar las estadísticas.')
      }).finally(() => { if (!ctrl.signal.aborted) setCargando(false) })
    return () => ctrl.abort()
  }, [token, onSesionExpirada, revision])

  const elegidas = useMemo(() => categoriasComparadas(datos, categorias), [datos, categorias])
  const ranking = useMemo(() => clasificacionComparada(datos, categorias, area), [datos, categorias, area])
  const maximo = Math.max(1, ...elegidas.map(c => c.personasInscritas))
  const cambiar = id => setCategorias(prev => prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id])
  const limitar = categorias.length > 0
  const participacionesFiltradas = elegidas.reduce((s, c) => s + c.personasInscritas, 0)

  return <section className="comparacion-categorias">
    <header className="pagina__cabecera"><div>
      <h1>Comparación por categorías</h1>
      <p>Participaciones registradas y clasificación oficial de las cinco áreas del T&C Week.</p>
    </div><div className="pagina__acciones">
      <button type="button" className="boton boton--claro" onClick={() => setRevision(v => v + 1)} disabled={cargando}>
        <Icono nombre="actualizar" tam={18} /> {cargando ? 'Actualizando…' : 'Actualizar'}
      </button>
    </div></header>
    {error && <p className="aviso aviso--error" role="alert">{error}</p>}
    {cargando && !datos && <p className="cargando" role="status">Consultando las estadísticas de VYNK…</p>}
    {datos && <>
      <div className="comparacion-categorias__indicadores">
        <div><span>Participaciones activas</span><strong>{formato(datos.totales.participaciones)}</strong></div>
        <div><span>Categorías</span><strong>{formato(datos.totales.categorias)}</strong></div>
        <div><span>Equipos completos</span><strong>{formato(datos.totales.equiposCompletos)}</strong></div>
        <div><span>Puntos oficiales</span><strong>{formato(datos.totales.puntosOficiales)}</strong></div>
      </div>
      <section className="tarjeta comparacion-categorias__tarjeta">
        <div className="comparacion-categorias__cabecera"><div><h2>Participación por categoría</h2>
          <p>Selecciona las categorías a comparar. Sin selección se muestran todas.</p></div>
          <button className="boton boton--claro" type="button" onClick={() => setCategorias([])} disabled={!limitar}>Ver todas</button>
        </div>
        <fieldset className="comparacion-categorias__filtros"><legend>Categorías a comparar</legend>
          {datos.categorias.map(c => <label key={c.id} className={categorias.includes(c.id) ? 'comparacion-categorias__filtro elegido' : 'comparacion-categorias__filtro'}>
            <input type="checkbox" checked={categorias.includes(c.id)} onChange={() => cambiar(c.id)} />{c.nombre}
          </label>)}
        </fieldset>
        <p className="comparacion-categorias__nota">{formato(participacionesFiltradas)} participaciones en la selección. Son inscripciones por actividad, no estudiantes únicos del evento.</p>
        {!elegidas.length ? <p className="comparacion-categorias__vacio">Aún no hay categorías registradas.</p> :
          <div className="comparacion-categorias__barras" role="list" aria-label="Comparación de participaciones">
            {elegidas.map(c => <div role="listitem" key={c.id} className="comparacion-categorias__barra-fila">
              <div className="comparacion-categorias__barra-dato"><strong>{c.nombre}</strong><span>{formato(c.personasInscritas)} participaciones</span></div>
              <div className="comparacion-categorias__pista" role="img" aria-label={`${c.personasInscritas} participaciones de ${c.nombre}`}>
                <span style={{ width: `${100 * c.personasInscritas / maximo}%` }} />
              </div>
              <div className="comparacion-categorias__detalles">
                <span>{c.actividades} actividades</span>
                <span>{c.equiposCompletos} equipos completos</span>
                <span>{c.equiposIncompletos} pendientes</span>
                <span>Cupos: {c.cuposPersonasOcupados} personas / {c.cuposEquiposOcupados} equipos</span>
              </div>
            </div>)}</div>}
      </section>
      <section className="tarjeta comparacion-categorias__tarjeta">
        <div className="comparacion-categorias__cabecera"><div><h2>Clasificación oficial de las cinco áreas</h2>
          <p>Los puntos provienen exclusivamente de competencias publicadas. La comparación respeta el filtro de categorías.</p></div>
          <label className="comparacion-categorias__selector">Filtrar área
            <select value={area} onChange={e => setArea(e.target.value)}>
              <option value="">Todas las áreas</option>
              {CINCO_AREAS.map(nombre => <option value={nombre} key={nombre}>{nombre}</option>)}
            </select>
          </label>
        </div>
        <p className="comparacion-categorias__nota">{ranking.totalCompetencias} competencias con resultados oficiales · {formato(ranking.puntosTotales)} puntos en la selección.</p>
        <div className="comparacion-categorias__tabla-wrap"><table className="comparacion-categorias__tabla"><thead><tr><th>Puesto</th><th>Área</th><th>Puntos</th><th>Primeros puestos</th><th>Desglose</th></tr></thead><tbody>
          {ranking.areas.map(a => <tr key={a.area}><td>{a.posicion}.º</td><th scope="row">{a.area}</th><td><strong>{formato(a.puntos)}</strong></td><td>{a.primerosPuestos}</td>
            <td>{a.desglose.length ? <details><summary>Ver {a.desglose.length} resultados</summary><ul>
              {a.desglose.map((p, i) => <li key={`${p.actividadId}-${p.puesto}-${i}`}>{p.competencia} · {p.categoria}: puesto {p.puesto}, {p.puntos} pts</li>)}
            </ul></details> : <span className="comparacion-categorias__sin-puntos">Sin puntos</span>}</td></tr>)}
        </tbody></table></div>
        <p className="comparacion-categorias__nota">Si una categoría no tiene resultados publicados, sus puntos serán cero. El ranking es de consulta y no modifica los resultados.</p>
      </section>
      <p className="comparacion-categorias__pie">{datos.aclaracion}</p>
    </>}
  </section>
}
