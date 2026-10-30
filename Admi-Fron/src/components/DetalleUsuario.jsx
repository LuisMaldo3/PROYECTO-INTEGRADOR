import { useEffect, useRef, useState } from 'react'
import Icono from './Iconos'
import ConfirmarRol from './ConfirmarRol'
import SelectorRol from './SelectorRol'
import PermisosUsuario from './PermisosUsuario'
import EstadoUsuario from './EstadoUsuario'
import { useCambioRol } from '../hooks/useCambioRol'
import { obtenerUsuario } from '../services/usuariosService'
import { etiquetaEstado, etiquetaRol } from '../services/usuarios'

/** Vista de perfil independiente: no desplaza ni reduce la tabla de usuarios. */
export default function DetalleUsuario({ token, usuarioId, onCerrar, onSesionExpirada, onRolCambiado }) {
  const [estado, setEstado] = useState({ cargando: true, error: '', usuario: null })
  const [pestana, setPestana] = useState('informacion')
  const [aviso, setAviso] = useState('')
  const tituloRef = useRef(null)

  const cambio = useCambioRol({
    token,
    usuarioId,
    onSesionExpirada,
    onCambiado: (actualizado) => {
      setEstado({ cargando: false, error: '', usuario: actualizado })
      setAviso('El rol se actualizó correctamente.')
      onRolCambiado?.()
    },
  })

  useEffect(() => {
    let cancelado = false
    setEstado({ cargando: true, error: '', usuario: null })
    setPestana('informacion')
    setAviso('')
    obtenerUsuario(token, usuarioId)
      .then((usuario) => {
        if (!cancelado) setEstado({ cargando: false, error: '', usuario })
      })
      .catch((e) => {
        if (cancelado) return
        if (e.estado === 401) return onSesionExpirada(e.detalle)
        setEstado({ cargando: false, error: e.message, usuario: null })
      })
    return () => { cancelado = true }
  }, [token, usuarioId, onSesionExpirada])

  useEffect(() => {
    if (!estado.cargando) tituloRef.current?.focus()
  }, [estado.cargando])

  const { cargando, error, usuario } = estado
  const nombre = usuario?.nombre?.trim() || usuario?.correo || 'Usuario'
  const actualizar = (nuevo) => {
    setEstado({ cargando: false, error: '', usuario: nuevo })
    setAviso(nuevo.activo === false ? 'Cuenta desactivada.' : 'Cuenta activada.')
    onRolCambiado?.()
  }

  return (
    <section className="perfil" aria-label="Perfil del usuario">
      <div className="perfil__navegacion">
        <button type="button" className="perfil__volver" onClick={onCerrar}>
          <Icono nombre="flecha-izquierda" tam={18} /> Volver a usuarios
        </button>
        <span className="perfil__ruta">Usuarios <span>/</span> Perfil</span>
      </div>

      {cargando && <div className="perfil__cargando" role="status">Cargando información del perfil…</div>}
      {error && <div className="perfil__error"><p className="aviso aviso--error" role="alert">{error}</p><button className="boton boton--claro" onClick={onCerrar}>Volver al listado</button></div>}

      {usuario && (
        <>
          <header className="perfil__cabecera">
            <div className="perfil__avatar" aria-hidden="true">{nombre.charAt(0).toUpperCase()}</div>
            <div className="perfil__identidad">
              <p className="perfil__eyebrow">PERFIL DE LA COMUNIDAD</p>
              <h1 tabIndex={-1} ref={tituloRef}>{nombre}</h1>
              <p className="perfil__correo"><Icono nombre="correo" tam={16} />{usuario.correo}</p>
              <div className="perfil__etiquetas">
                <span className={`etiqueta etiqueta--${etiquetaRol(usuario.rol).toLowerCase()}`}>{etiquetaRol(usuario.rol)}</span>
                <span className={`perfil__estado ${usuario.activo === false ? 'perfil__estado--inactivo' : ''}`}><span className={`punto punto--${usuario.activo === false ? 'off' : 'on'}`} />{etiquetaEstado(usuario.activo)}</span>
              </div>
            </div>
          </header>

          <nav className="perfil__pestanas" aria-label="Secciones del perfil">
            {[
              ['informacion', 'Información', 'usuarios'],
              ['acceso', 'Rol y estado', 'candado'],
              ['permisos', 'Permisos', 'check'],
            ].map(([id, titulo, icono]) => (
              <button key={id} type="button" onClick={() => { setPestana(id); setAviso('') }} className={`perfil__pestana${pestana === id ? ' perfil__pestana--activa' : ''}`} aria-current={pestana === id ? 'page' : undefined}>
                <Icono nombre={icono} tam={17} /> {titulo}
              </button>
            ))}
          </nav>

          {aviso && <p className="aviso aviso--ok perfil__aviso" role="status"><Icono nombre="check" tam={18} /> {aviso}</p>}

          {pestana === 'informacion' && (
            <div className="perfil__contenido">
              <section className="perfil__tarjeta" aria-labelledby="perfil-datos-titulo">
                <div className="perfil__tarjeta-cabecera">
                  <div><h2 id="perfil-datos-titulo">Información de la cuenta</h2><p>Datos registrados en la plataforma institucional.</p></div>
                  <span className="perfil__subicono"><Icono nombre="usuarios" tam={19} /></span>
                </div>
                <dl className="perfil__datos">
                  <div><dt>Nombre completo</dt><dd>{usuario.nombre || 'No registrado'}</dd></div>
                  <div><dt>Correo institucional</dt><dd>{usuario.correo || 'No registrado'}</dd></div>
                  <div><dt>Rol asignado</dt><dd>{etiquetaRol(usuario.rol)}</dd></div>
                  <div><dt>Estado de la cuenta</dt><dd>{etiquetaEstado(usuario.activo)}</dd></div>
                  <div className="perfil__datos-ancho"><dt>Identificador de usuario</dt><dd className="mono">{usuario.id}</dd></div>
                </dl>
              </section>
              <aside className="perfil__ayuda">
                <span className="perfil__subicono"><Icono nombre="candado" tam={19} /></span>
                <h2>Gestiona su acceso</h2>
                <p>Desde este perfil puedes modificar su rol, activar o desactivar la cuenta y revisar los permisos asignados.</p>
                <button type="button" className="boton" onClick={() => setPestana('acceso')}>Administrar acceso <Icono nombre="flecha-derecha" tam={17} /></button>
                <p className="perfil__nota">El nombre y el correo son de solo lectura en este panel.</p>
              </aside>
            </div>
          )}

          {pestana === 'acceso' && (
            <div className="perfil__configuracion">
              <section className="perfil__tarjeta">
                <div className="perfil__tarjeta-cabecera"><div><h2>Rol y responsabilidades</h2><p>Selecciona el rol que tendrá esta cuenta.</p></div></div>
                <SelectorRol rolActual={usuario.rol} deshabilitado={cambio.enviando} onElegir={cambio.pedir} />
                <p className="perfil__texto-ayuda">Para proteger los cambios, se solicitará una confirmación antes de actualizar el rol.</p>
              </section>
              <section className="perfil__tarjeta">
                <EstadoUsuario token={token} usuario={usuario} onSesionExpirada={onSesionExpirada} onActualizado={actualizar} />
              </section>
            </div>
          )}

          {pestana === 'permisos' && (
            <section className="perfil__tarjeta perfil__tarjeta--permisos">
              <PermisosUsuario token={token} usuario={usuario} onSesionExpirada={onSesionExpirada} onAviso={setAviso} />
            </section>
          )}

          {cambio.pendiente && (
            <ConfirmarRol usuario={usuario} rolNuevo={cambio.pendiente} enviando={cambio.enviando} error={cambio.error} onConfirmar={cambio.confirmar} onCancelar={cambio.cancelar} />
          )}
        </>
      )}
    </section>
  )
}
