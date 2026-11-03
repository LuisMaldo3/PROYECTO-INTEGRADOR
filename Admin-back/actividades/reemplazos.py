"""US-36 · Supervisión administrativa de reemplazos y cupos (solo lectura).

Fuente de cupos: colecciones `actividades` e `inscripciones`, igual que US-35.
Fuente del historial: colección `reemplazos_inscripciones`, escrita por el flujo de
sustitución del ORGANIZADOR (Usuario-back/Spring Boot). No se deducen reemplazos
por cancelaciones: una cancelación NO implica que haya una sustitución.

Contrato de auditoría para el flujo organizador:
  actividadId, estudianteSalienteId, estudianteEntranteId, organizadorId,
  fecha (datetime UTC), estado='realizado', nombres opcionales.
Este módulo NO modifica inscripciones, cupos ni escribe eventos.
"""
from collections import defaultdict
from datetime import datetime, timezone

from . import mongo
from .inscritos import CAMPOS_INSCRIPCION, _resumen_actividad

ESTADOS_APLICADOS = {"realizado", "confirmado", "completado", "aplicado"}


def _fecha_iso(valor):
    if isinstance(valor, datetime):
        return valor.replace(tzinfo=timezone.utc).isoformat() if valor.tzinfo is None else valor.isoformat()
    if isinstance(valor, str):
        return valor
    return None


def _evento_publico(doc):
    return {
        "id": str(doc.get("_id", "")),
        "fecha": _fecha_iso(doc.get("fecha")),
        "estudianteSalienteId": str(doc.get("estudianteSalienteId") or ""),
        "estudianteEntranteId": str(doc.get("estudianteEntranteId") or ""),
        "nombreSaliente": str(doc.get("nombreSaliente") or ""),
        "nombreEntrante": str(doc.get("nombreEntrante") or ""),
        "organizadorId": str(doc.get("organizadorId") or ""),
        "estado": str(doc.get("estado") or "realizado"),
    }


def resumen_reemplazos():
    """Devuelve historial confirmado y supervisión de límites, sin realizar reemplazos."""
    por_actividad = defaultdict(list)
    for registro in mongo.inscripciones().find({}, CAMPOS_INSCRIPCION):
        if registro.get("actividadId") is not None:
            por_actividad[str(registro["actividadId"])].append(registro)

    eventos_por_actividad = defaultdict(list)
    for evento in mongo.reemplazos_inscripciones().find({}):
        estado = str(evento.get("estado") or "realizado").lower()
        if estado not in ESTADOS_APLICADOS or not evento.get("actividadId"):
            continue
        if not evento.get("estudianteSalienteId") or not evento.get("estudianteEntranteId"):
            continue
        eventos_por_actividad[str(evento["actividadId"])].append(_evento_publico(evento))

    actividades = []
    for actividad in mongo.actividades().find({}):
        identificador = str(actividad["_id"])
        inscritos = _resumen_actividad(actividad, por_actividad[identificador])
        historial = sorted(eventos_por_actividad[identificador],
                           key=lambda e: e["fecha"] or "", reverse=True)
        capacidad = inscritos["capacidad"]
        ocupados = inscritos["ocupados"]
        actividades.append({
            **inscritos,
            "cuposDisponibles": max(capacidad - ocupados, 0) if capacidad is not None else None,
            "excedeCupo": capacidad is not None and ocupados > capacidad,
            "reemplazosConfirmados": len(historial),
            "historial": historial,
        })

    actividades.sort(key=lambda a: (-int(a["excedeCupo"]), -a["reemplazosConfirmados"], a["nombre"].lower()))
    vigentes = [a for a in actividades if a["estado"] != "cancelada"]
    return {
        "generadoEn": datetime.now(timezone.utc).isoformat(),
        "historialDisponible": any(a["reemplazosConfirmados"] for a in actividades),
        "totales": {
            "actividades": len(vigentes),
            "reemplazosConfirmados": sum(a["reemplazosConfirmados"] for a in vigentes),
            "actividadesConExceso": sum(bool(a["excedeCupo"]) for a in vigentes),
            "equiposIncompletos": sum(a["equiposIncompletos"] for a in vigentes),
        },
        "actividades": actividades,
    }
