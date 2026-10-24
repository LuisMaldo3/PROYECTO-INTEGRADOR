"""US-26 · Tareas 1 y 2: fecha y horario de una actividad.

fecha      "AAAA-MM-DD"  (obligatoria, un día real del calendario)
horaInicio "HH:MM" 24 h  (obligatoria)
horaFin    "HH:MM" 24 h  (opcional; si viene, debe ser DESPUÉS de la de inicio)
"""
import re
from datetime import date

_FECHA = re.compile(r"^\d{4}-\d{2}-\d{2}$")
_HORA = re.compile(r"^([01]\d|2[0-3]):[0-5]\d$")

# Mismos mensajes que la web (Admi-Fron/src/services/validacionActividad.js).
FECHA_FALTA = "Elige la fecha de la actividad."
FECHA_MALA = "La fecha no es válida."
INICIO_FALTA = "Elige la hora de inicio."
INICIO_MALA = "La hora de inicio no es válida."
FIN_MALA = "La hora de fin no es válida."
FIN_ANTES = "La hora de fin debe ser después de la de inicio."


def _texto(valor):
    return valor.strip() if isinstance(valor, str) else ""


def _fecha_real(texto):
    if not _FECHA.match(texto):
        return False
    try:
        date.fromisoformat(texto)
    except ValueError:  # por ejemplo 2026-02-30
        return False
    return True


def validar_horario(datos):
    """Devuelve (horario, errores).

    horario = {"fecha", "horaInicio", "horaFin"} (horaFin es None si no se indicó).
    errores = { campo: mensaje } solo con los campos que fallaron; {} si todo está bien.
    """
    datos = datos if isinstance(datos, dict) else {}
    fecha = _texto(datos.get("fecha"))
    inicio = _texto(datos.get("horaInicio"))
    fin = _texto(datos.get("horaFin"))
    errores = {}

    if not fecha:
        errores["fecha"] = FECHA_FALTA
    elif not _fecha_real(fecha):
        errores["fecha"] = FECHA_MALA

    if not inicio:
        errores["horaInicio"] = INICIO_FALTA
    elif not _HORA.match(inicio):
        errores["horaInicio"] = INICIO_MALA

    if fin:
        if not _HORA.match(fin):
            errores["horaFin"] = FIN_MALA
        elif "horaInicio" not in errores and fin <= inicio:  # "HH:MM" en 24 h se compara bien como texto
            errores["horaFin"] = FIN_ANTES

    return {"fecha": fecha, "horaInicio": inicio, "horaFin": fin or None}, errores
