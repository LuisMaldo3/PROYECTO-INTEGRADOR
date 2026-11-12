import { useEffect, useState } from 'react'
import { listarActividades, listarCategorias, listarUbicaciones, listarOrganizadores, cancelarActividad } from '../services/actividadesService'
import FormularioActividad from './FormularioActividad'
import ListaActividades from './ListaActividades'

/**
 * Pantalla «Actividades»: guarda el arreglo de actividades y lo entrega por
 * props al listado, al resumen y al buscador. Las filas avisan con funciones
 * (onEditar, onCancelar); el padre decide y actualiza el estado siempre con un
 * arreglo nuevo (spread para crear, map para editar o cancelar), nunca con
 * push ni splice.
 */
export default function PantallaActividades({ token, onSesionExpirada }) {
  const [estado, setEstado] = useState({ cargando: true, error: '', actividades: [] })
  const [consulta, setConsulta] = useState('')
  const [filtro, setFiltro] = useState('todas')
  // Lo que el formulario necesita para sus listas desplegables: categorías, ubicaciones y organizadores.
  const [catalogos, setCatalogos] = useState({ cargando: true, error: '', categorias: [], ubicaciones: [], organizadores: [] })
  const [formularioAbierto, setFormularioAbierto] = useState(false)
  const [actividadEditando, setActividadEditando] = useState(null)
  const [aviso, setAviso] = useState('')

  useEffect(() => {
    let cancelado = false
    listarActividades(token)
      .then((actividades) => {
        if (!cancelado) setEstado({ cargando: false, error: '', actividades })
      })
      .catch((e) => {
        if (cancelado) return
        if (e.estado === 401) return onSesionExpirada(e.detalle)
        setEstado({ cargando: false, error: e.message, actividades: [] })
      })
    return () => {
      cancelado = true
    }
  }, [token, onSesionExpirada])

  useEffect(() => {
    let cancelado = false
    Promise.all([listarCategorias(token), listarUbicaciones(token), listarOrganizadores(token)])
      .then(([categorias, ubicaciones, organizadores]) => {
        if (!cancelado) setCatalogos({ cargando: false, error: '', categorias, ubicaciones, organizadores })
      })
      .catch((e) => {
        if (cancelado) return
        if (e.estado === 401) return onSesionExpirada(e.detalle)
        setCatalogos({ cargando: false, error: e.message, categorias: [], ubicaciones: [], organizadores: [] })
      })
    return () => {
      cancelado = true
    }
  }, [token, onSesionExpirada])

  function recargarActividades() {
    setEstado((e) => ({ ...e, cargando: true }))
    listarActividades(token)
      .then((actividades) => setEstado({ cargando: false, error: '', actividades }))
      .catch((e) => {
        if (e.estado === 401) onSesionExpirada(e.detalle)
        else setEstado({ cargando: false, error: e.message, actividades: [] })
      })
  }

  // El servicio entrega la actividad completa (igual que listarActividades): si se edita, se
  // reemplaza esa fila con map(); si es nueva, se agrega al final con un arreglo nuevo (spread).
  function alGuardar(actividad) {
    setFormularioAbierto(false)
    setEstado((e) => ({
      ...e,
      actividades: actividadEditando
        ? e.actividades.map((a) => (a.id === actividad.id ? actividad : a))
        : [...e.actividades, actividad],
    }))
    const huboCambios = Boolean(actividad.cambioHorario || actividad.cambioUbicacion)
    setAviso(actividad.avisosError || (actividadEditando && huboCambios
      ? (actividad.avisarInscritosSolicitado
          ? `Cambios guardados. ${actividad.avisosGenerados ?? 0} notificación(es) registradas para inscritos.`
          : 'Cambios guardados sin enviar notificaciones.')
      : `Actividad «${actividad.nombre}» ${actividadEditando ? 'actualizada' : 'creada'}.`))
    setActividadEditando(null)
  }

  // Cancelar no borra la fila (se conserva el historial): solo cambia su estado, con map().
  async function alCancelar(actividad) {
    const respuesta = await cancelarActividad(token, actividad.id)
    setEstado((e) => ({ ...e, actividades: e.actividades.map((a) => (a.id === actividad.id ? { ...a, estado: 'cancelada' } : a)) }))
    return respuesta
  }

  const { actividades, cargando, error } = estado
  const visibles = actividades.filter((a) =>
    (filtro === 'todas' || (filtro === 'canceladas' ? a.estado === 'cancelada' : a.estado !== 'cancelada')) &&
    `${a.nombre} ${a.descripcion} ${a.ubicacion?.nombre || ''}`.toLocaleLowerCase('es').includes(consulta.toLocaleLowerCase('es').trim()),
  )

  return (
    <div className="pantalla">
      {aviso && (
        <p className="aviso aviso--ok" role="status">
          {aviso}
        </p>
      )}
      {catalogos.error && (
        <p className="aviso aviso--error" role="alert">
          No se pudieron cargar las categorías, ubicaciones y organizadores: {catalogos.error}
        </p>
      )}

      <ListaActividades
        token={token}
        actividades={visibles}
        todasLasActividades={actividades}
        terminoBusqueda={consulta}
        cargando={cargando}
        error={error}
        consulta={consulta}
        filtro={filtro}
        onConsulta={setConsulta}
        onFiltro={setFiltro}
        onActualizar={recargarActividades}
        nuevaDeshabilitada={catalogos.cargando || Boolean(catalogos.error)}
        onNueva={() => {
          setAviso('')
          setActividadEditando(null)
          setFormularioAbierto(true)
        }}
        onEditar={(actividad) => {
          setAviso('')
          setActividadEditando(actividad)
          setFormularioAbierto(true)
        }}
        onCancelar={alCancelar}
        onSesionExpirada={onSesionExpirada}
      />

      {formularioAbierto && (
        <FormularioActividad
          token={token}
          actividad={actividadEditando}
          categorias={catalogos.categorias}
          ubicaciones={catalogos.ubicaciones}
          organizadores={catalogos.organizadores}
          onSesionExpirada={onSesionExpirada}
          onCancelar={() => setFormularioAbierto(false)}
          onCreada={alGuardar}
        />
      )}
    </div>
  )
}
