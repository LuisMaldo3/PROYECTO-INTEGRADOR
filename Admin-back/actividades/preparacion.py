"""US-25 · Tarea 1: prepara las colecciones "categorias" y "actividades" en MongoDB.

Es seguro repetirlo: crea lo que falta y nunca pisa lo que ya existe.
Se ejecuta con:  python manage.py preparar_actividades
"""

# (id, nombre). El id es un texto fijo y legible; es lo que guarda cada actividad en categoriaId.
CATEGORIAS_INICIALES = [
    ("academico", "Académico"),
    ("cultura", "Cultura"),
    ("deportes", "Deportes"),
    ("social", "Social"),
    ("tecnologia", "Tecnología"),
    ("voluntariado", "Voluntariado"),
]


def preparar(categorias, actividades):
    """Crea los índices y las categorías iniciales. Devuelve cuántas categorías se crearon."""
    categorias.create_index("nombre", unique=True)  # no puede haber dos categorías con el mismo nombre
    actividades.create_index([("creadaEn", -1)])  # la lista se pide de la más nueva a la más antigua
    actividades.create_index("categoriaId")

    creadas = 0
    for id_, nombre in CATEGORIAS_INICIALES:
        if not categorias.find_one({"_id": id_}):
            categorias.insert_one({"_id": id_, "nombre": nombre})
            creadas += 1
    return creadas
