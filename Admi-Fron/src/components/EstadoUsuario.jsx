import { useState } from 'react'
import { cambiarEstado } from '../services/usuariosService'

export default function EstadoUsuario({ token, usuario, onActualizado, onSesionExpirada }) {
  const [confirmando, setConfirmando] = useState(false)
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')
  const proximo = usuario.activo === false

  async function aplicar() {
    setGuardando(true); setError('')
    try {
      const resultado = await cambiarEstado(token, usuario.id, proximo)
      setConfirmando(false)
      onActualizado(resultado)
    } catch (e) {
      if (e.estado === 401) onSesionExpirada(e.detalle)
      else setError(e.message)
    } finally { setGuardando(false) }
  }

  return <section className="gestion-estado" aria-labelledby="estado-titulo">
    <h3 id="estado-titulo">Estado de la cuenta</h3>
    <p className="suave">Una cuenta inactiva no podrá usar las rutas administrativas. El bloqueo del login en Spring Boot requiere su integración.</p>
    <label className="permiso-item"><span><strong>{usuario.activo === false ? 'Cuenta desactivada' : 'Cuenta activa'}</strong><small>Desactivar o reactivar acceso</small></span>
      <input type="checkbox" className="interruptor" checked={usuario.activo !== false} onChange={() => { setError(''); setConfirmando(true) }} /></label>
    {confirmando && <div className="confirmacion-fondo" role="presentation" onMouseDown={(e) => { if (e.target === e.currentTarget && !guardando) setConfirmando(false) }}>
      <div role="alertdialog" className="confirmacion-panel" aria-modal="true" aria-labelledby="conf-estado">
        <div className="confirmacion-icono" aria-hidden="true">!</div>
        <h2 id="conf-estado">{proximo ? 'Activar' : 'Desactivar'} cuenta</h2>
        <p>¿Confirmas {proximo ? 'la activación' : 'la desactivación'} de <strong>{usuario.nombre ?? usuario.correo}</strong>?</p>
        {error && <p role="alert" className="aviso aviso--error">{error}</p>}
        <div className="confirmacion-acciones"><button type="button" className="boton boton--claro" disabled={guardando} onClick={() => setConfirmando(false)}>Cancelar</button>
          <button type="button" className={proximo ? 'boton' : 'boton boton--peligro'} disabled={guardando} onClick={aplicar}>{guardando ? 'Guardando…' : (proximo ? 'Sí, activar' : 'Sí, desactivar')}</button></div>
      </div>
    </div>}
  </section>
}
