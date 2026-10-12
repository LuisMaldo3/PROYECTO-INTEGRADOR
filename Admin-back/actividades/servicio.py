"""US-25 · Tarea 2: lógica de las actividades (sin HTTP). Aquí se valida y se guarda en MongoDB."""
from datetime import datetime, timezone

from . import mongo
from .horario import validar_horario
from usuarios import mongo as mongo_usuarios
from usuarios.vistas_util import buscar_por_id

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
    """Devuelve (nombre, descripcion, categoria, ubicacion, horario) limpios o lanza DatosInvalidos con todos los errores."""
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

    ubicacion = None
    ubicacion_id = _texto(datos.get("ubicacionId"))
    if not ubicacion_id:
        errores["ubicacionId"] = "Elige una ubicación."
    else:
        ubicacion = mongo.ubicaciones().find_one({"_id": ubicacion_id})
        if not ubicacion:
            errores["ubicacionId"] = "Elige una ubicación válida."

    organizador_id = _texto(datos.get("organizadorId"))
    organizador = buscar_por_id(mongo_usuarios.usuarios(), organizador_id) if organizador_id else None
    if not organizador_id:
        errores["organizadorId"] = "Elige un organizador responsable."
    elif not organizador or organizador.get("rol", "usuario") != "organizador" or not organizador.get("activo", True):
        errores["organizadorId"] = "Elige un organizador activo y válido."

    maximo = datos.get("maximoParticipantes")
    if isinstance(maximo, bool) or not isinstance(maximo, int) or maximo <= 0:
        errores["maximoParticipantes"] = "El máximo de participantes debe ser un entero mayor que cero."

    horario, errores_horario = validar_horario(datos)
    errores.update(errores_horario)

    if errores:
        raise DatosInvalidos(errores)
    return nombre, descripcion, categoria, ubicacion, horario, maximo, organizador


def _iso(fecha):
    # Mongo devuelve fechas sin zona (en UTC): se marca como UTC para que el navegador la convierta bien.
    if fecha.tzinfo is None:
        fecha = fecha.replace(tzinfo=timezone.utc)
    return fecha.isoformat()


def calcular_cupos_disponibles(doc):
    """Cupos restantes, nunca negativos; None para actividades antiguas sin límite."""
    maximo = doc.get("maximoParticipantes")
    if maximo is None:
        return None
    return max(0, maximo - doc.get("participantesInscritos", 0))


def a_json(doc, nombres_categoria, nombres_ubicacion=None):
    categoria_id = doc.get("categoriaId")
    ubicacion_id = doc.get("ubicacionId")
    nombres_ubicacion = nombres_ubicacion or {}
    return {
        "id": str(doc["_id"]),
        "nombre": doc.get("nombre"),
        "descripcion": doc.get("descripcion"),
        "categoria": {"id": categoria_id, "nombre": nombres_categoria.get(categoria_id, "Sin categoría")},
        # Las actividades viejas, creadas antes de las ubicaciones, no la tienen: ubicacion = None.
        "ubicacion": {"id": ubicacion_id, "nombre": nombres_ubicacion.get(ubicacion_id, "Sin ubicación")} if ubicacion_id else None,
        "fecha": doc.get("fecha"),  # las actividades viejas, creadas antes del horario, no la tienen
        "horaInicio": doc.get("horaInicio"),
        "horaFin": doc.get("horaFin"),
        "maximoParticipantes": doc.get("maximoParticipantes"),
        "participantesInscritos": doc.get("participantesInscritos", 0),
        "cuposDisponibles": calcular_cupos_disponibles(doc),
        "organizadorId": doc.get("organizadorId"),
        "organizador": doc.get("organizador"),
        "creadaPor": doc.get("creadaPor"),
        "creadaEn": _iso(doc["creadaEn"]),
    }


def crear_actividad(datos, admin_id):
    """Valida y guarda una actividad nueva. Devuelve la actividad creada lista para JSON."""
    nombre, descripcion, categoria, ubicacion, horario, maximo, organizador = validar(datos)
    doc = {
        "nombre": nombre,
        "descripcion": descripcion,
        "categoriaId": categoria["_id"],
        "ubicacionId": ubicacion["_id"],
        "maximoParticipantes": maximo,
        "participantesInscritos": 0,
        "organizadorId": str(organizador["_id"]),
        "organizador": {"id": str(organizador["_id"]), "nombre": organizador.get("nombre", "")},
        **horario,
        "creadaPor": str(admin_id),
        "creadaEn": datetime.now(timezone.utc),
    }
    doc["_id"] = mongo.actividades().insert_one(doc).inserted_id
    return a_json(doc, {categoria["_id"]: categoria["nombre"]}, {ubicacion["_id"]: ubicacion["nombre"]})


def listar_actividades():
    """Todas las actividades por fecha y hora de inicio; las que no tienen fecha (las viejas) van al final."""
    nombres = {c["_id"]: c["nombre"] for c in mongo.categorias().find({})}
    nombres_ubicacion = {u["_id"]: u["nombre"] for u in mongo.ubicaciones().find({})}
    docs = list(mongo.actividades().find({}))
    docs.sort(key=lambda d: (d.get("fecha") is None, d.get("fecha") or "", d.get("horaInicio") or "", d.get("nombre") or ""))
    return [a_json(d, nombres, nombres_ubicacion) for d in docs]


def listar_categorias():
    """Categorías disponibles para el formulario, por nombre."""
    docs = mongo.categorias().find({}).sort("nombre", 1)
    return [{"id": c["_id"], "nombre": c["nombre"]} for c in docs]


def listar_ubicaciones():
    """Ubicaciones disponibles para el formulario, por nombre."""
    docs = mongo.ubicaciones().find({}).sort("nombre", 1)
    return [{"id": u["_id"], "nombre": u["nombre"]} for u in docs]
