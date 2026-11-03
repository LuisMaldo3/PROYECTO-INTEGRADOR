import { useCallback, useEffect, useMemo, useState } from 'react'
import { obtenerAgenda } from '../services/agendaService'
import { obtenerCatalogo } from '../services/catalogoService'
import { listarAvisosPublicos } from '../services/avisosPublicos'
import { urlImagenActividad, imagenTematicaActividad } from '../services/portadaActividad'
import { bloqueDia, nombreDia, rangoHoras, hoyISO } from '../services/presentacion'
import { claveArea } from '../services/areas'
import campus from '../assets/campus.webp'
import { obtenerSemanaEspecial } from '../services/campanas'

const MINUTO = 60 * 1000
const fechaAviso = (iso) => iso ? new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(iso)) : ''
const esReciente = (iso) => iso && Date.now() - Date.parse(iso) < 3 * 24 * 60 * MINUTO
const ordenar = (lista) => lista.slice().sort((a, b) => `${a.fecha || ''} ${a.horaInicio || ''}`.localeCompare(`${b.fecha || ''} ${b.horaInicio || ''}`))

/** Inicio: avisos de Tecsup, agenda próxima y actividades por descubrir. */
export function Inicio({ token, usuario }) {
  const [estado, setEstado] = useState({ cargando: true, agenda: [], catalogo: [], agendaError: false, catalogoError: false })
  const [avisos, setAvisos] = useState({ cargando: true, lista: [], error: '' })
  const [revision, setRevision] = useState(0)
  const [imagenFallida, setImagenFallida] = useState({})
  const marcarFallida = useCallback((id) => setImagenFallida(f => f[id] ? f : { ...f, [id]: true }), [])

  useEffect(() => {
    const ctrl = new AbortController()
    Promise.allSettled([obtenerAgenda(token, ctrl.signal), obtenerCatalogo(ctrl.signal)]).then(([agenda, catalogo]) => {
      if (ctrl.signal.aborted) return
      setEstado({
        cargando: false,
        agenda: agenda.status === 'fulfilled' ? agenda.value : [],
        catalogo: catalogo.status === 'fulfilled' ? catalogo.value : [],
        agendaError: agenda.status === 'rejected',
        catalogoError: catalogo.status === 'rejected',
      })
    })
    return () => ctrl.abort()
  }, [token, revision])

  // Los avisos se vuelven a pedir cada minuto y al volver a la pestaña:
  // lo que publique administración aparece sin recargar la página.
  const cargarAvisos = useCallback((signal) => {
    listarAvisosPublicos(signal)
      .then(lista => { if (!signal?.aborted) setAvisos({ cargando: false, lista, error: '' }) })
      .catch(e => { if (!signal?.aborted) setAvisos(a => ({ ...a, cargando: false, error: e.message || 'No se pudieron cargar los avisos.' })) })
  }, [])
  useEffect(() => {
    const ctrl = new AbortController()
    cargarAvisos(ctrl.signal)
    const refrescar = () => { if (document.visibilityState === 'visible') cargarAvisos(ctrl.signal) }
    const intervalo = window.setInterval(refrescar, MINUTO)
    window.addEventListener('focus', refrescar)
    document.addEventListener('visibilitychange', refrescar)
    return () => {
      ctrl.abort()
      window.clearInterval(intervalo)
      window.removeEventListener('focus', refrescar)
      document.removeEventListener('visibilitychange', refrescar)
    }
  }, [cargarAvisos, revision])

  const hoy = hoyISO()
  const primerNombre = String(usuario?.nombre || '').trim().split(/\s+/)[0]
  const especial = useMemo(() => estado.catalogoError ? null : obtenerSemanaEspecial(estado.catalogo, hoy), [estado.catalogo, estado.catalogoError, hoy])
  const descubrir = useMemo(() => ordenar(estado.catalogo.filter(a => a.fecha && a.fecha >= hoy)).slice(0, 4), [estado.catalogo, hoy])
  const proximasTodas = useMemo(() => ordenar(estado.agenda.filter(a => a.estado !== 'cancelada' && a.fecha >= hoy)), [estado.agenda, hoy])
  const proximas = proximasTodas.slice(0, 3)
  const cifra = (valor, error) => estado.cargando || error ? '—' : valor
  const [destacado, ...otrosAvisos] = avisos.lista

  return (
    <div className="inicio-panel">
      <section className="inicio-panel__hero" style={{ '--foto-campus': `url(${campus})` }}>
        <div className="inicio-panel__hero-texto">
          <h2>{primerNombre ? `Hola, ${primerNombre}` : 'Bienvenido a VYNK'}</h2>
          <p>Encuentra actividades del campus, inscríbete y organiza tu semana.</p>
          <div className="inicio-panel__acciones">
            <a href="#/actividades" className="vynk-up-btn vynk-up-btn--light">Explorar actividades</a>
            <a href="#/agenda" className="vynk-up-btn vynk-up-btn--glass">Ver mi agenda</a>
          </div>
        </div>
        <dl className="inicio-panel__metricas">
          <div><dt>Inscripciones</dt><dd>{cifra(estado.agenda.length, estado.agendaError)}</dd></div>
          <div><dt>Próximas</dt><dd>{cifra(proximasTodas.length, estado.agendaError)}</dd></div>
          <div><dt>Publicadas</dt><dd>{cifra(estado.catalogo.length, estado.catalogoError)}</dd></div>
        </dl>
      </section>

      {(estado.agendaError || estado.catalogoError) && <div className="inicio-panel__error" role="alert">
        {estado.agendaError && estado.catalogoError ? 'No se pudieron cargar tu agenda ni las actividades.' : estado.agendaError ? 'No se pudo cargar tu agenda.' : 'No se pudieron cargar las actividades.'}
        <button type="button" onClick={() => { setEstado(e => ({ ...e, cargando: true })); setRevision(n => n + 1) }}>Reintentar</button>
      </div>}

      {especial && <aside className="inicio-panel__especial" aria-label="Semana especial">
        <div><h3>T&amp;C Week</h3><p>{especial.cantidad} {especial.cantidad === 1 ? 'actividad' : 'actividades'} entre las cinco áreas, del {nombreDia(especial.inicio)} al {nombreDia(especial.fin)}.</p></div>
        <a href="#/actividades" className="vynk-up-btn vynk-up-btn--primary">Ver competencias</a>
      </aside>}

      <div className="inicio-panel__columnas">
        <section className="inicio-avisos" aria-labelledby="avisos-titulo">
          <div className="inicio-panel__seccion"><h2 id="avisos-titulo">Avisos de Tecsup</h2>{avisos.lista.length > 0 && <span className="inicio-panel__contador">{avisos.lista.length}</span>}</div>
          {avisos.cargando && <div className="inicio-panel__vacio" role="status">Cargando avisos…</div>}
          {!avisos.cargando && avisos.error && avisos.lista.length === 0 && <div className="inicio-panel__error" role="alert">{avisos.error} <button type="button" onClick={() => cargarAvisos()}>Reintentar</button></div>}
          {!avisos.cargando && !avisos.error && avisos.lista.length === 0 && <div className="inicio-panel__vacio">No hay avisos por ahora. Cuando administración publique uno, aparecerá aquí.</div>}
          {destacado && <article className="inicio-avisos__destacado">
            {destacado.portada?.url && !imagenFallida[destacado.id] && <img src={urlImagenActividad({ portada: destacado.portada })} ref={el => { if (el?.complete && el.naturalWidth === 0) marcarFallida(destacado.id) }} onError={() => marcarFallida(destacado.id)} alt="" referrerPolicy="no-referrer" />}
            <div>
              <div className="inicio-avisos__meta">{esReciente(destacado.publicadoEn) && <span className="inicio-avisos__nuevo">Nuevo</span>}<time dateTime={destacado.publicadoEn}>{fechaAviso(destacado.publicadoEn)}</time></div>
              <h3>{destacado.titulo}</h3>
              <p>{destacado.mensaje}</p>
              {destacado.portada?.tipo === 'commons' && destacado.portada?.creditoUrl && <a className="vynk-credito-aviso" href={destacado.portada.creditoUrl} target="_blank" rel="noopener noreferrer">Foto: {destacado.portada.autor || 'Wikimedia Commons'}</a>}
            </div>
          </article>}
          {otrosAvisos.length > 0 && <ul className="inicio-avisos__lista">{otrosAvisos.slice(0, 4).map(aviso => <li key={aviso.id}>
            <details>
              <summary><span className="inicio-avisos__meta">{esReciente(aviso.publicadoEn) && <span className="inicio-avisos__nuevo">Nuevo</span>}<time dateTime={aviso.publicadoEn}>{fechaAviso(aviso.publicadoEn)}</time></span><strong>{aviso.titulo}</strong></summary>
              <p>{aviso.mensaje}</p>
            </details>
          </li>)}</ul>}
        </section>

        <div className="inicio-panel__lateral">
          <section aria-labelledby="inicio-agenda">
            <div className="inicio-panel__seccion"><h2 id="inicio-agenda">Próximo en tu agenda</h2><a href="#/agenda">Ver agenda</a></div>
            {estado.cargando && <div className="inicio-panel__vacio" role="status">Cargando…</div>}
            {!estado.cargando && !estado.agendaError && proximas.length === 0 && <div className="inicio-panel__vacio">No tienes actividades próximas. <a href="#/actividades">Busca una</a>.</div>}
            {proximas.length > 0 && <ul className="inicio-lista">{proximas.map((a, i) => <li key={a.id || a.actividadId || i} data-area={claveArea(a.areaTecsup)}>
              <span className="inicio-lista__fecha">{a.fecha ? a.fecha.slice(8, 10) : '—'}<small>{bloqueDia(a.fecha)?.mes || ''}</small></span>
              <div><strong>{a.nombreActividad || a.nombre || 'Actividad'}</strong><small>{rangoHoras(a.horaInicio, a.horaFin)} · {a.ubicacion || 'Lugar por confirmar'}</small></div>
            </li>)}</ul>}
          </section>

          <section aria-labelledby="inicio-descubrir">
            <div className="inicio-panel__seccion"><h2 id="inicio-descubrir">Para descubrir</h2><a href="#/actividades">Ver todas</a></div>
            {!estado.cargando && !estado.catalogoError && descubrir.length === 0 && <div className="inicio-panel__vacio">Aún no hay nuevas actividades.</div>}
            {descubrir.length > 0 && <ul className="inicio-lista">{descubrir.map((a, i) => <li key={a.id || i} data-area={claveArea(a.areaTecsup)}>
              <a href={`#/actividad?id=${encodeURIComponent(a.id)}`}>
                <img src={urlImagenActividad(a) || imagenTematicaActividad(a)} onError={e => { e.currentTarget.onerror = null; e.currentTarget.src = imagenTematicaActividad(a) }} alt="" loading="lazy" />
                <div><strong>{a.nombre || 'Actividad'}</strong><small>{nombreDia(a.fecha)} · {rangoHoras(a.horaInicio, a.horaFin)}</small></div>
              </a>
            </li>)}</ul>}
          </section>
        </div>
      </div>
    </div>
  )
}
