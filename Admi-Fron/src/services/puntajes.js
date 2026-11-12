/** Hoja de puntajes (gymkanas y concursos por puntos) · lógica sin React ni red, se prueba con node --test. */

/** Identificador de una prueba a partir de su nombre, sin repetir los que ya existen. */
export function idPrueba(nombre, existentes = []) {
  const base = String(nombre).normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '').slice(0, 30) || 'prueba'
  let id = base, n = 2
  while (existentes.includes(id)) id = `${base}-${n++}`
  return id
}

/** Lo que se envía al servidor: casillas vacías no se mandan; los números van como números. */
export function cuerpoHoja(pruebas, puntajes, version) {
  const limpios = {}
  for (const [pid, fila] of Object.entries(puntajes)) {
    const f = {}
    for (const [prueba, valor] of Object.entries(fila ?? {})) if (valor !== '' && valor != null && pruebas.some((x) => x.id === prueba)) f[prueba] = Number(valor)
    if (Object.keys(f).length) limpios[pid] = f
  }
  return { pruebas: pruebas.map((x) => ({ id: x.id, nombre: x.nombre.trim() })), puntajes: limpios, version }
}

/** Total en pantalla mientras se escribe (el orden oficial lo calcula el servidor al guardar). */
export const totalFila = (fila, pruebas) => pruebas.reduce((s, x) => s + (Number(fila?.[x.id]) || 0), 0)

export const etiquetaCategoria = (c) => ({ varones: 'Varones', damas: 'Damas', mixto: 'Mixto' })[c] ?? ''
