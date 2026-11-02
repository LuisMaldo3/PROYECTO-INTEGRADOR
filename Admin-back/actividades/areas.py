"""Áreas oficiales de T&C Week; sin crear departamentos inexistentes."""
from .competencias import AREAS_TECSUP
from usuarios import mongo as mongo_usuarios
from usuarios.vistas_util import buscar_por_id


def normalizar_areas(datos):
    from .servicio import DatosInvalidos
    area = datos.get('areaTecsup') or ''
    if area not in ('', *AREAS_TECSUP):
        raise DatosInvalidos({'areaTecsup': 'Selecciona una de las cinco áreas oficiales.'})
    encargados = datos.get('encargadosPorArea') or {}
    if not isinstance(encargados, dict) or any(a not in AREAS_TECSUP for a in encargados):
        raise DatosInvalidos({'encargadosPorArea': 'Las áreas asignadas no son válidas.'})
    validados = {}
    for nombre_area in AREAS_TECSUP:
        identificador = encargados.get(nombre_area)
        if not identificador:
            continue
        if not isinstance(identificador, str) or len(identificador) > 100:
            raise DatosInvalidos({'encargadosPorArea': f'Encargado inválido para {nombre_area}.'})
        usuario = buscar_por_id(mongo_usuarios.usuarios(), identificador)
        if not usuario or usuario.get('rol', 'usuario') != 'organizador' or not usuario.get('activo', True):
            raise DatosInvalidos({'encargadosPorArea': f'Selecciona un organizador activo para {nombre_area}.'})
        validados[nombre_area] = {'id': str(usuario['_id']), 'nombre': usuario.get('nombre', '')}
    return {'areaTecsup': area, 'encargadosPorArea': validados}
