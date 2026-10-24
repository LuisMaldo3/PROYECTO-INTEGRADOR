"""Identifica cambios reales de fecha, comienzo o final (incluido None / '')."""
CAMPOS_HORARIO = ("fecha", "horaInicio", "horaFin")


def detectar_cambios_horario(antes, despues):
    return {campo: {"anterior": antes.get(campo) or None, "nuevo": despues.get(campo) or None}
            for campo in CAMPOS_HORARIO
            if (antes.get(campo) or None) != (despues.get(campo) or None)}
