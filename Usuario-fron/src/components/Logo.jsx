/** Nombre de la app: VYN en blanco y la K en celeste (como el logo original). */
export function Logo({ className = '' }) {
  return (
    <span className={`logo ${className}`.trim()} aria-label="VYNK">
      <span aria-hidden="true">VYN</span>
      <span className="logo-k" aria-hidden="true">K</span>
    </span>
  )
}
