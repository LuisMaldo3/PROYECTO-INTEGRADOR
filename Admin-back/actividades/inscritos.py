"""US-35 · Número de inscritos por actividad (solo lectura, para el panel de administración).

Por cada actividad se cuentan:
- personasInscritas: estudiantes distintos con una inscripción activa (no cancelada).
- En competencias por equipos (US-35-T05) se distingue además:
  - equiposRegistrados: equipos creados por un capitán.
  - equiposCompletos: equipos con al menos 2 integrantes y TODOS con su inscripción personal
    confirmada (misma regla que resultados_competencias.participantes_confirmados).
  - equiposIncompletos: registrados − completos.
- ocupados: lo que consume cupos. En competencias por equipos el cupo es por equipo
  registrado; en lo demás, por persona.

Se lee la colección de inscripciones una sola vez (sin una consulta por actividad).
"""
from collections import defaultdict
from datetime import datetime, timezone

from . import mongo

ESTADOS_INACTIVOS = {"cancelada", "cancelado"}
CAMPOS_INSCRIPCION = {"actividadId": 1, "estudianteId": 1, "estado": 1, "nombreEquipo": 1,
                      "rolEquipo": 1, "equipoId": 1, "integrantes": 1}


def _es_por_equipos(actividad):
    return actividad.get("tipoActividad") == "competencia" and actividad.get("modalidad") == "equipos"


def contar_inscripciones(registros):
    """Cuenta personas y equipos de UNA actividad a partir de sus inscripciones (dicts de Mongo)."""
    activos = [r for r in registros if str(r.get("estado", "inscrita")).lower() not in ESTADOS_INACTIVOS]
    personas = {str(r.get("estudianteId")) for r in activos if r.get("estudianteId")}
    por_alumno = {str(r.get("estudianteId")): r for r in activos if r.get("estudianteId")}

    registrados = completos = 0
    for r in activos:
        if r.get("rolEquipo") == "integrante" or not r.get("nombreEquipo"):
            continue
        registrados += 1
        miembros = [str(m) for m in (r.get("integrantes") or [])]
        if len(miembros) >= 2 and all(
                m in por_alumno and str(por_alumno[m].get("equipoId")) == str(r.get("_id")) for m in miembros):
            completos += 1

    return {
        "personasInscritas": len(personas),
        "equiposRegistrados": registrados,
        "equiposCompletos": completos,
        "equiposIncompletos": registrados - completos,
        "cancelaciones": len(registros) - len(activos),
    }


def _resumen_actividad(actividad, registros):
    conteo = contar_inscripciones(registros)
    por_equipos = _es_por_equipos(actividad)
    ocupados = conteo["equiposRegistrados"] if por_equipos else conteo["personasInscritas"]
    capacidad = actividad.get("maximoParticipantes")
    capacidad = capacidad if isinstance(capacidad, int) and capacidad > 0 else None
    return {
        "id": str(actividad["_id"]),
        "nombre": actividad.get("nombre", "Actividad"),
        "fecha": actividad.get("fecha"),
        "estado": actividad.get("estado", "programada"),
        "tipoActividad": actividad.get("tipoActividad", "actividad"),
        "modalidad": actividad.get("modalidad") if actividad.get("tipoActividad") == "competencia" else None,
        "areaTecsup": actividad.get("areaTecsup", ""),
        "unidadCupo": "equipos" if por_equipos else "estudiantes",
        "capacidad": capacidad,
        "ocupados": ocupados,
        "porcentajeOcupacion": round(ocupados * 100 / capacidad) if capacidad else None,
        **conteo,
    }


def inscritos_por_actividad():
    """Resumen de inscritos de todas las actividades, de la más concurrida a la menos."""
    por_actividad = defaultdict(list)
    for registro in mongo.inscripciones().find({}, CAMPOS_INSCRIPCION):
        por_actividad[str(registro.get("actividadId"))].append(registro)

    actividades = [_resumen_actividad(a, por_actividad.get(str(a["_id"]), []))
                   for a in mongo.actividades().find({})]
    actividades.sort(key=lambda a: (-a["personasInscritas"], -a["equiposRegistrados"], a["nombre"].lower()))

    vigentes = [a for a in actividades if a["estado"] != "cancelada"]
    return {
        "generadoEn": datetime.now(timezone.utc).isoformat(),
        "totales": {
            "actividades": len(vigentes),
            "personasInscritas": sum(a["personasInscritas"] for a in vigentes),
            "equiposRegistrados": sum(a["equiposRegistrados"] for a in vigentes),
            "equiposCompletos": sum(a["equiposCompletos"] for a in vigentes),
            "sinInscritos": sum(1 for a in vigentes if a["personasInscritas"] == 0),
        },
        "actividades": actividades,
    }
