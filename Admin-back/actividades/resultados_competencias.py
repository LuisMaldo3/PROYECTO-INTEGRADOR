"""US-32/US-41: revisar, corregir, publicar y anunciar resultados.
El registro por el organizador y el cálculo de la tabla acumulada quedan para Sprint 3.
"""
from datetime import datetime, timezone
from . import mongo
from .servicio import DatosInvalidos
from .competencias import AREAS_TECSUP
from usuarios.vistas_util import buscar_por_id


def _actividad(actividad_id):
    actividad = buscar_por_id(mongo.actividades(), actividad_id)
    if actividad is None:
        raise LookupError('Competencia no encontrada.')
    if actividad.get('tipoActividad') != 'competencia':
        raise DatosInvalidos({'actividad': 'La actividad no está configurada como competencia.'})
    return actividad


def _identificador(doc):
    return str(doc.get('_id'))


def _serializar(doc):
    if not doc:
        return {'estado': 'sin_resultados', 'posiciones': []}
    return {'id': _identificador(doc), 'actividadId': str(doc['actividadId']),
            'estado': doc.get('estado', 'pendiente'), 'posiciones': doc.get('posiciones', []),
            'publicadoEn': doc.get('publicadoEn').isoformat() if doc.get('publicadoEn') else None,
            'avisoPublicado': bool(doc.get('avisoId')),
            'origen': doc.get('origen', 'administracion'),
            'registradoPor': doc.get('registradoPor')}




def participantes_confirmados(actividad):
    """Una plaza grupal es el registro del capitán, cuyos integrantes se confirmaron personalmente.
    Los registros individuales de miembros NUNCA se convierten en participantes adicionales.
    """
    registros = list(mongo.inscripciones().find({'actividadId': str(actividad['_id']), 'estado': 'inscrita'}))
    por_alumno = {str(i.get('estudianteId')): i for i in registros}
    validos = {}
    for insc in registros:
        if insc.get('rolEquipo') == 'integrante':
            continue
        grupal = bool(insc.get('nombreEquipo'))
        if grupal:
            miembros = insc.get('integrantes') or []
            if len(miembros) < 2:
                continue
            if any(not (str(miembro) in por_alumno and
                str(por_alumno[str(miembro)].get('equipoId')) == str(insc['_id']))
                for miembro in miembros):
                continue
        validos[str(insc['_id'])] = insc
    return validos

def obtener(actividad_id):
    actividad = _actividad(actividad_id)
    return _serializar(mongo.resultados_competencias().find_one({'actividadId': str(actividad['_id'])}))


