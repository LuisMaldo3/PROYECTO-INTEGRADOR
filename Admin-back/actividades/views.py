import json

from django.http import JsonResponse
from django.views.decorators.http import require_GET, require_http_methods

from usuarios.auth import exigir_admin

from . import servicio, organizadores


@require_http_methods(["GET", "POST"])
@exigir_admin
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
