import { useCallback, useEffect, useState } from 'react'
import Icono from './Iconos'
import Escudo from './Escudo'
import { claveArea } from '../services/areas'
import { cerrarSemana, estadoCierre, reabrirSemana } from '../services/puntajesService'

/** Cierre de la T&C Week: publica al departamento ganador (con un aviso para todos). */
export default function CierreSemana({ token, onCambio }) {
  const [e, setE] = useState(null)
  const [confirmando, setConfirmando] = useState(false)
  const [mensaje, setMensaje] = useState('')
  const [ocupado, setOcupado] = useState(false)
  const cargar = useCallback(() => estadoCierre(token).then(setE).catch((x) => setMensaje(x?.message || 'No se pudo consultar el cierre.')), [token])
  useEffect(() => { cargar() }, [cargar])
  if (!e) return null

  async function accion(fn) {
    setOcupado(true); setMensaje('')
    try { setE(await fn(token)); setConfirmando(false); onCambio?.() } catch (x) { setMensaje(Object.values(x?.errores ?? {})[0] || x?.message || 'No se pudo completar.') } finally { setOcupado(false) }
  }

  if (e.cerrada) return (
    <section className="cierre cierre--cerrada" data-area={claveArea(e.ganador.area)}>
      <Escudo id={e.ganador.departamento} tam={64} decorativo />
      <div><p className="cierre__etiqueta">Ganador de la T&amp;C Week</p><h2>{e.ganador.area}</h2><p>{e.ganador.puntos} puntos. Se publicó un aviso para todos.</p></div>
      <button type="button" className="boton boton--claro boton--chico" onClick={() => accion(reabrirSemana)} disabled={ocupado}>Reabrir</button>
      {mensaje && <p className="aviso aviso--error" role="alert">{mensaje}</p>}
    </section>)

  return (
    <section className="cierre">
      <div><h2>Cierre de la T&amp;C Week</h2>
        <p className="suave">{e.lider ? (e.empate ? `Hay empate en el primer lugar (${e.tabla[0].area} y ${e.tabla[1].area}). Publica un desempate antes de cerrar.` : `Va primero ${e.lider.area} con ${e.lider.puntos} puntos. Al cerrar se publica como ganador y se envía un aviso a todos.`) : 'Todavía no hay puntos oficiales publicados.'}</p></div>
      {mensaje && <p className="aviso aviso--error" role="alert"><Icono nombre="alerta" tam={18} />{mensaje}</p>}
      {!confirmando ? <button type="button" className="boton" disabled={!e.lider || e.empate} onClick={() => setConfirmando(true)}><Icono nombre="trofeo" tam={18} /> Cerrar la semana y publicar al ganador</button> : (
        <div className="puntos-torneo__confirmar" role="alertdialog" aria-label="Confirmar cierre">
          <p><strong>¿Publicar a {e.lider.area} como ganador?</strong> Se enviará un aviso a todos los estudiantes. Podrás reabrir si fue un error.</p>
          <div className="puntos-torneo__botones"><button type="button" className="boton boton--claro" onClick={() => setConfirmando(false)} disabled={ocupado}>Todavía no</button><button type="button" className="boton" onClick={() => accion(cerrarSemana)} disabled={ocupado}>{ocupado ? 'Publicando…' : 'Sí, publicar ganador'}</button></div>
        </div>)}
    </section>)
}
