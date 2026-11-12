import { useCallback, useEffect, useState } from 'react'
import Escudo from './Escudo'
import PanelPartidoOrganizador from './PanelPartidoOrganizador'
import { Campeon, Llaves } from './TorneosEstudiante'
import { claveArea } from '../services/areas'
import { avance } from '../services/torneos'
import { mensajeDeError } from '../services/torneosOrganizador'
import { cuerpoHoja, etiquetaCategoria, idPrueba, totalFila } from '../services/puntajes'
import { crearTorneo, guardarHoja, listarCompetencias, listarMisTorneos, obtenerHoja, proponerSorteo } from '../services/torneosOrganizadorService'

/** Armar el torneo de una competencia: sorteo automático o propuesta de la IA (que se revisa antes de crear). */
function ArmarTorneo({ token, competencia, onCreado }) {
  const [propuesta, setPropuesta] = useState(null)
  const [tercero, setTercero] = useState(true)
  const [ocupado, setOcupado] = useState('')
  const [error, setError] = useState('')
  async function hacer(nombre, fn) {
    setOcupado(nombre); setError('')
    try { await fn() } catch (e) { setError(mensajeDeError(e)) } finally { setOcupado('') }
  }
  const crear = (datos) => hacer('crear', async () => onCreado(await crearTorneo(token, { actividadId: competencia.id, tercerPuesto: tercero, ...datos })))
  return (
    <div className="oc-bloque">
      <h3>Armar el torneo (eliminación directa)</h3>
      <p className="oc-suave">Participan los cinco departamentos, como manda la T&C Week. Puedes sortear o pedirle a la IA una propuesta de cruces; tú decides.</p>
      <label className="oc-casilla"><input type="checkbox" checked={tercero} onChange={(e) => setTercero(e.target.checked)} /> Incluir partido por el tercer puesto</label>
      {error && <p className="ot-aviso ot-aviso--error" role="alert">{error}</p>}
      {propuesta && (
        <div className="oc-propuesta" role="status">
          <p><strong>Propuesta de la IA:</strong> {propuesta.explicacion}</p>
          <ul>{propuesta.cruces.map((c, i) => <li key={i}>{c.descanso ? `${c.a ?? c.b} descansa` : `${c.a} vs ${c.b}`}</li>)}</ul>
        </div>)}
      <div className="oc-botones">
        <button type="button" className="ot-boton ot-boton--claro" disabled={!!ocupado} onClick={() => hacer('ia', async () => setPropuesta(await proponerSorteo(token, competencia.id)))}>{ocupado === 'ia' ? 'La IA está pensando…' : propuesta ? 'Pedir otra propuesta' : 'Proponer con IA'}</button>
        {propuesta && <button type="button" className="ot-boton" disabled={!!ocupado} onClick={() => crear({ participantes: propuesta.orden, sorteo: false })}>Crear con esta propuesta</button>}
        <button type="button" className={propuesta ? 'ot-boton ot-boton--claro' : 'ot-boton'} disabled={!!ocupado} onClick={() => crear({ sorteo: true })}>{ocupado === 'crear' ? 'Creando…' : 'Crear con sorteo automático'}</button>
      </div>
    </div>
  )
}

