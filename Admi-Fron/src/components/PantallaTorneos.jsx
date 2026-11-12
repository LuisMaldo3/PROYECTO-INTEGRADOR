import { useEffect, useMemo, useState } from 'react'
import Icono from './Iconos'
import Escudo from './Escudo'
import LlavesTorneo, { TarjetaPartido } from './LlavesTorneo'
import PanelPartido from './PanelPartido'
import FormularioTorneo from './FormularioTorneo'
import PuntosTorneo from './PuntosTorneo'
import { useTorneos } from '../hooks/useTorneos'
import { eliminarTorneo } from '../services/torneosService'
import { claveArea } from '../services/areas'
import { resumenTorneo, tituloAccion } from '../services/torneos'
import { formatearFechaActividad } from '../services/formatoActividad'
import { listarActividades } from '../services/actividadesService'
import { competenciasConPuntaje } from '../services/puntajesService'
import { etiquetaCategoria } from '../services/puntajes'

/** Todas las competencias de la semana y en qué estado está cada una: el administrador las controla todas desde aquí. */
function ResumenCompetencias({ token, version, onVer, onArmar, onHoja, onEliminar, onSesionExpirada }) {
  const [datos, setDatos] = useState({ cargando: true, competencias: [], sinConfigurar: [] })
  const [confirmar, setConfirmar] = useState(null)
  const [borrando, setBorrando] = useState(false)
  useEffect(() => {
    let activo = true
    Promise.all([competenciasConPuntaje(token), listarActividades(token)])
      .then(([competencias, actividades]) => {
        if (!activo) return
        const deportivas = actividades.filter((a) => a.categoria?.id === 'deportes' && a.tipoActividad !== 'competencia' && a.estado !== 'cancelada')
        setDatos({ cargando: false, competencias, sinConfigurar: deportivas })
      })
      .catch((e) => { if (activo) { if (e?.estado === 401) onSesionExpirada?.(e.detalle); setDatos((d) => ({ ...d, cargando: false })) } })
    return () => { activo = false }
  }, [token, version, onSesionExpirada])
  if (datos.cargando) return null
  return (
    <section className="tarjeta resumen-comp" aria-labelledby="rc-titulo">
      <div className="resumen-comp__cabecera"><h2 id="rc-titulo">Competencias de la semana</h2><span className="suave">{datos.competencias.length} {datos.competencias.length === 1 ? 'competencia' : 'competencias'}</span></div>
      {datos.sinConfigurar.length > 0 && (
        <p className="aviso aviso--atencion"><Icono nombre="alerta" tam={18} />
          {datos.sinConfigurar.length === 1 ? 'Hay 1 actividad de Deportes' : `Hay ${datos.sinConfigurar.length} actividades de Deportes`} que no {datos.sinConfigurar.length === 1 ? 'está marcada' : 'están marcadas'} como competencia ({datos.sinConfigurar.map((a) => a.nombre).join(', ')}). Edítalas en Actividades y elige «Competencia» para armar su torneo y darles puntos.</p>)}
      {datos.competencias.length === 0 ? <p className="suave">Todavía no hay competencias. Crea una en Actividades: elige la categoría (por ejemplo, Deportes), el tipo «Competencia» y los puntos por puesto.</p> : (
        <ul className="resumen-comp__lista">
          {datos.competencias.map((c) => (
            <li key={c.id}>
              <div><strong>{c.nombre}</strong><small>{[etiquetaCategoria(c.categoriaGenero), c.fecha && formatearFechaActividad(c.fecha), c.puntosPorPuesto?.length ? `Puntos: ${c.puntosPorPuesto.join(' · ')}` : 'Puntos: se escriben al final'].filter(Boolean).join(' · ')}</small></div>
              <span className={`resumen-comp__estado resumen-comp__estado--${c.torneoId ? 'torneo' : c.tieneHoja ? 'hoja' : 'nada'}`}>{c.torneoId ? 'Torneo armado' : c.tieneHoja ? 'Hoja de puntajes' : 'Sin armar'}</span>
              <div className="resumen-comp__acciones">
                {c.torneoId && confirmar !== c.id && <><button type="button" className="boton boton--chico" onClick={() => onVer(c.torneoId)}>Ver llaves</button><button type="button" className="boton boton--chico boton--peligro-claro" onClick={() => setConfirmar(c.id)}>Eliminar</button></>}
                {c.torneoId && confirmar === c.id && (
                  <span className="resumen-comp__confirmar" role="alertdialog" aria-label={`Eliminar el torneo de ${c.nombre}`}>¿Eliminar el torneo? Se pierden sus llaves y resultados.
                    <button type="button" className="boton boton--chico boton--claro" disabled={borrando} onClick={() => setConfirmar(null)}>No</button>
                    <button type="button" className="boton boton--chico boton--peligro" disabled={borrando} onClick={async () => { setBorrando(true); try { await onEliminar(c.torneoId) } finally { setBorrando(false); setConfirmar(null) } }}>{borrando ? 'Eliminando…' : 'Sí, eliminar'}</button>
                  </span>)}
                {c.tieneHoja && <button type="button" className="boton boton--chico" onClick={onHoja}>Abrir hoja</button>}
                {!c.torneoId && !c.tieneHoja && <><button type="button" className="boton boton--chico" onClick={() => onArmar(c.id)}>Armar torneo</button><button type="button" className="boton boton--chico boton--claro" onClick={onHoja}>Hoja de puntajes</button></>}
              </div>
            </li>))}
        </ul>)}
    </section>
  )
}

