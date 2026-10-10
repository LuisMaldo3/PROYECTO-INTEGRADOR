from bson import ObjectId
from bson.errors import InvalidId


def buscar_por_id(coleccion, id_texto, proyeccion=None):
    """Spring Data guarda el id como ObjectId; por si acaso tambien prueba como texto."""
    try:
        doc = coleccion.find_one({"_id": ObjectId(id_texto)}, proyeccion)
        if doc:
            return doc
    except (InvalidId, TypeError):
        pass
    return coleccion.find_one({"_id": id_texto}, proyeccion)


# Solo estos campos salen hacia el frontend. NUNCA passwordHash.
CAMPOS_PUBLICOS = {"nombre": 1, "correo": 1, "rol": 1, "activo": 1}


def a_json(doc):
    return {
        "id": str(doc["_id"]),
        "nombre": doc.get("nombre"),
        "correo": doc.get("correo"),
        "rol": doc.get("rol", "usuario"),
        "activo": doc.get("activo", True),
    }
