import { useEffect, useRef, useState } from 'react'
import Icono from './Iconos'
import ConfirmarRol from './ConfirmarRol'
import SelectorRol from './SelectorRol'
import PermisosUsuario from './PermisosUsuario'
import EstadoUsuario from './EstadoUsuario'
import { useCambioRol } from '../hooks/useCambioRol'
import { obtenerUsuario, verificarAreaTecsup } from '../services/usuariosService'
import { etiquetaEstado, etiquetaRol } from '../services/usuarios'

/** Vista de perfil independiente: no desplaza ni reduce la tabla de usuarios. */
export default function DetalleUsuario({ token, usuarioId, onCerrar, onSesionExpirada, onRolCambiado }) {
  const [estado, setEstado] = useState({ cargando: true, error: '', usuario: null })
  const [pestana, setPestana] = useState('informacion')
  const [aviso, setAviso] = useState('')
  const [areaElegida, setAreaElegida] = useState('')
  const [guardandoArea, setGuardandoArea] = useState(false)
  const [errorArea, setErrorArea] = useState('')
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
    setErrorArea('')
    obtenerUsuario(token, usuarioId)
      .then((usuario) => {
        if (!cancelado) { setEstado({ cargando: false, error: '', usuario }); setAreaElegida(usuario.areaTecsup || '') }
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
      </div>

      {cargando && <div className="perfil__cargando" role="status">Cargando información del perfil…</div>}
      {error && <div className="perfil__error"><p className="aviso aviso--error" role="alert">{error}</p><button className="boton boton--claro" onClick={onCerrar}>Volver al listado</button></div>}

      {usuario && (
        <>
          <header className="perfil__cabecera">
            <div className="perfil__avatar" aria-hidden="true">{nombre.charAt(0).toUpperCase()}</div>
            <div className="perfil__identidad">
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
                  <div><h2 id="perfil-datos-titulo">Información de la cuenta</h2></div>
                  <span className="perfil__subicono"><Icono nombre="usuarios" tam={19} /></span>
                </div>
                <dl className="perfil__datos">
                  <div><dt>Nombre completo</dt><dd>{usuario.nombre || 'No registrado'}</dd></div>
                  <div><dt>Correo institucional</dt><dd>{usuario.correo || 'No registrado'}</dd></div>
                  <div><dt>Rol asignado</dt><dd>{etiquetaRol(usuario.rol)}</dd></div>
                  <div><dt>Estado de la cuenta</dt><dd>{etiquetaEstado(usuario.activo)}</dd></div>
                  <div className="perfil__datos-ancho"><dt>Área de Tecsup</dt><dd>{usuario.areaVerificada ? usuario.areaTecsup : 'Área pendiente de verificación'}</dd></div>
                  <div className="perfil__datos-ancho"><dt>Identificador de usuario</dt><dd className="mono">{usuario.id}</dd></div>
                </dl>
              </section>
              <aside className="perfil__ayuda">
                <span className="perfil__subicono"><Icono nombre="candado" tam={19} /></span>
                <h2>Gestiona su acceso</h2>
                <p>Cambia su rol, verifica su área o desactiva la cuenta.</p>
                <button type="button" className="boton" onClick={() => setPestana('acceso')}>Administrar acceso</button>
                <p className="perfil__nota">El nombre y el correo no se pueden editar aquí.</p>
              </aside>
            </div>
          )}

          {pestana === 'acceso' && (
            <div className="perfil__configuracion">
              <section className="perfil__tarjeta">
                <div className="perfil__tarjeta-cabecera"><div><h2>Rol</h2><p>Define qué puede hacer esta cuenta.</p></div></div>
                <SelectorRol rolActual={usuario.rol} deshabilitado={cambio.enviando} onElegir={cambio.pedir} />
                <p className="perfil__texto-ayuda">Se pedirá confirmación antes de cambiar el rol.</p>
              </section>
              <section className="perfil__tarjeta" aria-label="Área institucional">
                <div className="perfil__tarjeta-cabecera"><div><h2>Área de Tecsup</h2><p>Los puntos de sus competencias se suman a esta área. Sin área verificada no suma puntos.</p></div></div>
                <form onSubmit={async (e) => { e.preventDefault(); setGuardandoArea(true); setAviso(''); setErrorArea(''); try {
                  const nuevo = await verificarAreaTecsup(token, usuarioId, areaElegida)
                  setEstado(v => ({ ...v, usuario: nuevo })); onRolCambiado?.()
                  setAviso(nuevo.areaVerificada ? `Área verificada: ${nuevo.areaTecsup}` : 'Se retiró la verificación del área.')
                } catch (error) {
                  if (error.estado === 401) onSesionExpirada?.(error.detalle)
                  else setErrorArea(error.detalle || error.message || 'No se pudo verificar el área.')
                } finally { setGuardandoArea(false) } }}>
                  <label htmlFor="area-tecsup-usuario">Área asignada</label>
                  <select id="area-tecsup-usuario" value={areaElegida} onChange={e => setAreaElegida(e.target.value)} disabled={guardandoArea}>
                    <option value="">Sin área verificada</option>
                    {['Gestión y Producción','Tecnología Digital','Minería y Procesos Químicos','Electricidad y Electrónica','Mecánica'].map(area => <option key={area} value={area}>{area}</option>)}
                  </select>
                  {errorArea && <p className="aviso aviso--error" role="alert">{errorArea}</p>}
                  <div className="perfil__form-acciones"><button type="submit" className="boton" disabled={guardandoArea || areaElegida === (usuario.areaTecsup || '')}>{guardandoArea ? 'Guardando…' : 'Guardar área'}</button></div>
                </form>
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
