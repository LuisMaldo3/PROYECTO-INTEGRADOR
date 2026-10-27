"""Notificaciones privadas a inscritos; deduplicación y estados cancelados.

La aplicación de estudiantes debe leer sus propias notificaciones con autorización.
No se devuelven identificadores privados en las respuestas del panel.
"""
from datetime import datetime, timezone
from bson import ObjectId
from bson.errors import InvalidId
from . import mongo

EXCLUIDOS = {"cancelada", "cancelado", "anulada", "anulado", "retirada", "retirado"}


def _ids_posibles(actividad_id):
    ids = [str(actividad_id)]
    try:
        ids.append(ObjectId(str(actividad_id)))
    except (InvalidId, TypeError):
        pass
    return ids


def destinatarios_de(actividad_id):
    inscritos = mongo.inscripciones().find({"actividadId": {"$in": _ids_posibles(actividad_id)}})
    return sorted({str(usuario) for i in inscritos
                   if str(i.get("estado", "inscrito")).lower() not in EXCLUIDOS
                   for usuario in [i.get("usuarioId") or i.get("estudianteId")] if usuario})


def notificar(actividad_id, titulo, mensaje, tipo, editor_id, **adicionales):
    """Retorna (documentos insertados, destinatarios) o propaga error de Mongo."""
    destinatarios = destinatarios_de(actividad_id)
    if not destinatarios:
        return 0, 0
    ahora = datetime.now(timezone.utc)
    documentos = [{"usuarioId": usuario, "actividadId": str(actividad_id),
                   "tipo": tipo, "titulo": titulo[:160], "mensaje": mensaje[:2000],
                   "leida": False, "creadaEn": ahora, "creadaPor": str(editor_id),
                   **adicionales} for usuario in destinatarios]
    resultado = mongo.notificaciones().insert_many(documentos, ordered=True)
    return len(resultado.inserted_ids), len(destinatarios)


def notificar_cambio_ubicacion(actividad_id, nombre, anterior, nueva, editor_id):
    return notificar(actividad_id, f"Cambio de ubicación: {nombre}",
                    f"La actividad «{nombre}» cambió de ubicación: de {anterior} a {nueva}. "
                    "Revisa el nuevo lugar antes de asistir.", "CAMBIO_UBICACION", editor_id,
                    ubicacionAnterior=anterior, ubicacionNueva=nueva)


def notificar_cancelacion(actividad_id, nombre, editor_id):
    return notificar(actividad_id, f"Actividad cancelada: {nombre}",
                    f"La actividad «{nombre}» fue cancelada. Ya no se realizará; "
                    "revisa tus inscripciones para conocer otras actividades.",
                    "CANCELACION_ACTIVIDAD", editor_id)
