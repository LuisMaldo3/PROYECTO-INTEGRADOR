import json

from django.http import JsonResponse
from django.views.decorators.http import require_GET, require_http_methods

from usuarios.auth import exigir_admin, exigir_organizador
from usuarios.permisos import exigir_permiso

from . import servicio, organizadores, avisos as servicio_avisos, mongo, inscritos as servicio_inscritos


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
        aviso = servicio_avisos.crear_aviso(cuerpo, request.admin_id)
    except servicio_avisos.AvisoInvalido as error:
        return JsonResponse({"codigo": "DATOS_INVALIDOS", "mensaje": str(error), "errores": error.errores}, status=400)
    return JsonResponse(aviso, status=201)


@require_GET
def avisos_publicados(request):
    """Solo comunicados ya visibles; nunca expone avisos programados."""
    lista = servicio_avisos.listar_publicados()
    # La página pública no necesita revelar quién redactó cada aviso.
    publicados = [{clave: valor for clave, valor in aviso.items() if clave != "autorId"} for aviso in lista]
    return JsonResponse({"total": len(publicados), "avisos": publicados})


@require_GET
def catalogo_estudiantes(request):
    """Consulta de solo lectura: solo actividades disponibles, sin credenciales de administrador."""
    lista = [a for a in servicio.listar_actividades() if a.get("estado") == "programada"]
    return JsonResponse({"total": len(lista), "actividades": lista})


@require_http_methods(["GET", "PUT"])
@exigir_admin
@exigir_permiso("actividades.gestionar")
def resultados_competencia(request, actividad_id):
    """US-32: consultar/corregir propuesta administrativa; no alterar un resultado publicado."""
    from . import resultados_competencias as resultados
    try:
        if request.method == "GET":
            return JsonResponse(resultados.obtener(actividad_id))
        cuerpo = json.loads(request.body or b"{}")
        return JsonResponse(resultados.preparar_propuesta(actividad_id, cuerpo, request.admin_id))
    except servicio.DatosInvalidos as e:
        return JsonResponse({"codigo": "DATOS_INVALIDOS", "mensaje": str(e), "errores": e.errores}, status=400)
    except LookupError:
        return JsonResponse({"codigo": "NO_ENCONTRADA", "mensaje": "Competencia no encontrada."}, status=404)
    except (ValueError, TypeError):
        return JsonResponse({"codigo": "JSON_INVALIDO", "mensaje": "JSON inválido."}, status=400)


@require_http_methods(["POST"])
@exigir_admin
@exigir_permiso("actividades.gestionar")
def publicar_resultados(request, actividad_id):
    from . import resultados_competencias as resultados
    try:
        return JsonResponse(resultados.publicar(actividad_id, request.admin_id))
    except LookupError:
        return JsonResponse({"codigo": "NO_ENCONTRADA", "mensaje": "Competencia no encontrada."}, status=404)
    except servicio.DatosInvalidos as e:
        return JsonResponse({"codigo": "DATOS_INVALIDOS", "mensaje": str(e), "errores": e.errores}, status=409)


@require_http_methods(["POST"])
@exigir_admin
@exigir_permiso("actividades.gestionar")
def anunciar_resultados(request, actividad_id):
    from . import resultados_competencias as resultados
    from usuarios.permisos import permisos_de
    if not permisos_de(request.admin_id, "administrador").get("avisos.publicar", False):
        return JsonResponse({"codigo": "PERMISO_DENEGADO", "mensaje": "Falta permiso para publicar avisos."}, status=403)
    try:
        return JsonResponse(resultados.anunciar(actividad_id, request.admin_id), status=201)
    except LookupError:
        return JsonResponse({"codigo": "NO_ENCONTRADA", "mensaje": "Competencia no encontrada."}, status=404)
    except servicio.DatosInvalidos as e:
        return JsonResponse({"codigo": "DATOS_INVALIDOS", "mensaje": str(e), "errores": e.errores}, status=409)


@require_GET
@exigir_admin
@exigir_permiso("actividades.gestionar")
def participantes_competencia(request, actividad_id):
    from . import resultados_competencias as resultados
    try:
        actividad = resultados._actividad(actividad_id)
    except LookupError:
        return JsonResponse({"codigo": "NO_ENCONTRADA", "mensaje": "Competencia no encontrada."}, status=404)
    except servicio.DatosInvalidos as e:
        return JsonResponse({"codigo": "DATOS_INVALIDOS", "mensaje": str(e)}, status=400)
    registros = resultados.participantes_confirmados(actividad).values()
    lista = [{"id": str(p["_id"]), "nombre": p.get("nombreEquipo") or p.get("nombreEstudiante") or "Participante",
              "areaTecsup": p.get("areaTecsup") if p.get("areaVerificada") is True else None,
              "integrantesConfirmados": len(p.get("integrantes", [])) if p.get("nombreEquipo") else 1} for p in registros]
    return JsonResponse({"total": len(lista), "participantes": lista})


