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
