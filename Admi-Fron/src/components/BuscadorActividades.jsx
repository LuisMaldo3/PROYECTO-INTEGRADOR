import Icono from './Iconos'

const FILTROS = [['todas', 'Todas'], ['activas', 'Activas'], ['canceladas', 'Canceladas']]

/**
 * Controlado por el padre: no tiene estado propio. `consulta` y `filtro`
 * viven en PantallaActividades; cada tecla o clic avisa con `onConsulta` /
 * `onFiltro`, y es el padre quien decide cómo queda el arreglo filtrado.
 */
export default function BuscadorActividades({ consulta, onConsulta, filtro, onFiltro }) {
  return (
    <div className="barra-filtros">
      <label className="buscador barra-filtros__busca">
        <Icono nombre="buscar" tam={18} />
        <input type="search" placeholder="Buscar actividad o lugar" value={consulta} onChange={(e) => onConsulta(e.target.value)} aria-label="Buscar actividades" />
      </label>
      <div className="pestanas" role="group" aria-label="Filtrar actividades">
        {FILTROS.map(([id, nombre]) => (
          <button type="button" key={id} className={filtro === id ? 'pestana pestana--activa' : 'pestana'} onClick={() => onFiltro(id)}>{nombre}</button>
        ))}
      </div>
    </div>
  )
}