@require_GET
@exigir_admin
@exigir_permiso('actividades.gestionar')
def clasificacion_general(request):
    from .resultados_competencias import clasificacion_general as calcular
    return JsonResponse(calcular())


@require_GET
@exigir_admin
def inscritos_por_actividad(request):
    """US-35: número de inscritos por actividad (personas y, en competencias por equipos, equipos)."""
    return JsonResponse(servicio_inscritos.inscritos_por_actividad())


@require_GET
@exigir_admin
@exigir_permiso("actividades.gestionar")
def seguimiento_reemplazos(request):
    """US-36 · Vista administrativa, sin capacidad de escribir reemplazos ni cupos."""
    from .reemplazos import resumen_reemplazos
    return JsonResponse(resumen_reemplazos())


@require_GET
@exigir_admin
def ranking_demanda(request):
    """US-37: ranking de demanda; IA opcional bajo solicitud expresa en la web."""
    from .demanda import obtener_demanda
    return JsonResponse(obtener_demanda(con_ia=request.GET.get('ia') == '1'))


@require_GET
@exigir_admin
def estadisticas_intereses(request):
    """US-38: solamente administración. Datos agregados; Gemini opcional."""
    from .intereses import obtener_intereses
    return JsonResponse(obtener_intereses(con_ia=request.GET.get('ia') == '1'))


@require_GET
@exigir_admin
def resumen_evento(request):
    """US-39: resumen estadístico oficial; Gemini solamente bajo solicitud del admin."""
    from .reporte_evento import obtener_reporte
    return JsonResponse(obtener_reporte(con_ia=request.GET.get('ia') == '1'))


@require_GET
@exigir_admin
def comparacion_categorias(request):
    """US-40 · Datos de participación y tabla oficial de solo lectura."""
    from .comparacion_categorias import obtener_comparacion
    return JsonResponse(obtener_comparacion())


# ---------- T&C ARENA (torneos de eliminación directa entre departamentos) ----------
from . import cierre as servicio_cierre, departamentos as catalogo_departamentos, puntajes as servicio_puntajes, torneos as servicio_torneos  # noqa: E402
from .gemini_config import GeminiError  # noqa: E402


def _cuerpo_json(request):
    try:
        cuerpo = json.loads(request.body or b"{}")
    except ValueError:
        return None, JsonResponse({"codigo": "JSON_INVALIDO", "mensaje": "El cuerpo debe ser JSON válido."}, status=400)
    if not isinstance(cuerpo, dict):
        return None, JsonResponse({"codigo": "JSON_INVALIDO", "mensaje": "Se espera un objeto JSON."}, status=400)
    return cuerpo, None


def _ejecutar(accion):
    """Traduce los errores del servicio de torneos a respuestas HTTP con el formato del proyecto."""
    try:
        return JsonResponse(accion())
    except servicio.DatosInvalidos as e:
        return JsonResponse({"codigo": "DATOS_INVALIDOS", "mensaje": str(e), "errores": e.errores}, status=400)
    except servicio_torneos.NoEncontrado as e:
        return JsonResponse({"codigo": "NO_ENCONTRADO", "mensaje": str(e)}, status=404)
    except servicio_torneos.PermisoDenegado as e:
        return JsonResponse({"codigo": "PERMISO_DENEGADO", "mensaje": str(e)}, status=403)
    except (servicio_torneos.Conflicto, servicio_puntajes.Conflicto, servicio_cierre.Conflicto) as e:
        return JsonResponse({"codigo": "CONFLICTO", "mensaje": str(e)}, status=409)
    except GeminiError as e:
        return JsonResponse({"codigo": "IA_NO_DISPONIBLE", "tipo": e.tipo, "mensaje": str(e)}, status=503)


@require_GET
@exigir_admin
def lista_departamentos(request):
    return JsonResponse({"departamentos": catalogo_departamentos.DEPARTAMENTOS})


