import { useEffect, useState } from 'react'
import { listarCategorias } from '../services/actividadesService'
import FormularioActividad from './FormularioActividad'
import ListaActividades from './ListaActividades'

/** Pantalla «Actividades» del menú: la lista (tarea 5) y el formulario «Nueva actividad» (tarea 4). */
export default function PantallaActividades({ token, onSesionExpirada }) {
  const [categorias, setCategorias] = useState({ cargando: true, error: '', lista: [] })
  const [formularioAbierto, setFormularioAbierto] = useState(false)
  const [version, setVersion] = useState(0)
  const [aviso, setAviso] = useState('')

  useEffect(() => {
    let cancelado = false
    listarCategorias(token)
      .then((lista) => {
        if (!cancelado) setCategorias({ cargando: false, error: '', lista })
      })
      .catch((e) => {
        if (cancelado) return
        if (e.estado === 401) return onSesionExpirada(e.detalle)
        setCategorias({ cargando: false, error: e.message, lista: [] })
      })
    return () => {
      cancelado = true
    }
  }, [token, onSesionExpirada])

  return (
    <div className="pantalla">
      {aviso && (
        <p className="aviso aviso--ok" role="status">
          {aviso}
        </p>
      )}
      {categorias.error && (
        <p className="aviso aviso--error" role="alert">
          No se pudieron cargar las categorías: {categorias.error}
        </p>
      )}

      <ListaActividades
        token={token}
        version={version}
        nuevaDeshabilitada={categorias.cargando || Boolean(categorias.error)}
        onNueva={() => {
          setAviso('')
          setFormularioAbierto(true)
        }}
        onSesionExpirada={onSesionExpirada}
      />

      {formularioAbierto && (
        <FormularioActividad
          token={token}
          categorias={categorias.lista}
          onSesionExpirada={onSesionExpirada}
          onCancelar={() => setFormularioAbierto(false)}
          onCreada={(actividad) => {
            setFormularioAbierto(false)
            setAviso(`Actividad «${actividad.nombre}» creada.`)
            setVersion((n) => n + 1)
          }}
        />
      )}
    </div>
  )
}
