"""Registra notificaciones para los inscritos, sin exponer datos privados en la respuesta.

Contrato asumido de Usuario-back: colección `inscripciones` con actividadId y
usuarioId (o estudianteId). El consumidor Spring Boot debe mostrar la colección
`notificaciones` a cada usuario autenticado y validar su identidad.
"""
from datetime import datetime, timezone
from bson import ObjectId
from bson.errors import InvalidId
from . import mongo


def _ids_posibles(actividad_id):
    ids = [str(actividad_id)]
    try:
        ids.append(ObjectId(str(actividad_id)))
    except (InvalidId, TypeError):
        pass
    return ids


def crear_avisos_inscritos(actividad_id, nombre, cambios, editor_id):
    """Devuelve (avisos_creados, cantidad_destinatarios), solo si existen inscripciones verificables."""
    ids = _ids_posibles(actividad_id)
    cursor = mongo.inscripciones().find({"actividadId": {"$in": ids}})
    destinatarios = set()
    for inscripcion in cursor:
        # No notificar inscripciones canceladas.
        if str(inscripcion.get("estado", "inscrito")).lower() in {"cancelada", "cancelado", "anulada"}:
            continue
        usuario_id = inscripcion.get("usuarioId") or inscripcion.get("estudianteId")
        if usuario_id:
            destinatarios.add(str(usuario_id))
    if not destinatarios:
        return 0, 0
    ahora = datetime.now(timezone.utc)
    detalles = ", ".join(cambios)
    documentos = [{
        "usuarioId": u,
        "actividadId": str(actividad_id),
        "tipo": "CAMBIO_HORARIO",
        "titulo": "Cambio de horario: " + nombre[:80],
        "mensaje": "Se actualizó el horario de la actividad. Campos modificados: " + detalles + ". Revisa los nuevos horarios.",
        "cambios": cambios,
        "leida": False,
        "creadaEn": ahora,
        "creadaPor": str(editor_id),
    } for u in destinatarios]
    resultado = mongo.notificaciones().insert_many(documentos, ordered=True)
    return len(resultado.inserted_ids), len(destinatarios)
