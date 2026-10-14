import Icono from './Iconos'

/** Mensaje centrado para cuando no hay nada que mostrar: sin datos, error o falta algo. tono: "neutro" | "error". */
export default function EstadoPantalla({ icono = 'vacio', titulo, texto, tono = 'neutro', accion = null }) {
  return (
    <div className={`estado-pantalla estado-pantalla--${tono}`} role={tono === 'error' ? 'alert' : 'status'}>
      <span className="estado-pantalla__icono">
        <Icono nombre={icono} tamano={26} />
      </span>
      <h3>{titulo}</h3>
      {texto && <p>{texto}</p>}
      {accion}
    </div>
  )
}
