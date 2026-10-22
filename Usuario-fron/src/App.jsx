import { useEffect, useState } from 'react'
import { Agenda } from './components/Agenda'
import { Asistente } from './components/Asistente'
import { Catalogo } from './components/Catalogo'
import { Inicio } from './components/Inicio'
import { Intereses } from './components/Intereses'
import { Recomendaciones } from './components/Recomendaciones'
import { OrganizadorActividades } from './components/OrganizadorActividades'
import { OrganizadorInscritos } from './components/OrganizadorInscritos'
import { OrganizadorAsistencia } from './components/OrganizadorAsistencia'
import { OrganizadorReporte } from './components/OrganizadorReporte'
import { Login } from './components/Login'
import { PantallaBloqueada } from './components/PantallaBloqueada'
import { PantallaVacia } from './components/PantallaVacia'
import { Perfil } from './components/Perfil'
import { Shell } from './components/Shell'
import { useHash } from './hooks/useHash'
import { cerrarSesion, cerrarSesionEnServidor, guardarSesion, leerSesion, quienSoy, sesionCerradaEnOtraPestana } from './services/authService'
import { normalizarRol, resolverRuta, pantallaDe } from './services/permisos'
import { consultarEstadoIntereses, omitirIntereses } from './services/interesesService'
import { debeSolicitarIntereses } from './services/intereses'

// Pantallas sin datos de sesión: cada una tiene su archivo para llenarla cuando toque su historia.
const SECCIONES = { inicio: Inicio, catalogo: Catalogo, agenda: Agenda, asistente: Asistente, recomendaciones: Recomendaciones, 'organizador-actividades': OrganizadorActividades, 'organizador-inscritos': OrganizadorInscritos, 'organizador-asistencia': OrganizadorAsistencia, 'organizador-reporte': OrganizadorReporte }

function App() {
  const [sesion, setSesion] = useState(leerSesion)
  const hash = useHash()
  // El estado se consulta por token, no por dispositivo: cambiar de usuario no hereda el onboarding.
  const [inicioIntereses, setInicioIntereses] = useState({ token: null, estado: 'revisando' })

  // Al abrir la web con una sesión guardada, se pregunta al backend «quién soy»:
  // así el rol mostrado es el actual (el administrador pudo cambiarlo) y, si la
  // cuenta ya no existe o está desactivada, se cierra la sesión. Si no hay conexión,
  // se mantiene lo guardado.
  const token = sesion?.token
  const claveOmitir = `vynk:intereses:omision:${sesion?.usuario?.correo || ''}`
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

  useEffect(() => {
    if (!token) return undefined
    const ctrl = new AbortController()
    consultarEstadoIntereses(token, ctrl.signal)
      .then(({ configurados, omitido }) => {
        if (ctrl.signal.aborted) return
        const pendienteLocal = localStorage.getItem(claveOmitir) === 'pendiente'
        if (configurados || omitido) localStorage.removeItem(claveOmitir)
        if (pendienteLocal && !configurados && !omitido) {
          // Recuperar la decisión que se realizó sin conectividad.
          omitirIntereses(token).then(() => localStorage.removeItem(claveOmitir)).catch(() => {})
        }
        setInicioIntereses({ token, estado: pendienteLocal || !debeSolicitarIntereses({ configurados, omitido }) ? 'normal' : 'primera-vez' })
      })
      .catch((e) => {
        if (ctrl.signal.aborted) return
        if (e.estado === 401) {
          cerrarSesion()
          window.location.hash = ''
          setSesion(null)
          return
        }
        // Una caída temporal del backend no impide usar una sesión válida.
        setInicioIntereses({ token, estado: 'normal' })
      })
    return () => ctrl.abort()
  }, [token])

  // Si se cierra la sesión en otra pestaña, esta también vuelve al inicio de sesión
  // (el navegador avisa con el evento "storage" cuando otra pestaña borra los datos).
  useEffect(() => {
    const alCambiarElAlmacenamiento = () => {
      // Solo se mira si falta el token; no se usa leerSesion() porque borra datos y podría
      // destruir un inicio de sesión que otra pestaña está guardando en ese momento.
      if (sesionCerradaEnOtraPestana()) {
        window.location.hash = ''
        setSesion(null)
      }
    }
    window.addEventListener('storage', alCambiarElAlmacenamiento)
    return () => window.removeEventListener('storage', alCambiarElAlmacenamiento)
  }, [])

  const salir = () => {
    if (token) cerrarSesionEnServidor(token) // sin esperar la respuesta: la pantalla cambia al instante
    cerrarSesion()
    window.location.hash = '' // la próxima persona no debe caer en la pantalla de la anterior
    setSesion(null)
  }

  if (!sesion) return <Login onSesion={setSesion} />

  // El rol decide qué menú se ve y qué pantallas se pueden abrir (permisos.js).
  const rol = normalizarRol(sesion.usuario.rol)
  const { ruta, pantalla, estado } = resolverRuta(hash, rol)
  const revisando = inicioIntereses.token !== token || inicioIntereses.estado === 'revisando'
  const primeraVez = !revisando && inicioIntereses.estado === 'primera-vez'
  const terminarPersonalizacion = () => {
    setInicioIntereses({ token, estado: 'normal' })
    window.location.hash = '#/inicio'
  }
  const omitirPersonalizacion = () => {
    localStorage.setItem(claveOmitir, 'pendiente')
    terminarPersonalizacion()
  }
  const guardarPersonalizacion = () => {
    localStorage.removeItem(claveOmitir)
    terminarPersonalizacion()
  }
  const pantallaVisible = primeraVez ? pantallaDe('intereses') : pantalla
  const rutaVisible = primeraVez ? 'intereses' : ruta

  let contenido
  if (revisando) {
    contenido = <p role="status" className="intereses__guia">Comprobando tus preferencias…</p>
  } else if (primeraVez) {
    contenido = <Intereses token={sesion.token} onSesionExpirada={salir} primerIngreso onGuardado={guardarPersonalizacion} onOmitir={omitirPersonalizacion} />
  } else if (estado !== 'permitida') {
    contenido = <PantallaBloqueada estado={estado} pantalla={pantalla} rol={rol} />
  } else if (ruta === 'intereses') {
    contenido = <Intereses token={sesion.token} onSesionExpirada={salir} />
  } else if (ruta === 'perfil') {
    contenido = <Perfil usuario={sesion.usuario} rol={rol} token={sesion.token} onSalir={salir} />
  } else {
    const Seccion = SECCIONES[ruta]
    contenido = Seccion ? <Seccion token={sesion.token} correo={sesion.usuario.correo} /> : <PantallaVacia />
  }

  const permitida = revisando || primeraVez || estado === 'permitida'

  return (
    <Shell
      rol={rol}
      rutaActual={pantallaVisible?.menuActivo ?? rutaVisible}
      titulo={revisando ? 'Preparando tu experiencia' : permitida ? pantallaVisible?.titulo : undefined}
      descripcion={revisando ? 'Configurando tu inicio en VYNK.' : permitida ? pantallaVisible?.descripcion : undefined}
    >
      {contenido}
    </Shell>
  )
}

export default App
