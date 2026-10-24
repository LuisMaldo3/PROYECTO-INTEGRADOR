"""Conexion a la MISMA base MongoDB que usa Usuario-back (Spring Boot)."""
from django.conf import settings
from pymongo import MongoClient

_cliente = None


def db():
    global _cliente
    if _cliente is None:
        _cliente = MongoClient(
            settings.MONGODB_URI,
            serverSelectionTimeoutMS=5000,
            connectTimeoutMS=5000,
        )
    try:
        return _cliente.get_default_database()  # la base que trae la URI (…/vynk)
    except Exception:
        return _cliente[settings.MONGODB_DB_POR_DEFECTO]


def usuarios():
    return db()["usuarios"]


def tokens_revocados():
    return db()["tokens_revocados"]


def permisos_usuarios():
    return db()["permisos_usuarios"]
