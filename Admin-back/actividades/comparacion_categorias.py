"""US-40: comparación de participaciones por categoría y clasificación oficial por área.

Una participación es una inscripción activa en una actividad. Una misma persona
puede sumar en varias actividades; NO es el número de personas únicas de todo
el evento. En competencias por equipos se informan los equipos por separado.
Los resultados oficiales se reutilizan desde US-39: jamás se acumulan en BD.
"""
from datetime import datetime, timezone
from .competencias import AREAS_TECSUP
from .reporte_evento import preparar_reporte


def preparar_comparacion(actividades, categorias, inscripciones, reporte):
    """Transformación pura testeable sin MongoDB ni Django."""
    catalogo = {str(c['_id']): str(c.get('nombre') or 'Sin nombre')
                for c in categorias if isinstance(c, dict) and c.get('_id') is not None}
    conteos = {str(a['id']): a for a in inscripciones.get('actividades', []) if a.get('id') is not None}
    registros = {ident: {
        'id': ident, 'nombre': nombre, 'actividades': 0, 'personasInscritas': 0,
        'equiposRegistrados': 0, 'equiposCompletos': 0, 'equiposIncompletos': 0,
        'cuposPersonasOcupados': 0, 'cuposPersonasTotales': 0,
        'cuposEquiposOcupados': 0, 'cuposEquiposTotales': 0,
    } for ident, nombre in catalogo.items()}

    activas = 0
    for a in actividades:
        if not isinstance(a, dict) or a.get('estado') == 'cancelada':
            continue
        identificador = str(a.get('_id') or '')
        if not identificador:
            continue
        categoria_id = str(a.get('categoriaId') or '')
        if categoria_id not in registros:
            registros[categoria_id] = {
                'id': categoria_id, 'nombre': 'Sin categoría', 'actividades': 0, 'personasInscritas': 0,
                'equiposRegistrados': 0, 'equiposCompletos': 0, 'equiposIncompletos': 0,
                'cuposPersonasOcupados': 0, 'cuposPersonasTotales': 0,
                'cuposEquiposOcupados': 0, 'cuposEquiposTotales': 0,
            }
        fila = registros[categoria_id]
        cuenta = conteos.get(identificador, {})
        fila['actividades'] += 1
        activas += 1
        for campo in ('personasInscritas', 'equiposRegistrados', 'equiposCompletos', 'equiposIncompletos'):
            fila[campo] += max(0, int(cuenta.get(campo) or 0))
        unidad = cuenta.get('unidadCupo')
        if unidad not in ('equipos', 'estudiantes'):
            unidad = ('equipos' if a.get('tipoActividad') == 'competencia' and
                      a.get('modalidad') == 'equipos' else 'estudiantes')
        nombre_campo = 'Equipos' if unidad == 'equipos' else 'Personas'
        fila[f'cupos{nombre_campo}Ocupados'] += max(0, int(cuenta.get('ocupados') or 0))
        capacidad = cuenta.get('capacidad')
        if type(capacidad) is int and capacidad > 0:
            fila[f'cupos{nombre_campo}Totales'] += capacidad

    categorias_ordenadas = sorted(registros.values(), key=lambda r: (-r['personasInscritas'], r['nombre'].casefold()))
    total_participaciones = sum(c['personasInscritas'] for c in categorias_ordenadas)
    for registro in categorias_ordenadas:
        registro['porcentajeParticipaciones'] = round(
            100 * registro['personasInscritas'] / total_participaciones, 1
        ) if total_participaciones else 0.0

    # Misma clasificación exacta que US-39; filtrarla en la interfaz no altera el total.
    tabla = [{
        'area': a['area'], 'posicion': a['posicion'], 'puntos': a['puntos'],
        'primerosPuestos': a['primerosPuestos'], 'desglose': list(a['desglose']),
    } for a in reporte['areas'] if a.get('area') in AREAS_TECSUP]
    metadatos = {str(a['_id']): a for a in actividades if isinstance(a, dict) and a.get('_id') is not None}
    return {
        'generadoEn': datetime.now(timezone.utc).isoformat(),
        'criterio': 'Participaciones activas por actividad y categoría; clasificación desde resultados oficiales publicados.',
        'aclaracion': ('Una persona inscrita en varias actividades cuenta una vez en cada actividad. '
                       'No se suman cupos por equipos y cupos por personas como si fueran la misma unidad. '
                       'Los puntos no se almacenan de nuevo y se excluyen competencias canceladas.'),
        'totales': {'categorias': len(categorias_ordenadas), 'actividadesVigentes': activas,
                   'participaciones': total_participaciones,
                   'equiposCompletos': sum(c['equiposCompletos'] for c in categorias_ordenadas),
                   'puntosOficiales': reporte['totales']['puntosOficiales'],
                   'competenciasConResultados': reporte['totales']['competenciasConResultados']},
        'categorias': categorias_ordenadas,
        'clasificacion': {'areas': tabla, 'totalCompetencias': reporte['totales']['competenciasConResultados'],
                          'criterioDesempate': 'Más puntos, luego más primeros puestos y finalmente orden alfabético'},
        'competencias': [
            {'actividadId': r['actividadId'], 'nombre': r['nombre'], 'posiciones': r['posiciones'],
             'categoriaId': str(metadatos.get(r['actividadId'], {}).get('categoriaId') or ''),
             'categoriaNombre': catalogo.get(str(metadatos.get(r['actividadId'], {}).get('categoriaId') or ''), 'Sin categoría'),
             'puntosDistribuidos': r['puntosDistribuidos']} for r in reporte['resultados']
        ],
    }


def obtener_comparacion():
    # Se reutilizan EXACTAMENTE los mismos cálculos de US-35 y US-39.
    from . import mongo
    from .inscritos import inscritos_por_actividad
    estadisticas = inscritos_por_actividad()
    actividades = list(mongo.actividades().find({}, {
        '_id': 1, 'nombre': 1, 'estado': 1, 'tipoActividad': 1, 'modalidad': 1, 'categoriaId': 1}))
    categorias = list(mongo.categorias().find({}, {'_id': 1, 'nombre': 1}))
    resultados = list(mongo.resultados_competencias().find({'estado': 'publicado'}, {
        '_id': 1, 'actividadId': 1, 'estado': 1, 'publicadoEn': 1, 'posiciones': 1}))
    reporte = preparar_reporte(actividades, resultados, estadisticas)
    return preparar_comparacion(actividades, categorias, estadisticas, reporte)
