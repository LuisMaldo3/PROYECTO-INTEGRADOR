"""US-25 · Tarea 1: prepara las colecciones "categorias" y "actividades" en MongoDB.

Es seguro repetirlo: crea lo que falta y nunca pisa lo que ya existe.
Se ejecuta con:  python manage.py preparar_actividades
"""

# (id, nombre). El id es un texto fijo y legible; es lo que guarda cada actividad en categoriaId.
# Salen de la programación de la T&C Week 2026: «Actividad cultural» y «Concurso T&C Week».
CATEGORIAS_INICIALES = [
    ("actividad-cultural", "Actividad cultural"),
    ("concurso-tc-week", "Concurso T&C Week"),
]

# Categorías de la primera versión de prueba. Se borran solo si ninguna actividad las usa.
CATEGORIAS_RETIRADAS = ["academico", "cultura", "deportes", "social", "tecnologia", "voluntariado"]


def retirar_obsoletas(categorias, actividades):
    """Borra las categorías viejas que ya no se usan. Devuelve cuántas borró."""
    retiradas = 0
    for id_ in CATEGORIAS_RETIRADAS:
        if categorias.find_one({"_id": id_}) and not actividades.find_one({"categoriaId": id_}):
            categorias.delete_one({"_id": id_})
            retiradas += 1
    return retiradas


def preparar(categorias, actividades):
    """Crea los índices y las categorías iniciales. Devuelve (categorías creadas, categorías viejas retiradas)."""
    categorias.create_index("nombre", unique=True)  # no puede haber dos categorías con el mismo nombre
    actividades.create_index([("creadaEn", -1)])
    actividades.create_index("categoriaId")

    creadas = 0
    for id_, nombre in CATEGORIAS_INICIALES:
        if not categorias.find_one({"_id": id_}):
            categorias.insert_one({"_id": id_, "nombre": nombre})
            creadas += 1
    return creadas, retirar_obsoletas(categorias, actividades)