/** Hoja de puntajes (gymkanas y concursos): pruebas y el puntaje de cada participante. */
function Hoja({ token, competencia }) {
  const [hoja, setHoja] = useState(null)
  const [pruebas, setPruebas] = useState([])
  const [puntajes, setPuntajes] = useState({})
  const [nueva, setNueva] = useState('')
  const [mensaje, setMensaje] = useState({ tipo: '', texto: '' })
  const [ocupado, setOcupado] = useState(false)
  const cargar = (h) => {
    setHoja(h); setPruebas(h.pruebas)
    setPuntajes(Object.fromEntries(h.participantes.map((p) => [p.id, Object.fromEntries(h.pruebas.map((x) => [x.id, h.puntajes[p.id]?.[x.id] ?? '']))])))
  }
  useEffect(() => {
    let activo = true
    obtenerHoja(token, competencia.id).then((h) => { if (activo) cargar(h) }).catch((e) => { if (activo) setMensaje({ tipo: 'error', texto: mensajeDeError(e) }) })
    return () => { activo = false }
  }, [token, competencia.id])
  if (!hoja) return mensaje.texto ? <p className="ot-aviso ot-aviso--error" role="alert">{mensaje.texto}</p> : <p className="oc-suave" role="status">Abriendo la hoja…</p>

  async function guardar() {
    setOcupado(true); setMensaje({ tipo: '', texto: '' })
    try { cargar(await guardarHoja(token, competencia.id, cuerpoHoja(pruebas, puntajes, hoja.version))); setMensaje({ tipo: 'ok', texto: 'Guardado. El administrador revisa y publica los puestos.' }) }
    catch (e) { setMensaje({ tipo: 'error', texto: mensajeDeError(e) }) } finally { setOcupado(false) }
  }
  const agregar = (ev) => { ev.preventDefault(); if (nueva.trim().length < 2) return; setPruebas([...pruebas, { id: idPrueba(nueva, pruebas.map((x) => x.id)), nombre: nueva.trim() }]); setNueva('') }

  return (
    <div className="oc-bloque">
      <h3>Hoja de puntajes</h3>
      {mensaje.texto && <p className={`ot-aviso ot-aviso--${mensaje.tipo === 'ok' ? 'ok' : 'error'}`} role={mensaje.tipo === 'ok' ? 'status' : 'alert'}>{mensaje.texto}</p>}
      <ul className="oc-pruebas">{pruebas.map((x) => <li key={x.id}>{x.nombre}<button type="button" aria-label={`Quitar ${x.nombre}`} onClick={() => setPruebas(pruebas.filter((y) => y.id !== x.id))}>×</button></li>)}</ul>
      <form className="oc-nueva" onSubmit={agregar}><input value={nueva} onChange={(e) => setNueva(e.target.value)} placeholder="Nueva prueba (ej. Carrera de relevos)" maxLength="60" aria-label="Nueva prueba" /><button type="submit" className="ot-boton ot-boton--claro" disabled={nueva.trim().length < 2}>Agregar</button></form>
      {hoja.participantes.length === 0 ? <p className="oc-suave">Todavía no hay inscritos confirmados.</p> : pruebas.length > 0 && (
        <div className="oc-tabla"><table>
          <thead><tr><th scope="col">Participante</th>{pruebas.map((x) => <th key={x.id} scope="col">{x.nombre}</th>)}<th scope="col">Total</th></tr></thead>
          <tbody>{hoja.participantes.map((p) => (
            <tr key={p.id} data-area={claveArea(p.area)}>
              <th scope="row"><span className="oc-equipo"><Escudo id={p.departamento} tam={22} decorativo /><span><strong>{p.nombre}</strong><small>{p.area || 'Sin área'}</small></span></span></th>
              {pruebas.map((x) => <td key={x.id}><input type="number" min="0" step="any" inputMode="decimal" value={puntajes[p.id]?.[x.id] ?? ''} aria-label={`${p.nombre}: ${x.nombre}`} onChange={(e) => setPuntajes((a) => ({ ...a, [p.id]: { ...a[p.id], [x.id]: e.target.value } }))} /></td>)}
              <td className="oc-total">{totalFila(puntajes[p.id], pruebas)}</td>
            </tr>))}</tbody>
        </table></div>)}
      <button type="button" className="ot-boton" onClick={guardar} disabled={ocupado || pruebas.length === 0}>{ocupado ? 'Guardando…' : 'Guardar hoja'}</button>
      {hoja.tabla.some((f) => f.conPuntaje) && <p className="oc-suave">Va primero: <strong>{hoja.tabla[0].participante.nombre}</strong> ({hoja.tabla[0].total} puntos).</p>}
    </div>
  )
}

