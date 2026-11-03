import { useCallback, useRef, useState } from 'react'
import DetalleUsuario from './components/DetalleUsuario'
import ListaUsuarios from './components/ListaUsuarios'
import Login from './components/Login'
import MenuAdmin from './components/MenuAdmin'
import PantallaActividades from './components/PantallaActividades'
import PantallaInscritos from './components/PantallaInscritos'
import PantallaReemplazos from './components/PantallaReemplazos'
import PantallaDemanda from './components/PantallaDemanda'
import PantallaAvisos from './components/PantallaAvisos'
import ClasificacionGeneral from './components/ClasificacionGeneral'
import { cerrarSesion, guardarSesion, leerSesion } from './services/authService'

export default function App() {
  const [sesion, setSesion] = useState(() => leerSesion())
  const [vista, setVista] = useState('actividades')
  const [seleccionadoId, setSeleccionadoId] = useState(null)
  const [aviso, setAviso] = useState('')
  const desplazamientoLista = useRef(0)
  const [versionLista, setVersionLista] = useState(0) // sube cuando cambia un rol: la lista se vuelve a pedir

  const abrirPerfil = (id) => {
    desplazamientoLista.current = window.scrollY
    setSeleccionadoId(id)
    window.scrollTo({ top: 0, behavior: 'instant' })
  }

  const cerrarPerfil = () => {
    setSeleccionadoId(null)
    requestAnimationFrame(() => window.scrollTo({ top: desplazamientoLista.current, behavior: 'instant' }))
  }

  const salir = useCallback(async () => {
    await cerrarSesion(sesion?.token)
    setSeleccionadoId(null)
    setSesion(null)
  }, [sesion])

  // Si el servidor responde 401 (token vencido o sesión cerrada) se vuelve al login.
  const sesionExpirada = useCallback((detalle) => {
    setAviso(
      detalle
        ? `El servidor rechazó el acceso (${detalle})`
        : 'Tu sesión expiró. Vuelve a iniciar sesión.',
    )
    cerrarSesion(null)
    setSeleccionadoId(null)
    setSesion(null)
  }, [])

  if (!sesion) {
    return (
      <Login
        aviso={aviso}
        onEntrar={(nueva) => {
          setAviso('')
          guardarSesion(nueva)
          setSesion(nueva)
        }}
      />
    )
  }

  return (
    <div className="panel">
      <MenuAdmin
        activa={vista}
        onElegir={(id) => {
          setVista(id)
          setSeleccionadoId(null)
          window.scrollTo({ top: 0 })
        }}
        usuario={sesion.usuario}
        onCerrarSesion={salir}
      />

      <main className="contenido" id="contenido">
        {vista === 'usuarios' && (
          <div className="usuarios-vista">
            <div className="usuarios-vista__listado" hidden={Boolean(seleccionadoId)}>
              <ListaUsuarios
                token={sesion.token}
                seleccionadoId={seleccionadoId}
                onSeleccionar={abrirPerfil}
                onSesionExpirada={sesionExpirada}
                version={versionLista}
              />
            </div>
            {seleccionadoId && (
              <DetalleUsuario
                token={sesion.token}
                usuarioId={seleccionadoId}
                onCerrar={cerrarPerfil}
                onRolCambiado={() => setVersionLista((n) => n + 1)}
                onSesionExpirada={sesionExpirada}
              />
            )}
          </div>
        )}
        {vista === 'actividades' && <PantallaActividades token={sesion.token} onSesionExpirada={sesionExpirada} />}
        {vista === 'inscritos' && <PantallaInscritos token={sesion.token} onSesionExpirada={sesionExpirada} />}
        {vista === 'reemplazos' && <PantallaReemplazos token={sesion.token} onSesionExpirada={sesionExpirada} />}
        {vista === 'demanda' && <PantallaDemanda token={sesion.token} onSesionExpirada={sesionExpirada} />}
        {vista === 'avisos' && <PantallaAvisos token={sesion.token} onSesionExpirada={sesionExpirada} />}
        {vista === 'clasificacion' && <ClasificacionGeneral token={sesion.token} />}
      </main>
    </div>
  )
}
