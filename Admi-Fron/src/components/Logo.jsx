/** Logo de VYNK: «VYN» y la «K» celeste, igual que en la web de estudiantes. */
export default function Logo({ etiqueta = 'Admin', claro = false }) {
  return (
    <span className={`marca${claro ? ' marca--claro' : ''}`} aria-label={`VYNK ${etiqueta}`}>
      <span className="marca__texto" aria-hidden="true">VYN<span className="marca__k">K</span></span>
      {etiqueta && <span className="marca__etiqueta" aria-hidden="true">{etiqueta}</span>}
    </span>
  )
}
