import { useState } from 'react'
import Icono from './Iconos'
import Logo from './Logo'
import { AuthError, loginAdmin } from '../services/authService'

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
    <main className="login">
      <div className="login__contenedor">
        <aside className="login__presentacion" aria-label="VYNK administración">
          <div className="login__brand"><Logo /><span className="login__brand-badge">ADMIN</span></div>
          <div className="login__presentacion-texto">
            <span className="login__eyebrow">TU CENTRO DE GESTIÓN</span>
            <h2>Todo bajo control.<br/><em>Todo en VYNK.</em></h2>
            <p>Coordina actividades, gestiona a tu comunidad y mantén a todos informados desde un solo espacio.</p>
            <div className="login__funciones"><span><Icono nombre="calendario" tam={17}/> Actividades</span><span><Icono nombre="usuarios" tam={17}/> Comunidad</span><span><Icono nombre="correo" tam={17}/> Avisos</span></div>
          </div>
          <div className="login__orbita" aria-hidden="true"><div className="login__orbita-centro"><Icono nombre="calendario" tam={48}/></div><span className="login__orbita-item login__orbita-item--uno"><Icono nombre="usuarios" tam={24}/></span><span className="login__orbita-item login__orbita-item--dos"><Icono nombre="correo" tam={24}/></span><span className="login__orbita-item login__orbita-item--tres"><Icono nombre="check" tam={24}/></span></div>
          <div className="login__pie-marca">VYNK · Gestión de comunidad universitaria</div>
        </aside>
        <section className="login__area">
          <form className="login__tarjeta" onSubmit={enviar}>
            <div className="login__icono"><Icono nombre="candado" tam={23}/></div>
            <p className="eyebrow">ACCESO PARA ADMINISTRADORES</p>
            <h1 className="login__titulo">Bienvenido de nuevo</h1>
            <p className="login__sub">Ingresa tus credenciales para acceder al panel de administración.</p>
            <label className="campo">Correo institucional
              <span className="entrada"><Icono nombre="correo" tam={18}/><input type="email" value={correo} onChange={(e) => setCorreo(e.target.value)} placeholder="nombre@tecsup.edu.pe" autoComplete="username" required autoFocus/></span>
            </label>
            <label className="campo">Contraseña
              <span className="entrada"><Icono nombre="candado" tam={18}/><input type={verClave ? 'text' : 'password'} value={password} onChange={(e) => setPassword(e.target.value)} placeholder="Ingresa tu contraseña" autoComplete="current-password" required/><button type="button" className="entrada__mostrar" onClick={() => setVerClave((v) => !v)} aria-label={verClave ? 'Ocultar contraseña' : 'Mostrar contraseña'}>{verClave ? 'Ocultar' : 'Mostrar'}</button></span>
            </label>
            {error && <p className="aviso aviso--error" role="alert"><Icono nombre="alerta" tam={18}/>{error}</p>}
            <button className="boton login__enviar" type="submit" disabled={enviando}>{enviando ? 'Verificando acceso…' : 'Ingresar al panel'} <span aria-hidden="true">→</span></button>
            <p className="login__pie-form">Acceso exclusivo para cuentas autorizadas.</p>
          </form>
        </section>
      </div>
    </main>
  )
}
