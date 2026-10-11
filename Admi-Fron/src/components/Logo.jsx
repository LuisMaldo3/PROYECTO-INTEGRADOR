/**
 * Logo de VYNK: la «V» celeste sobre cuadrado azul marino (la misma del favicon) y el nombre en letras espaciadas.
 * Si cambian el logo, solo hay que cambiar este <svg>; en todo el panel se usa este componente.
 * `claro` = para fondos blancos (el texto va en azul marino).
 */
export default function Logo({ tam = 36, texto = true, claro = false, etiqueta = 'Admin' }) {
  return (
    <span className={`marca${claro ? ' marca--claro' : ''}`}>
      <svg width={tam} height={tam} viewBox="0 0 64 64" role="img" aria-label="VYNK">
        <defs>
          <linearGradient id="vynk-fondo" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0" stopColor="#0b2a6f" />
            <stop offset="1" stopColor="#020b24" />
          </linearGradient>
        </defs>
        <rect width="64" height="64" rx="15" fill="url(#vynk-fondo)" />
        <rect x=".75" y=".75" width="62.5" height="62.5" rx="14.5" fill="none" stroke="#38bdf8" strokeOpacity=".35" strokeWidth="1.5" />
        <path d="M17 19 32 46 47 19" fill="none" stroke="#38bdf8" strokeWidth="7" strokeLinecap="round" strokeLinejoin="round" />
        <circle cx="32" cy="46" r="3.2" fill="#0ea5e9" />
      </svg>
      {texto && (
        <span className="marca__texto">
          VYNK {etiqueta && <small>{etiqueta}</small>}
        </span>
      )}
    </span>
  )
}
