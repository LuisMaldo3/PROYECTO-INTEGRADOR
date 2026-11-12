"""Gymkanas y concursos por puntos.

Todos compiten a la vez en varias pruebas; cada participante recibe un puntaje por prueba y el total define el orden.
Quien organiza la competencia (o el administrador) llena la hoja. El administrador envía los puestos al flujo oficial
de «Resultados» y desde allí los publica: los puntos para la tabla acumulada son los que configuró en la competencia.
"""
import re
from datetime import datetime, timezone

from . import mongo, torneos
from .servicio import DatosInvalidos

ID_PRUEBA = re.compile(r"^[a-z0-9-]{1,40}$")
MAX_PRUEBAS = 20
MAX_PUNTAJE = 100000


class Conflicto(ValueError):
    pass


def _ahora():
    return datetime.now(timezone.utc)


def _actividad(actividad_id):
    actividad = torneos._cargar_actividad(actividad_id) if actividad_id else None
    if not actividad:
        raise torneos.NoEncontrado("La competencia no existe.")
    if actividad.get("tipoActividad") != "competencia":
        raise DatosInvalidos({"actividadId": "Solo las competencias tienen hoja de puntajes."})
    return actividad


def _numero(v):
    return v if (type(v) in (int, float)) and 0 <= v <= MAX_PUNTAJE else None


def tabla(pruebas, puntajes, participantes):
    """Orden por total; si empatan, gana quien ganó más pruebas; luego, por nombre (y se marca el empate para revisarlo)."""
    filas = []
    for p in participantes:
        propios = puntajes.get(p["id"], {})
        total = round(sum(_numero(propios.get(x["id"])) or 0 for x in pruebas), 2)
        ganadas = sum(1 for x in pruebas if _numero(propios.get(x["id"])) is not None and
                      _numero(propios.get(x["id"])) == max((_numero(puntajes.get(o["id"], {}).get(x["id"])) or 0) for o in participantes) > 0)
        filas.append({"participante": p, "total": total, "pruebasGanadas": ganadas,
                      "conPuntaje": any(_numero(propios.get(x["id"])) is not None for x in pruebas)})
    filas.sort(key=lambda f: (-f["total"], -f["pruebasGanadas"], f["participante"]["nombre"].lower()))
    for i, f in enumerate(filas):
        f["puesto"] = i + 1
        anterior = filas[i - 1] if i else None
        f["empate"] = bool(anterior and (anterior["total"], anterior["pruebasGanadas"]) == (f["total"], f["pruebasGanadas"]))
        if f["empate"]:
            anterior["empate"] = True
    return filas


def _json(actividad, doc, participantes):
    pruebas, puntajes = doc.get("pruebas", []), doc.get("puntajes", {})
    return {"actividadId": str(actividad["_id"]), "nombre": actividad.get("nombre", ""), "puntosPorPuesto": actividad.get("puntosPorPuesto") or [],
            "pruebas": pruebas, "puntajes": puntajes, "participantes": participantes, "tabla": tabla(pruebas, puntajes, participantes),
            "modoParticipantes": torneos.modo_de(doc) if doc else "departamentos", "version": doc.get("version", 0), "puestosEnviados": bool(doc.get("puestosEnviados")), "resultadosPublicados": bool(doc.get("resultadosPublicados")),
            "actualizadoEn": doc["actualizadoEn"].isoformat() if hasattr(doc.get("actualizadoEn"), "isoformat") else None}


def hoja(actividad_id):
    actividad = _actividad(actividad_id)
    doc = mongo.puntajes().find_one({"actividadId": str(actividad["_id"])}) or {}
    return _json(actividad, doc, torneos.participantes_segun(actividad, torneos.modo_de(doc) if doc else "departamentos"))


