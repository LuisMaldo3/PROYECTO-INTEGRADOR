import { useEffect, useMemo, useState } from 'react'
import Icono from './Iconos'
import { listarUsuarios } from '../services/usuariosService'
import { etiquetaEstado, etiquetaRol, filtrarUsuarios } from '../services/usuarios'

/** Tarea 4 · Pantalla con la lista de usuarios (la carga de datos es la tarea 3, en usuariosService). */
export default function ListaUsuarios({ token, seleccionadoId, onSeleccionar, onSesionExpirada, version = 0 }) {
  const [estado, setEstado] = useState({ cargando: true, error: '', usuarios: [] })
  const [texto, setTexto] = useState('')
  const [rol, setRol] = useState('')
  const [recarga, setRecarga] = useState(0)

  useEffect(() => {
    let cancelado = false
    setEstado((e) => ({ ...e, cargando: true, error: '' }))
    const temporizador = setTimeout(() => {
      listarUsuarios(token, texto)
      .then((usuarios) => {
        if (!cancelado) setEstado({ cargando: false, error: '', usuarios })
      })
      .catch((e) => {
        if (cancelado) return
        if (e.estado === 401) return onSesionExpirada(e.detalle)
        setEstado({ cargando: false, error: e.message, usuarios: [] })
      })
    }, texto.trim() ? 280 : 0)
    return () => {
      cancelado = true
      clearTimeout(temporizador)
    }
  }, [token, texto, recarga, version, onSesionExpirada])

  const visibles = useMemo(() => filtrarUsuarios(estado.usuarios, '', rol), [estado.usuarios, texto, rol])

  return (
    <section className="lista">
      <header className="lista__cabecera">
        <div>
          <p className="eyebrow">GESTIÓN DE COMUNIDAD</p><h1>Usuarios</h1>
          <p className="suave">
            {estado.cargando ? 'Buscando usuarios…' : `${visibles.length} cuentas encontradas`}
          </p>
        </div>
        <button type="button" className="boton boton--claro" onClick={() => setRecarga((n) => n + 1)}>
          <Icono nombre="actualizar" tam={18} /> Actualizar
        </button>
      </header>

      <div className="metricas-usuarios" aria-label="Resumen de cuentas">
        <div className="metrica"><span className="metrica__icono" aria-hidden="true"><Icono nombre="usuarios" tam={20}/></span><strong>{visibles.length}</strong><span>Usuarios encontrados</span></div>
        <div className="metrica"><span className="metrica__icono" aria-hidden="true"><Icono nombre="calendario" tam={20}/></span><strong>{visibles.filter((u) => u.rol === 'organizador').length}</strong><span>Organizadores</span></div>
        <div className="metrica"><span className="metrica__icono" aria-hidden="true"><Icono nombre="check" tam={20}/></span><strong>{visibles.filter((u) => u.activo !== false).length}</strong><span>Cuentas activas</span></div>
      </div>
      <div className="filtros" role="search" aria-label="Buscar usuarios">
        <span className="filtros__titulo">Filtrar usuarios</span>
        <span className="buscador">
          <Icono nombre="buscar" tam={18} />
          <input
            type="search"
            placeholder="Buscar por nombre o correo"
            value={texto}
            onChange={(e) => setTexto(e.target.value)}
            aria-label="Buscar usuarios"
            maxLength={100}
          />
        </span>
        {texto && <button type="button" className="boton boton--claro" onClick={() => setTexto('')} aria-label="Limpiar búsqueda">Limpiar</button>}
        <select value={rol} onChange={(e) => setRol(e.target.value)} aria-label="Filtrar por rol">
          <option value="">Todos los roles</option>
          <option value="usuario">Usuario</option>
          <option value="organizador">Organizador</option>
          <option value="administrador">Administrador</option>
        </select>
      </div>

      {estado.error && (
        <p className="aviso aviso--error" role="alert">
          <Icono nombre="alerta" tam={18} /> {estado.error}
        </p>
      )}

      {estado.cargando && <div className="usuarios-cargando" role="status">Buscando cuentas en el servidor…</div>}

      {!estado.cargando && !estado.error && visibles.length === 0 && (
        <div className="vacio">
          <Icono nombre="usuarios" tam={40} />
          <p>No hay usuarios que coincidan.</p>
        </div>
      )}

      {!estado.cargando && visibles.length > 0 && (
        <div className="tabla-envoltura">
          <table className="tabla tabla--usuarios">
            <thead>
              <tr>
                <th>Usuario</th>
                <th>Rol</th>
                <th>Estado</th>
                <th className="tabla__acciones-titulo">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {visibles.map((u) => (
                <tr
                  key={u.id}
                  className={u.id === seleccionadoId ? 'fila fila--activa' : 'fila'}
                  onClick={() => onSeleccionar(u.id)}
                  onKeyDown={(e) => { if (e.target !== e.currentTarget) return; if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); onSeleccionar(u.id) } }}
                  tabIndex={0}
                >
                  <td>
                    <span className="persona">
                      <span className="avatar avatar--mini" aria-hidden="true">
                        {(u.nombre ?? u.correo ?? '?').trim().charAt(0).toUpperCase()}
                      </span>
                      <span className="persona__datos"><strong>{u.nombre || 'Sin nombre'}</strong><small>{u.correo}</small></span>
                    </span>
                  </td>
                  <td>
                    <span className={`etiqueta etiqueta--${etiquetaRol(u.rol).toLowerCase()}`}>{etiquetaRol(u.rol)}</span>
                  </td>
                  <td>
                    <span className={`punto punto--${u.activo === false ? 'off' : 'on'}`} /> {etiquetaEstado(u.activo)}
                  </td>
                  <td className="tabla__acciones-celda">
                    <button type="button" className="boton-perfil" onClick={(evento) => { evento.stopPropagation(); onSeleccionar(u.id) }} aria-label={`Ver perfil de ${u.nombre || u.correo}`}>
                      Ver perfil <Icono nombre="flecha-derecha" tam={16} />
                    </button>
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