def preparar_propuesta(actividad_id, datos, admin_id):
    """Edición administrativa de propuesta; nunca publica ni calcula automáticamente puntos."""
    actividad = _actividad(actividad_id)
    if actividad.get('estado') == 'cancelada':
        raise DatosInvalidos({'actividad': 'La competencia está cancelada.'})
    posiciones = datos.get('posiciones') if isinstance(datos, dict) else None
    if not isinstance(posiciones, list) or not 1 <= len(posiciones) <= 100:
        raise DatosInvalidos({'posiciones': 'Registra entre 1 y 100 posiciones.'})
    inscritas = participantes_confirmados(actividad)
    usados, puestos, limpias = set(), set(), []
    for item in posiciones:
        if not isinstance(item, dict):
            raise DatosInvalidos({'posiciones': 'Cada posición debe ser un objeto.'})
        inscripcion_id = str(item.get('inscripcionId', '')).strip()
        puesto = item.get('puesto')
        if inscripcion_id not in inscritas or inscripcion_id in usados:
            raise DatosInvalidos({'posiciones': 'Cada inscripción debe existir, estar activa y aparecer una sola vez.'})
        if type(puesto) is not int or not 1 <= puesto <= 100 or puesto in puestos:
            raise DatosInvalidos({'posiciones': 'Los puestos deben ser enteros únicos entre 1 y 100.'})
        puntos = item.get('puntos')
        if puntos is None:
            tabla = actividad.get('puntosPorPuesto') or []
            puntos = tabla[puesto - 1] if puesto <= len(tabla) else 0
        if type(puntos) is not int or not 0 <= puntos <= 10000:
            raise DatosInvalidos({'puntos': 'Indica puntos enteros de 0 a 10000.'})
        usados.add(inscripcion_id); puestos.add(puesto)
        inscripcion = inscritas[inscripcion_id]
        # La afiliación proviene del servidor, nunca de un campo libre en la propuesta.
        area = inscripcion.get('areaTecsup') if inscripcion.get('areaVerificada') is True else None
        limpias.append({'inscripcionId': inscripcion_id, 'puesto': puesto, 'puntos': puntos,
                        'nombre': inscripcion.get('nombreEquipo') or inscripcion.get('nombreEstudiante') or 'Participante',
                        'areaTecsup': area if area in AREAS_TECSUP else None})
    limpias.sort(key=lambda v: v['puesto'])
    ahora = datetime.now(timezone.utc)
    coleccion = mongo.resultados_competencias()
    coleccion.create_index("actividadId", unique=True)
    vigente = coleccion.find_one({"actividadId": str(actividad["_id"])})
    if vigente and vigente.get("estado") == "publicado":
        raise DatosInvalidos({"resultados": "Los resultados ya publicados no pueden sustituirse mediante una propuesta."})
    filtro = {'actividadId': str(actividad['_id']), 'estado': {'$ne': 'publicado'}}
    actualizado = mongo.resultados_competencias().find_one_and_update(filtro, {
        '$set': {'posiciones': limpias, 'estado': 'pendiente', 'modificadoPor': str(admin_id),
                 'modificadoEn': ahora}, '$setOnInsert': {'actividadId': str(actividad['_id'])}},
        upsert=True, return_document=True)
    return _serializar(actualizado)


def publicar(actividad_id, admin_id):
    actividad = _actividad(actividad_id)
    ahora = datetime.now(timezone.utc)
    coleccion = mongo.resultados_competencias()
    pendiente = coleccion.find_one({'actividadId': str(actividad['_id']), 'estado': 'pendiente',
                                   'posiciones.0': {'$exists': True}})
    if not pendiente:
        raise DatosInvalidos({'resultados': 'No hay una propuesta pendiente con posiciones válidas.'})
    confirmados = participantes_confirmados(actividad)
    final = []
    for p in pendiente.get('posiciones', []):
        registro = confirmados.get(str(p.get('inscripcionId')))
        if not registro:
            raise DatosInvalidos({'resultados': 'Una inscripción fue cancelada o el equipo no tiene confirmación individual de sus integrantes.'})
        area = registro.get('areaTecsup') if registro.get('areaVerificada') is True else None
        if area not in AREAS_TECSUP:
            raise DatosInvalidos({'resultados': 'Verifica el área institucional de todos los participantes antes de publicar puntos.'})
        if registro.get('nombreEquipo'):
            miembros = registro.get('integrantes') or []
            coleccion_insc = mongo.inscripciones()
            inscritos = {str(i.get('estudianteId')): i for i in coleccion_insc.find({
                'actividadId': str(actividad['_id']), 'estado': 'inscrita',
                'equipoId': str(registro['_id'])})}
            if any(miembro not in inscritos or inscritos[miembro].get('areaVerificada') is not True
                   or inscritos[miembro].get('areaTecsup') != area for miembro in miembros):
                raise DatosInvalidos({'resultados': 'Todos los integrantes de un equipo deben confirmar su inscripción y pertenecer al mismo área verificada.'})
        final.append({**p, 'areaTecsup': area,
                      'nombre': registro.get('nombreEquipo') or registro.get('nombreEstudiante') or 'Participante'})
    doc = coleccion.find_one_and_update(
        {'_id': pendiente['_id'], 'estado': 'pendiente'},
        {'$set': {'posiciones': final, 'estado': 'publicado', 'publicadoPor': str(admin_id),
                  'publicadoEn': ahora}}, return_document=True)
    if doc is None:
        raise DatosInvalidos({'resultados': 'Los resultados cambiaron mientras se revisaban. Recarga la página.'})
    return _serializar(doc)


