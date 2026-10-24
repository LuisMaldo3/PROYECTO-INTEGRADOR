"""Lógica de negocio sobre los usuarios (sin HTTP): aquí se escribe en MongoDB."""
from . import mongo
from .vistas_util import CAMPOS_PUBLICOS, buscar_por_id


def cambiar_rol(usuario_id, rol):
    """US-24 · Tarea 1: guarda el nuevo rol del usuario.

    `rol` ya debe venir validado (ver roles.validar_rol). Devuelve el usuario
    actualizado con los campos públicos (nunca passwordHash) o None si no existe.
    """
    coleccion = mongo.usuarios()
    existente = buscar_por_id(coleccion, usuario_id, {"_id": 1})
    if not existente:
        return None
    # Se usa el _id tal como está guardado (ObjectId o texto) para no fallar al actualizar.
    coleccion.update_one({"_id": existente["_id"]}, {"$set": {"rol": rol}})
    return buscar_por_id(coleccion, usuario_id, CAMPOS_PUBLICOS)


def cambiar_estado(usuario_id, activo):
    """US · Activa o desactiva usando el ID real; no modifica credenciales."""
    coleccion = mongo.usuarios()
    existente = buscar_por_id(coleccion, usuario_id, {"_id": 1})
    if not existente:
        return None
    coleccion.update_one({"_id": existente["_id"]}, {"$set": {"activo": activo}})
    return buscar_por_id(coleccion, usuario_id, CAMPOS_PUBLICOS)


def establecer_permisos(usuario_id, nuevos, admin_id):
    """Actualiza solo claves del catálogo, sin eliminar permisos no incluidos."""
    from datetime import datetime, timezone
    from .permisos import CATALOGO, permisos_de
    if not isinstance(nuevos, dict) or not nuevos or any(
            k not in CATALOGO or not isinstance(v, bool) for k, v in nuevos.items()):
        raise ValueError("Se requiere un objeto de permisos conocidos con valores true/false.")
    if str(usuario_id) == str(admin_id) and nuevos.get("usuarios.gestionar_permisos") is False:
        raise ValueError("No puedes quitarte el permiso de gestionar permisos.")
    usuario = buscar_por_id(mongo.usuarios(), usuario_id, {"_id": 1, "rol": 1})
    if usuario is None:
        return None
    mongo.permisos_usuarios().update_one(
        {"_id": str(usuario["_id"])},
        {"$set": {"permisos." + k: v for k, v in nuevos.items()} |
                 {"actualizadoPor": str(admin_id), "actualizadoEn": datetime.now(timezone.utc)}},
        upsert=True,
    )
    return permisos_de(str(usuario["_id"]), usuario.get("rol", "usuario"))
