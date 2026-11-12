"""Cierre de la T&C Week: el administrador publica al departamento ganador con la tabla acumulada oficial.

La tabla sale de los resultados oficiales publicados (la misma que ven todos); aquí no se calcula ni se cambia ningún punto.
Con empate en el primer lugar no se cierra: primero hay que publicar un desempate.
"""
from datetime import datetime, timezone

from . import departamentos, mongo
from .servicio import DatosInvalidos

ID = "tcweek"


class Conflicto(ValueError):
    pass


def _tabla():
    from .resultados_competencias import clasificacion_general
    return [{"area": a["area"], "puntos": a["puntos"], "primerosPuestos": a["primerosPuestos"]} for a in clasificacion_general()["areas"]]


def _con_departamento(fila):
    d = departamentos.por_area_tecsup(fila["area"])
    return {**fila, "departamento": d["id"] if d else None}


def estado():
    tabla = [_con_departamento(f) for f in _tabla()]
    lider = tabla[0] if tabla and tabla[0]["puntos"] > 0 else None
    empate = bool(lider and len(tabla) > 1 and (tabla[1]["puntos"], tabla[1]["primerosPuestos"]) == (lider["puntos"], lider["primerosPuestos"]))
    doc = mongo.cierre().find_one({"_id": ID}) or {}
    cerrada = doc.get("estado") == "cerrada"
    return {"cerrada": cerrada, "ganador": doc.get("ganador") if cerrada else None,
            "tablaFinal": doc.get("tabla") if cerrada else None,
            "cerradaEn": doc["cerradaEn"].isoformat() if cerrada and hasattr(doc.get("cerradaEn"), "isoformat") else None,
            "tabla": tabla, "lider": lider, "empate": empate}


def cerrar(admin_id):
    actual = estado()
    if actual["cerrada"]:
        raise Conflicto("La T&C Week ya está cerrada.")
    if not actual["lider"]:
        raise DatosInvalidos({"cierre": "Todavía no hay puntos oficiales publicados."})
    if actual["empate"]:
        a, b = actual["tabla"][0]["area"], actual["tabla"][1]["area"]
        raise Conflicto(f"Hay empate en el primer lugar entre {a} y {b} (mismos puntos y primeros puestos). Publica un desempate antes de cerrar.")
    from .avisos import crear_aviso
    ganador, tabla = actual["lider"], actual["tabla"]
    podio = "; ".join(f"{i + 1}.º {f['area']} ({f['puntos']} pts)" for i, f in enumerate(tabla[:3]))
    aviso = crear_aviso({"titulo": f"¡{ganador['area']} gana la T&C Week!",
                         "mensaje": f"Con {ganador['puntos']} puntos, {ganador['area']} es el departamento ganador de la T&C Week. "
                                    f"Podio: {podio}. ¡Felicitaciones a todos los participantes!"}, admin_id)
    ahora = datetime.now(timezone.utc)
    nuevo = {"_id": ID, "estado": "cerrada", "ganador": ganador, "tabla": tabla,
             "avisoId": (aviso or {}).get("id") if isinstance(aviso, dict) else None, "cerradaPor": str(admin_id), "cerradaEn": ahora}
    if mongo.cierre().find_one({"_id": ID}):
        mongo.cierre().replace_one({"_id": ID}, nuevo)
    else:
        mongo.cierre().insert_one(nuevo)
    return estado()


def reabrir(admin_id):
    doc = mongo.cierre().find_one({"_id": ID})
    if not doc or doc.get("estado") != "cerrada":
        raise Conflicto("La T&C Week no está cerrada.")
    mongo.cierre().replace_one({"_id": ID}, {**doc, "estado": "abierta", "reabiertaPor": str(admin_id), "reabiertaEn": datetime.now(timezone.utc)})
    return estado()
