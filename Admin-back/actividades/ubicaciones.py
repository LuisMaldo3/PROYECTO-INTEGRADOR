"""US-27 · Tarea 1: la tabla (colección) de ubicaciones donde se hacen las actividades."""

# (id, nombre). El id es un texto fijo y legible; es lo que guarda cada actividad en ubicacionId.
# Salen de la programación de la T&C Week 2026.
UBICACIONES_INICIALES = [
    ("auditorio", "Auditorio"),
    ("patio-central", "Patio central"),
    ("polideportivo", "Polideportivo"),
]


def preparar_ubicaciones(ubicaciones):
    """Crea el índice y las ubicaciones iniciales. Es seguro repetirlo: nunca pisa lo que ya existe.

    Devuelve cuántas ubicaciones se crearon.
    """
    ubicaciones.create_index("nombre", unique=True)  # no puede haber dos ubicaciones con el mismo nombre
    creadas = 0
    for id_, nombre in UBICACIONES_INICIALES:
        if not ubicaciones.find_one({"_id": id_}):
            ubicaciones.insert_one({"_id": id_, "nombre": nombre})
            creadas += 1
    return creadas
