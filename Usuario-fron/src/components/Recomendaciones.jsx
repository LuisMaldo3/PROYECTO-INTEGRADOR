import { useEffect, useState } from 'react'
import { cargarRecomendaciones, descartarRecomendacion, retirarTarjetaDescartada } from '../services/recomendacionesService'
import { TarjetaRecomendada } from './TarjetaRecomendada'
export function Recomendaciones() {
  const [datos, setDatos] = useState(null)
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(true)
  const [version, setVersion] = useState(0)
  const [descartandoId, setDescartandoId] = useState(null)
  const [avisoDescarte, setAvisoDescarte] = useState('')
  const [errorDescarte, setErrorDescarte] = useState('')
  useEffect(() => {
    const controller = new AbortController()
    setCargando(true); setError('')
    cargarRecomendaciones(controller.signal).then(setDatos).catch(e => { if (!controller.signal.aborted) setError(e.message) }).finally(() => { if (!controller.signal.aborted) setCargando(false) })
    return () => controller.abort()
  }, [version])
  async function descartar(a) {
    if (descartandoId || !a?.id || !datos) return
    const anterior = datos
    setDescartandoId(a.id)
    setAvisoDescarte('')
    setErrorDescarte('')
    // Retirar la tarjeta de inmediato; se restaura si falla la API.
    setDatos(actual => retirarTarjetaDescartada(actual, a.id))
    try {
      await descartarRecomendacion(a.id)
      setAvisoDescarte(`Dejaremos de recomendarte «${a.nombre}».`)
    } catch (e) {
      setDatos(anterior)
      setErrorDescarte(e.message || 'No se pudo guardar tu preferencia.')
    } finally {
      setDescartandoId(null)
    }
  }
  return <div className="rec-pagina">
    <header className="rec-hero"><div><h2>Elegidas según tus intereses</h2><p>Puedes cambiar tus intereses desde tu perfil.</p></div><div className="rec-hero__acciones"><span>{datos?.origen === 'ia' ? 'Ordenadas con IA' : 'Por tus intereses'}</span><button onClick={() => setVersion(v => v+1)} disabled={cargando || Boolean(descartandoId)}>Actualizar</button></div></header>
    {avisoDescarte && <p className="rec-alerta rec-alerta--ok" role="status">{avisoDescarte}</p>}
    {errorDescarte && <p className="rec-alerta rec-alerta--error" role="alert">{errorDescarte}</p>}
    {cargando && <p className="rec-estado" role="status">Buscando actividades para ti…</p>}
    {error && <div className="rec-estado" role="alert">{error}<button onClick={() => setVersion(v=>v+1)}>Reintentar</button></div>}
    {!cargando && !error && datos && <>{datos.aviso && <p className="rec-aviso" role="status">{datos.aviso}</p>}
      {datos.origen === 'sin_intereses' && <a className="rec-enlace" href="#/intereses">Elegir mis intereses</a>}
      {datos.origen === 'sin_horario' && <div className="rec-sin-horario"><strong>Primero confirma tu horario semanal</strong><p>Ve a Mi agenda, abre «Mis horas libres» y guarda tu horario de clases.</p><a className="rec-enlace" href="#/agenda">Configurar mi disponibilidad</a></div>}
      {datos.origen !== 'sin_horario' && !datos.actividades?.length && <p className="rec-estado">No hay más sugerencias por ahora.</p>}
      <div className="rec-grid">{datos.actividades?.map((a,i) => <TarjetaRecomendada key={a.id} actividad={a} posicion={i+1} onDescartar={() => descartar(a)} deshabilitado={Boolean(descartandoId)}/>)}</div>
    </>}
  </div>
}