def anunciar(actividad_id, admin_id):
    from .avisos import crear_aviso
    actividad = _actividad(actividad_id)
    coleccion = mongo.resultados_competencias()
    doc = coleccion.find_one({'actividadId': str(actividad['_id']), 'estado': 'publicado'})
    if not doc:
        raise DatosInvalidos({'resultados': 'Publica primero los resultados.'})
    if doc.get('avisoId'):
        raise DatosInvalidos({'aviso': 'Los ganadores ya fueron anunciados.'})
    primero = next((p for p in doc.get('posiciones', []) if p.get('puesto') == 1), None)
    if not primero:
        raise DatosInvalidos({'resultados': 'Falta el primer puesto.'})
    # Reservar publicación para reducir riesgo de dobles avisos ante dos administradores.
    reserva = coleccion.update_one({'_id': doc['_id'], 'avisoId': {'$exists': False}},
                                  {'$set': {'avisoId': 'publicando'}})
    if not reserva.modified_count:
        raise DatosInvalidos({'aviso': 'El anuncio ya está generado o en proceso.'})
    try:
        aviso = crear_aviso({'portada': actividad.get('portada'), 'titulo': f"Resultados: {actividad.get('nombre', 'Competencia')}"[:100],
            'mensaje': f"Ganador: {primero['nombre']}. Consulta las posiciones oficiales en VYNK, sección Resultados."}, admin_id)
    except Exception:
        coleccion.update_one({'_id': doc['_id'], 'avisoId': 'publicando'}, {'$unset': {'avisoId': ''}})
        raise
    coleccion.update_one({'_id': doc['_id']}, {'$set': {'avisoId': aviso['id']}})
    return aviso


def clasificacion_general():
    """Clasificación recalculada de fuentes oficiales, una sola sede, sin doble conteo."""
    puntos = {area: 0 for area in AREAS_TECSUP}
    victorias = {area: 0 for area in AREAS_TECSUP}
    desglose = {area: [] for area in AREAS_TECSUP}
    procesadas = set()
    contadas = 0
    for doc in mongo.resultados_competencias().find({'estado': 'publicado'}):
        actividad_id = str(doc.get('actividadId'))
        if actividad_id in procesadas:
            continue
        procesadas.add(actividad_id)
        actividad = buscar_por_id(mongo.actividades(), actividad_id)
        if not actividad or actividad.get('estado') == 'cancelada' or actividad.get('tipoActividad') != 'competencia':
            continue
        contadas += 1
        for resultado in doc.get('posiciones') or []:
            area = resultado.get('areaTecsup')
            valor = resultado.get('puntos')
            if area not in puntos or type(valor) is not int or not 0 <= valor <= 10000:
                continue
            puntos[area] += valor
            if resultado.get('puesto') == 1:
                victorias[area] += 1
            desglose[area].append({'actividadId': actividad_id,
                                   'competencia': actividad.get('nombre', 'Competencia'),
                                   'puesto': resultado.get('puesto'), 'puntos': valor})
    areas = [{'area': area, 'puntos': puntos[area], 'primerosPuestos': victorias[area],
              'desglose': desglose[area]} for area in AREAS_TECSUP]
    areas.sort(key=lambda a: (-a['puntos'], -a['primerosPuestos'], a['area']))
    for indice, area in enumerate(areas, 1):
        area['posicion'] = indice
    return {'estado': 'publicado' if contadas else 'sin_resultados',
            'totalCompetencias': contadas, 'areas': areas,
            'criterioDesempate': 'Más puntos, más primeros puestos y orden alfabético'}
