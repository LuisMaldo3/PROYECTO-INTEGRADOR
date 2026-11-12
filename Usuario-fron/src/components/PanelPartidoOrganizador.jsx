import { useEffect, useRef, useState } from 'react'
import Escudo from './Escudo'
import { claveArea } from '../services/areas'
import { equipo } from '../services/torneos'
import { ACCION_ESTADO, cuerpoResultado, esCorreccion, formularioDeResultado, mensajeDeError, puedeProgramar, puedeRegistrar, transicionesDisponibles } from '../services/torneosOrganizador'
import { accionPartido } from '../services/torneosOrganizadorService'

/** Ventana de un enfrentamiento para el organizador: resultado, horario y lugar, y estado. Sin validar ni anular. */
export default function PanelPartidoOrganizador({ token, torneo, partido, onActualizado, onCerrar, onSesionExpirada }) {
  const [res, setRes] = useState(() => formularioDeResultado(partido))
  const [hor, setHor] = useState({ fecha: partido.fecha || '', hora: partido.hora || '', lugar: partido.lugar || '' })
  const [errores, setErrores] = useState({})
  const [mensaje, setMensaje] = useState({ tipo: '', texto: '' })
  const [ocupado, setOcupado] = useState('')
  const cerrarRef = useRef(null)
  const modo = torneo.modoResultado
  const nombreA = equipo(torneo, partido.a)?.nombre ?? 'Por definir', nombreB = equipo(torneo, partido.b)?.nombre ?? 'Por definir'

  useEffect(() => {
    cerrarRef.current?.focus()
    const salir = (e) => { if (e.key === 'Escape') onCerrar() }
    document.addEventListener('keydown', salir)
    return () => document.removeEventListener('keydown', salir)
  }, [onCerrar])

  async function ejecutar(nombre, accion, cuerpo, exito) {
    setOcupado(nombre); setErrores({}); setMensaje({ tipo: '', texto: '' })
    try { onActualizado(await accionPartido(token, torneo.id, partido.id, accion, cuerpo)); setMensaje({ tipo: 'ok', texto: exito }) }
    catch (e) {
      if (e?.estado === 401) { onSesionExpirada?.(e.message); return }
      setErrores(e?.errores ?? {}); setMensaje({ tipo: 'error', texto: mensajeDeError(e) })
    } finally { setOcupado('') }
  }
  const guardarResultado = (ev) => { ev.preventDefault(); ejecutar('resultado', 'resultado', cuerpoResultado(modo, res), esCorreccion(partido) ? 'Resultado corregido. Administración debe validarlo otra vez.' : 'Resultado guardado. El ganador ya avanzó a la siguiente ronda.') }
  const guardarHorario = (ev) => { ev.preventDefault(); ejecutar('horario', 'programar', hor, 'Horario guardado.') }

  const campo = (k) => errores[k] && <span className="ot-error" role="alert">{errores[k]}</span>
  const empate = modo === 'marcador' && res.a !== '' && res.b !== '' && Number(res.a) === Number(res.b)
  const meta = torneo.setsParaGanar ?? 2

  return (
    <div className="ot-fondo" role="presentation" onMouseDown={(e) => { if (e.target === e.currentTarget) onCerrar() }}>
      <div className="ot-ventana" role="dialog" aria-modal="true" aria-labelledby="ot-titulo">
        <header className="ot-cabecera">
          <div><h2 id="ot-titulo">{partido.nombreRonda}</h2><p>{torneo.deporte} · {nombreA} vs {nombreB}</p></div>
          <button type="button" className="ot-cerrar" onClick={onCerrar} ref={cerrarRef} aria-label="Cerrar">×</button>
        </header>

        <div className="ot-cruce" aria-hidden="true">
          {[partido.a, partido.b].map((id, i) => <div key={i} className="ot-equipo" data-area={claveArea(equipo(torneo, id)?.area)}><Escudo id={equipo(torneo, id)?.departamento} tam={52} decorativo /><strong>{equipo(torneo, id)?.nombre}</strong>{equipo(torneo, id)?.area !== equipo(torneo, id)?.nombre && <small>{equipo(torneo, id)?.area}</small>}</div>)}
          <span className="ot-vs">VS</span>
        </div>

        {mensaje.texto && <p className={`ot-aviso ot-aviso--${mensaje.tipo === 'ok' ? 'ok' : 'error'}`} role={mensaje.tipo === 'ok' ? 'status' : 'alert'}>{mensaje.texto}</p>}

        {puedeRegistrar(partido) ? (
          <form className="ot-bloque" onSubmit={guardarResultado} noValidate>
            <h3>{esCorreccion(partido) ? 'Corregir resultado' : 'Registrar resultado'}</h3>
            {modo === 'marcador' && (<>
              <div className="ot-marcador">
                <label>{nombreA}<input type="number" min="0" max="999" inputMode="numeric" value={res.a} onChange={(e) => setRes({ ...res, a: e.target.value })} aria-invalid={!!errores.a} />{campo('a')}</label>
                <span aria-hidden="true">–</span>
                <label>{nombreB}<input type="number" min="0" max="999" inputMode="numeric" value={res.b} onChange={(e) => setRes({ ...res, b: e.target.value })} aria-invalid={!!errores.b} />{campo('b')}</label>
              </div>
              {empate && <label className="ot-campo">Empate: ¿quién ganó en el desempate?
                <select value={res.ganadorPenales} onChange={(e) => setRes({ ...res, ganadorPenales: e.target.value })} aria-invalid={!!errores.ganadorPenales}><option value="">Elige un departamento</option><option value="a">{nombreA}</option><option value="b">{nombreB}</option></select>{campo('ganadorPenales')}</label>}
            </>)}
            {modo === 'sets' && (<>
              <div className="ot-marcador">
                <label>Sets de {nombreA}<input type="number" min="0" max={meta} inputMode="numeric" value={res.setsA} onChange={(e) => setRes({ ...res, setsA: e.target.value })} aria-invalid={!!errores.setsA} />{campo('setsA')}</label>
                <span aria-hidden="true">–</span>
                <label>Sets de {nombreB}<input type="number" min="0" max={meta} inputMode="numeric" value={res.setsB} onChange={(e) => setRes({ ...res, setsB: e.target.value })} aria-invalid={!!errores.setsB} />{campo('setsB')}</label>
              </div>
              <p className="ot-ayuda">Gana quien llegue a {meta} sets.</p>
            </>)}
            {modo === 'ganador' && (
              <fieldset className="ot-ganador"><legend>¿Quién ganó?</legend>
                {['a', 'b'].map((lado) => (
                  <label key={lado} className={res.ganador === lado ? 'ot-ganador__opcion ot-ganador__opcion--activa' : 'ot-ganador__opcion'} data-area={claveArea(equipo(torneo, partido[lado])?.area)}>
                    <input type="radio" name="ganador" value={lado} checked={res.ganador === lado} onChange={() => setRes({ ...res, ganador: lado })} /><Escudo id={equipo(torneo, partido[lado])?.departamento} tam={32} decorativo /><span>{equipo(torneo, partido[lado])?.nombre}</span>
                  </label>))}
                {campo('ganador')}
              </fieldset>)}
            <label className="ot-campo">Observaciones <small>(opcional)</small><textarea rows="2" maxLength="300" value={res.observaciones} onChange={(e) => setRes({ ...res, observaciones: e.target.value })} placeholder="Criterio del jurado, incidencias…" /></label>
            <button type="submit" className="ot-boton" disabled={!!ocupado}>{ocupado === 'resultado' ? 'Guardando…' : esCorreccion(partido) ? 'Guardar corrección' : 'Guardar resultado'}</button>
          </form>
        ) : <p className="ot-nota">{!partido.a || !partido.b ? 'Todavía no se conocen los dos departamentos: se definen cuando termine la ronda anterior.' : `El enfrentamiento está ${partido.estado.replace('_', ' ')}.`}</p>}

        {puedeProgramar(partido) && (
          <form className="ot-bloque" onSubmit={guardarHorario} noValidate>
            <h3>Horario y lugar</h3>
            <div className="ot-fila">
              <label className="ot-campo">Fecha<input type="date" value={hor.fecha} onChange={(e) => setHor({ ...hor, fecha: e.target.value })} aria-invalid={!!errores.fecha} />{campo('fecha')}</label>
              <label className="ot-campo">Hora<input type="time" value={hor.hora} onChange={(e) => setHor({ ...hor, hora: e.target.value })} aria-invalid={!!errores.hora} />{campo('hora')}</label>
              <label className="ot-campo">Cancha o lugar<input maxLength="80" value={hor.lugar} onChange={(e) => setHor({ ...hor, lugar: e.target.value })} placeholder="Cancha 1" aria-invalid={!!errores.lugar} />{campo('lugar')}</label>
            </div>
            <p className="ot-ayuda">Debe estar dentro de las fechas oficiales de la edición. Un departamento no puede jugar dos partidos a la vez ni dos partidos usar la misma cancha.</p>
            <button type="submit" className="ot-boton ot-boton--claro" disabled={!!ocupado}>{ocupado === 'horario' ? 'Guardando…' : 'Guardar horario'}</button>
          </form>)}

        {transicionesDisponibles(partido).length > 0 && (
          <div className="ot-bloque"><h3>Estado: {partido.estado.replace('_', ' ')}</h3>
            <div className="ot-estados">{transicionesDisponibles(partido).map((e) => <button key={e} type="button" className="ot-boton ot-boton--chico" disabled={!!ocupado} onClick={() => ejecutar(e, 'estado', { estado: e }, 'Estado actualizado.')}>{ACCION_ESTADO[e] ?? e}</button>)}</div>
          </div>)}
        <p className="ot-nota">La validación de los resultados la hace administración.</p>
      </div>
    </div>
  )
}
