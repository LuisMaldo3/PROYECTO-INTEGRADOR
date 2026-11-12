import { useEffect, useState } from 'react'
import Icono from './Iconos'
import Escudo from './Escudo'
import { claveArea } from '../services/areas'
import { estadoPuestos, puntosDelPuesto, puntosEscritos } from '../services/torneos'
import { eliminarTorneo, enviarPuestos, participantesDisponibles } from '../services/torneosService'

/** Puestos finales del torneo → «Resultados» de la competencia (los publica el administrador), y eliminar el torneo. */
export default function PuntosTorneo({ token, torneo, onActualizado, onEliminado, onSesionExpirada }) {
  const [puntos, setPuntos] = useState([])
  const [valores, setValores] = useState({})
  const [mensaje, setMensaje] = useState({ tipo: '', texto: '' })
  const [ocupado, setOcupado] = useState('')
  const [confirmando, setConfirmando] = useState(false)
  const estado = estadoPuestos(torneo)
  // Lo que se ve en cada casilla: lo escrito o, si no, los puntos que tuviera configurados la competencia.
  const valorDe = (puesto) => valores[puesto] ?? (puntos.length >= puesto ? String(puntos[puesto - 1]) : '')

  useEffect(() => {
    let activo = true
    if (torneo.actividadId) participantesDisponibles(token, torneo.actividadId).then((r) => { if (activo) setPuntos(r.puntosPorPuesto ?? []) }).catch(() => {})
    return () => { activo = false }
  }, [token, torneo.actividadId])

  async function enviar(publicar) {
    const cuerpo = puntosEscritos(torneo.posiciones ?? [], Object.fromEntries((torneo.posiciones ?? []).map((p) => [p.puesto, valorDe(p.puesto)])))
    if (!cuerpo) { setMensaje({ tipo: 'error', texto: 'Escribe los puntos de todos los puestos (números enteros desde 0).' }); return }
    setOcupado(publicar ? 'publicar' : 'guardar'); setMensaje({ tipo: '', texto: '' })
    try {
      const r = await enviarPuestos(token, torneo.id, cuerpo, publicar)
      onActualizado(r)
      if (publicar && !r.resultadosPublicados) setMensaje({ tipo: 'error', texto: 'Los puntos se guardaron, pero no se pudieron publicar. Inténtalo de nuevo.' })
      else setMensaje({ tipo: 'ok', texto: publicar ? 'Resultados oficiales publicados. Los puntos ya suman a la tabla acumulada.' : 'Puntos guardados sin publicar.' })
    } catch (e) {
      if (e?.estado === 401) { onSesionExpirada?.(e.detalle); return }
      setMensaje({ tipo: 'error', texto: Object.values(e?.errores ?? {})[0] || e?.message || 'No se pudieron guardar los puntos.' })
    } finally { setOcupado('') }
  }

  async function eliminar() {
    setOcupado('eliminar'); setMensaje({ tipo: '', texto: '' })
    try { await eliminarTorneo(token, torneo.id); onEliminado(torneo.id) }
    catch (e) {
      if (e?.estado === 401) { onSesionExpirada?.(e.detalle); return }
      setMensaje({ tipo: 'error', texto: e?.message || 'No se pudo eliminar el torneo.' }); setOcupado(''); setConfirmando(false)
    }
  }

  return (
    <div className="puntos-torneo">
      {mensaje.texto && <p className={`aviso ${mensaje.tipo === 'ok' ? 'aviso--ok' : 'aviso--error'}`} role={mensaje.tipo === 'ok' ? 'status' : 'alert'}><Icono nombre={mensaje.tipo === 'ok' ? 'check' : 'alerta'} tam={18} />{mensaje.texto}</p>}

      <section className="puntos-torneo__bloque" aria-labelledby="pt-pos">
        <div><h3 id="pt-pos">Puestos finales y puntos</h3><p className="suave">Los puestos salen de los resultados validados. Escribe cuántos puntos recibe cada puesto y publica: así suman a la tabla acumulada.</p></div>
        <p className={`puntos-torneo__estado puntos-torneo__estado--${estado.tono}`}><Icono nombre={estado.tono === 'ok' ? 'check' : 'alerta'} tam={16} />{estado.texto}</p>
        {torneo.posiciones?.length > 0 && (
          <ol className="puntos-torneo__posiciones">
            {torneo.posiciones.map((p) => (
              <li key={p.puesto} data-area={claveArea(p.participante?.area)}>
                <span className="puntos-torneo__puesto">{p.puesto}</span><Escudo id={p.participante?.departamento} tam={28} decorativo />
                <span><strong>{p.participante?.nombre}</strong>{p.participante?.area && p.participante.area !== p.participante.nombre && <small className="puntos-torneo__area">{p.participante.area}</small>}</span>
                {torneo.resultadosPublicados ? <span className="puntos-torneo__pts">{valorDe(p.puesto) || puntosDelPuesto(puntos, p.puesto)} pts</span> : (
                  <label className="puntos-torneo__campo"><input type="number" min="0" max="10000" step="1" inputMode="numeric" value={valorDe(p.puesto)} onChange={(e) => setValores({ ...valores, [p.puesto]: e.target.value })} aria-label={`Puntos del ${p.puesto}.º puesto`} /> pts</label>)}
              </li>))}
          </ol>)}
        {torneo.posiciones?.length > 0 && !torneo.resultadosPublicados && (
          <div className="modal__acciones">
            <button type="button" className="boton boton--claro" onClick={() => enviar(false)} disabled={!!ocupado}>{ocupado === 'guardar' ? 'Guardando…' : 'Guardar sin publicar'}</button>
            <button type="button" className="boton" onClick={() => enviar(true)} disabled={!!ocupado}>{ocupado === 'publicar' ? 'Publicando…' : 'Publicar resultados oficiales'}</button>
          </div>)}
      </section>

      <section className="puntos-torneo__bloque puntos-torneo__peligro" aria-labelledby="pt-el">
        <div><h3 id="pt-el">Eliminar torneo</h3><p className="suave">¿Te equivocaste al crearlo? Se borra el torneo con sus partidos y resultados. Los resultados oficiales ya publicados de la competencia no se tocan. No se puede deshacer.</p></div>
        {!confirmando ? <button type="button" className="boton boton--peligro" onClick={() => setConfirmando(true)}>Eliminar este torneo</button> : (
          <div className="puntos-torneo__confirmar" role="alertdialog" aria-label="Confirmar eliminación">
            <p><strong>¿Eliminar el torneo de «{torneo.deporte}»?</strong> Se pierden sus llaves y resultados.</p>
            <div className="puntos-torneo__botones"><button type="button" className="boton boton--claro" onClick={() => setConfirmando(false)} disabled={!!ocupado}>No, conservarlo</button><button type="button" className="boton boton--peligro" onClick={eliminar} disabled={!!ocupado}>{ocupado === 'eliminar' ? 'Eliminando…' : 'Sí, eliminar'}</button></div>
          </div>)}
      </section>
    </div>
  )
}
