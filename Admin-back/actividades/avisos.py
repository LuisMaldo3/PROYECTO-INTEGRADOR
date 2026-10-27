"""Avisos administrativos inmediatos y programados (MongoDB, UTC)."""
from datetime import datetime, timezone
from . import mongo

TITULO_MAX = 100
MENSAJE_MAX = 2000


class AvisoInvalido(ValueError):
    def __init__(self, errores):
        self.errores = errores
        super().__init__("Revisa los datos del aviso.")


def validar_aviso(datos):
    datos = datos if isinstance(datos, dict) else {}
    titulo = datos.get("titulo")
    mensaje = datos.get("mensaje")
    titulo = titulo.strip() if isinstance(titulo, str) else ""
    mensaje = mensaje.strip() if isinstance(mensaje, str) else ""
    errores = {}
    if not titulo:
        errores["titulo"] = "Escribe un título."
    elif len(titulo) > TITULO_MAX:
        errores["titulo"] = f"Máximo {TITULO_MAX} caracteres."
    if not mensaje:
        errores["mensaje"] = "Escribe el mensaje."
    elif len(mensaje) > MENSAJE_MAX:
        errores["mensaje"] = f"Máximo {MENSAJE_MAX} caracteres."
    if errores:
        raise AvisoInvalido(errores)
    return titulo, mensaje


def fecha_programada(datos, ahora=None):
    """None => publicar ahora. Se exige ISO 8601 con zona para evitar ambigüedad."""
    datos = datos if isinstance(datos, dict) else {}
    valor = datos.get("publicarEn")
    if valor is None or valor == "":
        return None
    if not isinstance(valor, str) or len(valor) > 40:
        raise AvisoInvalido({"publicarEn": "Indica una fecha válida."})
    try:
        fecha = datetime.fromisoformat(valor.replace("Z", "+00:00"))
    except ValueError:
        raise AvisoInvalido({"publicarEn": "Fecha y hora incorrectas (ISO 8601)."}) from None
    if fecha.tzinfo is None or fecha.utcoffset() is None:
        raise AvisoInvalido({"publicarEn": "La fecha debe incluir zona horaria."})
    fecha = fecha.astimezone(timezone.utc)
    if fecha <= (ahora or datetime.now(timezone.utc)):
        raise AvisoInvalido({"publicarEn": "La publicación debe estar programada para el futuro."})
    return fecha


def _iso(fecha):
    if fecha is None:
        return None
    if fecha.tzinfo is None:
        fecha = fecha.replace(tzinfo=timezone.utc)
    return fecha.isoformat()


def a_json(doc):
    return {"id": str(doc["_id"]), "titulo": doc["titulo"], "mensaje": doc["mensaje"],
            "creadoEn": _iso(doc["creadoEn"]), "autorId": str(doc["autorId"]),
            "estado": doc.get("estado", "publicado"),
            "publicarEn": _iso(doc.get("publicarEn")),
            "publicadoEn": _iso(doc.get("publicadoEn", doc.get("creadoEn") if doc.get("estado") != "programado" else None))}


def crear_aviso(datos, autor_id, ahora=None):
    titulo, mensaje = validar_aviso(datos)
    ahora = ahora or datetime.now(timezone.utc)
    futuro = fecha_programada(datos, ahora)
    doc = {"titulo": titulo, "mensaje": mensaje, "autorId": str(autor_id), "creadoEn": ahora,
           "estado": "programado" if futuro else "publicado", "publicarEn": futuro,
           "publicadoEn": None if futuro else ahora}
    doc["_id"] = mongo.avisos().insert_one(doc).inserted_id
    return a_json(doc)


def publicar_pendientes(ahora=None):
    """Transición atómica por documento; seguro al ejecutar múltiples trabajadores."""
    ahora = ahora or datetime.now(timezone.utc)
    resultado = mongo.avisos().update_many(
        {"estado": "programado", "publicarEn": {"$lte": ahora}},
        {"$set": {"estado": "publicado", "publicadoEn": ahora}})
    return resultado.modified_count


def listar_avisos():
    publicar_pendientes()  # recupera publicaciones si el planificador estuvo apagado
    return [a_json(d) for d in mongo.avisos().find({}).sort("creadoEn", -1).limit(200)]


def listar_publicados():
    publicar_pendientes()
    # Avisos antiguos no tenían el campo estado; se consideran ya publicados.
    filtro = {"$or": [{"estado": "publicado"}, {"estado": {"$exists": False}}]}
    return [a_json(d) for d in mongo.avisos().find(filtro).sort("creadoEn", -1).limit(100)]
