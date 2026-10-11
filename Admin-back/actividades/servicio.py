"""US-25 · Tarea 2: lógica de las actividades (sin HTTP). Aquí se valida y se guarda en MongoDB."""
from datetime import datetime, timezone

from . import mongo

# Mismos límites y mensajes que la web (Admi-Fron/src/services/validacionActividad.js).
NOMBRE_MIN, NOMBRE_MAX = 3, 80
DESCRIPCION_MIN, DESCRIPCION_MAX = 10, 500


class DatosInvalidos(ValueError):
    """Los datos de la actividad no pasan la revisión. `errores` = { campo: mensaje }."""

    def __init__(self, errores):
        super().__init__("Revisa los datos de la actividad.")
        self.errores = errores


def _texto(valor):
    return valor.strip() if isinstance(valor, str) else ""


def validar(datos):
    """Devuelve (nombre, descripcion, categoria) limpios o lanza DatosInvalidos con todos los errores."""
    datos = datos if isinstance(datos, dict) else {}
    nombre = _texto(datos.get("nombre"))
    descripcion = _texto(datos.get("descripcion"))
    categoria_id = _texto(datos.get("categoriaId"))
    errores = {}

    if not nombre:
        errores["nombre"] = "Escribe el nombre de la actividad."
    elif len(nombre) < NOMBRE_MIN:
        errores["nombre"] = f"El nombre debe tener al menos {NOMBRE_MIN} caracteres."
    elif len(nombre) > NOMBRE_MAX:
        errores["nombre"] = f"El nombre no puede pasar de {NOMBRE_MAX} caracteres."

    if not descripcion:
        errores["descripcion"] = "Escribe una descripción."
    elif len(descripcion) < DESCRIPCION_MIN:
        errores["descripcion"] = f"La descripción debe tener al menos {DESCRIPCION_MIN} caracteres."
    elif len(descripcion) > DESCRIPCION_MAX:
        errores["descripcion"] = f"La descripción no puede pasar de {DESCRIPCION_MAX} caracteres."

    categoria = None
    if not categoria_id:
        errores["categoriaId"] = "Elige una categoría."
    else:
        categoria = mongo.categorias().find_one({"_id": categoria_id})
        if not categoria:
            errores["categoriaId"] = "Elige una categoría válida."

    if errores:
        raise DatosInvalidos(errores)
    return nombre, descripcion, categoria


def _iso(fecha):
    # Mongo devuelve fechas sin zona (en UTC): se marca como UTC para que el navegador la convierta bien.
    if fecha.tzinfo is None:
        fecha = fecha.replace(tzinfo=timezone.utc)
    return fecha.isoformat()


def a_json(doc, nombres_categoria):
    categoria_id = doc.get("categoriaId")
    return {
        "id": str(doc["_id"]),
        "nombre": doc.get("nombre"),
        "descripcion": doc.get("descripcion"),
        "categoria": {"id": categoria_id, "nombre": nombres_categoria.get(categoria_id, "Sin categoría")},
        "creadaPor": doc.get("creadaPor"),
        "creadaEn": _iso(doc["creadaEn"]),
    }


def crear_actividad(datos, admin_id):
    """Valida y guarda una actividad nueva. Devuelve la actividad creada lista para JSON."""
    nombre, descripcion, categoria = validar(datos)
    doc = {
        "nombre": nombre,
        "descripcion": descripcion,
        "categoriaId": categoria["_id"],
        "creadaPor": str(admin_id),
        "creadaEn": datetime.now(timezone.utc),
    }
    doc["_id"] = mongo.actividades().insert_one(doc).inserted_id
    return a_json(doc, {categoria["_id"]: categoria["nombre"]})


def listar_actividades():
    """Todas las actividades, de la más nueva a la más antigua."""
    nombres = {c["_id"]: c["nombre"] for c in mongo.categorias().find({})}
    docs = mongo.actividades().find({}).sort("creadaEn", -1)
    return [a_json(d, nombres) for d in docs]


def listar_categorias():
    """Categorías disponibles para el formulario, por nombre."""
    docs = mongo.categorias().find({}).sort("nombre", 1)
    return [{"id": c["_id"], "nombre": c["nombre"]} for c in docs]
