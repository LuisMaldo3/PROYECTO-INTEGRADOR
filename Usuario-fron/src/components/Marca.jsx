import bannerImg from '../assets/banner.webp'

const CARACTERISTICAS = [
  {
    titulo: 'PERSONAS',
    detalle: 'QUE INSPIRAN',
    iconos: [
      'M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2',
      'M23 21v-2a4 4 0 0 0-3-3.87',
      'M16 3.13a4 4 0 0 1 0 7.75',
    ],
    circulo: { cx: 9, cy: 7, r: 4 },
  },
  {
    titulo: 'IDEAS',
    detalle: 'QUE TRANSFORMAN',
    iconos: [
      'M9 18h6',
      'M10 22h4',
      'M15.09 14c.18-.98.65-1.74 1.41-2.5A4.65 4.65 0 0 0 18 8 6 6 0 0 0 6 8c0 1 .23 2.23 1.5 3.5A4.61 4.61 0 0 1 8.91 14',
    ],
  },
  {
    titulo: 'TECNOLOGÍA',
    detalle: 'QUE CONECTA',
    iconos: [
      'M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z',
    ],
    circulo: { cx: 12, cy: 12, r: 3 },
  },
]

/** Lado de marca: VYNK, escudo T&C Week y valores. */
export function Marca() {
  return (
    <section className="vynk-left-brand" aria-label="VYNK, T&C Week Tecsup">
      <h1 className="vynk-title">VYNK</h1>

      <div className="vynk-shield-box">
        <img src={bannerImg} alt="T&C Week Tecsup" className="vynk-shield-img" width="640" height="640" />
      </div>

      <p className="vynk-subheading">
        <span>TECNOLOGÍA Y CULTURA</span>
        <span>QUE NOS UNE</span>
      </p>

      <ul className="vynk-features-grid">
        {CARACTERISTICAS.map((c) => (
          <li className="vynk-feature-col" key={c.titulo}>
            <svg className="feature-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
              {c.circulo && <circle {...c.circulo} />}
              {c.iconos.map((d) => (
                <path d={d} key={d} />
              ))}
            </svg>
            <span className="feat-title">{c.titulo}</span>
            <span className="feat-desc">{c.detalle}</span>
          </li>
        ))}
      </ul>
    </section>
  )
}
