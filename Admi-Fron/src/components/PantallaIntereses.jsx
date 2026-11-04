import { useEffect, useMemo, useState } from 'react'
import Icono from './Iconos'
import { consultarIntereses } from '../services/interesesService'
import { filtrarIntereses } from '../services/intereses'
import './PantallaIntereses.css'

const FECHA = new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeStyle: 'short' })

/** US-38: análisis de preferencias en formato agregado, sin datos personales. */
export default function PantallaIntereses({ token, onSesionExpirada }) {
  const [datos, setDatos] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [analizando, setAnalizando] = useState(false)
  const [error, setError] = useState('')
  const [busqueda, setBusqueda] = useState('')
  const [soloElegidos, setSoloElegidos] = useState(false)
  const [version, setVersion] = useState(0)

  useEffect(() => {
    const ctrl = new AbortController()
    setCargando(true)
    setError('')
    consultarIntereses(token, { signal: ctrl.signal })
      .then((respuesta) => { if (!ctrl.signal.aborted) setDatos(respuesta) })
      .catch((e) => {
        if (ctrl.signal.aborted) return
        if (e.estado === 401) return onSesionExpirada?.(e.detalle)
        setError(e.message || 'No se pudieron consultar los intereses.')
      })
      .finally(() => { if (!ctrl.signal.aborted) setCargando(false) })
    return () => ctrl.abort()
  }, [token, onSesionExpirada, version])

  const resumirConIA = async () => {
    if (analizando) return
    setAnalizando(true)
    setError('')
    try {
      const respuesta = await consultarIntereses(token, { conIA: true })
      setDatos(respuesta)
    } catch (e) {
      if (e.estado === 401) return onSesionExpirada?.(e.detalle)
      setError(e.message || 'No se pudo generar el resumen de IA.')
    } finally {
      setAnalizando(false)
    }
  }

  const lista = useMemo(() => filtrarIntereses(datos?.intereses || [], busqueda, soloElegidos), [datos, busqueda, soloElegidos])
  const totales = datos?.totales
  const fecha = datos?.generadoEn ? new Date(datos.generadoEn) : null
  const fechaValida = fecha && !Number.isNaN(fecha.getTime())

  return (
    <section className="intereses-panel">
      <header className="pagina__cabecera">
        <div>
          <h1>Intereses de los estudiantes</h1>
          <p>Preferencias elegidas en VYNK: recuento real por categoría y resumen opcional con IA.</p>
        </div>
        <div className="pagina__acciones">
          <button type="button" className="boton boton--claro" disabled={cargando || analizando} onClick={() => setVersion((v) => v + 1)}>
            <Icono nombre="actualizar" tam={18} /> {cargando ? 'Actualizando…' : 'Actualizar'}
          </button>
          <button type="button" className="boton boton--primario" disabled={cargando || analizando || !totales?.selecciones} onClick={resumirConIA}>
            <Icono nombre="grafico" tam={18} /> {analizando ? 'Resumiendo…' : 'Resumir con IA'}
          </button>
        </div>
      </header>

      <dl className="cifras" aria-label="Totales de intereses">
        <div><dt>Estudiantes activos</dt><dd>{totales?.estudiantesActivos ?? '—'}</dd></div>
        <div><dt>Con intereses guardados</dt><dd>{totales?.conIntereses ?? '—'}</dd></div>
        <div><dt>Sin intereses elegidos</dt><dd>{totales?.sinIntereses ?? '—'}</dd></div>
        <div><dt>Selecciones registradas</dt><dd>{totales?.selecciones ?? '—'}</dd></div>
      </dl>

      {error && <div className="aviso aviso--error" role="alert">{error} <button type="button" className="boton boton--chico" onClick={() => setVersion((v) => v + 1)}>Reintentar</button></div>}
      {cargando && !datos && <p className="cargando" role="status">Consultando intereses guardados en MongoDB…</p>}

      {datos && <>
        <section className="tarjeta intereses-panel__analisis" aria-label="Interpretación de intereses">
          <div className="intereses-panel__cabecera"><h2>Resumen de preferencias</h2><span>{datos.ia.estado === 'disponible' ? 'Generado con Gemini' : 'Datos registrados'}</span></div>
          <p className="intereses-panel__resumen">{datos.ia.texto || datos.ia.mensaje || 'El gráfico se calcula sin IA.'}</p>
          <small>El resumen de IA es orientativo. Se envían únicamente cifras agrupadas, no datos personales.</small>
        </section>

        <div className="intereses-panel__filtros filtros" role="search" aria-label="Filtrar intereses">
          <label className="buscador"><Icono nombre="buscar" tam={18} />
            <input type="search" value={busqueda} onChange={(e) => setBusqueda(e.target.value)} placeholder="Buscar interés" aria-label="Buscar interés" />
          </label>
          <label className="intereses-panel__opcion"><input type="checkbox" checked={soloElegidos} onChange={(e) => setSoloElegidos(e.target.checked)} /> Solo con selecciones</label>
        </div>

        <section className="tarjeta intereses-panel__grafico" aria-label="Gráfico de intereses elegidos">
          <div className="intereses-panel__cabecera"><h2>Ranking de intereses</h2><span>{lista.length} categorías</span></div>
          <p className="intereses-panel__criterio">{datos.criterio}</p>
          {totales?.conIntereses === 0 ? <p className="intereses-panel__vacio">Todavía ningún estudiante activo ha elegido un interés válido. El gráfico aparecerá cuando existan preferencias guardadas.</p> : (
            lista.length === 0 ? <p className="intereses-panel__vacio">No hay intereses que coincidan con tus filtros.</p> :
            <ol className="intereses-panel__lista">
              {lista.map((interes) => <li key={interes.id} className="intereses-panel__item">
                <div className="intereses-panel__posicion">{String(interes.posicion).padStart(2, '0')}</div>
                <div className="intereses-panel__detalle">
                  <strong>{interes.nombre}</strong>
                  <div className="intereses-panel__barra" role="img" aria-label={`${interes.porcentaje}% de estudiantes con intereses seleccionados eligieron ${interes.nombre}`}>
                    <span style={{ width: `${interes.porcentaje}%` }} />
                  </div>
                </div>
                <div className="intereses-panel__cantidad"><strong>{interes.estudiantes}</strong><span>estudiantes · {interes.porcentaje}%</span></div>
              </li>)}
            </ol>
          )}
        </section>
        <p className="intereses-panel__nota">{datos.aclaracion}{fechaValida ? ` · Actualizado: ${FECHA.format(fecha)}` : ''}</p>
      </>}
    </section>
  )
}