def guardar(actividad_id, datos, usuario_id):
    """Guarda pruebas y puntajes. Usa `version` para no pisar lo que otra persona guardó mientras tanto."""
    actividad = _actividad(actividad_id)
    aid = str(actividad["_id"])
    if any(t.get("estado") != "anulado" for t in mongo.torneos().find({"actividadId": aid})):
        raise Conflicto("Esta competencia se juega como torneo (T&C ARENA); no usa hoja de puntajes.")
    actual_previo = mongo.puntajes().find_one({"actividadId": aid})
    modo = datos.get("modoParticipantes") or (torneos.modo_de(actual_previo) if actual_previo else "departamentos")
    if modo not in torneos.MODOS_PARTICIPANTES:
        raise DatosInvalidos({"modoParticipantes": "Elige si participan los departamentos o los equipos inscritos."})
    participantes = torneos.participantes_segun(actividad, modo)
    validos = {p["id"] for p in participantes}
    errores = {}
    pruebas = datos.get("pruebas")
    if not isinstance(pruebas, list) or not 1 <= len(pruebas) <= MAX_PRUEBAS:
        errores["pruebas"] = f"Agrega entre 1 y {MAX_PRUEBAS} pruebas."
        pruebas = []
    limpias, vistas = [], set()
    for x in pruebas:
        nombre = str((x or {}).get("nombre") or "").strip() if isinstance(x, dict) else ""
        ident = str((x or {}).get("id") or "").strip() if isinstance(x, dict) else ""
        if not ID_PRUEBA.match(ident) or ident in vistas or not 2 <= len(nombre) <= 60:
            errores["pruebas"] = "Cada prueba necesita un nombre (de 2 a 60 caracteres) y no puede repetirse."
            break
        vistas.add(ident)
        limpias.append({"id": ident, "nombre": nombre})
    puntajes = datos.get("puntajes", {})
    limpios = {}
    if not isinstance(puntajes, dict):
        errores["puntajes"] = "Los puntajes no tienen el formato esperado."
    else:
        for pid, por_prueba in puntajes.items():
            if pid not in validos or not isinstance(por_prueba, dict):
                errores["puntajes"] = "Hay puntajes de alguien que no está inscrito o no confirmó su inscripción."
                break
            fila = {}
            for prueba, valor in por_prueba.items():
                if prueba not in vistas:
                    continue  # prueba eliminada: su puntaje se descarta
                if valor is None or valor == "":
                    continue
                if _numero(valor) is None:
                    errores["puntajes"] = f"Los puntajes deben ser números entre 0 y {MAX_PUNTAJE}."
                    break
                fila[prueba] = round(valor, 2) if isinstance(valor, float) else valor
            if fila:
                limpios[pid] = fila
    if errores:
        raise DatosInvalidos(errores)

    coleccion = mongo.puntajes()
    actual = coleccion.find_one({"actividadId": aid})
    version = (actual or {}).get("version", 0)
    if datos.get("version", version) != version:
        raise Conflicto("Otra persona guardó cambios en esta hoja. Recarga para ver lo último.")
    ahora = _ahora()
    doc = {**(actual or {}), "actividadId": aid, "modoParticipantes": modo, "pruebas": limpias, "puntajes": limpios, "version": version + 1,
           "actualizadoPor": str(usuario_id), "actualizadoEn": ahora, "puestosEnviados": False}
    doc["auditoria"] = ((actual or {}).get("auditoria") or [])[-199:] + [{"accion": "guardar", "por": str(usuario_id), "en": ahora,
                                                                          "pruebas": len(limpias), "participantes": len(limpios)}]
    if actual:
        coleccion.replace_one({"_id": actual["_id"]}, doc)
    else:
        doc["_id"] = coleccion.insert_one(doc).inserted_id
    return _json(actividad, doc, participantes)


def enviar_puestos(actividad_id, usuario_id, puntos=None, publicar=False):
    """Lleva el orden final a «Resultados» con los puntos escritos a mano al final; con `publicar`, los publica."""
    from .resultados_competencias import preparar_propuesta, publicar as publicar_oficial
    actual = hoja(actividad_id)
    filas = [f for f in actual["tabla"] if f["conPuntaje"]]
    if not filas:
        raise Conflicto("Todavía no hay puntajes registrados.")
    manuales = torneos.puntos_manuales([(f["puesto"], f["participante"]["id"], None) for f in filas], puntos) if puntos is not None else None
    actual_doc = mongo.puntajes().find_one({"actividadId": actual["actividadId"]}) or {}
    if torneos.modo_de(actual_doc) == "departamentos":
        from .resultados_departamentos import guardar as guardar_por_departamento
        guardar_por_departamento(actual["actividadId"], [(f["puesto"], f["participante"]["id"]) for f in filas], manuales, usuario_id, publicar)
        mongo.puntajes().replace_one({"_id": actual_doc["_id"]}, {**actual_doc, "puestosEnviados": True,
                                                                   "resultadosPublicados": bool(publicar) or actual_doc.get("resultadosPublicados", False)})
        actual.update(puestosEnviados=True, resultadosPublicados=bool(publicar) or bool(actual_doc.get("resultadosPublicados")))
        return actual
    preparar_propuesta(actual["actividadId"], {"posiciones": [{"inscripcionId": f["participante"]["id"], "puesto": f["puesto"],
                                                               **({"puntos": manuales[f["puesto"]]} if manuales else {})} for f in filas]}, usuario_id)
    if publicar:
        publicar_oficial(actual["actividadId"], usuario_id)
    doc = mongo.puntajes().find_one({"actividadId": actual["actividadId"]})
    mongo.puntajes().replace_one({"_id": doc["_id"]}, {**doc, "puestosEnviados": True, "resultadosPublicados": bool(publicar) or doc.get("resultadosPublicados", False)})
    actual.update(puestosEnviados=True, resultadosPublicados=bool(publicar) or bool(doc.get("resultadosPublicados")))
    return actual
