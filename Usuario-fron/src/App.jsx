import { useState } from 'react'
import { Login } from './components/Login'
import { Inicio } from './components/Inicio'
import { cerrarSesion, leerSesion } from './services/authService'

function App() {
  const [sesion, setSesion] = useState(leerSesion)

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