function Campeon({ torneo }) {
  if (!torneo.campeon) return null
  return (
    <div className="campeon" data-area={claveArea(torneo.campeon.area)}>
      <Escudo id={torneo.campeon.departamento} tam={64} decorativo />
      <div>
        <p className="campeon__etiqueta">{torneo.campeonValidado ? 'Campeón' : 'Campeón (resultado por validar)'}</p>
        <h2>{torneo.campeon.nombre}</h2>{torneo.campeon.area && torneo.campeon.area !== torneo.campeon.nombre && <p className="campeon__sub">{torneo.campeon.area}</p>}
        {torneo.subcampeon && <p className="campeon__sub"><Escudo id={torneo.subcampeon.departamento} tam={20} decorativo /> <span>{torneo.subcampeon.nombre}, subcampeón</span></p>}
      </div>
    </div>
  )
}

function Calendario({ torneo, onElegir }) {
  const jugables = torneo.partidos.filter((p) => !p.descanso && p.a && p.b)
  const dias = useMemo(() => {
    const mapa = new Map()
    for (const p of jugables.filter((x) => x.fecha)) mapa.set(p.fecha, [...(mapa.get(p.fecha) ?? []), p])
    return [...mapa.entries()].sort(([a], [b]) => a.localeCompare(b)).map(([fecha, ps]) => [fecha, ps.sort((x, y) => (x.hora || '').localeCompare(y.hora || ''))])
  }, [jugables])
  const sinHorario = jugables.filter((p) => !p.fecha && p.estado !== 'finalizado')
  if (!dias.length && !sinHorario.length) return <p className="vacio">Todavía no hay enfrentamientos con horario. Abre un enfrentamiento en «Llaves» para programarlo.</p>
  return (
    <div className="calendario-torneo">
      {dias.map(([fecha, ps]) => <section key={fecha}><h3>{formatearFechaActividad(fecha)}</h3><div className="calendario-torneo__lista">{ps.map((p) => <TarjetaPartido key={p.id} partido={p} modo={torneo.modoResultado} onElegir={onElegir} />)}</div></section>)}
      {sinHorario.length > 0 && <section><h3>Sin horario</h3><div className="calendario-torneo__lista">{sinHorario.map((p) => <TarjetaPartido key={p.id} partido={p} modo={torneo.modoResultado} onElegir={onElegir} />)}</div></section>}
    </div>
  )
}

