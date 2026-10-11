/** Lista desplegable para elegir la ubicación de la actividad. No guarda nada: avisa con onCambiar(id). */
export default function SelectorUbicacion({ valor = '', ubicaciones = [], error = '', deshabilitado = false, onCambiar, onSalir }) {
  return (
    <label className="campo">
      Ubicación
      <select
        value={valor}
        onChange={(e) => onCambiar(e.target.value)}
        onBlur={onSalir}
        disabled={deshabilitado}
        aria-invalid={error ? 'true' : undefined}
      >
        <option value="">Elige una ubicación</option>
        {ubicaciones.map((u) => (
          <option key={u.id} value={u.id}>
            {u.nombre}
          </option>
        ))}
      </select>
      {error && (
        <span className="campo__error" role="alert">
          {error}
        </span>
      )}
    </label>
  )
}
