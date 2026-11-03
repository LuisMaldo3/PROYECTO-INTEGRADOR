import { useEffect, useState } from 'react'
import { anunciarGanadores, guardarPropuesta, obtenerResultadosCompetencia, participantesCompetencia, publicarResultados } from '../services/competenciasService'

export default function ResultadosCompetencia({ actividad, token, onCerrar }) {
  const [estado, setEstado] = useState({ cargando: true, participantes: [], resultados: null, error: '' })
  const [posiciones, setPosiciones] = useState([])
  const [procesando, setProcesando] = useState(false)
  const [mensaje, setMensaje] = useState('')
  const recargar = async () => {
    setEstado(e => ({ ...e, cargando: true, error: '' }))
    try {
      const [p, r] = await Promise.all([participantesCompetencia(token, actividad.id), obtenerResultadosCompetencia(token, actividad.id)])
      setEstado({ cargando: false, participantes: p.participantes || [], resultados: r, error: '' })
      setPosiciones((r.posiciones || []).map(x => ({ inscripcionId: x.inscripcionId, puesto: x.puesto, puntos: x.puntos ?? 0 })))
    } catch (e) { setEstado({ cargando: false, participantes: [], resultados: null, error: e.message }) }
  }
  useEffect(() => { recargar() }, [actividad.id, token]) // cada competencia carga sus datos propios
  const operar = async (accion) => {
    setProcesando(true); setMensaje('')
    try { await accion(); await recargar(); setMensaje('Operación guardada correctamente.') }
    catch (e) { setMensaje(e.message) }
    finally { setProcesando(false) }
  }
  const publicada = estado.resultados?.estado === 'publicado'
  const enviadaPorOrganizador = estado.resultados?.origen === 'organizador'
  const pendiente = estado.resultados?.estado === 'pendiente'
  const guardadas = (estado.resultados?.posiciones || []).map(p => [String(p.inscripcionId), Number(p.puesto), Number(p.puntos ?? 0)]).sort((a, b) => a[1] - b[1])
  const editadas = posiciones.map(p => [String(p.inscripcionId), Number(p.puesto), Number(p.puntos ?? 0)]).sort((a, b) => a[1] - b[1])
  const cambiosSinGuardar = JSON.stringify(guardadas) !== JSON.stringify(editadas)
  return <div className="confirmacion-fondo" role="presentation"><div className="resultados-modal" role="dialog" aria-modal="true" aria-label={`Resultados de ${actividad.nombre}`}>
    <div className="resultados-modal__encabezado"><div><h2>{actividad.nombre}</h2><p className="suave">Revisa los puestos y puntos antes de publicarlos.</p></div><button type="button" className="boton boton--claro" onClick={onCerrar} aria-label="Cerrar revisión de resultados">Cerrar</button></div>
    <ol className="resultados-modal__flujo"><li>El organizador registra</li><li>Tú revisas y publicas</li><li>Los estudiantes lo ven</li></ol>
    {estado.cargando && <p role="status">Cargando participantes…</p>}
    {estado.error && <p className="aviso aviso--error">{estado.error}</p>}
    {!estado.cargando && !estado.error && <>
      <div className="resultados-modal__contexto"><span className={publicada ? 'resultados-modal__estado resultados-modal__estado--publicado' : 'resultados-modal__estado'}>{publicada ? 'Publicado oficialmente' : pendiente ? 'Pendiente de validación' : 'Sin resultados registrados'}</span><span>{estado.participantes.length} inscripciones activas</span></div>
      {pendiente && cambiosSinGuardar && <p className="resultados-modal__origen" role="status">Tienes cambios sin guardar. Guarda las correcciones antes de aprobar los resultados.</p>}
      {pendiente && enviadaPorOrganizador && <p className="resultados-modal__origen" role="status">El organizador responsable envió esta propuesta. Comprueba cada posición antes de publicarla.</p>}
      {!pendiente && !publicada && <p className="resultados-modal__origen">El organizador todavía no envió los puestos. Como administrador también puedes preparar una propuesta desde esta pantalla.</p>}
      {!estado.participantes.length && <p className="suave">No hay inscritos. Las propuestas necesitan inscripciones reales.</p>}
      <div className="resultados-modal__filas">
        {posiciones.map((posicion, i) => <div className="resultados-modal__fila" key={i}>
          <label>Puesto <input type="number" min="1" max="100" disabled={publicada} value={posicion.puesto} onChange={e => setPosiciones(prev => prev.map((p,j) => j === i ? {...p, puesto: Number(e.target.value)} : p))} /></label>
          <label>Participante <select value={posicion.inscripcionId} disabled={publicada} onChange={e => setPosiciones(prev => prev.map((p,j) => j === i ? {...p, inscripcionId: e.target.value} : p))}>
            {estado.participantes.map(a => <option key={a.id} value={a.id}>{a.nombre}{a.areaTecsup ? ` · ${a.areaTecsup}` : ''}</option>)}
          </select></label>
          <label>Puntos propuestos <input type="number" min="0" max="10000" disabled={publicada} value={posicion.puntos ?? 0} onChange={e => setPosiciones(prev => prev.map((p,j) => j === i ? {...p, puntos: Number(e.target.value)} : p))}/></label>
          {!publicada && <button type="button" className="boton boton--claro" onClick={() => setPosiciones(prev => prev.filter((_,j) => j !== i))}>Quitar</button>}
        </div>)}
      </div>
      {!publicada && <div className="resultados-modal__acciones">
        <button type="button" className="boton boton--claro" disabled={!estado.participantes.length || procesando} onClick={() => setPosiciones(prev => [...prev, {puesto: prev.length + 1, puntos: actividad.puntosPorPuesto?.[prev.length] ?? 0, inscripcionId: estado.participantes.find(p => !prev.some(x => x.inscripcionId === p.id))?.id || estado.participantes[0]?.id}])}>Añadir posición</button>
        <button type="button" className="boton" disabled={!posiciones.length || procesando} onClick={() => operar(() => guardarPropuesta(token, actividad.id, posiciones))}>Guardar correcciones</button>
        <button type="button" className="boton" disabled={estado.resultados?.estado !== 'pendiente' || procesando || !posiciones.length || cambiosSinGuardar} onClick={() => window.confirm('¿Publicar oficialmente estas posiciones?') && operar(() => publicarResultados(token, actividad.id))}>Aprobar y publicar resultados</button>
      </div>}
      {publicada && <div className="resultados-modal__acciones"><button type="button" className="boton" disabled={procesando || estado.resultados.avisoPublicado} onClick={() => window.confirm('¿Publicar el aviso del ganador para todos los estudiantes?') && operar(() => anunciarGanadores(token, actividad.id))}>{estado.resultados.avisoPublicado ? 'Ganadores anunciados' : 'Anunciar ganadores'}</button></div>}
      {mensaje && <p role="status" className="aviso">{mensaje}</p>}
    </>}
  </div></div>
}