/** T&C ARENA: un torneo de eliminación directa por deporte, entre departamentos. */
export default function PantallaTorneos({ token, tipo = 'arena', onSesionExpirada, onIrA }) {
  const { torneos, cargando, error, recargar, reemplazar, quitar } = useTorneos({ token, tipo, onSesionExpirada })
  const [elegido, setElegido] = useState(null)
  const [pestana, setPestana] = useState('llaves')
  const [creando, setCreando] = useState(false)
  const [actividadInicial, setActividadInicial] = useState('')
  const [partidoId, setPartidoId] = useState(null)
  const titulos = tituloAccion(tipo)

  const torneo = torneos.find((t) => t.id === elegido) ?? torneos[0] ?? null
  const partido = torneo?.partidos.find((p) => p.id === partidoId) ?? null
  const avance = torneo ? resumenTorneo(torneo) : null

  return (
    <section className="torneos">
      <header className="pagina__cabecera">
        <div>
          <h1>{titulos.menu}</h1>
          <p>Torneos de eliminación directa entre los cinco departamentos, que participan por obligación. Los ganadores avanzan hasta la final.</p>
        </div>
        <div className="pagina__acciones">
          <button type="button" className="boton boton--claro" onClick={recargar} disabled={cargando}><Icono nombre="actualizar" tam={18} /> {cargando ? 'Cargando…' : 'Actualizar'}</button>
          <button type="button" className="boton" onClick={() => { setActividadInicial(''); setCreando(true) }}><Icono nombre="mas" tam={18} /> {titulos.nuevo}</button>
        </div>
      </header>

      <ResumenCompetencias token={token} version={torneos.length} onSesionExpirada={onSesionExpirada} onVer={(id) => { setElegido(id); setPestana('llaves') }}
        onArmar={(id) => { setActividadInicial(id); setCreando(true) }} onHoja={() => onIrA?.('puntajes')}
        onEliminar={async (id) => {
          try { await eliminarTorneo(token, id); quitar(id); if (elegido === id) setElegido(null) }
          catch (e) { if (e?.estado === 401) onSesionExpirada?.(e.detalle); else window.alert(e?.message || 'No se pudo eliminar el torneo.') }
        }} />

      {error && <p className="aviso aviso--error" role="alert"><Icono nombre="alerta" tam={18} />{error}<button type="button" className="boton boton--chico" onClick={recargar}>Reintentar</button></p>}
      {cargando && !torneos.length && <div className="cargando" role="status">Cargando torneos…</div>}
      {!cargando && !error && !torneos.length && (
        <div className="vacio vacio--accion"><Icono nombre="balon" tam={32} /><h2>Todavía no hay torneos</h2><p>Elige una competencia y el sistema arma las llaves con los cinco departamentos, sin repetir enfrentamientos.</p><button type="button" className="boton" onClick={() => setCreando(true)}>{titulos.nuevo}</button></div>)}

      {torneo && (
        <>
          {torneos.length > 1 && (
            <div className="pestanas torneos__selector" role="group" aria-label="Torneo">
              {torneos.map((t) => <button key={t.id} type="button" className={t.id === torneo.id ? 'pestana pestana--activa' : 'pestana'} aria-pressed={t.id === torneo.id} onClick={() => { setElegido(t.id); setPartidoId(null) }}>{t.deporte}{t.pendientesDeValidar > 0 && <span title="Resultados por validar">{t.pendientesDeValidar}</span>}</button>)}
            </div>)}

          <div className="torneos__resumen tarjeta">
            <div>
              <h2>{torneo.deporte}</h2>
              <p className="suave">{torneo.participantes.length} participantes · {torneo.modoResultado === 'sets' ? `Sets (gana quien llegue a ${torneo.setsParaGanar})` : 'Marcador'} · {torneo.sorteo ? 'Sorteo automático' : 'Orden manual'}{torneo.terceroPuesto ? ' · Con partido por el 3.er puesto' : ''}</p>
            </div>
            <ul className="torneos__participantes" aria-label="Departamentos participantes">{torneo.participantes.map((d) => <li key={d.id} data-area={claveArea(d.area)} title={`${d.nombre} · ${d.area || 'Sin área'}`}><Escudo id={d.departamento} tam={30} /></li>)}</ul>
            <div className="torneos__avance" aria-label={`${avance.jugados} de ${avance.total} enfrentamientos jugados`}>
              <strong>{avance.jugados}/{avance.total}</strong><small>jugados</small>
              <span className="torneos__barra" aria-hidden="true"><span style={{ width: `${avance.porcentaje}%` }} /></span>
            </div>
            {torneo.pendientesDeValidar > 0 && <span className="cruce__sello cruce__sello--aviso">{torneo.pendientesDeValidar} por validar</span>}
          </div>

          <Campeon torneo={torneo} />

          <div className="pestanas torneos__pestanas" role="tablist" aria-label="Vista del torneo">
            {[['llaves', 'Llaves'], ['calendario', 'Calendario'], ['puntos', 'Puestos y eliminar']].map(([id, texto]) => <button key={id} role="tab" type="button" aria-selected={pestana === id} className={pestana === id ? 'pestana pestana--activa' : 'pestana'} onClick={() => setPestana(id)}>{texto}</button>)}
          </div>

          <div className="tarjeta torneos__cuerpo">
            {pestana === 'llaves' && <LlavesTorneo torneo={torneo} onElegir={(p) => setPartidoId(p.id)} seleccionadoId={partidoId} />}
            {pestana === 'calendario' && <Calendario torneo={torneo} onElegir={(p) => setPartidoId(p.id)} />}
            {pestana === 'puntos' && <PuntosTorneo key={torneo.id} token={token} torneo={torneo} onActualizado={reemplazar} onEliminado={(id) => { quitar(id); setElegido(null); setPestana('llaves') }} onSesionExpirada={onSesionExpirada} />}
          </div>
        </>)}

      {creando && <FormularioTorneo token={token} tipo={tipo} actividadInicial={actividadInicial} onSesionExpirada={onSesionExpirada} onCerrar={() => setCreando(false)} onCreado={(t) => { reemplazar(t); setElegido(t.id); setCreando(false); setPestana('llaves') }} />}
      {partido && torneo && <PanelPartido key={partido.id} token={token} torneo={torneo} partido={partido} puedeValidarResultados onSesionExpirada={onSesionExpirada} onCerrar={() => setPartidoId(null)} onActualizado={reemplazar} />}
    </section>
  )
}
