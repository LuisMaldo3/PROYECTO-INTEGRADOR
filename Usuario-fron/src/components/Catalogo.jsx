import { useEffect, useMemo, useState } from 'react'
import { obtenerCatalogo } from '../services/catalogoService'
import { agruparPorDia, cantidad, estadoCupos, estiloCategoria, nombreDia, rangoHoras } from '../services/presentacion'
import EstadoPantalla from './EstadoPantalla'
import Icono from './Iconos'
import './Actividades.css'
import './Catalogo.css'

function Tarjeta({ actividad: a }) {
  const categoria = estiloCategoria({ id: a.categoriaId })
  const cupos = estadoCupos(a.cuposDisponibles)
  return (
    <article className="actividad">
      <div className="actividad__arriba">
        <span className={`categoria categoria--${categoria.clase}`}>
          <Icono nombre={categoria.icono} tamano={14} />
          {categoria.nombre}
        </span>
        <span className={`cupos cupos--${cupos.tono}`}>{cupos.texto}</span>
      </div>
      <h3>{a.nombre}</h3>
      <p className={`actividad__descripcion${a.descripcion ? '' : ' actividad__descripcion--vacia'}`}>
        {a.descripcion || 'Sin descripción por ahora.'}
      </p>
      <ul className="actividad__datos">
        <li>
          <Icono nombre="reloj" tamano={16} />
          {rangoHoras(a.horaInicio, a.horaFin)}
        </li>
        {a.organizador && (
          <li>
            <Icono nombre="usuarios" tamano={16} />
            {a.organizador}
          </li>
        )}
      </ul>
    </article>
  )
}

/** Catálogo: las actividades publicadas, agrupadas por día. */
export function Catalogo() {
  const [estado, setEstado] = useState({ cargando: true, error: '', actividades: [] })
  const [version, setVersion] = useState(0)

  useEffect(() => {
    const ctrl = new AbortController()
    obtenerCatalogo(ctrl.signal)
      .then((actividades) => {
        if (!ctrl.signal.aborted) setEstado({ cargando: false, error: '', actividades })
      })
      .catch((e) => {
        if (!ctrl.signal.aborted) setEstado({ cargando: false, error: e.message, actividades: [] })
      })
    return () => ctrl.abort()
  }, [version])

  const recargar = () => {
    setEstado((e) => ({ ...e, cargando: true, error: '' }))
    setVersion((v) => v + 1)
  }
  const { cargando, error, actividades } = estado
  const grupos = useMemo(() => agruparPorDia(actividades), [actividades])

  const botonActualizar = (
    <button type="button" className="boton-secundario" onClick={recargar} disabled={cargando}>
      <Icono nombre="actualizar" tamano={16} /> Actualizar
    </button>
  )

  if (error) {
    return <EstadoPantalla icono="alerta" tono="error" titulo="No pudimos cargar el catálogo" texto={error} accion={botonActualizar} />
  }

  return (
    <div className="catalogo">
      <div className="lista-barra">
        <p>{cargando ? 'Cargando actividades…' : cantidad(actividades.length, 'actividad disponible', 'actividades disponibles')}</p>
        {botonActualizar}
      </div>

      {cargando && actividades.length === 0 && (
        <div className="catalogo__grid" aria-busy="true">
          {Array.from({ length: 6 }, (_, n) => (
            <span key={n} className="actividad actividad--esqueleto" />
          ))}
        </div>
      )}

      {!cargando && actividades.length === 0 && (
        <EstadoPantalla
          icono="calendario"
          titulo="Pronto habrá actividades para ti"
          texto="Cuando el administrador publique actividades, aparecerán aquí automáticamente."
        />
      )}

      {grupos.map((g) => (
        <section className="dia" key={g.fecha ?? 'sin-fecha'}>
          <header className="dia__cabecera">
            <h2>{nombreDia(g.fecha)}</h2>
            <span>{cantidad(g.items.length, 'actividad', 'actividades')}</span>
          </header>
          <div className="catalogo__grid">
            {g.items.map((a) => (
              <Tarjeta key={a.id} actividad={a} />
            ))}
          </div>
        </section>
      ))}
    </div>
  )
}
