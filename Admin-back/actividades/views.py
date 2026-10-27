import json

from django.http import JsonResponse
from django.views.decorators.http import require_GET, require_http_methods

from usuarios.auth import exigir_admin
from usuarios.permisos import exigir_permiso

from . import servicio, organizadores, avisos as servicio_avisos


@require_http_methods(["GET", "POST"])
@exigir_admin
@exigir_permiso("actividades.gestionar")
def actividades(request):
    """GET: lista de actividades. POST: crea una ({nombre, descripcion, categoriaId})."""
    if request.method == "GET":
        lista = servicio.listar_actividades()
        return JsonResponse({"total": len(lista), "actividades": lista})

    try:
        cuerpo = json.loads(request.body or b"{}")
    except ValueError:
        return JsonResponse({"codigo": "JSON_INVALIDO", "mensaje": "El cuerpo debe ser un JSON válido."}, status=400)
    try:
        actividad = servicio.crear_actividad(cuerpo, request.admin_id)
    except servicio.DatosInvalidos as error:
        return JsonResponse({"codigo": "DATOS_INVALIDOS", "mensaje": str(error), "errores": error.errores}, status=400)
    return JsonResponse(actividad, status=201)


@require_GET
@exigir_admin
def categorias(request):
    """Categorías para el desplegable del formulario."""
    return JsonResponse({"categorias": servicio.listar_categorias()})


@require_GET
@exigir_admin
def ubicaciones(request):
    """Ubicaciones para el desplegable del formulario."""
    return JsonResponse({"ubicaciones": servicio.listar_ubicaciones()})


@require_GET
@exigir_admin
def listar_organizadores(request):
    """Organizadores activos para asignar al crear actividades."""
    return JsonResponse({"organizadores": organizadores.listar_organizadores()})


@require_http_methods(["PUT", "PATCH"])
@exigir_admin
@exigir_permiso("actividades.gestionar")
def editar_actividad(request, actividad_id):
    try:
        cuerpo = json.loads(request.body or b"{}")
    except ValueError:
        return JsonResponse({"codigo": "JSON_INVALIDO", "mensaje": "El cuerpo debe ser JSON válido."}, status=400)
    if not isinstance(cuerpo, dict):
        return JsonResponse({"codigo": "JSON_INVALIDO", "mensaje": "Se espera un objeto JSON."}, status=400)
    try:
        actividad = servicio.modificar_actividad(actividad_id, cuerpo, request.admin_id)
    except LookupError:
        return JsonResponse({"codigo": "NO_ENCONTRADA", "mensaje": "Actividad no encontrada."}, status=404)
    except servicio.DatosInvalidos as error:
        return JsonResponse({"codigo": "DATOS_INVALIDOS", "mensaje": str(error), "errores": error.errores}, status=400)
    return JsonResponse(actividad)


@require_http_methods(["PATCH"])
@exigir_admin
@exigir_permiso("actividades.gestionar")
def cancelar_actividad(request, actividad_id):
    try:
        actividad = servicio.cancelar_actividad(actividad_id, request.admin_id)
    except LookupError:
        return JsonResponse({"codigo": "NO_ENCONTRADA", "mensaje": "Actividad no encontrada."}, status=404)
    except servicio.DatosInvalidos as error:
        return JsonResponse({"codigo": "DATOS_INVALIDOS", "mensaje": str(error), "errores": error.errores}, status=409)
    return JsonResponse(actividad)


@require_http_methods(["GET", "POST"])
@exigir_admin
def avisos(request):
    if request.method == "GET":
        lista = servicio_avisos.listar_avisos()
        return JsonResponse({"total": len(lista), "avisos": lista})
    # La consulta del historial sigue disponible; publicar requiere privilegio granular.
    from usuarios.permisos import permisos_de
    if not permisos_de(request.admin_id, "administrador").get("avisos.publicar", False):
        return JsonResponse({"codigo": "PERMISO_DENEGADO", "mensaje": "No tienes permiso para publicar avisos."}, status=403)
    try:
        cuerpo = json.loads(request.body or b"{}")
    except ValueError:
        return JsonResponse({"codigo": "JSON_INVALIDO", "mensaje": "JSON no válido."}, status=400)
    try:
        titulo, mensaje = servicio_avisos.validar_aviso(cuerpo)
    except servicio_avisos.AvisoInvalido as error:
        return JsonResponse({"codigo": "DATOS_INVALIDOS", "mensaje": str(error), "errores": error.errores}, status=400)
    from datetime import datetime, timezone
    doc = {"titulo": titulo, "mensaje": mensaje, "autorId": request.admin_id,
           "creadoEn": datetime.now(timezone.utc)}
    resultado = servicio_avisos.mongo.avisos().insert_one(doc)
    doc["_id"] = resultado.inserted_id
    return JsonResponse(servicio_avisos.a_json(doc), status=201)


@require_GET
def catalogo_estudiantes(request):
    """Consulta de solo lectura: solo actividades disponibles, sin credenciales de administrador."""
    lista = [a for a in servicio.listar_actividades() if a.get("estado") == "programada"]
    return JsonResponse({"total": len(lista), "actividades": lista})
