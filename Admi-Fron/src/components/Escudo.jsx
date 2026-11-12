import { departamentoPorId, rutaEscudo } from '../services/departamentos'

/** Escudo del departamento. Es el mismo en todos los deportes y concursos. */
export default function Escudo({ id, tam = 40, decorativo = false, className = '' }) {
  const d = departamentoPorId(id)
  const alto = Math.round((tam * 112) / 96)
  if (!d) return <span className={`escudo escudo--vacio ${className}`.trim()} style={{ width: tam, height: alto }} aria-hidden="true" />
  return <img className={`escudo ${className}`.trim()} src={rutaEscudo(id)} width={tam} height={alto} alt={decorativo ? '' : `Escudo de ${d.nombre}`} loading="lazy" />
}

/** Escudo + nombre del departamento. */
export function Departamento({ id, tam = 28, mostrarCorto = false }) {
  const d = departamentoPorId(id)
  return (
    <span className="departamento" data-area={d ? `dep-${d.id}` : undefined}>
      <Escudo id={id} tam={tam} decorativo />
      <span>{d ? (mostrarCorto ? d.corto : d.nombre) : 'Por definir'}</span>
    </span>
  )
}
