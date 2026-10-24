import json

from django.http import JsonResponse
from django.views.decorators.http import require_GET, require_http_methods

from . import mongo, roles, servicio
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


@require_http_methods(["PATCH"])
@exigir_admin
def cambiar_rol_usuario(request, usuario_id):
    """US-24 · Tarea 1: cambia el rol de un usuario. Cuerpo: {"rol": "organizador"}."""
    try:
        cuerpo = json.loads(request.body or b"{}")
    except ValueError:
        return JsonResponse({"codigo": "JSON_INVALIDO", "mensaje": "El cuerpo debe ser un JSON válido."}, status=400)
    rol_pedido = cuerpo.get("rol") if isinstance(cuerpo, dict) else None

    # La regla de qué roles se aceptan vive en roles.py (US-24 · Tarea 2).
    try:
        rol = roles.validar_rol(rol_pedido)
    except roles.RolInvalido as error:
        return JsonResponse({"codigo": "ROL_INVALIDO", "mensaje": str(error)}, status=400)

    usuario = servicio.cambiar_rol(usuario_id, rol)
    if not usuario:
        return JsonResponse({"codigo": "USUARIO_NO_ENCONTRADO", "mensaje": "Usuario no encontrado."}, status=404)
    return JsonResponse(a_json(usuario))
