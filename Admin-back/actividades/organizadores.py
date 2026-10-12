"""Consulta los usuarios activos con rol organizador desde la colección compartida."""
from usuarios import mongo

def listar_organizadores():
    documentos = mongo.usuarios().find({"rol": "organizador", "activo": {"$ne": False}})
    resultado = [{"id": str(u["_id"]), "nombre": u.get("nombre", ""), "correo": u.get("correo", "")} for u in documentos]
    return sorted(resultado, key=lambda u: (u["nombre"].casefold(), u["id"]))
