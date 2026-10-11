import { ROLES, hayCambio } from '../services/roles'

/** Tarea 4 · Lista para elegir el nuevo rol. No guarda nada: avisa con onElegir(rol) y el padre pide confirmación. */
export default function SelectorRol({ rolActual, deshabilitado = false, onElegir }) {
  return (
    <label className="campo selector-rol">
      Cambiar rol
      <select
        value={rolActual?.trim().toLowerCase() ?? ''}
        disabled={deshabilitado}
        onChange={(e) => hayCambio(rolActual, e.target.value) && onElegir(e.target.value)}
      >
        {ROLES.map((r) => (
          <option key={r.valor} value={r.valor}>
            {r.etiqueta}
          </option>
        ))}
      </select>
    </label>
  )
}
