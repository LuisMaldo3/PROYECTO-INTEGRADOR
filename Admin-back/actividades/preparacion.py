"""US-25 · Tarea 1: prepara las colecciones "categorias" y "actividades" en MongoDB.

Es seguro repetirlo: crea lo que falta y nunca pisa lo que ya existe.
Se ejecuta con:  python manage.py preparar_actividades
"""

# (id, nombre). El id es un texto fijo y legible; es lo que guarda cada actividad en categoriaId.
# Son las dos categorías de la programación de la T&C Week 2026.
CATEGORIAS_INICIALES = [
    ("cultural", "Actividad cultural"),
    ("concurso", "Concurso T&C Week"),
]

# Categorías de la primera versión. Ya no se usan: se retiran solo si ninguna actividad las tiene.
CATEGORIAS_ANTIGUAS = ["academico", "cultura", "deportes", "social", "tecnologia", "voluntariado"]


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


def retirar_antiguas(categorias, actividades):
    """Borra las categorías de la primera versión que ninguna actividad usa. Devuelve cuántas borró.

    Si una actividad todavía usa una categoría antigua, esa categoría se queda (no se pierden datos).
    """
    retiradas = 0
    for id_ in CATEGORIAS_ANTIGUAS:
        if categorias.find_one({"_id": id_}) and actividades.count_documents({"categoriaId": id_}) == 0:
            categorias.delete_one({"_id": id_})
            retiradas += 1
    return retiradas
