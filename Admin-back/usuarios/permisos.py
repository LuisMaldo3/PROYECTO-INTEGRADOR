"""Permisos administrativos adicionales por cuenta, persistidos en MongoDB.

La ausencia de configuración conserva los privilegios administrativos previos.
Las cuentas no administradoras no adquieren acceso al panel solo por tener permisos.
"""
from functools import wraps
from django.http import JsonResponse
from . import mongo

CATALOGO = {
    "usuarios.ver": "Consultar usuarios",
    "usuarios.editar_rol": "Cambiar roles",
    "usuarios.gestionar_permisos": "Administrar permisos",
    "usuarios.cambiar_estado": "Activar o desactivar cuentas",
    "actividades.gestionar": "Administrar actividades",
    "avisos.publicar": "Publicar avisos",
}


def permisos_de(usuario_id, rol="usuario"):
    predeterminados = {nombre: rol == "administrador" for nombre in CATALOGO}
    registro = mongo.permisos_usuarios().find_one({"_id": str(usuario_id)}, {"permisos": 1})
    if registro:
        almacenados = registro.get("permisos", {})
        predeterminados.update({k: v for k, v in almacenados.items()
                                 if k in CATALOGO and isinstance(v, bool)})
    return predeterminados


def exigir_permiso(nombre):
    if nombre not in CATALOGO:
        raise ValueError("Permiso no registrado")
    def decorar(vista):
        @wraps(vista)
        def verificar(request, *args, **kwargs):
            if not permisos_de(request.admin_id, "administrador").get(nombre, False):
                return JsonResponse({"codigo": "PERMISO_DENEGADO", "mensaje": "No tienes permiso para realizar esta acción."}, status=403)
            return vista(request, *args, **kwargs)
        return verificar
    return decorar
