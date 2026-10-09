import { useEffect, useState } from 'react'
import { Agenda } from './components/Agenda'
import { Asistente } from './components/Asistente'
import { Catalogo } from './components/Catalogo'
import { Inicio } from './components/Inicio'
import { Login } from './components/Login'
import { PantallaBloqueada } from './components/PantallaBloqueada'
import { PantallaVacia } from './components/PantallaVacia'
import { Perfil } from './components/Perfil'
import { Shell } from './components/Shell'
import { useHash } from './hooks/useHash'
import { cerrarSesion, guardarSesion, leerSesion, quienSoy } from './services/authService'
import { normalizarRol, resolverRuta } from './services/permisos'

// Pantallas sin datos de sesión: cada una tiene su archivo para llenarla cuando toque su historia.
const SECCIONES = { inicio: Inicio, catalogo: Catalogo, agenda: Agenda, asistente: Asistente }

function App() {
  const [sesion, setSesion] = useState(leerSesion)
  const hash = useHash()

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
        window.location.hash = ''
        setSesion(null)
      })
    return () => {
      cancelado = true
    }
  }, [token])

  const salir = () => {
    cerrarSesion()
    window.location.hash = '' // la próxima persona no debe caer en la pantalla de la anterior
    setSesion(null)
  }

  if (!sesion) return <Login onSesion={setSesion} />

  // El rol decide qué menú se ve y qué pantallas se pueden abrir (permisos.js).
  const rol = normalizarRol(sesion.usuario.rol)
  const { ruta, pantalla, estado } = resolverRuta(hash, rol)

  let contenido
  if (estado !== 'permitida') {
    contenido = <PantallaBloqueada estado={estado} pantalla={pantalla} rol={rol} />
  } else if (ruta === 'perfil') {
    contenido = <Perfil usuario={sesion.usuario} rol={rol} token={sesion.token} onSalir={salir} />
  } else {
    const Seccion = SECCIONES[ruta]
    contenido = Seccion ? <Seccion /> : <PantallaVacia />
  }

  const permitida = estado === 'permitida'

  return (
    <Shell
      rol={rol}
      rutaActual={ruta}
      titulo={permitida ? pantalla.titulo : undefined}
      descripcion={permitida ? pantalla.descripcion : undefined}
    >
      {contenido}
    </Shell>
  )
}

export default App
