"""Departamentos que compiten en T&C Week (no son las carreras: ellas se mantienen aparte).

Fuente única en el backend. Cada departamento tiene un identificador estable, un color y la
«areaTecsup» con que ya se guarda el área de los estudiantes, para enlazar estudiantes y equipos
sin tocar los datos existentes.

Colores definidos para el proyecto (validar con la organización de TECSUP antes de presentarlos
como identidad oficial). La denominación del departamento morado (DPI o DPG) está por confirmar.
"""

DEPARTAMENTOS = [
    {"id": "quimica", "nombre": "Química", "corto": "QUÍM", "color": "verde", "hex": "#12A150",
     "areaTecsup": "Minería y Procesos Químicos", "denominacionPorConfirmar": False},
    {"id": "mecanica", "nombre": "Mecánica", "corto": "MEC", "color": "amarillo", "hex": "#F5B700",
     "areaTecsup": "Mecánica", "denominacionPorConfirmar": False},
    {"id": "td", "nombre": "TD", "corto": "TD", "color": "rojo", "hex": "#DC2626",
     "areaTecsup": "Tecnología Digital", "denominacionPorConfirmar": False},
    {"id": "electricidad", "nombre": "Electricidad", "corto": "ELEC", "color": "azul", "hex": "#1D6FE0",
     "areaTecsup": "Electricidad y Electrónica", "denominacionPorConfirmar": False},
    {"id": "dpg", "nombre": "DPG", "corto": "DPG", "color": "morado", "hex": "#7C3AED",
     "areaTecsup": "Gestión y Producción", "denominacionPorConfirmar": True},
]

_POR_ID = {d["id"]: d for d in DEPARTAMENTOS}


def ids_validos():
    return set(_POR_ID)


def obtener(departamento_id):
    return _POR_ID.get(departamento_id)


def nombre(departamento_id):
    d = _POR_ID.get(departamento_id)
    return d["nombre"] if d else None


def por_area_tecsup(area):
    """Departamento al que pertenece un área existente de los estudiantes (o None)."""
    for d in DEPARTAMENTOS:
        if d["areaTecsup"] == area:
            return d
    return None
