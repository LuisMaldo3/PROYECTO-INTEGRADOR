"""Detecta una modificación real de la ubicación guardada de una actividad."""


def detectar_cambio_ubicacion(antes, despues):
    anterior = antes.get("ubicacionId") or None
    nuevo = despues.get("ubicacionId") or None
    if str(anterior) == str(nuevo):
        return {}
    return {"ubicacionId": {"anterior": anterior, "nuevo": nuevo}}
