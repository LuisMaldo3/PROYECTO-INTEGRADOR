export function portadaTematica(a = {}) {
  const n = `${a.nombre || ''} ${a.areaTecsup || ''}`.toLowerCase()
  if (/f[uú]tbol|soccer/.test(n)) return '/departamentos/futbol.svg'
  if (/qu[ií]mica|miner[ií]a|laboratorio/.test(n)) return '/departamentos/quimica.svg'
  if (/electr[oó]nica|electricidad/.test(n)) return '/departamentos/electronica.svg'
  if (/mec[aá]nica|automotriz/.test(n)) return '/departamentos/mecanica.svg'
  if (/software|programaci[oó]n|digital|tecnolog/.test(n)) return '/departamentos/digital.svg'
  if (/gesti[oó]n|producci[oó]n/.test(n)) return '/departamentos/gestion.svg'
  return '/departamentos/actividades.svg'
}
