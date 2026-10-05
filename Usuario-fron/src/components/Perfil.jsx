import { useState } from 'react'
import { definirPassword } from '../services/authService'
import { Fondo } from './Fondo'

/** Pantalla que se muestra tras iniciar sesión. Permite crear/cambiar la contraseña. */
export function Perfil({ usuario, token, onSalir }) {
  const inicial = (usuario.nombre || usuario.correo || '?').trim().charAt(0).toUpperCase()
  const [nueva, setNueva] = useState('')
  const [mensaje, setMensaje] = useState(null) // { tipo: 'ok' | 'error', texto }
  const [guardando, setGuardando] = useState(false)

  const guardar = async (e) => {
    e.preventDefault()
    setMensaje(null)
    if (nueva.length < 8) {
      setMensaje({ tipo: 'error', texto: 'La contraseña debe tener al menos 8 caracteres.' })
      return
    }
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
      <div className="avatar" aria-hidden="true">{inicial}</div>
      <h2 className="card-title">¡Hola, {usuario.nombre?.split(' ')[0] || 'bienvenido'}!</h2>
      <p className="card-subtitle">Iniciaste sesión correctamente.</p>

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

      <form className="perfil-password" onSubmit={guardar}>
        <label htmlFor="nueva-password">Crear o cambiar contraseña</label>
        <p className="card-nota">Con una contraseña podrás entrar con tu correo sin usar Google.</p>
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

      <button type="button" className="btn-secundario" onClick={onSalir}>
        Cerrar sesión
      </button>
    </Fondo>
  )
}
