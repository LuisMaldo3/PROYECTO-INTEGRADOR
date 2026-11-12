import { useEffect, useRef, useState } from 'react'
import Icono from './Iconos'
import Escudo from './Escudo'
import { claveArea } from '../services/areas'
import { ACCION_ESTADO, cuerpoResultado, esCorreccion, formularioDeResultado, puedeProgramar, puedeRegistrar, puedeValidar, transicionesDisponibles } from '../services/torneos'
import { cambiarEstadoPartido, programarPartido, registrarResultado, validarResultado } from '../services/torneosService'

/**
 * Ventana de un enfrentamiento: resultado, horario y lugar, estado y validación.
 * Cada botón solo aparece si la acción es posible; el backend vuelve a comprobarlo todo.
 */
export default function PanelPartido({ token, torneo, partido, puedeValidarResultados, onActualizado, onCerrar, onSesionExpirada }) {
  const [res, setRes] = useState(() => formularioDeResultado(partido))
  const [hor, setHor] = useState({ fecha: partido.fecha || '', hora: partido.hora || '', lugar: partido.lugar || '' })
  const [errores, setErrores] = useState({})
  const [mensaje, setMensaje] = useState({ tipo: '', texto: '' })
  const [ocupado, setOcupado] = useState('')
  const cerrarRef = useRef(null)
  const modo = torneo.modoResultado

  useEffect(() => {
    cerrarRef.current?.focus()
    const salir = (e) => { if (e.key === 'Escape') onCerrar() }
    document.addEventListener('keydown', salir)
    return () => document.removeEventListener('keydown', salir)
  }, [onCerrar])

  async function ejecutar(nombre, accion, exito) {
    setOcupado(nombre); setErrores({}); setMensaje({ tipo: '', texto: '' })
    try {
      onActualizado(await accion())
      setMensaje({ tipo: 'ok', texto: exito })
    } catch (e) {
      if (e?.estado === 401) { onSesionExpirada?.(e.detalle); return }
      setErrores(e?.errores ?? {})
      const lista = Object.values(e?.errores ?? {})
      setMensaje({ tipo: 'error', texto: lista.length === 1 ? lista[0] : lista.length ? 'Revisa los datos marcados.' : e?.message || 'No se pudo completar la acción.' })
    } finally { setOcupado('') }
  }

  const guardarResultado = (ev) => { ev.preventDefault(); ejecutar('resultado', () => registrarResultado(token, torneo.id, partido.id, cuerpoResultado(modo, res)), esCorreccion(partido) ? 'Resultado corregido. Debe validarse otra vez.' : 'Resultado guardado. El ganador ya avanzó a la siguiente ronda.') }
  const guardarHorario = (ev) => { ev.preventDefault(); ejecutar('horario', () => programarPartido(token, torneo.id, partido.id, hor), 'Horario guardado.') }
  const cambiarEstado = (estado) => ejecutar(estado, () => cambiarEstadoPartido(token, torneo.id, partido.id, estado), 'Estado actualizado.')
  const validar = () => ejecutar('validar', () => validarResultado(token, torneo.id, partido.id), 'Resultado validado.')

  const campo = (clave) => errores[clave] && <span className="campo__error" role="alert">{errores[clave]}</span>
  const empate = modo === 'marcador' && res.a !== '' && res.b !== '' && Number(res.a) === Number(res.b)
  const registrar = puedeRegistrar(partido)
  const metaSets = torneo.setsParaGanar ?? 2

  return (
    <div className="modal" role="presentation" onMouseDown={(e) => { if (e.target === e.currentTarget) onCerrar() }}>
      <div className="modal__ventana modal__ventana--ancha partido-panel" role="dialog" aria-modal="true" aria-labelledby="partido-titulo">
        <div className="modal__cabecera">
          <div className="modal__distintivo" aria-hidden="true"><Icono nombre={torneo.tipo === 'karaduo' ? 'versus' : 'balon'} tam={22} /></div>
          <div>
            <h2 id="partido-titulo">{partido.nombreRonda}</h2>
            <p>{torneo.deporte} · {partido.a?.nombre ?? 'Por definir'} vs {partido.b?.nombre ?? 'Por definir'}</p>
          </div>
          <button type="button" className="detalle__cerrar" onClick={onCerrar} ref={cerrarRef} aria-label="Cerrar"><Icono nombre="cerrar" tam={20} /></button>
        </div>

        <div className="partido-panel__cruce" aria-hidden="true">
          {[partido.a, partido.b].map((e, i) => (
            <div key={i} className="partido-panel__equipo" data-area={e ? claveArea(e.area) : undefined}>
              <Escudo id={e?.departamento} tam={56} decorativo /><strong>{e?.nombre ?? 'Por definir'}</strong>
            </div>
          ))}
          <span className="partido-panel__vs">VS</span>
        </div>

        {mensaje.texto && <p className={`aviso ${mensaje.tipo === 'ok' ? 'aviso--ok' : 'aviso--error'}`} role={mensaje.tipo === 'ok' ? 'status' : 'alert'}><Icono nombre={mensaje.tipo === 'ok' ? 'check' : 'alerta'} tam={18} />{mensaje.texto}</p>}

        {registrar ? (
          <form className="partido-panel__bloque" onSubmit={guardarResultado} noValidate>
            <h3>{esCorreccion(partido) ? 'Corregir resultado' : 'Registrar resultado'}</h3>
            {modo === 'marcador' && (
              <>
                <div className="marcador-campos">
                  <label className="campo">{partido.a.nombre}<input type="number" min="0" max="999" inputMode="numeric" value={res.a} onChange={(e) => setRes({ ...res, a: e.target.value })} aria-invalid={!!errores.a} />{campo('a')}</label>
                  <span aria-hidden="true">–</span>
                  <label className="campo">{partido.b.nombre}<input type="number" min="0" max="999" inputMode="numeric" value={res.b} onChange={(e) => setRes({ ...res, b: e.target.value })} aria-invalid={!!errores.b} />{campo('b')}</label>
                </div>
                {empate && (
                  <label className="campo">Empate: ¿quién ganó en el desempate?
                    <select value={res.ganadorPenales} onChange={(e) => setRes({ ...res, ganadorPenales: e.target.value })} aria-invalid={!!errores.ganadorPenales}>
                      <option value="">Elige un departamento</option><option value="a">{partido.a.nombre}</option><option value="b">{partido.b.nombre}</option>
                    </select>{campo('ganadorPenales')}
                  </label>)}
              </>)}
            {modo === 'sets' && (
              <div className="marcador-campos">
                <label className="campo">Sets de {partido.a.nombre}<input type="number" min="0" max={metaSets} inputMode="numeric" value={res.setsA} onChange={(e) => setRes({ ...res, setsA: e.target.value })} aria-invalid={!!errores.setsA} />{campo('setsA')}</label>
                <span aria-hidden="true">–</span>
                <label className="campo">Sets de {partido.b.nombre}<input type="number" min="0" max={metaSets} inputMode="numeric" value={res.setsB} onChange={(e) => setRes({ ...res, setsB: e.target.value })} aria-invalid={!!errores.setsB} />{campo('setsB')}</label>
              </div>)}
            {modo === 'sets' && <p className="campo__ayuda">Gana quien llegue a {metaSets} sets.</p>}
            {modo === 'ganador' && (
              <fieldset className="elegir-ganador"><legend>¿Quién ganó?</legend>
                {['a', 'b'].map((lado) => (
                  <label key={lado} className={res.ganador === lado ? 'elegir-ganador__opcion elegir-ganador__opcion--activa' : 'elegir-ganador__opcion'} data-area={claveArea(partido[lado].area)}>
                    <input type="radio" name="ganador" value={lado} checked={res.ganador === lado} onChange={() => setRes({ ...res, ganador: lado })} />
                    <Escudo id={partido[lado].departamento} tam={34} decorativo /><span>{partido[lado].nombre}</span>
                  </label>))}
                {campo('ganador')}
              </fieldset>)}
            <label className="campo">Observaciones <small>(opcional)</small><textarea rows="2" maxLength="300" value={res.observaciones} onChange={(e) => setRes({ ...res, observaciones: e.target.value })} placeholder="Criterio del jurado, incidencias…" /></label>
            <div className="modal__acciones"><button type="submit" className="boton" disabled={!!ocupado}>{ocupado === 'resultado' ? 'Guardando…' : esCorreccion(partido) ? 'Guardar corrección' : 'Guardar resultado'}</button></div>
          </form>
        ) : (
          <p className="suave partido-panel__nota">{!partido.a || !partido.b ? 'Todavía no se conocen los dos departamentos: se definen cuando termine la ronda anterior.' : `El enfrentamiento está ${partido.estado.replace('_', ' ')}.`}</p>
        )}

        {puedeValidar(partido) && puedeValidarResultados && (
          <div className="partido-panel__bloque partido-panel__validar">
            <div><h3>Validar resultado</h3><p className="suave">Un resultado validado deja de ser provisional para los estudiantes.</p></div>
            <button type="button" className="boton boton--claro" onClick={validar} disabled={!!ocupado}><Icono nombre="check" tam={18} />{ocupado === 'validar' ? 'Validando…' : 'Validar'}</button>
          </div>)}

        {puedeProgramar(partido) && (
          <form className="partido-panel__bloque" onSubmit={guardarHorario} noValidate>
            <h3>Horario y lugar</h3>
            <div className="campo-fila">
              <label className="campo">Fecha<input type="date" value={hor.fecha} onChange={(e) => setHor({ ...hor, fecha: e.target.value })} aria-invalid={!!errores.fecha} />{campo('fecha')}</label>
              <label className="campo">Hora<input type="time" value={hor.hora} onChange={(e) => setHor({ ...hor, hora: e.target.value })} aria-invalid={!!errores.hora} />{campo('hora')}</label>
              <label className="campo">Cancha o lugar<input maxLength="80" value={hor.lugar} onChange={(e) => setHor({ ...hor, lugar: e.target.value })} placeholder="Cancha 1" aria-invalid={!!errores.lugar} />{campo('lugar')}</label>
            </div>
            <p className="campo__ayuda">Un departamento no puede jugar dos partidos a la vez, ni dos partidos usar la misma cancha. El sistema lo revisa en todos los torneos.</p>
            <div className="modal__acciones"><button type="submit" className="boton boton--claro" disabled={!!ocupado}>{ocupado === 'horario' ? 'Guardando…' : 'Guardar horario'}</button></div>
          </form>)}

        {transicionesDisponibles(partido).length > 0 && (
          <div className="partido-panel__bloque">
            <h3>Estado: {partido.estado.replace('_', ' ')}</h3>
            <div className="partido-panel__estados">
              {transicionesDisponibles(partido).map((e) => (
                <button key={e} type="button" className={`boton boton--chico${e === 'cancelado' ? ' boton--peligro-discreto' : ''}`} onClick={() => cambiarEstado(e)} disabled={!!ocupado}>{ACCION_ESTADO[e] ?? e}</button>))}
            </div>
          </div>)}
      </div>
    </div>
  )
}
