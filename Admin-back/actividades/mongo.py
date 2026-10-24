"""Colecciones de MongoDB (las "tablas") de las actividades. Misma base que usuarios."""
from usuarios import mongo as base


def actividades():
    return base.db()["actividades"]


def categorias():
    return base.db()["categorias"]


def avisos():
    return base.db()["avisos"]


def ubicaciones():
    return base.db()["ubicaciones"]


def inscripciones():
    return base.db()["inscripciones"]


def notificaciones():
    return base.db()["notificaciones"]
