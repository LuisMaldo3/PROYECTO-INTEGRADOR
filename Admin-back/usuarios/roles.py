"""Roles que se pueden asignar (US-24 · Tarea 2).

DEBEN COINCIDIR con Roles.java de Usuario-back (usuario, organizador, administrador).
"""

ROLES_VALIDOS = ("usuario", "organizador", "administrador")


class RolInvalido(ValueError):
    """El rol pedido no se puede asignar. El mensaje está listo para mostrarse."""


def validar_rol(rol):
    """Devuelve el rol normalizado (sin espacios y en minúsculas) o lanza RolInvalido."""
    if not isinstance(rol, str) or not rol.strip():
        raise RolInvalido("Falta indicar el rol.")
    limpio = rol.strip().lower()
    if limpio not in ROLES_VALIDOS:
        raise RolInvalido(f"Rol no válido. Usa uno de: {', '.join(ROLES_VALIDOS)}.")
    return limpio