@require_http_methods(["GET", "POST"])
@exigir_admin
@exigir_permiso("torneos.gestionar")
def torneos(request):
    """GET ?tipo=arena: lista de torneos. POST: crea un torneo y genera sus llaves."""
    if request.method == "GET":
        return JsonResponse({"torneos": servicio_torneos.listar(request.GET.get("tipo"))})
    cuerpo, error = _cuerpo_json(request)
    if error:
        return error
    respuesta = _ejecutar(lambda: servicio_torneos.crear_torneo(cuerpo, request.admin_id))
    if respuesta.status_code == 200:
        respuesta.status_code = 201
    return respuesta


@require_http_methods(["GET", "DELETE"])
@exigir_admin
@exigir_permiso("torneos.gestionar")
def detalle_torneo(request, torneo_id):
    """GET: el torneo con su historial. DELETE: lo elimina (y sus puntos dejan de contar en la tabla acumulada)."""
    if request.method == "DELETE":
        return _ejecutar(lambda: servicio_torneos.eliminar_torneo(torneo_id, request.admin_id))
    return _ejecutar(lambda: servicio_torneos.detalle(torneo_id))


@require_http_methods(["POST"])
@exigir_admin
@exigir_permiso("torneos.gestionar")
def enviar_puestos_torneo(request, torneo_id):
    """Puestos finales con los puntos escritos a mano ({"puntos": {"1": 10, ...}, "publicar": true})."""
    cuerpo, error = _cuerpo_json(request)
    if error:
        return error
    return _ejecutar(lambda: servicio_torneos.enviar_puestos(torneo_id, request.admin_id, cuerpo.get("puntos"), cuerpo.get("publicar") is True))


@require_GET
@exigir_admin
@exigir_permiso("torneos.gestionar")
def participantes_torneo(request):
    """Equipos o alumnos confirmados de una competencia, para armar el torneo."""
    return _ejecutar(lambda: servicio_torneos.participantes_disponibles(request.GET.get("actividadId", "")))


@require_http_methods(["POST"])
@exigir_admin
@exigir_permiso("torneos.gestionar")
def programar_partido(request, torneo_id, partido_id):
    cuerpo, error = _cuerpo_json(request)
    if error:
        return error
    return _ejecutar(lambda: servicio_torneos.programar(torneo_id, partido_id, cuerpo, request.admin_id))


@require_http_methods(["POST"])
@exigir_admin
@exigir_permiso("torneos.gestionar")
def estado_partido(request, torneo_id, partido_id):
    cuerpo, error = _cuerpo_json(request)
    if error:
        return error
    return _ejecutar(lambda: servicio_torneos.cambiar_estado(torneo_id, partido_id, cuerpo.get("estado"), request.admin_id))


@require_http_methods(["POST"])
@exigir_admin
@exigir_permiso("torneos.gestionar")
def resultado_partido(request, torneo_id, partido_id):
    cuerpo, error = _cuerpo_json(request)
    if error:
        return error
    return _ejecutar(lambda: servicio_torneos.registrar_resultado(torneo_id, partido_id, cuerpo, request.admin_id))


@require_http_methods(["POST"])
@exigir_admin
@exigir_permiso("torneos.validar")
def validar_partido(request, torneo_id, partido_id):
    return _ejecutar(lambda: servicio_torneos.validar_resultado(torneo_id, partido_id, request.admin_id))


# ---------- IA: propuesta de sorteo (propone; no crea ni decide nada) ----------

@require_http_methods(["POST"])
@exigir_admin
@exigir_permiso("torneos.gestionar")
def propuesta_ia_torneo(request):
    cuerpo, error = _cuerpo_json(request)
    if error:
        return error
    return _ejecutar(lambda: servicio_torneos.proponer_sorteo(cuerpo.get("actividadId"), cuerpo.get("participantes"), modo=cuerpo.get("modoParticipantes") or "departamentos"))


# ---------- Gymkanas y concursos por puntos ----------

@require_GET
@exigir_admin
def competencias_puntajes(request):
    return JsonResponse({"competencias": servicio_torneos.competencias_de_organizador(request.admin_id, "administrador")})


@require_http_methods(["GET", "PUT"])
@exigir_admin
def hoja_puntajes(request, actividad_id):
    if request.method == "GET":
        return _ejecutar(lambda: servicio_puntajes.hoja(actividad_id))
    cuerpo, error = _cuerpo_json(request)
    if error:
        return error
    return _ejecutar(lambda: servicio_puntajes.guardar(actividad_id, cuerpo, request.admin_id))


