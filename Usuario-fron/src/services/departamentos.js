/**
 * Departamentos que compiten en T&C Week (no son las carreras: ellas se mantienen aparte).
 * Fuente única de nombre, color y escudo para toda la web. Debe coincidir con
 * Admin-back/actividades/departamentos.py y con el panel Admi-Fron (los identificadores son los mismos).
 *
 * Los colores son la identidad definida para el proyecto: validar con la organización de TECSUP
 * antes de presentarlos como oficiales. La denominación del departamento morado (DPI o DPG) está
 * por confirmar: para cambiarla basta editar `nombre` y `corto` de «dpg» aquí y en el backend.
 */
export const DEPARTAMENTOS = Object.freeze([
  { id: 'quimica', nombre: 'Química', corto: 'QUÍM', color: 'verde', hex: '#12A150', area: 'Minería y Procesos Químicos', clave: 'quimica' },
  { id: 'mecanica', nombre: 'Mecánica', corto: 'MEC', color: 'amarillo', hex: '#F5B700', area: 'Mecánica', clave: 'mecanica' },
  { id: 'td', nombre: 'TD', corto: 'TD', color: 'rojo', hex: '#DC2626', area: 'Tecnología Digital', clave: 'digital' },
  { id: 'electricidad', nombre: 'Electricidad', corto: 'ELEC', color: 'azul', hex: '#1D6FE0', area: 'Electricidad y Electrónica', clave: 'electro' },
  { id: 'dpg', nombre: 'DPG', corto: 'DPG', color: 'morado', hex: '#7C3AED', area: 'Gestión y Producción', clave: 'gestion', porConfirmar: true },
])

export const departamentoPorId = (id) => DEPARTAMENTOS.find((d) => d.id === id) ?? null

/** Departamento al que pertenece un área de las que ya guardan los estudiantes (o null). */
export const departamentoDeArea = (area) => DEPARTAMENTOS.find((d) => d.area === area) ?? null

/** Clave que usa el CSS en data-area para pintar con el color del departamento. */
export const claveDepartamento = (id) => departamentoPorId(id)?.clave ?? 'general'

export const nombreDepartamento = (id) => departamentoPorId(id)?.nombre ?? 'Por definir'

const BASE = (typeof import.meta !== 'undefined' && import.meta.env && import.meta.env.BASE_URL) || '/'
export const rutaEscudo = (id) => `${BASE}escudos/${id}.svg`
