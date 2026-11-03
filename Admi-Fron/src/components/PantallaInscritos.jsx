import { useMemo, useState } from 'react'
import Icono from './Iconos'
import GraficoInscritos from './GraficoInscritos'
import { useInscritosPorActividad } from '../hooks/useInscritosPorActividad'
import { FILTROS, ORDENES, describirInscritos, filtrarActividades, ordenarActividades } from '../services/inscritos'
import { formatearFechaActividad } from '../services/formatoActividad'

const horaActualizacion = (iso) => iso ? new Intl.DateTimeFormat('es-PE', { timeStyle: 'short' }).format(new Date(iso)) : ''

/** US-35 · Número de inscritos por actividad: resumen, gráfico y tabla. */
export default function PantallaInscritos({ token, onSesionExpirada }) {
  const { datos, cargando, error, recargar } = useInscritosPorActividad({ token, onSesionExpirada })
  const [filtro, setFiltro] = useState('todas')
  const [orden, setOrden] = useState('mas')
  const [busqueda, setBusqueda] = useState('')

  const visibles = useMemo(
    () => ordenarActividades(filtrarActividades(datos.actividades, { filtro, busqueda }), orden),
    [datos.actividades, filtro, busqueda, orden])
  const t = datos.totales
  const cifra = (v) => (cargando && !t ? '—' : v ?? 0)

  return (
    <section className="inscritos">
      <header className="pagina__cabecera">
        <div>
          <h1>Inscritos por actividad</h1>
          <p>Cuántas personas se inscribieron en cada actividad y, en competencias por equipos, cuántos equipos están completos.</p>
        </div>
        <div className="pagina__acciones">
          {datos.generadoEn && <span className="inscritos__hora">Actualizado {horaActualizacion(datos.generadoEn)}</span>}
          <button type="button" className="boton boton--claro" onClick={recargar} disabled={cargando}>
            <Icono nombre="actualizar" tam={18} /> {cargando ? 'Cargando…' : 'Actualizar'}
          </button>
        </div>
      </header>

      <dl className="cifras" aria-label="Resumen de inscritos">
        <div><dt>Personas inscritas</dt><dd>{cifra(t?.personasInscritas)}</dd></div>
        <div><dt>Equipos completos</dt><dd>{cifra(t?.equiposCompletos)}{t?.equiposRegistrados > 0 && <small> de {t.equiposRegistrados}</small>}</dd></div>
        <div><dt>Actividades</dt><dd>{cifra(t?.actividades)}</dd></div>
        <div><dt>Sin inscritos</dt><dd>{cifra(t?.sinInscritos)}</dd></div>
      </dl>

      {error && <p className="aviso aviso--error" role="alert"><Icono nombre="alerta" tam={18} />{error}
        <button type="button" className="boton boton--chico" onClick={recargar}>Reintentar</button></p>}

      <div className="filtros" role="search" aria-label="Filtrar actividades">
        <label className="buscador"><Icono nombre="buscar" tam={18} />
          <input type="search" value={busqueda} onChange={(e) => setBusqueda(e.target.value)} placeholder="Buscar actividad" aria-label="Buscar actividad" />
        </label>
        <div className="pestanas" role="group" aria-label="Tipo de actividad">
          {FILTROS.map((f) => <button key={f.id} type="button" className={filtro === f.id ? 'pestana pestana--activa' : 'pestana'} aria-pressed={filtro === f.id} onClick={() => setFiltro(f.id)}>{f.titulo}</button>)}
        </div>
        <label className="inscritos__orden"><span className="sr-only">Ordenar por</span>
          <select value={orden} onChange={(e) => setOrden(e.target.value)}>
            {ORDENES.map((o) => <option key={o.id} value={o.id}>{o.titulo}</option>)}
          </select>
        </label>
      </div>

      {cargando && !t && <div className="cargando" role="status">Cargando inscritos…</div>}
      {!cargando && !error && visibles.length === 0 && <div className="vacio"><Icono nombre="usuarios" tam={28} /><p>{datos.actividades.length ? 'No hay actividades con ese filtro.' : 'Todavía no hay actividades registradas.'}</p></div>}

      {visibles.length > 0 && <>
        <section className="tarjeta inscritos__grafico" aria-labelledby="inscritos-grafico">
          <h2 id="inscritos-grafico">Gráfico</h2>
          <GraficoInscritos actividades={visibles} />
        </section>

        <section className="tabla-envoltura" aria-label="Detalle de inscritos por actividad">
          <table className="tabla tabla--inscritos">
            <thead>
              <tr>
                <th>Actividad</th>
                <th>Fecha</th>
                <th className="num">Personas</th>
                <th className="num">Equipos completos</th>
                <th className="num">Cupos usados</th>
                <th className="num">Cancelaciones</th>
              </tr>
            </thead>
            <tbody>
              {visibles.map((a) => (
                <tr key={a.id}>
                  <td><span className="celda-principal"><strong>{a.nombre}</strong><span>{a.tipoActividad === 'competencia' ? (a.porEquipos ? 'Competencia por equipos' : 'Competencia individual') : 'Actividad'}{a.areaTecsup ? ` · ${a.areaTecsup}` : ''}</span></span></td>
                  <td>{a.fecha ? formatearFechaActividad(a.fecha) : <span className="sin-dato">Sin fecha</span>}</td>
                  <td className="num" title={describirInscritos(a)}><strong>{a.personasInscritas}</strong></td>
                  <td className="num">{a.porEquipos ? `${a.equiposCompletos} de ${a.equiposRegistrados}` : <span className="sin-dato">—</span>}</td>
                  <td className="num">{a.capacidad ? <span className={a.porcentaje >= 100 ? 'ocupacion ocupacion--lleno' : a.porcentaje >= 80 ? 'ocupacion ocupacion--alto' : 'ocupacion'}>{a.ocupados}/{a.capacidad} · {a.porcentaje}%</span> : <span className="sin-dato">Sin límite</span>}</td>
                  <td className="num">{a.cancelaciones || <span className="sin-dato">0</span>}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      </>}
    </section>
  )
}
