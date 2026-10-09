import { useEffect, useState } from 'react'
import { Login } from './components/Login'
import { Inicio } from './components/Inicio'
import { cerrarSesion, guardarSesion, leerSesion, quienSoy } from './services/authService'

function App() {
  const [sesion, setSesion] = useState(leerSesion)

  // Al abrir la web con una sesión guardada, se pregunta al backend «quién soy»:
  // así el rol mostrado es el actual (el administrador pudo cambiarlo) y, si la
  // cuenta ya no existe o está desactivada, se cierra la sesión. Si no hay conexión,
  // se mantiene lo guardado.
  const token = sesion?.token
  useEffect(() => {
    if (!token) return undefined
    let cancelado = false
    quienSoy(token)
      .then((usuario) => {
        if (cancelado) return
        const actualizada = { token, usuario }
        guardarSesion(actualizada)
        setSesion(actualizada)
      })
      .catch((e) => {
        if (cancelado || e.codigo !== 'SESION_EXPIRADA') return
        cerrarSesion()
        setSesion(null)
      })
    return () => {
      cancelado = true
    }
  }, [token])

  const salir = () => {
    cerrarSesion()
    setSesion(null)
  }

  return sesion ? (
    <Inicio usuario={sesion.usuario} token={sesion.token} onSalir={salir} />
  ) : (
    <Login onSesion={setSesion} />
  )
}

export default App
