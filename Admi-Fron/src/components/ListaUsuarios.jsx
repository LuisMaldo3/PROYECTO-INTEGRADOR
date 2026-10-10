import { useEffect, useMemo, useState } from 'react'
import { listarUsuarios } from '../services/usuariosService'
import { etiquetaEstado, etiquetaRol, filtrarUsuarios } from '../services/usuarios'

/** Tarea 4 · Pantalla con la lista de usuarios (la carga de datos es la tarea 3, en usuariosService). */
export default function ListaUsuarios({ token, seleccionadoId, onSeleccionar, onSesionExpirada }) {
  const [estado, setEstado] = useState({ cargando: true, error: '', usuarios: [] })
  const [texto, setTexto] = useState('')
  const [rol, setRol] = useState('')
  const [recarga, setRecarga] = useState(0)

  useEffect(() => {
    let cancelado = false
    setEstado((e) => ({ ...e, cargando: true, error: '' }))
    listarUsuarios(token)
      .then((usuarios) => {
        if (!cancelado) setEstado({ cargando: false, error: '', usuarios })
      })
      .catch((e) => {
        if (cancelado) return
        if (e.estado === 401) return onSesionExpirada(e.detalle)
        setEstado({ cargando: false, error: e.message, usuarios: [] })
      })
    return () => {
      cancelado = true
    }
  }, [token, recarga, onSesionExpirada])

  const visibles = useMemo(() => filtrarUsuarios(estado.usuarios, texto, rol), [estado.usuarios, texto, rol])

  return (
    <section className="lista">
      <header className="lista__cabecera">
        <div>
          <h1>Usuarios registrados</h1>
          <p className="suave">
            {estado.cargando ? 'Cargando…' : `${visibles.length} de ${estado.usuarios.length} usuarios`}
          </p>
        </div>
        <button type="button" className="boton boton--claro" onClick={() => setRecarga((n) => n + 1)}>
          Actualizar
        </button>
      </header>

      <div className="filtros">
        <input
          type="search"
          placeholder="Buscar por nombre o correo"
          value={texto}
          onChange={(e) => setTexto(e.target.value)}
          aria-label="Buscar usuarios"
        />
        <select value={rol} onChange={(e) => setRol(e.target.value)} aria-label="Filtrar por rol">
          <option value="">Todos los roles</option>
          <option value="usuario">Usuario</option>
          <option value="organizador">Organizador</option>
          <option value="administrador">Administrador</option>
        </select>
      </div>

      {estado.error && (
        <p className="aviso aviso--error" role="alert">
          {estado.error}
        </p>
      )}

      {!estado.cargando && !estado.error && visibles.length === 0 && (
        <p className="vacio">No hay usuarios que coincidan.</p>
      )}

      {visibles.length > 0 && (
        <div className="tabla-envoltura">
          <table className="tabla">
            <thead>
              <tr>
                <th>Nombre</th>
                <th>Correo</th>
                <th>Rol</th>
                <th>Estado</th>
              </tr>
            </thead>
            <tbody>
              {visibles.map((u) => (
                <tr
                  key={u.id}
                  className={u.id === seleccionadoId ? 'fila fila--activa' : 'fila'}
                  onClick={() => onSeleccionar(u.id)}
                  onKeyDown={(e) => (e.key === 'Enter' || e.key === ' ') && onSeleccionar(u.id)}
                  tabIndex={0}
                >
                  <td>{u.nombre ?? '—'}</td>
                  <td>{u.correo}</td>
                  <td>
                    <span className={`etiqueta etiqueta--${etiquetaRol(u.rol).toLowerCase()}`}>{etiquetaRol(u.rol)}</span>
                  </td>
                  <td>
                    <span className={`punto punto--${u.activo === false ? 'off' : 'on'}`} /> {etiquetaEstado(u.activo)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
