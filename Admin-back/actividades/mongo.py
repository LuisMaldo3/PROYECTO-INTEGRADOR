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


def resultados_competencias():
    return base.db()["resultados_competencias"]


def reemplazos_inscripciones():
    """Auditoría escrita por el servicio del organizador; el admin solo consulta."""
    return base.db()["reemplazos_inscripciones"]


def torneos():
    """T&C ARENA: torneos de eliminación directa con sus enfrentamientos y auditoría."""
    return base.db()["torneos"]


def torneos_eliminados():
    """Copia de los torneos eliminados (quién y cuándo), por si hay que revisar algo."""
    return base.db()["torneos_eliminados"]


def puntajes():
    """Hojas de puntajes de gymkanas y concursos por puntos: pruebas y puntaje de cada participante."""
    return base.db()["puntajes"]


def cierre():
    """Cierre de la T&C Week: ganador general publicado por el administrador."""
    return base.db()["cierre_tcweek"]