/** Organizador: solo las competencias que le asignó el administrador. */
export default function OrganizadorCompetencias({ token }) {
  const [estado, setEstado] = useState({ cargando: true, error: '', competencias: [], torneos: [] })
  const [elegida, setElegida] = useState(null)
  const [modo, setModo] = useState({})
  const [partidoId, setPartidoId] = useState(null)
  const [version, setVersion] = useState(0)

  const cargar = useCallback((signal) => Promise.all([listarCompetencias(token, signal), listarMisTorneos(token, signal)])
    .then(([competencias, torneos]) => { if (!signal?.aborted) setEstado({ cargando: false, error: '', competencias, torneos }) })
    .catch((e) => { if (!signal?.aborted && e.name !== 'AbortError') setEstado({ cargando: false, error: mensajeDeError(e), competencias: [], torneos: [] }) }), [token])
  useEffect(() => { const c = new AbortController(); cargar(c.signal); return () => c.abort() }, [cargar, version])

  if (estado.cargando) return <div className="tc-vacio" role="status">Cargando tus competencias…</div>
  if (estado.error) return <div className="tc-error" role="alert">{estado.error} <button className="tc-accion" type="button" onClick={() => { setEstado((e) => ({ ...e, cargando: true })); setVersion((v) => v + 1) }}>Reintentar</button></div>
  if (!estado.competencias.length) return <div className="tc-vacio">Todavía no te asignaron competencias. Cuando el administrador te asigne una, aparecerá aquí.</div>

  const comp = estado.competencias.find((c) => c.id === elegida) ?? estado.competencias[0]
  const torneo = estado.torneos.find((t) => t.actividadId === comp.id) ?? null
  const partido = torneo?.partidos.find((p) => p.id === partidoId) ?? null
  const reemplazar = (t) => setEstado((e) => ({ ...e, torneos: e.torneos.some((x) => x.id === t.id) ? e.torneos.map((x) => (x.id === t.id ? t : x)) : [...e.torneos, t] }))
  const usaHoja = comp.tieneHoja || modo[comp.id] === 'hoja'

  return (
    <div className="torneo-est">
      <div className="segmentos" role="group" aria-label="Competencia">{estado.competencias.map((c) => <button key={c.id} type="button" aria-pressed={c.id === comp.id} onClick={() => { setElegida(c.id); setPartidoId(null) }}>{c.nombre}{c.categoriaGenero ? ` · ${etiquetaCategoria(c.categoriaGenero)}` : ''}</button>)}</div>
      {torneo ? (<>
        <div className="torneo-est__resumen"><div><h2>{torneo.deporte}</h2><p>{torneo.participantes.length} participantes · {avance(torneo).jugados} de {avance(torneo).total} enfrentamientos jugados{torneo.pendientesDeValidar ? ` · ${torneo.pendientesDeValidar} por validar` : ''}</p></div></div>
        <p className="torneo-est__intro">Toca un enfrentamiento para programarlo o registrar el marcador. El ganador avanza solo; el administrador valida y publica los resultados oficiales.</p>
        <Campeon torneo={torneo} />
        <div className="torneo-est__cuerpo"><Llaves torneo={torneo} onElegir={(p) => setPartidoId(p.id)} seleccionadoId={partidoId} /></div>
        {partido && <PanelPartidoOrganizador key={partido.id} token={token} torneo={torneo} partido={partido} onActualizado={reemplazar} onCerrar={() => setPartidoId(null)} onSesionExpirada={() => setVersion((v) => v + 1)} />}
      </>) : usaHoja ? <Hoja key={comp.id} token={token} competencia={comp} /> : (
        <div className="oc-opciones">
          <ArmarTorneo key={comp.id} token={token} competencia={comp} onCreado={(t) => reemplazar(t)} />
          <div className="oc-bloque"><h3>Gymkana o concurso por puntos</h3><p className="oc-suave">Si todos compiten a la vez en varias pruebas, anota el puntaje de cada prueba y el sistema arma el orden final.</p>
            <button type="button" className="ot-boton ot-boton--claro" onClick={() => setModo((m) => ({ ...m, [comp.id]: 'hoja' }))}>Usar hoja de puntajes</button></div>
        </div>)}
    </div>
  )
}
