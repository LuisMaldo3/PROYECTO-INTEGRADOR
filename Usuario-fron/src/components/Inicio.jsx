import { useState } from 'react'
import { definirPassword } from '../services/authService'
import { Fondo } from './Fondo'

/** Pantalla a la que se llega después de iniciar sesión. */
export function Inicio({ usuario, token, onSalir }) {
  const [nueva, setNueva] = useState('')
  const [mensaje, setMensaje] = useState(null) // { tipo: 'ok' | 'error', texto }
  const [guardando, setGuardando] = useState(false)

  const guardar = async (e) => {
    e.preventDefault()
    setMensaje(null)
    setGuardando(true)
    try {
      await definirPassword(token, nueva)
      setNueva('')
      setMensaje({ tipo: 'ok', texto: 'Contraseña guardada. Ya puedes entrar con tu correo y contraseña.' })
    } catch (err) {
      setMensaje({ tipo: 'error', texto: err.message })
    } finally {
      setGuardando(false)
    }
  }

  return (
    <Fondo>
      <h2 className="card-title">¡Hola, {usuario.nombre?.split(' ')[0] || 'bienvenido'}!</h2>
      <p className="card-subtitle">Ya iniciaste sesión.</p>

      <dl className="perfil-datos">
        <div>
          <dt>Nombre</dt>
          <dd>{usuario.nombre || '—'}</dd>
        </div>
        <div>
          <dt>Correo</dt>
          <dd>{usuario.correo}</dd>
        </div>
        <div>
          <dt>Rol</dt>
          <dd><span className="rol-badge">{usuario.rol}</span></dd>
        </div>
      </dl>

      {/* Opcional: solo se muestra si la persona lo abre */}
      <details className="opcion-password">
        <summary>Configurar contraseña (opcional)</summary>
        <form onSubmit={guardar}>
          <p className="card-nota">Con una contraseña podrás entrar con tu correo sin usar Google.</p>
          <label htmlFor="nueva-password">Nueva contraseña</label>
          <div className="input-box">
            <input
              id="nueva-password"
              type="password"
              autoComplete="new-password"
              placeholder="Mínimo 8 caracteres"
              value={nueva}
              onChange={(e) => setNueva(e.target.value)}
              disabled={guardando}
              minLength={8}
              maxLength={64}
              required
            />
          </div>
          {mensaje && (
            <div className={mensaje.tipo === 'ok' ? 'card-ok-msg' : 'card-error-msg'} role={mensaje.tipo === 'ok' ? 'status' : 'alert'}>
              {mensaje.texto}
            </div>
          )}
          <button type="submit" className="btn-blue-submit" disabled={guardando}>
            {guardando ? 'Guardando…' : 'Guardar contraseña'}
          </button>
        </form>
      </details>

      <button type="button" className="btn-secundario" onClick={onSalir}>
        Cerrar sesión
      </button>
    </Fondo>
  )
}
