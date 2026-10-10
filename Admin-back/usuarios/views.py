from django.http import JsonResponse
from django.views.decorators.http import require_GET

from . import mongo
from .auth import exigir_admin
from .vistas_util import CAMPOS_PUBLICOS, a_json, buscar_por_id


@require_GET
@exigir_admin
def listar_usuarios(request):
    """US-23 · Tarea 1: lista de usuarios registrados."""
    docs = mongo.usuarios().find({}, CAMPOS_PUBLICOS).sort("nombre", 1)
    usuarios = [a_json(d) for d in docs]
    return JsonResponse({"total": len(usuarios), "usuarios": usuarios})


@require_GET
@exigir_admin
def detalle_usuario(request, usuario_id):
    """US-23 · Tarea 2: detalle de un usuario."""
    doc = buscar_por_id(mongo.usuarios(), usuario_id, CAMPOS_PUBLICOS)
    if not doc:
        return JsonResponse({"codigo": "USUARIO_NO_ENCONTRADO", "mensaje": "Usuario no encontrado."}, status=404)
    return JsonResponse(a_json(doc))
