import { useState } from 'react'
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
        <h1 className="login__logo">VYNK</h1>
        <p className="login__sub">Panel de administración</p>

        <label className="campo">
          Correo institucional
          <input
            type="email"
            value={correo}
            onChange={(e) => setCorreo(e.target.value)}
            placeholder="nombre@tecsup.edu.pe"
            autoComplete="username"
            required
          />
        </label>

        <label className="campo">
          Contraseña
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
            required
          />
        </label>

        {error && (
          <p className="aviso aviso--error" role="alert">
            {error}
          </p>
        )}

        <button className="boton" type="submit" disabled={enviando}>
          {enviando ? 'Entrando…' : 'Entrar'}
        </button>
      </form>
    </main>
  )
}
