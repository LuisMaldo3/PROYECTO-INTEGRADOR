import { useEffect, useState } from 'react'
import Icono from './Iconos'
import ConfirmarRol from './ConfirmarRol'
import SelectorRol from './SelectorRol'
import PermisosUsuario from './PermisosUsuario'
import EstadoUsuario from './EstadoUsuario'
import { useCambioRol } from '../hooks/useCambioRol'
import { obtenerUsuario } from '../services/usuariosService'
import { etiquetaEstado, etiquetaRol } from '../services/usuarios'

/** Tarea 5 · Panel con el detalle del usuario elegido en la lista. */
export default function DetalleUsuario({ token, usuarioId, onCerrar, onSesionExpirada, onRolCambiado }) {
  const [estado, setEstado] = useState({ cargando: true, error: '', usuario: null })
  const [aviso, setAviso] = useState('')

  const cambio = useCambioRol({
    token,
    usuarioId,
    onSesionExpirada,
    onCambiado: (actualizado) => {
      setEstado({ cargando: false, error: '', usuario: actualizado })
      setAviso('Rol actualizado.')
      onRolCambiado?.()
    },
  })

  useEffect(() => {
    let cancelado = false
    setEstado({ cargando: true, error: '', usuario: null })
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
    return () => {
      cancelado = true
    }
  }, [token, usuarioId, onSesionExpirada])

  const { cargando, error, usuario } = estado

  return (
    <aside className="detalle" aria-label="Detalle del usuario">
      <header className="detalle__cabecera">
        <h2>Detalle del usuario</h2>
        <button type="button" className="detalle__cerrar" onClick={onCerrar} aria-label="Cerrar detalle">
          <Icono nombre="cerrar" tam={18} />
        </button>
      </header>

      {cargando && <p className="suave">Cargando…</p>}

      {error && (
        <p className="aviso aviso--error" role="alert">
          {error}
        </p>
      )}

      {usuario && (
        <>
          <div className="detalle__avatar" aria-hidden="true">
            {(usuario.nombre ?? usuario.correo ?? '?').trim().charAt(0).toUpperCase()}
          </div>
          <dl className="detalle__datos">
            <dt>Nombre</dt>
            <dd>{usuario.nombre ?? '—'}</dd>
            <dt>Correo</dt>
            <dd>{usuario.correo}</dd>
            <dt>Rol</dt>
            <dd>
              <span className={`etiqueta etiqueta--${etiquetaRol(usuario.rol).toLowerCase()}`}>{etiquetaRol(usuario.rol)}</span>
            </dd>
            <dt>Estado</dt>
            <dd>
              <span className={`punto punto--${usuario.activo === false ? 'off' : 'on'}`} /> {etiquetaEstado(usuario.activo)}
            </dd>
            <dt>Identificador</dt>
            <dd className="mono">{usuario.id}</dd>
          </dl>

          <EstadoUsuario token={token} usuario={usuario} onSesionExpirada={onSesionExpirada}
            onActualizado={(nuevo) => { setEstado({ cargando: false, error: '', usuario: nuevo }); setAviso(nuevo.activo ? 'Cuenta activada.' : 'Cuenta desactivada.'); onRolCambiado?.() }} />
          <PermisosUsuario token={token} usuario={usuario} onSesionExpirada={onSesionExpirada} onAviso={setAviso} />
          <SelectorRol rolActual={usuario.rol} deshabilitado={cambio.enviando} onElegir={cambio.pedir} />
          {aviso && (
            <p className="aviso aviso--ok" role="status">
              <Icono nombre="check" tam={18} /> {aviso}
            </p>
          )}

          {cambio.pendiente && (
            <ConfirmarRol
              usuario={usuario}
              rolNuevo={cambio.pendiente}
              enviando={cambio.enviando}
              error={cambio.error}
              onConfirmar={cambio.confirmar}
              onCancelar={cambio.cancelar}
            />
          )}
        </>
      )}
    </aside>
  )
}
