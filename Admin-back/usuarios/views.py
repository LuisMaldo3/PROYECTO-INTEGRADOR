import json
import re

from django.http import JsonResponse
from django.views.decorators.http import require_GET, require_http_methods

from . import mongo, roles, servicio
from .permisos import exigir_permiso
from .auth import exigir_admin
from .vistas_util import CAMPOS_PUBLICOS, a_json, buscar_por_id


@require_GET
@exigir_admin
@exigir_permiso("usuarios.ver")
def listar_usuarios(request):
    """US-23 · Tarea 1: lista de usuarios registrados."""
    busqueda = request.GET.get("q", "").strip()
    if len(busqueda) > 100:
        return JsonResponse({"codigo": "BUSQUEDA_INVALIDA", "mensaje": "Máximo 100 caracteres."}, status=400)
    filtro = ({"$or": [
        {"nombre": {"$regex": re.escape(busqueda), "$options": "i"}},
        {"correo": {"$regex": re.escape(busqueda), "$options": "i"}},
    ]} if busqueda else {})
    docs = mongo.usuarios().find(filtro, CAMPOS_PUBLICOS).sort("nombre", 1)
    usuarios = [a_json(d) for d in docs]
    return JsonResponse({"total": len(usuarios), "usuarios": usuarios})


@require_GET
@exigir_admin
@exigir_permiso("usuarios.ver")
def detalle_usuario(request, usuario_id):
    """US-23 · Tarea 2: detalle de un usuario."""
    doc = buscar_por_id(mongo.usuarios(), usuario_id, CAMPOS_PUBLICOS)
    if not doc:
        return JsonResponse({"codigo": "USUARIO_NO_ENCONTRADO", "mensaje": "Usuario no encontrado."}, status=404)
    return JsonResponse(a_json(doc))


@require_http_methods(["PATCH"])
@exigir_admin
@exigir_permiso("usuarios.editar_rol")
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


@require_http_methods(["GET", "PUT"])
@exigir_admin
@exigir_permiso("usuarios.gestionar_permisos")
def permisos_usuario(request, usuario_id):
    """GET consulta permisos efectivos; PUT establece un conjunto de cambios."""
    from .permisos import CATALOGO, permisos_de
    usuario = buscar_por_id(mongo.usuarios(), usuario_id, {"_id": 1, "rol": 1})
    if not usuario:
        return JsonResponse({"codigo": "USUARIO_NO_ENCONTRADO", "mensaje": "Usuario no encontrado."}, status=404)
    if request.method == "GET":
        return JsonResponse({"usuarioId": str(usuario["_id"]), "permisos": permisos_de(str(usuario["_id"]), usuario.get("rol", "usuario")), "catalogo": CATALOGO})
    try:
        cuerpo = json.loads(request.body or b"{}")
    except ValueError:
        return JsonResponse({"codigo": "JSON_INVALIDO", "mensaje": "JSON no válido."}, status=400)
    nuevos = cuerpo.get("permisos") if isinstance(cuerpo, dict) else None
    try:
        permisos = servicio.establecer_permisos(str(usuario["_id"]), nuevos, request.admin_id)
    except ValueError as error:
        return JsonResponse({"codigo": "PERMISOS_INVALIDOS", "mensaje": str(error)}, status=400)
    return JsonResponse({"usuarioId": str(usuario["_id"]), "permisos": permisos, "catalogo": CATALOGO})


@require_http_methods(["PATCH"])
@exigir_admin
@exigir_permiso("usuarios.cambiar_estado")
def cambiar_estado_usuario(request, usuario_id):
    try:
        cuerpo = json.loads(request.body or b"{}")
    except ValueError:
        return JsonResponse({"codigo": "JSON_INVALIDO", "mensaje": "JSON no válido."}, status=400)
    activo = cuerpo.get("activo") if isinstance(cuerpo, dict) else None
    if not isinstance(activo, bool):
        return JsonResponse({"codigo": "ESTADO_INVALIDO", "mensaje": "activo debe ser true o false."}, status=400)
    if str(usuario_id) == str(request.admin_id) and not activo:
        return JsonResponse({"codigo": "AUTO_DESACTIVACION", "mensaje": "No puedes desactivar tu propia cuenta."}, status=400)
    usuario = servicio.cambiar_estado(usuario_id, activo)
    if usuario is None:
        return JsonResponse({"codigo": "USUARIO_NO_ENCONTRADO", "mensaje": "Usuario no encontrado."}, status=404)
    return JsonResponse(a_json(usuario))
