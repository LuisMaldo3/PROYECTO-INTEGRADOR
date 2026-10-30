import { useEffect, useMemo, useState } from 'react'
import { obtenerCatalogo } from '../services/catalogoService'
import { agruparPorDia, cantidad, estadoCupos, estiloCategoria, nombreDia, rangoHoras, hoyISO } from '../services/presentacion'
import { esActividadTCWeek, filtrarPorEtapa } from '../services/campanas'
import EstadoPantalla from './EstadoPantalla'
import Icono from './Iconos'
import './Actividades.css'
import './Catalogo.css'

function Tarjeta({ actividad: a }) {
  const categoria = estiloCategoria({ id: a.categoriaId })
  const cupos = estadoCupos(a.cuposDisponibles)
  const especial = esActividadTCWeek(a)
  return (
    <article className="actividad">
      <div className="actividad__arriba">
        <span className={`categoria categoria--${categoria.clase}`}>
          <Icono nombre={categoria.icono} tamano={14} />
          {categoria.nombre}
        </span>
        <span className={`cupos cupos--${cupos.tono}`}>{cupos.texto}</span>
      </div>
      {especial && <span className="catalogo__campana">Evento especial · T&amp;C Week</span>}
      <div className="actividad__titulo"><h3>{a.nombre}</h3></div>
      <p className={`actividad__descripcion${a.descripcion ? '' : ' actividad__descripcion--vacia'}`}>
        {a.descripcion || 'Sin descripción por ahora.'}
      </p>
      <ul className="actividad__datos">
        <li>
          <Icono nombre="reloj" tamano={16} />
          {rangoHoras(a.horaInicio, a.horaFin)}
        </li>
        {a.organizador && (
          <li><Icono nombre="usuarios" tamano={16} />{a.organizador}</li>
        )}
        {a.ubicacion && (
          <li><Icono nombre="ubicacion" tamano={16} />{a.ubicacion}</li>
        )}
      </ul>
    </article>
  )
}

/** Catálogo: las actividades publicadas, agrupadas por día. */
export function Catalogo() {
  const [estado, setEstado] = useState({ cargando: true, error: '', actividades: [] })
  const [version, setVersion] = useState(0)
  const [busqueda, setBusqueda] = useState('')
  const [etapa, setEtapa] = useState('proximas')

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
  const visibles = useMemo(() => {
    const termino = busqueda.trim().toLocaleLowerCase('es-PE')
    const porFecha = filtrarPorEtapa(actividades, etapa, hoyISO())
    return termino ? porFecha.filter(a => `${a.nombre || ''} ${a.descripcion || ''} ${a.categoriaId || ''} ${a.organizador || ''}`.toLocaleLowerCase('es-PE').includes(termino)) : porFecha
  }, [actividades, busqueda, etapa])
  const grupos = useMemo(() => agruparPorDia(visibles), [visibles])

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
      <div className="catalogo__encabezado">
        <div><span className="catalogo__kicker">AGENDA DEL CAMPUS</span><h2>Encuentra tu próxima actividad</h2>
          <p>Explora las publicaciones de Tecsup. Los eventos especiales tienen sus propias fechas; el catálogo sigue activo todo el año.</p></div>
        {botonActualizar}
      </div>
      <div className="lista-barra"><p>{cargando ? 'Cargando actividades…' : cantidad(actividades.length, 'actividad publicada', 'actividades publicadas')}</p></div>
      <div className="catalogo__etapas" role="group" aria-label="Filtrar actividades según la fecha">
        <button type="button" aria-pressed={etapa === 'proximas'} onClick={() => setEtapa('proximas')}>Próximas</button>
        <button type="button" aria-pressed={etapa === 'todas'} onClick={() => setEtapa('todas')}>Todas</button>
        <button type="button" aria-pressed={etapa === 'pasadas'} onClick={() => setEtapa('pasadas')}>Finalizadas</button>
      </div>

      {!cargando && !error && actividades.length > 0 && <div className="catalogo__filtros">
        <label className="catalogo__buscar"><span aria-hidden="true">⌕</span><input type="search" placeholder="Buscar actividades, temas u organizadores" value={busqueda} onChange={e => setBusqueda(e.target.value)} aria-label="Buscar actividades del catálogo" /></label>
        <span className="catalogo__resultado">{visibles.length} resultados</span>
      </div>}

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

      {!cargando && actividades.length > 0 && visibles.length === 0 && <div className="catalogo__sin-resultados"><h3>{busqueda ? 'No encontramos coincidencias' : etapa === 'proximas' ? 'Todavía no hay próximas actividades' : etapa === 'pasadas' ? 'No hay actividades finalizadas' : 'No hay resultados'}</h3><p>{busqueda ? 'Prueba otro término de búsqueda.' : 'Las actividades aparecen aquí según las fechas publicadas por el organizador.'}</p><button type="button" className="boton-secundario" onClick={() => {setBusqueda('');setEtapa('todas')}}>Ver todas las actividades</button></div>}

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
