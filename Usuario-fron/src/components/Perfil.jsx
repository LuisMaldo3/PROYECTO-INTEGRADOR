import { useState } from 'react'
import { definirPassword } from '../services/authService'
import './Intereses.css'
import { ResumenIntereses } from './ResumenIntereses'
import { RolBadge } from './RolBadge'

/** Perfil: datos de la persona, contraseña opcional y cierre de sesión. */
export function Perfil({ usuario, rol, token, onSalir }) {
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
    <div className="perfil-contenido">
      <section className="perfil-seccion">
        <h2>Datos institucionales</h2>
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
            <dd><RolBadge rol={rol} /></dd>
          </div>
        </dl>
      </section>

      <section className="perfil-seccion">
        <h2>Contraseña (opcional)</h2>
        <form className="perfil-password" onSubmit={guardar}>
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
      </section>

      <section className="perfil-seccion perfil-seccion--ancha">
        <h2>Mis intereses</h2>
        <ResumenIntereses token={token} />
      </section>

      <div className="perfil-salida">
        <button type="button" className="btn-secundario" onClick={onSalir}>
          Cerrar sesión
        </button>
      </div>
    </div>
  )
}
