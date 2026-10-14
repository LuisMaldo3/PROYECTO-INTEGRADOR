/** Ids de las actividades que aparecen en algún cruce de horario (para resaltarlas en la agenda). */
export const actividadesEnCruce = (cruces) => new Set(cruces.flatMap((c) => [c.actividadAId, c.actividadBId]).filter(Boolean))
