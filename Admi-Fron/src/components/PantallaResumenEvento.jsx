import { useEffect, useMemo, useState } from 'react'
import Icono from './Iconos'
import { consultarResumenEvento } from '../services/resumenEventoService'
import { filtrarResultados } from '../services/resumenEvento'
import './PantallaResumenEvento.css'

const fecha = new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeStyle: 'short' })

function Cifra({ nombre, valor, detalle }) {
  return <div className="reporte-evento__cifra"><dt>{nombre}</dt><dd>{valor}</dd>{detalle && <small>{detalle}</small>}</div>
}

/** US-39 · Panel de solo lectura; publica únicamente resultados ya validados. */
export default function PantallaResumenEvento({ token, onSesionExpirada }) {
  const [datos, setDatos] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [generando, setGenerando] = useState(false)
  const [error, setError] = useState('')
  const [busqueda, setBusqueda] = useState('')
  const [version, setVersion] = useState(0)

  useEffect(() => {
    const ctrl = new AbortController()
    setCargando(true)
    setError('')
    consultarResumenEvento(token, { signal: ctrl.signal })
      .then(resultado => { if (!ctrl.signal.aborted) setDatos(resultado) })
      .catch(e => {
        if (ctrl.signal.aborted) return
        if (e.estado === 401) return onSesionExpirada?.(e.detalle)
        setError(e.message || 'No se pudo cargar el resumen del evento.')
      })
      .finally(() => { if (!ctrl.signal.aborted) setCargando(false) })
    return () => ctrl.abort()
  }, [token, onSesionExpirada, version])

  const generar = async () => {
    if (generando) return
    setGenerando(true)
    setError('')
    try {
      const respuesta = await consultarResumenEvento(token, { conIA: true })
      setDatos(respuesta)
    } catch (e) {
      if (e.estado === 401) return onSesionExpirada?.(e.detalle)
      setError(e.message || 'No se pudo generar el resumen del evento.')
    } finally {
      setGenerando(false)
    }
  }

  const totales = datos?.totales
  const maximo = Math.max(1, ...((datos?.areas || []).map(a => a.puntos)))
  const coincidencias = useMemo(() => filtrarResultados(datos?.resultados || [], busqueda), [datos, busqueda])
  const registro = datos?.generadoEn ? new Date(datos.generadoEn) : null
  const fechaValida = registro && !Number.isNaN(registro.getTime())

  return <section className="reporte-evento">
    <header className="pagina__cabecera">
      <div><h1>Resumen del T&C Week</h1><p>Desarrollo del evento, clasificación oficial y competencias con resultados publicados.</p></div>
      <div className="pagina__acciones">
        <button type="button" className="boton boton--claro" disabled={cargando || generando} onClick={() => setVersion(v => v + 1)}>
          <Icono nombre="actualizar" tam={18} /> {cargando ? 'Actualizando…' : 'Actualizar'}
        </button>
        <button type="button" className="boton boton--primario" onClick={generar}
          disabled={cargando || generando || !totales?.actividadesRegistradas}>
          <Icono nombre="grafico" tam={18} /> {generando ? 'Generando…' : 'Generar con IA'}
        </button>
      </div>
    </header>

    {error && <p className="aviso aviso--error" role="alert">{error}</p>}
    {cargando && !datos && <p className="cargando" role="status">Consultando los resultados oficiales de VYNK…</p>}

    {datos && <>
      <dl className="reporte-evento__totales" aria-label="Indicadores oficiales">
        <Cifra nombre="Actividades vigentes" valor={totales.actividadesVigentes} detalle={`${totales.actividadesCanceladas} canceladas`} />
        <Cifra nombre="Competencias publicadas" valor={totales.competenciasConResultados} detalle={`${totales.competenciasSinResultados} sin resultados oficiales`} />
        <Cifra nombre="Inscripciones activas" valor={totales.inscripcionesActivas} detalle="Participaciones por actividad" />
        <Cifra nombre="Puntos oficiales" valor={totales.puntosOficiales} detalle={`${totales.posicionesOficiales} posiciones validadas`} />
      </dl>

      <section className="tarjeta reporte-evento__resumen" aria-label="Resumen automático del evento">
        <div className="reporte-evento__seccion-cabecera"><h2>Resumen del desarrollo del evento</h2><span>{datos.ia.estado === 'disponible' ? 'Texto generado con Gemini' : 'Datos oficiales'}</span></div>
        {datos.ia.texto ? <p className="reporte-evento__texto">{datos.ia.texto}</p> :
          <p>{datos.ia.mensaje || 'Genera un resumen a partir de los resultados oficiales.'}</p>}
        <small>Las cifras son de MongoDB. La IA solo redacta una interpretación; no modifica puntajes ni declara al ganador general.</small>
      </section>

      <section className="tarjeta reporte-evento__areas" aria-label="Puntaje acumulado de las cinco áreas">
        <div className="reporte-evento__seccion-cabecera"><h2>Puntajes oficiales por área</h2><span>Las 5 áreas de Tecsup</span></div>
        <ol className="reporte-evento__ranking">
          {datos.areas.map(a => <li key={a.area} className="reporte-evento__area">
            <span className="reporte-evento__puesto">{String(a.posicion).padStart(2, '0')}</span>
            <div className="reporte-evento__area-info"><strong>{a.area}</strong><small>{a.primerosPuestos} primeros puestos</small>
              <div className="reporte-evento__barra" role="img" aria-label={`${a.puntos} puntos`}><span style={{ width: `${100 * a.puntos / maximo}%` }} /></div>
            </div><strong className="reporte-evento__puntos">{a.puntos} <small>pts</small></strong>
          </li>)}
        </ol>
      </section>

      <section className="tarjeta reporte-evento__competencias" aria-label="Resultados por competencia">
        <div className="reporte-evento__seccion-cabecera"><div><h2>Resultados de competencias</h2><p>Únicamente resultados publicados y aprobados.</p></div><span>{coincidencias.length} de {datos.resultados.length}</span></div>
        <label className="reporte-evento__buscar"><Icono nombre="buscar" tam={18} /><input type="search" value={busqueda}
          onChange={e => setBusqueda(e.target.value)} placeholder="Buscar competencia, área o ganador" aria-label="Buscar resultados oficiales" /></label>
        {coincidencias.length === 0 ? <p className="reporte-evento__vacio">{datos.resultados.length ? 'No hay competencias que coincidan con tu búsqueda.' : 'Aún no existen resultados oficiales publicados.'}</p> :
          <div className="reporte-evento__resultados">{coincidencias.map(r => <article className="reporte-evento__resultado" key={r.actividadId}>
            <div className="reporte-evento__resultado-cabecera"><div><h3>{r.nombre}</h3><small>{r.publicadoEn ? `Publicado: ${r.publicadoEn.slice(0, 10)}` : 'Resultado oficial publicado'}</small></div>
              <strong>{r.puntosDistribuidos} pts</strong></div>
            {r.ganadores.length ? <p className="reporte-evento__ganador"><strong>Primer puesto:</strong> {r.ganadores.map(g => `${g.nombre} — ${g.areaTecsup} (${g.puntos} pts)`).join(', ')}</p> : <p className="reporte-evento__aviso">Este resultado no registra un primer puesto válido.</p>}
            <details><summary>Ver {r.posiciones.length} posiciones oficiales</summary>
              <ol className="reporte-evento__posiciones">{r.posiciones.map(p => <li key={`${r.actividadId}-${p.puesto}`}><span>{p.puesto}.º · {p.nombre} <small>· {p.areaTecsup}</small></span><strong>{p.puntos} pts</strong></li>)}</ol>
            </details>
          </article>)}</div>}
      </section>
      <p className="reporte-evento__nota">{datos.criterio} {datos.aclaracion}{fechaValida ? ` · Consultado: ${fecha.format(registro)}` : ''}</p>
    </>}
  </section>
}
