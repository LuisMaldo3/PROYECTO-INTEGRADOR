/**
 * Hermano del listado: recibe TODAS las actividades del padre (sin filtrar por
 * el buscador, porque el resumen describe toda la semana, no solo lo que se ve
 * en pantalla) y calcula sus cifras en cada render. No tiene useState propio:
 * si lo tuviera, podría desincronizarse del arreglo real del padre.
 */
export default function ResumenActividades({ actividades, cargando }) {
  const cifra = (n) => (cargando ? '—' : n)
  return (
    <dl className="cifras" aria-label="Resumen de actividades">
      <div><dt>Total</dt><dd>{cifra(actividades.length)}</dd></div>
      <div><dt>Activas</dt><dd>{cifra(actividades.filter((a) => a.estado !== 'cancelada').length)}</dd></div>
      <div><dt>Competencias</dt><dd>{cifra(actividades.filter((a) => a.tipoActividad === 'competencia').length)}</dd></div>
      <div><dt>Canceladas</dt><dd>{cifra(actividades.filter((a) => a.estado === 'cancelada').length)}</dd></div>
    </dl>
  )
}
