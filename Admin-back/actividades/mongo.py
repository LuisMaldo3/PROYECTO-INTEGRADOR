"""Colecciones de MongoDB (las "tablas") de las actividades. Misma base que usuarios."""
from usuarios import mongo as base


def actividades():
    return base.db()["actividades"]


def categorias():
    return base.db()["categorias"]
