/** PLACEHOLDER · Tarea 4 es de Mishel. Se reemplaza por su archivo al integrar. */
export default function SelectorRol({ rolActual, deshabilitado = false, onElegir }) {
  const actual = rolActual?.trim().toLowerCase() ?? ''
  return (
    <label className="campo">
      Cambiar rol
      <select value={actual} disabled={deshabilitado} onChange={(e) => e.target.value !== actual && onElegir(e.target.value)}>
        <option value="usuario">Usuario</option>
        <option value="organizador">Organizador</option>
        <option value="administrador">Administrador</option>
      </select>
    </label>
  )
}
