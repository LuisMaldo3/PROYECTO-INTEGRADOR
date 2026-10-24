"""Persistencia y validaciones de avisos administrativos."""
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
    if not titulo: errores["titulo"] = "Escribe un título."
    elif len(titulo) > TITULO_MAX: errores["titulo"] = f"Máximo {TITULO_MAX} caracteres."
    if not mensaje: errores["mensaje"] = "Escribe el mensaje."
    elif len(mensaje) > MENSAJE_MAX: errores["mensaje"] = f"Máximo {MENSAJE_MAX} caracteres."
    if errores: raise AvisoInvalido(errores)
    return titulo, mensaje

def a_json(doc):
    return {"id": str(doc["_id"]), "titulo": doc["titulo"], "mensaje": doc["mensaje"],
            "creadoEn": doc["creadoEn"].isoformat(), "autorId": str(doc["autorId"])}

def listar_avisos():
    return [a_json(d) for d in mongo.avisos().find({}).sort("creadoEn", -1).limit(100)]
