import { useState } from 'react'
import Icono from './Iconos'
import Logo from './Logo'
import { AuthError, loginAdmin } from '../services/authService'

const AREAS = [['gestion', 'Gestión y Producción'], ['digital', 'Tecnología Digital'], ['quimica', 'Minería y Procesos Químicos'], ['electro', 'Electricidad y Electrónica'], ['mecanica', 'Mecánica']]

export default function Login({ onEntrar, aviso = '' }) {
  const [correo, setCorreo] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(aviso)
  const [enviando, setEnviando] = useState(false)
  const [verClave, setVerClave] = useState(false)

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
    <div className="login">
      <section className="login__campus" aria-label="VYNK, panel de administración">
        <Logo />
        <div className="login__texto">
          <h2>Gestiona actividades, avisos y resultados de Tecsup.</h2>
          <p>Lo que publiques aquí aparece en la web y la app de los estudiantes.</p>
        </div>
        <ul className="login__areas" aria-label="Áreas de Tecsup">
          {AREAS.map(([clave, area]) => <li key={clave} data-area={clave}>{area}</li>)}
        </ul>
      </section>

      <main className="login__area">
        <form className="login__formulario" onSubmit={enviar}>
          <div className="login__marca-movil"><Logo claro /></div>
          <h1>Panel de administración</h1>
          <p className="login__sub">Entra con tu cuenta de administrador.</p>
          <label className="campo">Correo institucional
            <span className="entrada"><Icono nombre="correo" tam={18}/><input type="email" value={correo} onChange={(e) => setCorreo(e.target.value)} placeholder="nombre@tecsup.edu.pe" autoComplete="username" required autoFocus/></span>
          </label>
          <label className="campo">Contraseña
            <span className="entrada"><Icono nombre="candado" tam={18}/><input type={verClave ? 'text' : 'password'} value={password} onChange={(e) => setPassword(e.target.value)} placeholder="Tu contraseña" autoComplete="current-password" required/><button type="button" className="entrada__mostrar" onClick={() => setVerClave((v) => !v)} aria-label={verClave ? 'Ocultar contraseña' : 'Mostrar contraseña'}>{verClave ? 'Ocultar' : 'Mostrar'}</button></span>
          </label>
          {error && <p className="aviso aviso--error" role="alert"><Icono nombre="alerta" tam={18}/>{error}</p>}
          <button className="boton boton--grande" type="submit" disabled={enviando}>{enviando ? 'Verificando…' : 'Entrar al panel'}</button>
          <p className="login__pie">Solo para cuentas con rol de administrador.</p>
        </form>
      </main>
    </div>
  )
}
