import { useEffect, useState } from 'react'
import { listarCategorias, listarUbicaciones } from '../services/actividadesService'
import FormularioActividad from './FormularioActividad'
import ListaActividades from './ListaActividades'

/** Pantalla «Actividades» del menú: la lista y el formulario «Nueva actividad». */
export default function PantallaActividades({ token, onSesionExpirada }) {
  // Lo que el formulario necesita para sus listas desplegables: categorías y ubicaciones.
  const [catalogos, setCatalogos] = useState({ cargando: true, error: '', categorias: [], ubicaciones: [] })
  const [formularioAbierto, setFormularioAbierto] = useState(false)
  const [version, setVersion] = useState(0)
  const [aviso, setAviso] = useState('')

  useEffect(() => {
    let cancelado = false
    Promise.all([listarCategorias(token), listarUbicaciones(token)])
      .then(([categorias, ubicaciones]) => {
        if (!cancelado) setCatalogos({ cargando: false, error: '', categorias, ubicaciones })
      })
      .catch((e) => {
        if (cancelado) return
        if (e.estado === 401) return onSesionExpirada(e.detalle)
        setCatalogos({ cargando: false, error: e.message, categorias: [], ubicaciones: [] })
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
      {catalogos.error && (
        <p className="aviso aviso--error" role="alert">
          No se pudieron cargar las categorías y ubicaciones: {catalogos.error}
        </p>
      )}

      <ListaActividades
        token={token}
        version={version}
        nuevaDeshabilitada={catalogos.cargando || Boolean(catalogos.error)}
        onNueva={() => {
          setAviso('')
          setFormularioAbierto(true)
        }}
        onSesionExpirada={onSesionExpirada}
      />

      {formularioAbierto && (
        <FormularioActividad
          token={token}
          categorias={catalogos.categorias}
          ubicaciones={catalogos.ubicaciones}
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
