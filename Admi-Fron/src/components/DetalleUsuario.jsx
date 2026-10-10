/** PLACEHOLDER · Tarea 5 es de Mishel. Se reemplaza por su archivo al integrar. */
export default function DetalleUsuario({ usuarioId, onCerrar }) {
  return (
    <aside className="detalle" aria-label="Detalle del usuario">
      <header className="detalle__cabecera">
        <h2>Detalle del usuario</h2>
        <button type="button" className="detalle__cerrar" onClick={onCerrar} aria-label="Cerrar detalle">
          ✕
        </button>
      </header>
      <p className="suave">Pendiente (Mishel). Usuario elegido: {usuarioId}</p>
    </aside>
  )
}
