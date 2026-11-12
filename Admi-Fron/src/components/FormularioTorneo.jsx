import { useEffect, useRef, useState } from 'react'
import Icono from './Iconos'
import Escudo from './Escudo'
import { claveArea } from '../services/areas'
import { descansos, tituloAccion } from '../services/torneos'
import { crearTorneo, participantesDisponibles, proponerSorteo } from '../services/torneosService'
import { listarActividades } from '../services/actividadesService'

const VACIO = { cargando: false, departamentos: [], inscritos: [], motivo: '' }

/** Crear un torneo de eliminación directa. En la T&C Week participan por obligación los cinco departamentos. */
export default function FormularioTorneo({ token, tipo, actividadInicial = '', onCreado, onCerrar, onSesionExpirada }) {
  const [competencias, setCompetencias] = useState([])
  const [actividadId, setActividadId] = useState(actividadInicial)
  const [disponibles, setDisponibles] = useState(VACIO)
  const [modo, setModo] = useState('departamentos')
  const [elegidos, setElegidos] = useState([])
  const [modoResultado, setModoResultado] = useState('marcador')
  const [sets, setSets] = useState(2)
  const [sorteo, setSorteo] = useState(true)
  const [tercero, setTercero] = useState(true)
  const [propuesta, setPropuesta] = useState(null)
  const [pensando, setPensando] = useState(false)
  const [errores, setErrores] = useState({})
  const [error, setError] = useState('')
  const [guardando, setGuardando] = useState(false)
  const primero = useRef(null)

  useEffect(() => {
    primero.current?.focus()
    const salir = (e) => { if (e.key === 'Escape') onCerrar() }
    document.addEventListener('keydown', salir)
    return () => document.removeEventListener('keydown', salir)
  }, [onCerrar])

  useEffect(() => {
    let activo = true
    listarActividades(token).then((lista) => { if (activo) setCompetencias(lista.filter((a) => a.tipoActividad === 'competencia' && a.estado !== 'cancelada')) })
      .catch((e) => { if (activo && e?.estado === 401) onSesionExpirada?.(e.detalle) })
    return () => { activo = false }
  }, [token, onSesionExpirada])

  // Al elegir la competencia: quedan marcados los cinco departamentos.
  useEffect(() => {
    if (!actividadId) return undefined
    let activo = true
    participantesDisponibles(token, actividadId)
      .then((r) => {
        if (!activo) return
        setDisponibles({ cargando: false, departamentos: r.departamentos ?? [], inscritos: r.participantes ?? [], motivo: r.motivo || '' })
        setModo('departamentos'); setElegidos((r.departamentos ?? []).map((d) => d.id))
      })
      .catch((e) => { if (activo) { if (e?.estado === 401) onSesionExpirada?.(e.detalle); else setError(e?.message || 'No se pudo cargar la competencia.') } })
    return () => { activo = false }
  }, [token, actividadId, onSesionExpirada])

  const elegirCompetencia = (id) => { setErrores({}); setError(''); setPropuesta(null); setElegidos([]); setDisponibles(id ? { ...VACIO, cargando: true } : VACIO); setActividadId(id) }
  const cambiarModo = (m) => { setModo(m); setPropuesta(null); setElegidos((m === 'departamentos' ? disponibles.departamentos : disponibles.inscritos).map((p) => p.id)) }
  const alternar = (id) => { setPropuesta(null); setElegidos((a) => (a.includes(id) ? a.filter((x) => x !== id) : [...a, id])) }
  const lista = modo === 'departamentos' ? disponibles.departamentos : disponibles.inscritos
  const sobran = descansos(elegidos.length)

  async function pedirPropuesta() {
    setPensando(true); setError('')
    try { const r = await proponerSorteo(token, actividadId, elegidos, modo); setElegidos(r.orden); setSorteo(false); setPropuesta(r) }
    catch (e) { if (e?.estado === 401) { onSesionExpirada?.(e.detalle); return } setError(e?.message || 'La IA no pudo proponer el sorteo. Usa el sorteo automático o el orden manual.') }
    finally { setPensando(false) }
  }

  async function enviar(ev) {
    ev.preventDefault()
    setGuardando(true); setErrores({}); setError('')
    try {
      const datos = { tipo, actividadId, participantes: elegidos, modoParticipantes: modo, sorteo, tercerPuesto: tercero, modoResultado,
        ...(modoResultado === 'sets' ? { setsParaGanar: Number(sets) } : {}) }
      onCreado(await crearTorneo(token, datos))
    } catch (e) {
      if (e?.estado === 401) { onSesionExpirada?.(e.detalle); return }
      setErrores(e?.errores ?? {}); setError(e?.errores && Object.keys(e.errores).length ? '' : e?.message || 'No se pudo crear el torneo.')
    } finally { setGuardando(false) }
  }

  const err = (k) => errores[k] && <span className="campo__error" role="alert">{errores[k]}</span>
  return (
    <div className="modal" role="presentation" onMouseDown={(e) => { if (e.target === e.currentTarget) onCerrar() }}>
      <form className="modal__ventana modal__ventana--ancha" role="dialog" aria-modal="true" aria-labelledby="torneo-titulo" onSubmit={enviar} noValidate>
        <div className="modal__cabecera">
          <div className="modal__distintivo" aria-hidden="true"><Icono nombre="balon" tam={22} /></div>
          <div><h2 id="torneo-titulo">{tituloAccion(tipo).nuevo}</h2><p>Eliminación directa entre los cinco departamentos. Los ganadores avanzan hasta la final.</p></div>
          <button type="button" className="detalle__cerrar" onClick={onCerrar} aria-label="Cerrar"><Icono nombre="cerrar" tam={20} /></button>
        </div>

        {error && <p className="aviso aviso--error" role="alert"><Icono nombre="alerta" tam={18} />{error}</p>}

        <label className="campo">Competencia
          <select ref={primero} value={actividadId} onChange={(e) => elegirCompetencia(e.target.value)} aria-invalid={!!errores.actividadId}>
            <option value="">Elige una competencia</option>
            {competencias.map((c) => <option key={c.id} value={c.id}>{c.nombre}</option>)}
          </select>
          <span className="campo__ayuda">Los puntos de cada puesto se escriben al final, cuando termine el torneo.</span>
          {err('actividadId')}
        </label>

        {actividadId && (
          <fieldset className="campo-grupo"><legend>Quiénes participan</legend>
            <div className="segmentado">
              <label><input type="radio" name="modo" checked={modo === 'departamentos'} onChange={() => cambiarModo('departamentos')} /><span>Los 5 departamentos (obligatorio)</span></label>
              <label><input type="radio" name="modo" checked={modo === 'inscritos'} onChange={() => cambiarModo('inscritos')} /><span>Equipos inscritos{disponibles.inscritos.length ? ` (${disponibles.inscritos.length})` : ''}</span></label>
            </div>
            {disponibles.cargando ? <p className="cargando" role="status">Cargando…</p> : lista.length === 0 ? (
              <p className="aviso aviso--atencion" role="status"><Icono nombre="alerta" tam={18} />{disponibles.motivo || 'No hay equipos inscritos confirmados.'}</p>) : (
              <ul className="participantes-elegir">
                {lista.map((p) => {
                  const orden = elegidos.indexOf(p.id) + 1
                  return (
                    <li key={p.id}><label className={orden ? 'participante-opcion participante-opcion--activa' : 'participante-opcion'} data-area={claveArea(p.area)}>
                      <input type="checkbox" checked={orden > 0} onChange={() => alternar(p.id)} />
                      <Escudo id={p.departamento} tam={26} decorativo />
                      <span><strong>{p.nombre}</strong><small>{modo === 'departamentos' ? 'Departamento' : p.area || 'Sin área'}</small></span>
                      {!sorteo && orden > 0 && <span className="escudo-opcion__orden" title="Orden de siembra">{orden}</span>}
                    </label></li>)
                })}
              </ul>)}
            {modo === 'departamentos' && <p className="campo__ayuda">Desmarca un departamento solo si no se presenta.</p>}
            {err('participantes')}
            {elegidos.length >= 2 && <p className="campo__ayuda">{sobran === 0 ? `${elegidos.length} participantes: todos juegan la primera ronda.` : `${elegidos.length} participantes: ${sobran} ${sobran === 1 ? 'descansa' : 'descansan'} en la primera ronda y ${sobran === 1 ? 'avanza' : 'avanzan'} sin jugar.`}</p>}
          </fieldset>)}

        <fieldset className="campo-grupo"><legend>¿Cómo se registra el resultado?</legend>
          <div className="segmentado">
            <label><input type="radio" name="resultado" checked={modoResultado === 'marcador'} onChange={() => setModoResultado('marcador')} /><span>Marcador (goles, puntos)</span></label>
            <label><input type="radio" name="resultado" checked={modoResultado === 'sets'} onChange={() => setModoResultado('sets')} /><span>Sets</span></label>
          </div>
          {modoResultado === 'sets' && <label className="campo campo--corto">Sets para ganar<select value={sets} onChange={(e) => setSets(e.target.value)}>{[2, 3, 4].map((n) => <option key={n} value={n}>{n}</option>)}</select>{err('setsParaGanar')}</label>}
        </fieldset>

        <fieldset className="campo-grupo"><legend>Orden de los enfrentamientos</legend>
          <div className="segmentado">
            <label><input type="radio" name="sorteo" checked={sorteo} onChange={() => setSorteo(true)} /><span>Sorteo automático</span></label>
            <label><input type="radio" name="sorteo" checked={!sorteo} onChange={() => setSorteo(false)} /><span>Asignación manual</span></label>
          </div>
          <p className="campo__ayuda">{sorteo ? 'El sistema sortea las llaves y guarda la semilla del sorteo en el historial.' : 'El orden en que marques a los participantes define la siembra: el 1 y el 2 solo se cruzan en la final.'}</p>
          <button type="button" className="boton boton--claro boton--chico propuesta-ia__boton" onClick={pedirPropuesta} disabled={pensando || elegidos.length < 2}><Icono nombre="chispa" tam={16} /> {pensando ? 'La IA está pensando…' : 'Proponer con IA'}</button>
          {propuesta && (
            <div className="propuesta-ia" role="status">
              <p><strong>Propuesta de la IA</strong> (puedes cambiar el orden desmarcando y marcando): {propuesta.explicacion}</p>
              <ul>{propuesta.cruces.map((c, i) => <li key={i}>{c.descanso ? `${c.a ?? c.b} descansa` : `${c.a} vs ${c.b}`}</li>)}</ul>
            </div>)}
        </fieldset>

        <label className="casilla"><input type="checkbox" checked={tercero} onChange={(e) => setTercero(e.target.checked)} /><span>Incluir partido por el tercer puesto <small>(así hay 3.º y 4.º; sin él, los perdedores de semifinales comparten el 3.º)</small></span></label>

        <div className="modal__acciones">
          <button type="button" className="boton boton--claro" onClick={onCerrar}>Cancelar</button>
          <button type="submit" className="boton" disabled={guardando || elegidos.length < 2}>{guardando ? 'Creando…' : 'Crear torneo y generar llaves'}</button>
        </div>
      </form>
    </div>
  )
}