@require_http_methods(["POST"])
@exigir_admin
def enviar_puestos_puntajes(request, actividad_id):
    cuerpo, error = _cuerpo_json(request)
    if error:
        return error
    return _ejecutar(lambda: servicio_puntajes.enviar_puestos(actividad_id, request.admin_id, cuerpo.get("puntos"), cuerpo.get("publicar") is True))


# ---------- Cierre de la T&C Week ----------

@require_http_methods(["GET", "POST", "DELETE"])
@exigir_admin
def cierre_semana(request):
    """GET: líder y estado. POST: cierra y publica el ganador (con aviso). DELETE: reabre si hubo un error."""
    if request.method == "POST":
        return _ejecutar(lambda: servicio_cierre.cerrar(request.admin_id))
    if request.method == "DELETE":
        return _ejecutar(lambda: servicio_cierre.reabrir(request.admin_id))
    return _ejecutar(servicio_cierre.estado)


# ---------- Organizador: solo sus competencias; no valida, no elimina y no publica ----------

@require_GET
@exigir_organizador
def organizador_competencias(request):
    return JsonResponse({"competencias": servicio_torneos.competencias_de_organizador(request.usuario_id, request.rol)})


@require_http_methods(["GET", "POST"])
@exigir_organizador
def organizador_torneos(request):
    if request.method == "GET":
        return JsonResponse({"torneos": servicio_torneos.listar_de_organizador(request.usuario_id, request.rol)})
    cuerpo, error = _cuerpo_json(request)
    if error:
        return error

    def crear():
        servicio_torneos.exigir_organizador_de(cuerpo.get("actividadId"), request.usuario_id, request.rol)
        return servicio_torneos.crear_torneo({**cuerpo, "tipo": "arena"}, request.usuario_id)
    respuesta = _ejecutar(crear)
    if respuesta.status_code == 200:
        respuesta.status_code = 201
    return respuesta


@require_GET
@exigir_organizador
def organizador_torneo(request, torneo_id):
    return _ejecutar(lambda: servicio_torneos.ver(torneo_id, request.usuario_id, request.rol))


@require_http_methods(["POST"])
@exigir_organizador
def organizador_accion_partido(request, torneo_id, partido_id, accion):
    """programar | estado | resultado (marcadores). Validar, eliminar y publicar quedan para administración."""
    if accion not in ("programar", "estado", "resultado"):
        return JsonResponse({"codigo": "NO_ENCONTRADO", "mensaje": "Acción no disponible."}, status=404)
    cuerpo, error = _cuerpo_json(request)
    if error:
        return error

    def hacer():
        servicio_torneos.verificar_organizador(torneo_id, request.usuario_id, request.rol)
        if accion == "programar":
            return servicio_torneos.programar(torneo_id, partido_id, cuerpo, request.usuario_id)
        if accion == "estado":
            return servicio_torneos.cambiar_estado(torneo_id, partido_id, cuerpo.get("estado"), request.usuario_id)
        return servicio_torneos.registrar_resultado(torneo_id, partido_id, cuerpo, request.usuario_id)
    return _ejecutar(hacer)


@require_http_methods(["POST"])
@exigir_organizador
def organizador_propuesta_ia(request):
    cuerpo, error = _cuerpo_json(request)
    if error:
        return error

    def proponer():
        servicio_torneos.exigir_organizador_de(cuerpo.get("actividadId"), request.usuario_id, request.rol)
        return servicio_torneos.proponer_sorteo(cuerpo.get("actividadId"), cuerpo.get("participantes"), modo=cuerpo.get("modoParticipantes") or "departamentos")
    return _ejecutar(proponer)


@require_http_methods(["GET", "PUT"])
@exigir_organizador
def organizador_puntajes(request, actividad_id):
    if request.method == "PUT":
        cuerpo, error = _cuerpo_json(request)
        if error:
            return error

    def hacer():
        servicio_torneos.exigir_organizador_de(actividad_id, request.usuario_id, request.rol)
        if request.method == "GET":
            return servicio_puntajes.hoja(actividad_id)
        return servicio_puntajes.guardar(actividad_id, cuerpo, request.usuario_id)
    return _ejecutar(hacer)


@require_GET
@exigir_admin
def estado_ia(request):
    """Estado de la IA; con ?probar=1 hace una prueba real con Google (sin mostrar la clave)."""
    from .gemini_config import diagnostico
    return JsonResponse(diagnostico(probar=request.GET.get("probar") == "1"))
