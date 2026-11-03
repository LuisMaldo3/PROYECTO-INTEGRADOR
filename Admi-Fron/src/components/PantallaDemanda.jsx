import { useEffect, useMemo, useRef, useState } from 'react'
import Icono from './Iconos'
import { consultarDemanda } from '../services/demandaService'
import { filtrarDemanda } from '../services/demanda'
import './PantallaDemanda.css'

const FECHA = new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeStyle: 'short' })

/** US-37 · Ranking de demanda basado en inscripciones y cupos reales. */
export default function PantallaDemanda({ token, onSesionExpirada }) {
  const [datos, setDatos] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [analizando, setAnalizando] = useState(false)
  const [tipo, setTipo] = useState('todas')
  const [busqueda, setBusqueda] = useState('')
  const [version, setVersion] = useState(0)
  const abort = useRef(null)

  useEffect(() => {
    const ctrl = new AbortController()
    abort.current = ctrl
    setCargando(true)
    setError('')
    consultarDemanda(token, { signal: ctrl.signal })
      .then((resultado) => { if (!ctrl.signal.aborted) setDatos(resultado) })
      .catch((e) => {
        if (ctrl.signal.aborted) return
        if (e.estado === 401) return onSesionExpirada?.(e.detalle)
        setError(e.message || 'No se pudo cargar la demanda.')
      })
      .finally(() => { if (!ctrl.signal.aborted) setCargando(false) })
    return () => ctrl.abort()
  }, [token, onSesionExpirada, version])

  const analizar = async () => {
    if (analizando) return
    setAnalizando(true)
    setError('')
    try {
      const resultado = await consultarDemanda(token, { conIA: true })
      setDatos(resultado)
    } catch (e) {
      if (e.estado === 401) return onSesionExpirada?.(e.detalle)
      setError(e.message || 'No se pudo solicitar el análisis de IA.')
    } finally {
      setAnalizando(false)
    }
  }

  const lista = useMemo(() => filtrarDemanda(datos?.actividades || [], { tipo, consulta: busqueda }), [datos, tipo, busqueda])
  const resumen = datos?.totales
  const encabezado = (valor) => resumen ? valor : '—'
  const fecha = datos?.generadoEn ? new Date(datos.generadoEn) : null
  const fechaValida = fecha && !Number.isNaN(fecha.getTime())

  return (
    <section className="demanda">
      <header className="pagina__cabecera">
        <div>
          <h1>Demanda de actividades</h1>
          <p>Ranking por ocupación de cupos e inscripciones vigentes del T&C Week.</p>
        </div>
        <div className="pagina__acciones">
          <button type="button" className="boton boton--claro" disabled={cargando || analizando} onClick={() => setVersion((v) => v + 1)}>
            <Icono nombre="actualizar" tam={18} /> {cargando ? 'Actualizando…' : 'Actualizar'}
          </button>
          <button type="button" className="boton boton--primario" disabled={cargando || analizando || !datos?.actividades.length} onClick={analizar}>
            <Icono nombre="grafico" tam={18} /> {analizando ? 'Analizando…' : 'Analizar con IA'}
          </button>
        </div>
      </header>

      <dl className="cifras" aria-label="Indicadores de demanda">
        <div><dt>Actividades vigentes</dt><dd>{encabezado(resumen?.actividades)}</dd></div>
        <div><dt>Demanda alta o cupo cubierto</dt><dd>{encabezado(resumen?.conAltaDemanda)}</dd></div>
        <div><dt>Con cupo definido</dt><dd>{encabezado(resumen?.conCupo)}</dd></div>
        <div><dt>Sin inscripciones</dt><dd>{encabezado(resumen?.sinInscripciones)}</dd></div>
      </dl>

      {error && <div className="aviso aviso--error" role="alert">{error} <button className="boton boton--chico" type="button" onClick={() => setVersion((v) => v + 1)}>Reintentar</button></div>}
      {cargando && !datos && <p className="cargando" role="status">Calculando demanda real de las actividades…</p>}

      {datos && <>
        <section className="demanda__analisis tarjeta" aria-label="Análisis de demanda">
          <div className="demanda__analisis-encabezado"><h2>Interpretación de los resultados</h2><span>{datos.ia.estado === 'disponible' ? 'Generado con Gemini' : 'Datos verificables'}</span></div>
          {datos.ia.texto ? <p className="demanda__ia-texto">{datos.ia.texto}</p> : <p>{datos.ia.mensaje || 'El ranking funciona independientemente del análisis de IA.'}</p>}
          <small>Las explicaciones de IA son orientativas; las cifras provienen de inscripciones reales y no constituyen predicciones.</small>
        </section>

        <div className="demanda__filtros filtros" role="search" aria-label="Filtrar ranking de demanda">
          <label className="buscador"><Icono nombre="buscar" tam={18} />
            <input type="search" placeholder="Buscar actividad o área" aria-label="Buscar actividad o área" value={busqueda} onChange={(e) => setBusqueda(e.target.value)} />
          </label>
          <label className="demanda__selector">Tipo
            <select value={tipo} onChange={(e) => setTipo(e.target.value)}>
              <option value="todas">Todas</option>
              <option value="actividades">Actividades</option>
              <option value="competencias">Competencias</option>
            </select>
          </label>
        </div>

        <section className="demanda__ranking tarjeta" aria-label="Ranking de actividades con mayor demanda">
          <div className="demanda__titulo"><h2>Ranking de demanda</h2><span>{lista.length} de {datos.actividades.length}</span></div>
          <p className="demanda__criterio">{datos.criterio}</p>
          {lista.length === 0 ? <p className="demanda__vacio">No hay actividades que coincidan con estos filtros.</p> : (
            <ol className="demanda__lista">
              {lista.map((item) => <li className="demanda__item" key={item.id}>
                <div className="demanda__posicion">{String(item.posicion).padStart(2, '0')}</div>
                <div className="demanda__datos">
                  <div className="demanda__nombre"><strong>{item.nombre}</strong><span>{item.nivelDemanda}</span></div>
                  <small>{item.areaTecsup || (item.tipoActividad === 'competencia' ? 'Competencia' : 'Actividad')} · Cupos por {item.unidadCupo}</small>
                  <div className="demanda__barra" role="img" aria-label={item.porcentajeOcupacion === null ? 'Sin límite definido' : `${item.porcentajeOcupacion}% de ocupación`}>
                    <span style={{ width: `${Math.min(100, item.porcentajeOcupacion ?? 0)}%` }} />
                  </div>
                </div>
                <div className="demanda__metricas"><strong>{item.porcentajeOcupacion === null ? '—' : `${item.porcentajeOcupacion}%`}</strong>
                  <span>{item.ocupados} de {item.capacidad ?? 'sin límite'} {item.unidadCupo}</span>
                  {item.unidadCupo === 'equipos' && <small>{item.personasInscritas} personas · {item.equiposCompletos} equipos completos</small>}
                </div>
              </li>)}
            </ol>
          )}
        </section>
        <p className="demanda__nota">{datos.aclaracion}{fechaValida ? ` · Actualizado: ${FECHA.format(fecha)}` : ''}</p>
      </>}
    </section>
  )
}
