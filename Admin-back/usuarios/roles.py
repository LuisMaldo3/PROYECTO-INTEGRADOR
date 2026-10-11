"""PLACEHOLDER · La tarea 2 (roles válidos) es de Jordi. Se reemplaza por su archivo al integrar.

Mientras tanto solo normaliza el texto y rechaza vacíos; NO limita a la lista de roles.
"""


class RolInvalido(ValueError):
    """El rol pedido no se puede asignar. El mensaje está listo para mostrarse."""


def validar_rol(rol):
    if not isinstance(rol, str) or not rol.strip():
        raise RolInvalido("Falta indicar el rol.")
    return rol.strip().lower()
