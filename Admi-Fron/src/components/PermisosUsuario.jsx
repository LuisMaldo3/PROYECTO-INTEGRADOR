import { useEffect, useState } from 'react'
import { guardarPermisos, obtenerPermisos } from '../services/usuariosService'

/** Permisos granulares: selección, vista previa y confirmación antes de persistir. */
export default function PermisosUsuario({ token, usuario, onSesionExpirada, onAviso }) {
  const [datos, setDatos] = useState(null)
  const [edicion, setEdicion] = useState({})
  const [confirmando, setConfirmando] = useState(false)
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')
  useEffect(() => {
    let vigente = true
    setDatos(null); setEdicion({}); setError(''); setConfirmando(false)
    obtenerPermisos(token, usuario.id).then((r) => { if (vigente) { setDatos(r); setEdicion(r.permisos) } })
      .catch((e) => { if (!vigente) return; if (e.estado === 401) onSesionExpirada(e.detalle); else setError(e.message) })
    return () => { vigente = false }
  }, [usuario.id, token, onSesionExpirada])

  const modificados = Object.fromEntries(Object.entries(edicion).filter(([k, v]) => datos?.permisos?.[k] !== v))
  const hayCambios = Object.keys(modificados).length > 0

  async function confirmar() {
    if (!hayCambios || guardando) return
    setGuardando(true); setError('')
    try {
      const nuevo = await guardarPermisos(token, usuario.id, modificados)
      setDatos(nuevo); setEdicion(nuevo.permisos); setConfirmando(false)
      onAviso('Permisos guardados correctamente.')
    } catch (e) {
      if (e.estado === 401) onSesionExpirada(e.detalle)
      else setError(e.message)
    } finally { setGuardando(false) }
  }

  return <section className="gestion-permisos" aria-labelledby="permisos-titulo">
    <div className="gestion-permisos__cabecera">
      <div><h3 id="permisos-titulo">Permisos de acceso</h3><p>Controla qué acciones administrativas puede realizar esta cuenta.</p></div>
    </div>
    {!datos && !error && <p className="suave">Cargando permisos…</p>}
    {error && <p role="alert" className="aviso aviso--error">{error}</p>}
    {datos && <>
      <div className="gestion-permisos__lista">
        {Object.entries(datos.catalogo).map(([clave, etiqueta]) => <label className="permiso-item" key={clave}>
          <span><strong>{etiqueta}</strong><small>{clave}</small></span>
          <input type="checkbox" className="interruptor" checked={!!edicion[clave]}
            onChange={(e) => setEdicion((anterior) => ({ ...anterior, [clave]: e.target.checked }))} />
        </label>)}
      </div>
      <p className="gestion-permisos__ayuda">Solo las cuentas con rol administrador pueden usar las rutas administrativas. Los permisos no sustituyen al rol.</p>
      <button type="button" className="boton" disabled={!hayCambios || guardando} onClick={() => setConfirmando(true)}>
        {hayCambios ? `Revisar ${Object.keys(modificados).length} cambio(s)` : 'Sin cambios pendientes'}
      </button>
    </>}
    {confirmando && <div className="confirmacion-fondo" role="presentation" onMouseDown={(e) => { if (e.target === e.currentTarget && !guardando) setConfirmando(false) }}>
      <div className="confirmacion-panel" role="alertdialog" aria-modal="true" aria-labelledby="conf-permisos">
        <div className="confirmacion-icono confirmacion-icono--azul" aria-hidden="true">✓</div>
        <h2 id="conf-permisos">Confirmar permisos</h2>
        <p>¿Aplicar {Object.keys(modificados).length} cambios a <strong>{usuario.nombre ?? usuario.correo}</strong>?</p>
        <ul className="resumen-cambios">{Object.entries(modificados).map(([clave, valor]) =>
          <li key={clave}><span>{datos.catalogo[clave]}</span><strong>{valor ? 'Permitir' : 'Restringir'}</strong></li>)}</ul>
        {error && <p className="aviso aviso--error" role="alert">{error}</p>}
        <div className="confirmacion-acciones"><button type="button" className="boton boton--claro" disabled={guardando} onClick={() => setConfirmando(false)}>Volver</button>
          <button type="button" className="boton" disabled={guardando} onClick={confirmar}>{guardando ? 'Guardando…' : 'Confirmar cambios'}</button></div>
      </div>
    </div>}
  </section>
}
