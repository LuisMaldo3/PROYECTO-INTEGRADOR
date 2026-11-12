"""US-28/US-31 · Configuración oficial de competencias de una sola sede.
No calcula puntos de tablas globales (pendiente del sprint 3).
"""

AREAS_TECSUP = (
    "Gestión y Producción",
    "Tecnología Digital",
    "Minería y Procesos Químicos",
    "Electricidad y Electrónica",
    "Mecánica",
)

# Categoría de una competencia. Cada categoría es una competencia aparte (con sus inscritos, llaves y puntos).
CATEGORIAS_GENERO = ("", "varones", "damas", "mixto")


def validar_configuracion(datos):
    from .servicio import DatosInvalidos
    tipo = datos.get("tipoActividad", "actividad")
    if tipo not in ("actividad", "competencia"):
        raise DatosInvalidos({"tipoActividad": "Elige actividad o competencia."})
    if tipo != "competencia":
        return {"tipoActividad": "actividad", "modalidad": None,
                "maxIntegrantesEquipo": None, "reglasCompetencia": "", "puntosPorPuesto": [], "categoriaGenero": ""}
    modalidad = datos.get("modalidad", "individual")
    errores = {}
    if modalidad not in ("individual", "equipos"):
        errores["modalidad"] = "Elige individual o equipos."
    reglas = datos.get("reglasCompetencia", "")
    if not isinstance(reglas, str) or len(reglas.strip()) > 2000:
        errores["reglasCompetencia"] = "El reglamento es opcional y admite hasta 2000 caracteres."
    maximo = datos.get("maxIntegrantesEquipo") if modalidad == "equipos" else None
    if modalidad == "equipos" and (type(maximo) is not int or not 2 <= maximo <= 30):
        errores["maxIntegrantesEquipo"] = "Indica entre 2 y 30 integrantes por equipo."
    puntajes = datos.get("puntosPorPuesto", [])
    if (not isinstance(puntajes, list) or len(puntajes) > 20 or
            any(type(p) is not int or not 0 <= p <= 10000 for p in puntajes)):
        errores["puntosPorPuesto"] = "Indica hasta 20 valores enteros no negativos."
    categoria = datos.get("categoriaGenero", "") or ""
    if categoria not in CATEGORIAS_GENERO:
        errores["categoriaGenero"] = "Elige varones, damas o mixto (o déjalo sin categoría)."
    if errores:
        raise DatosInvalidos(errores)
    return {"tipoActividad": tipo, "modalidad": modalidad, "categoriaGenero": categoria,
            "maxIntegrantesEquipo": maximo, "reglasCompetencia": reglas.strip(),
            "puntosPorPuesto": puntajes}
