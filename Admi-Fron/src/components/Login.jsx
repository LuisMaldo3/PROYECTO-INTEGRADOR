import { useState } from 'react'
import Icono from './Iconos'
import Logo from './Logo'
import { AuthError, loginAdmin } from '../services/authService'

export default function Login({ onEntrar, aviso = '' }) {
  const [correo, setCorreo] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(aviso)
  const [enviando, setEnviando] = useState(false)

  async function enviar(evento) {
    evento.preventDefault()
    setError('')
    setEnviando(true)
    try {
      onEntrar(await loginAdmin(correo, password))
    } catch (e) {
      setError(e instanceof AuthError ? e.message : 'No se pudo iniciar sesión. Inténtalo nuevamente.')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <main className="login">
      <form className="login__tarjeta" onSubmit={enviar}>
        <div className="login__logo">
          <Logo tam={52} texto={false} />
        </div>
        <h1 className="login__titulo">VYNK Admin</h1>
        <p className="login__sub">Ingresa con tu cuenta de administrador</p>

        <label className="campo">
          Correo institucional
          <span className="entrada">
            <Icono nombre="correo" tam={18} />
            <input
              type="email"
              value={correo}
              onChange={(e) => setCorreo(e.target.value)}
              placeholder="nombre@tecsup.edu.pe"
              autoComplete="username"
              required
            />
          </span>
        </label>

        <label className="campo">
          Contraseña
          <span className="entrada">
            <Icono nombre="candado" tam={18} />
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
              required
            />
          </span>
        </label>

        {error && (
          <p className="aviso aviso--error" role="alert">
            <Icono nombre="alerta" tam={18} /> {error}
          </p>
        )}

        <button className="boton" type="submit" disabled={enviando}>
          {enviando ? 'Entrando…' : 'Entrar'}
        </button>
      </form>
    </main>
  )
}
