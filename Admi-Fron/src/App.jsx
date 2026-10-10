import { useCallback, useState } from 'react'
import DetalleUsuario from './components/DetalleUsuario'
import ListaUsuarios from './components/ListaUsuarios'
import Login from './components/Login'
import MenuAdmin from './components/MenuAdmin'
import { cerrarSesion, guardarSesion, leerSesion } from './services/authService'

export default function App() {
  const [sesion, setSesion] = useState(() => leerSesion())
  const [vista, setVista] = useState('usuarios')
  const [seleccionadoId, setSeleccionadoId] = useState(null)
  const [aviso, setAviso] = useState('')

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
        }}
        usuario={sesion.usuario}
        onCerrarSesion={salir}
      />

      <main className="contenido">
        {vista === 'usuarios' && (
          <>
            <ListaUsuarios
              token={sesion.token}
              seleccionadoId={seleccionadoId}
              onSeleccionar={setSeleccionadoId}
              onSesionExpirada={sesionExpirada}
            />
            {seleccionadoId && (
              <DetalleUsuario
                token={sesion.token}
                usuarioId={seleccionadoId}
                onCerrar={() => setSeleccionadoId(null)}
                onSesionExpirada={sesionExpirada}
              />
            )}
          </>
        )}
      </main>
    </div>
  )
}
