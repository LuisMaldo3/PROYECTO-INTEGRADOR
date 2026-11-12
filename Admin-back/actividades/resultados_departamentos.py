"""Resultados oficiales por departamento.

En la T&C Week los cinco departamentos participan por obligación en todas las competencias, así que el resultado de un
torneo o de una gymkana se registra por departamento (no por inscripción). Se guarda en `resultados_competencias` con el
mismo formato que ya leen las tablas acumuladas de Django y de Spring, sin tocarlas. Los puntos los escribe el
administrador al final. Una vez publicados no se pisan.
"""
from datetime import datetime, timezone

from . import departamentos, mongo, torneos
from .servicio import DatosInvalidos


def guardar(actividad_id, filas, puntos, usuario_id, publicar):
    """`filas`: [(puesto, departamento)]. `puntos`: {puesto: puntos} escritos a mano (o None para usar los de la competencia)."""
    actividad = torneos._cargar_actividad(actividad_id)
    if not actividad or actividad.get("tipoActividad") != "competencia" or actividad.get("estado") == "cancelada":
        raise DatosInvalidos({"resultados": "La competencia no existe o no está vigente."})
    tabla = actividad.get("puntosPorPuesto") or []
    posiciones = []
    for puesto, dep in filas:
        d = departamentos.obtener(dep)
        if not d:
            raise DatosInvalidos({"resultados": "Hay un puesto con un departamento que no existe."})
        pts = puntos[puesto] if puntos else (tabla[puesto - 1] if puesto <= len(tabla) else 0)
        posiciones.append({"inscripcionId": f"departamento-{dep}", "departamentoId": dep, "puesto": puesto, "puntos": pts,
                           "areaTecsup": d["areaTecsup"], "nombre": d["areaTecsup"]})
    coleccion = mongo.resultados_competencias()
    aid = str(actividad["_id"])
    vigente = coleccion.find_one({"actividadId": aid})
    if vigente and vigente.get("estado") == "publicado":
        raise DatosInvalidos({"resultados": "Los resultados de esta competencia ya están publicados y no se pueden sustituir."})
    ahora = datetime.now(timezone.utc)
    doc = {**(vigente or {}), "actividadId": aid, "origen": "departamentos", "posiciones": posiciones,
           "estado": "publicado" if publicar else "pendiente", "modificadoPor": str(usuario_id), "modificadoEn": ahora}
    if publicar:
        doc.update(publicadoPor=str(usuario_id), publicadoEn=ahora)
    if vigente:
        coleccion.replace_one({"_id": vigente["_id"]}, doc)
    else:
        coleccion.insert_one(doc)
    return doc
