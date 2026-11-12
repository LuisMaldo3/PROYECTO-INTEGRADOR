import { useCallback, useEffect, useState } from 'react'
import Icono from './Iconos'
import Escudo from './Escudo'
import { claveArea } from '../services/areas'
import { cuerpoHoja, etiquetaCategoria, idPrueba, totalFila } from '../services/puntajes'
import { puntosEscritos } from '../services/torneos'
import { competenciasConPuntaje, enviarPuestosHoja, guardarHoja, obtenerHoja } from '../services/puntajesService'

/** Gymkanas y concursos por puntos: todos compiten a la vez, se suman los puntajes de cada prueba y sale el orden final. */
export default function PantallaPuntajes({ token, onSesionExpirada }) {
  const [competencias, setCompetencias] = useState([])
  const [actividadId, setActividadId] = useState('')
  const [hoja, setHoja] = useState(null)
  const [pruebas, setPruebas] = useState([])
  const [puntajes, setPuntajes] = useState({})
  const [nueva, setNueva] = useState('')
  const [mensaje, setMensaje] = useState({ tipo: '', texto: '' })
  const [ocupado, setOcupado] = useState('')
  const [valores, setValores] = useState({})

  const fallo = useCallback((e, porDefecto) => {
    if (e?.estado === 401) { onSesionExpirada?.(e.detalle); return }
    setMensaje({ tipo: 'error', texto: Object.values(e?.errores ?? {})[0] || e?.message || porDefecto })
  }, [onSesionExpirada])

  useEffect(() => {
    let activo = true
    competenciasConPuntaje(token).then((l) => { if (activo) setCompetencias(l) }).catch((e) => { if (activo) fallo(e, 'No se pudieron cargar las competencias.') })
    return () => { activo = false }
  }, [token, fallo])

  const cargar = (h) => {
    setHoja(h); setPruebas(h.pruebas)
    setPuntajes(Object.fromEntries(h.participantes.map((p) => [p.id, Object.fromEntries(h.pruebas.map((x) => [x.id, h.puntajes[p.id]?.[x.id] ?? '']))])))
  }

  async function elegir(id) {
    setActividadId(id); setHoja(null); setMensaje({ tipo: '', texto: '' })
    if (!id) return
    setOcupado('cargar')
    try { cargar(await obtenerHoja(token, id)) } catch (e) { fallo(e, 'No se pudo abrir la hoja.') } finally { setOcupado('') }
  }

  async function guardar() {
    setOcupado('guardar'); setMensaje({ tipo: '', texto: '' })
    try { cargar(await guardarHoja(token, actividadId, cuerpoHoja(pruebas, puntajes, hoja.version))); setMensaje({ tipo: 'ok', texto: 'Hoja guardada. El orden se recalculó.' }) }
    catch (e) { fallo(e, 'No se pudo guardar la hoja.') } finally { setOcupado('') }
  }

  async function enviar(publicar) {
    const filas = hoja.tabla.filter((f) => f.conPuntaje)
    const cuerpo = puntosEscritos(filas, Object.fromEntries(filas.map((f) => [f.puesto, valorDe(f.puesto)])))
    if (!cuerpo) { setMensaje({ tipo: 'error', texto: 'Escribe los puntos de todos los puestos (números enteros desde 0).' }); return }
    setOcupado('enviar'); setMensaje({ tipo: '', texto: '' })
    try {
      const r = await enviarPuestosHoja(token, actividadId, cuerpo, publicar)
      cargar(r)
      if (publicar && !r.resultadosPublicados) setMensaje({ tipo: 'error', texto: 'Los puntos se guardaron, pero no se pudieron publicar. Inténtalo de nuevo.' })
      else setMensaje({ tipo: 'ok', texto: publicar ? 'Resultados oficiales publicados. Los puntos ya suman a la tabla acumulada.' : 'Puntos guardados sin publicar.' })
    }
    catch (e) { fallo(e, 'No se pudieron guardar los puntos.') } finally { setOcupado('') }
  }

  const valorDe = (puesto) => valores[puesto] ?? (hoja && puesto <= hoja.puntosPorPuesto.length ? String(hoja.puntosPorPuesto[puesto - 1]) : '')
  const agregar = (ev) => {
    ev.preventDefault()
    const nombre = nueva.trim()
    if (nombre.length < 2) return
    const id = idPrueba(nombre, pruebas.map((x) => x.id))
    setPruebas([...pruebas, { id, nombre }]); setNueva('')
  }
  const quitar = (id) => setPruebas(pruebas.filter((x) => x.id !== id))
  const poner = (pid, prueba, valor) => setPuntajes((a) => ({ ...a, [pid]: { ...a[pid], [prueba]: valor } }))
  const disponibles = competencias.filter((c) => !c.torneoId)
  const sinGuardar = hoja && JSON.stringify(cuerpoHoja(pruebas, puntajes, hoja.version)) !== JSON.stringify(cuerpoHoja(hoja.pruebas, Object.fromEntries(hoja.participantes.map((p) => [p.id, hoja.puntajes[p.id] ?? {}])), hoja.version))

  return (
    <section className="puntajes">
      <header className="pagina__cabecera">
        <div><h1>Gymkanas y concursos</h1><p>Para competencias donde todos compiten a la vez: anota el puntaje de cada prueba y el sistema arma el orden final.</p></div>
      </header>
      {mensaje.texto && <p className={`aviso ${mensaje.tipo === 'ok' ? 'aviso--ok' : 'aviso--error'}`} role={mensaje.tipo === 'ok' ? 'status' : 'alert'}><Icono nombre={mensaje.tipo === 'ok' ? 'check' : 'alerta'} tam={18} />{mensaje.texto}</p>}
      <div className="tarjeta puntajes__elegir">
        <label className="campo">Competencia
          <select value={actividadId} onChange={(e) => elegir(e.target.value)}>
            <option value="">Elige una competencia</option>
            {disponibles.map((c) => <option key={c.id} value={c.id}>{c.nombre}{c.categoriaGenero ? ` · ${etiquetaCategoria(c.categoriaGenero)}` : ''}{c.tieneHoja ? ' (con hoja)' : ''}</option>)}
          </select>
          <span className="campo__ayuda">Las competencias que se juegan como torneo están en T&amp;C ARENA y no aparecen aquí.</span>
        </label>
      </div>
      {ocupado === 'cargar' && <p className="cargando" role="status">Abriendo la hoja…</p>}

      {hoja && (<>
        <div className="tarjeta puntajes__pruebas">
          <h2>Pruebas</h2>
          <ul className="puntajes__lista-pruebas">{pruebas.map((x) => <li key={x.id}><span>{x.nombre}</span><button type="button" className="boton-icono" aria-label={`Quitar ${x.nombre}`} onClick={() => quitar(x.id)}><Icono nombre="cerrar" tam={14} /></button></li>)}</ul>
          <form className="puntajes__nueva" onSubmit={agregar}><input value={nueva} onChange={(e) => setNueva(e.target.value)} placeholder="Nombre de la prueba (ej. Carrera de relevos)" maxLength="60" aria-label="Nueva prueba" /><button type="submit" className="boton boton--claro" disabled={nueva.trim().length < 2}><Icono nombre="mas" tam={16} /> Agregar prueba</button></form>
        </div>

        {hoja.participantes.length === 0 ? <p className="vacio">Esta competencia todavía no tiene inscritos confirmados.</p> : pruebas.length > 0 && (
          <div className="tarjeta tabla-envoltura">
            <table className="puntajes__tabla">
              <thead><tr><th scope="col">Participante</th>{pruebas.map((x) => <th key={x.id} scope="col">{x.nombre}</th>)}<th scope="col">Total</th></tr></thead>
              <tbody>{hoja.participantes.map((p) => (
                <tr key={p.id} data-area={claveArea(p.area)}>
                  <th scope="row"><span className="puntajes__equipo"><Escudo id={p.departamento} tam={24} decorativo /><span><strong>{p.nombre}</strong><small>{p.area || 'Sin área'}</small></span></span></th>
                  {pruebas.map((x) => <td key={x.id}><input type="number" min="0" step="any" inputMode="decimal" value={puntajes[p.id]?.[x.id] ?? ''} onChange={(e) => poner(p.id, x.id, e.target.value)} aria-label={`${p.nombre}: ${x.nombre}`} /></td>)}
                  <td className="puntajes__total">{totalFila(puntajes[p.id], pruebas)}</td>
                </tr>))}</tbody>
            </table>
          </div>)}
        <div className="puntajes__acciones"><button type="button" className="boton" onClick={guardar} disabled={!!ocupado || pruebas.length === 0}>{ocupado === 'guardar' ? 'Guardando…' : 'Guardar hoja'}</button>{sinGuardar && <span className="suave">Hay cambios sin guardar.</span>}</div>

        {hoja.tabla.some((f) => f.conPuntaje) && (
          <section className="tarjeta puntajes__orden" aria-labelledby="pz-orden">
            <h2 id="pz-orden">Orden final y puntos (escríbelos al terminar)</h2>
            <ol>{hoja.tabla.filter((f) => f.conPuntaje).map((f) => (
              <li key={f.participante.id} data-area={claveArea(f.participante.area)}><span className="puntos-torneo__puesto">{f.puesto}</span><Escudo id={f.participante.departamento} tam={26} decorativo />
                <span><strong>{f.participante.nombre}</strong><small>{f.total} puntos · {f.pruebasGanadas} {f.pruebasGanadas === 1 ? 'prueba ganada' : 'pruebas ganadas'}</small></span>
                {f.empate && <span className="cruce__sello cruce__sello--aviso" title="Mismo total y mismas pruebas ganadas: revisa el orden en Resultados antes de publicar">Empate</span>}
                {hoja.resultadosPublicados ? <span className="puntos-torneo__pts">Publicado</span> : <label className="puntos-torneo__campo"><input type="number" min="0" max="10000" step="1" inputMode="numeric" value={valorDe(f.puesto)} onChange={(e) => setValores({ ...valores, [f.puesto]: e.target.value })} aria-label={`Puntos del ${f.puesto}.º puesto`} /> pts</label>}</li>))}</ol>
            <p className="campo__ayuda">Con el mismo total gana quien ganó más pruebas. Si aun así empatan, se marca «Empate»: revisa el orden en Resultados antes de publicar.</p>
            {hoja.resultadosPublicados ? <p className="aviso aviso--ok" role="status"><Icono nombre="check" tam={18} />Resultados oficiales publicados: ya suman a la tabla acumulada.</p> : (
              <div className="modal__acciones"><button type="button" className="boton boton--claro" onClick={() => enviar(false)} disabled={!!ocupado || sinGuardar}>Guardar sin publicar</button><button type="button" className="boton" onClick={() => enviar(true)} disabled={!!ocupado || sinGuardar}>{ocupado === 'enviar' ? 'Publicando…' : 'Publicar resultados oficiales'}</button></div>)}
          </section>)}
      </>)}
    </section>
  )
}
