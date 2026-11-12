"""T&C ARENA y Karadúo: pruebas del fixture, los resultados, los horarios y la seguridad."""
import copy
import itertools
from unittest import mock

from django.http import JsonResponse
from django.test import RequestFactory, SimpleTestCase

from usuarios import permisos
from . import departamentos, torneos, views
from .servicio import DatosInvalidos

ADMIN = "admin-1"
TODOS = ["quimica", "mecanica", "td", "electricidad", "dpg"]


class _Resultado:
    def __init__(self, **kw):
        self.__dict__.update(kw)


class ColeccionEnMemoria:
    """Colección mínima (como MongoDB) para probar sin base de datos ni librerías nuevas."""

    def __init__(self):
        self.docs, self._n = [], itertools.count(1)

    @staticmethod
    def _coincide(doc, filtro):
        for clave, esperado in filtro.items():
            valor = doc.get(clave)
            if isinstance(esperado, dict):
                if "$in" in esperado and valor not in esperado["$in"]:
                    return False
                if "$ne" in esperado and valor == esperado["$ne"]:
                    return False
            elif valor != esperado:
                return False
        return True

    def find(self, filtro=None):
        return [copy.deepcopy(d) for d in self.docs if self._coincide(d, filtro or {})]

    def find_one(self, filtro):
        return next(iter(self.find(filtro)), None)

    def insert_one(self, doc):
        doc = copy.deepcopy(doc)
        doc["_id"] = f"t{next(self._n)}"
        self.docs.append(doc)
        return _Resultado(inserted_id=doc["_id"])

    def delete_one(self, filtro):
        for i, d in enumerate(self.docs):
            if self._coincide(d, filtro):
                del self.docs[i]
                return _Resultado(deleted_count=1)
        return _Resultado(deleted_count=0)

    def replace_one(self, filtro, nuevo):
        for i, d in enumerate(self.docs):
            if self._coincide(d, filtro):
                self.docs[i] = copy.deepcopy(nuevo)
                return _Resultado(matched_count=1)
        return _Resultado(matched_count=0)


class ColeccionResultados(ColeccionEnMemoria):
    def create_index(self, *a, **k):
        return None

    def find_one_and_update(self, filtro, cambios, upsert=False, return_document=None):
        for i, d in enumerate(self.docs):
            if self._coincide(d, filtro):
                self.docs[i] = {**d, **copy.deepcopy(cambios.get("$set", {}))}
                return copy.deepcopy(self.docs[i])
        if not upsert:
            return None
        nuevo = {**copy.deepcopy(cambios.get("$setOnInsert", {})), **copy.deepcopy(cambios.get("$set", {}))}
        self.insert_one(nuevo)
        return copy.deepcopy(self.docs[-1])


class BaseTorneos(SimpleTestCase):
    def setUp(self):
        self.col = ColeccionEnMemoria()
        self.col_eliminados, self.col_resultados, self.col_puntajes = ColeccionEnMemoria(), ColeccionResultados(), ColeccionEnMemoria()
        self.actividades, self.inscritos = {}, {}
        for objetivo, efecto in (("actividades.torneos._cargar_actividad", lambda aid: copy.deepcopy(self.actividades.get(aid))),
                                 ("actividades.torneos._participantes_de_actividad", lambda act: copy.deepcopy(self.inscritos.get(str(act["_id"]), [])))):
            parche = mock.patch(objetivo, side_effect=efecto)
            parche.start()
            self.addCleanup(parche.stop)
        for nombre, coleccion in (("torneos", self.col), ("torneos_eliminados", self.col_eliminados), ("resultados_competencias", self.col_resultados), ("puntajes", self.col_puntajes)):
            parche = mock.patch(f"actividades.mongo.{nombre}", return_value=coleccion)
            parche.start()
            self.addCleanup(parche.stop)

    def competencia(self, deporte, participantes, puntos=None):
        """Registra una competencia con sus inscritos confirmados (aquí, uno por departamento)."""
        aid = f"act-{deporte}"
        self.actividades[aid] = {"_id": aid, "nombre": deporte, "tipoActividad": "competencia", "estado": "programada", "puntosPorPuesto": puntos or []}
        self.inscritos[aid] = [{"id": d, "nombre": departamentos.nombre(d) or d, "area": (departamentos.obtener(d) or {}).get("areaTecsup", ""),
                                "departamento": d if departamentos.obtener(d) else None} for d in participantes]
        return aid

    def crear(self, participantes=None, tipo="arena", deporte="Fútbol", **extra):
        participantes = participantes or TODOS
        datos = {"tipo": tipo, "actividadId": self.competencia(deporte, participantes), "participantes": participantes, "sorteo": False,
                 "modoParticipantes": "inscritos"}
        datos.update(extra)
        return torneos.crear_torneo(datos, ADMIN)

    def partido(self, t, partido_id):
        return next(p for p in t["partidos"] if p["id"] == partido_id)


class LlavesTests(SimpleTestCase):
    def test_sin_duplicados_ni_departamentos_inventados(self):
        for n in range(2, 9):
            participantes = TODOS[:n] if n <= 5 else TODOS + [f"x{i}" for i in range(n - 5)]
            partidos = torneos.generar_llaves(participantes)
            tam = 1 << (n - 1).bit_length()
            extra = 1 if any(p.get("tercerPuesto") for p in partidos) else 0  # partido por el tercer puesto
            self.assertEqual(len(partidos), tam - 1 + extra, f"n={n}")
            ronda1 = [p for p in partidos if p["ronda"] == 1]
            vistos = [x for p in ronda1 for x in (p["a"], p["b"]) if x]
            self.assertCountEqual(vistos, participantes, f"n={n}: todos aparecen una sola vez")
            self.assertEqual(sum(p["descanso"] for p in ronda1), tam - n, f"n={n}")
            self.assertTrue(all(p["a"] or p["b"] for p in ronda1), "nunca descanso contra descanso")
            parejas = [frozenset((p["a"], p["b"])) for p in ronda1 if p["a"] and p["b"]]
            self.assertEqual(len(parejas), len(set(parejas)), "sin enfrentamientos repetidos")

    def test_cinco_departamentos_tres_descansan(self):
        partidos = torneos.generar_llaves(TODOS)
        ronda1 = [p for p in partidos if p["ronda"] == 1]
        self.assertEqual(sum(1 for p in ronda1 if not p["descanso"]), 1)
        self.assertEqual(sum(p["descanso"] for p in ronda1), 3)
        semifinales = [p for p in partidos if p["nombreRonda"] == "Semifinales"]
        self.assertEqual(len(semifinales), 2)
        # Los 3 que descansaron ya están en semifinales sin haber jugado.
        en_semis = [x for p in semifinales for x in (p["a"], p["b"]) if x]
        self.assertEqual(len(en_semis), 3)

    def test_los_dos_primeros_solo_se_cruzan_en_la_final(self):
        partidos = torneos.generar_llaves(TODOS[:4] + ["x1", "x2", "x3", "x4"])
        mitad = {p["id"]: ("alta" if p["posicion"] <= 2 else "baja") for p in partidos if p["ronda"] == 1}
        donde = lambda d: next(mitad[p["id"]] for p in partidos if p["ronda"] == 1 and d in (p["a"], p["b"]))
        self.assertNotEqual(donde("quimica"), donde("mecanica"))

    def test_nombres_de_ronda(self):
        self.assertEqual([p["nombreRonda"] for p in torneos.generar_llaves(["a", "b"])], ["Final"])
        nombres = {p["nombreRonda"] for p in torneos.generar_llaves(list("abcdefgh"))}
        self.assertEqual(nombres, {"Cuartos de final", "Semifinales", "Final", "Tercer puesto"})

    def test_menos_de_dos_no_se_puede(self):
        with self.assertRaises(DatosInvalidos):
            torneos.generar_llaves(["quimica"])


class CrearTorneoTests(BaseTorneos):
    def test_validaciones(self):
        aid = self.competencia("Fútbol", ["td", "mecanica"])
        casos = [({"tipo": "otro", "actividadId": aid}, "tipo"), ({"tipo": "arena"}, "actividadId"), ({"tipo": "arena", "actividadId": "no-existe"}, "actividadId"),
                 ({"tipo": "arena", "actividadId": aid, "participantes": ["td", "td"]}, "participantes"),
                 ({"tipo": "arena", "actividadId": aid, "participantes": ["td", "no-inscrito"]}, "participantes"),
                 ({"tipo": "arena", "actividadId": aid, "participantes": ["td"]}, "participantes"),
                 ({"tipo": "arena", "actividadId": aid, "modoResultado": "ganador"}, "modoResultado"),
                 ({"tipo": "arena", "actividadId": aid, "modoResultado": "sets", "setsParaGanar": 9}, "setsParaGanar")]
        for datos, campo in casos:
            with self.assertRaises(DatosInvalidos, msg=campo) as e:
                torneos.crear_torneo(datos, ADMIN)
            self.assertIn(campo, e.exception.errores)

    def test_sin_elegir_participantes_entran_todos_los_confirmados(self):
        aid = self.competencia("Vóley", ["td", "mecanica", "dpg"])
        t = torneos.crear_torneo({"tipo": "arena", "actividadId": aid, "sorteo": False, "modoParticipantes": "inscritos"}, ADMIN)
        self.assertEqual({p["id"] for p in t["participantes"]}, {"td", "mecanica", "dpg"})
        self.assertEqual((t["deporte"], t["actividadId"]), ("Vóley", aid), "el nombre sale de la competencia")

    def test_una_competencia_no_admite_dos_torneos_y_se_puede_recrear_si_se_elimina(self):
        t = self.crear(deporte="Fútbol")
        with self.assertRaises(DatosInvalidos) as e:
            torneos.crear_torneo({"tipo": "arena", "actividadId": "act-Fútbol", "sorteo": False}, ADMIN)
        self.assertIn("actividadId", e.exception.errores)
        self.crear(deporte="Vóley")
        torneos.eliminar_torneo(t["id"], ADMIN)
        torneos.crear_torneo({"tipo": "arena", "actividadId": "act-Fútbol", "sorteo": False}, ADMIN)

    def test_el_sorteo_es_reproducible_con_la_semilla(self):
        a = torneos.crear_torneo({"tipo": "arena", "actividadId": self.competencia("A1", TODOS), "sorteo": True}, ADMIN, semilla=7)
        b = torneos.crear_torneo({"tipo": "arena", "actividadId": self.competencia("B1", TODOS), "sorteo": True}, ADMIN, semilla=7)
        self.assertEqual([p["a"] and p["a"]["id"] for p in a["partidos"]], [p["a"] and p["a"]["id"] for p in b["partidos"]])

    def test_karaduo_solo_registra_ganador(self):
        t = self.crear(tipo="karaduo", deporte="Karadúo")
        self.assertEqual((t["modoResultado"], t["tipoNombre"]), ("ganador", "Karadúo"))

    def test_los_participantes_llevan_nombre_area_y_departamento(self):
        t = self.crear(["td", "mecanica"])
        self.assertEqual(self.partido(t, "R1-P1")["a"], {"id": "td", "nombre": "TD", "area": "Tecnología Digital", "departamento": "td"})


class ResultadosTests(BaseTorneos):
    def jugar_tres(self):
        """td descansa; mecanica vs dpg; luego final td vs ganador."""
        t = self.crear(["td", "mecanica", "dpg"])
        self.assertTrue(self.partido(t, "R1-P1")["descanso"])
        return t

    def test_el_ganador_avanza_hasta_el_campeon(self):
        t = self.jugar_tres()
        t = torneos.registrar_resultado(t["id"], "R1-P2", {"a": 2, "b": 1}, ADMIN)
        final = self.partido(t, "R2-P1")
        self.assertEqual((final["a"]["id"], final["b"]["id"]), ("td", "mecanica"))
        torneos.programar(t["id"], "R2-P1", {"fecha": "2026-11-18", "hora": "16:00", "lugar": "Cancha 1"}, ADMIN)
        t = torneos.registrar_resultado(t["id"], "R2-P1", {"a": 0, "b": 3}, ADMIN)
        self.assertEqual((t["estado"], t["campeon"]["id"], t["subcampeon"]["id"]), ("finalizado", "mecanica", "td"))

    def test_no_hay_empates_en_eliminacion_directa(self):
        t = self.jugar_tres()
        with self.assertRaises(DatosInvalidos) as e:
            torneos.registrar_resultado(t["id"], "R1-P2", {"a": 1, "b": 1}, ADMIN)
        self.assertIn("ganadorPenales", e.exception.errores)
        t = torneos.registrar_resultado(t["id"], "R1-P2", {"a": 1, "b": 1, "ganadorPenales": "b"}, ADMIN)
        self.assertEqual(self.partido(t, "R1-P2")["ganador"], "dpg")

    def test_marcador_invalido(self):
        t = self.jugar_tres()
        for datos in ({"a": -1, "b": 0}, {"a": "x", "b": 0}, {"a": 1}, {"a": 1000, "b": 0}, {"a": True, "b": 0}):
            with self.assertRaises(DatosInvalidos, msg=str(datos)):
                torneos.registrar_resultado(t["id"], "R1-P2", datos, ADMIN)

    def test_sets(self):
        t = self.crear(["td", "mecanica"], deporte="Vóley", modoResultado="sets", setsParaGanar=2)
        for malo in ({"setsA": 1, "setsB": 1}, {"setsA": 3, "setsB": 0}, {"setsA": 0, "setsB": 0}):
            with self.assertRaises(DatosInvalidos, msg=str(malo)):
                torneos.registrar_resultado(t["id"], "R1-P1", malo, ADMIN)
        t = torneos.registrar_resultado(t["id"], "R1-P1", {"setsA": 1, "setsB": 2}, ADMIN)
        self.assertEqual(t["campeon"]["id"], "mecanica")

    def test_karaduo_ganador(self):
        t = self.crear(["td", "mecanica"], tipo="karaduo")
        with self.assertRaises(DatosInvalidos):
            torneos.registrar_resultado(t["id"], "R1-P1", {"ganador": "mecanica"}, ADMIN)  # debe ser a o b
        t = torneos.registrar_resultado(t["id"], "R1-P1", {"ganador": "a", "observaciones": "Mejor presentación"}, ADMIN)
        self.assertEqual(t["campeon"]["id"], "td")

    def test_no_se_juega_un_descanso_ni_un_partido_sin_rivales(self):
        t = self.jugar_tres()
        with self.assertRaises(torneos.Conflicto):
            torneos.registrar_resultado(t["id"], "R1-P1", {"a": 1, "b": 0}, ADMIN)  # descanso
        with self.assertRaises(torneos.Conflicto):
            torneos.registrar_resultado(t["id"], "R2-P1", {"a": 1, "b": 0}, ADMIN)  # final sin rival todavía

    def test_correccion_cambia_el_avance_mientras_no_se_juegue_lo_siguiente(self):
        t = self.jugar_tres()
        t = torneos.registrar_resultado(t["id"], "R1-P2", {"a": 2, "b": 1}, ADMIN)
        torneos.programar(t["id"], "R2-P1", {"fecha": "2026-11-18", "hora": "16:00", "lugar": "Cancha 1"}, ADMIN)
        t = torneos.registrar_resultado(t["id"], "R1-P2", {"a": 0, "b": 4}, ADMIN)  # corrección
        final = self.partido(t, "R2-P1")
        self.assertEqual(final["b"]["id"], "dpg")
        self.assertEqual((final["estado"], final["fecha"]), ("pendiente", ""), "el rival cambió: se pierde el horario")
        torneos.programar(t["id"], "R2-P1", {"fecha": "2026-11-18", "hora": "16:00", "lugar": "Cancha 1"}, ADMIN)
        torneos.registrar_resultado(t["id"], "R2-P1", {"a": 1, "b": 0}, ADMIN)
        with self.assertRaises(torneos.Conflicto):  # ya se jugó la final: no se toca lo anterior
            torneos.registrar_resultado(t["id"], "R1-P2", {"a": 5, "b": 0}, ADMIN)

    def test_validacion_y_correccion_la_invalida(self):
        t = self.jugar_tres()
        with self.assertRaises(torneos.Conflicto):
            torneos.validar_resultado(t["id"], "R1-P2", ADMIN)  # aún sin resultado
        t = torneos.registrar_resultado(t["id"], "R1-P2", {"a": 2, "b": 1}, ADMIN)
        self.assertEqual(t["pendientesDeValidar"], 1)
        t = torneos.validar_resultado(t["id"], "R1-P2", ADMIN)
        self.assertTrue(self.partido(t, "R1-P2")["validado"])
        with self.assertRaises(torneos.Conflicto):
            torneos.validar_resultado(t["id"], "R1-P2", ADMIN)
        t = torneos.registrar_resultado(t["id"], "R1-P2", {"a": 3, "b": 1}, ADMIN)
        self.assertFalse(self.partido(t, "R1-P2")["validado"], "corregir obliga a validar de nuevo")

    def test_la_auditoria_guarda_quien_y_que_cambio(self):
        t = self.jugar_tres()
        torneos.registrar_resultado(t["id"], "R1-P2", {"a": 2, "b": 1}, "organizador-9")
        t = torneos.registrar_resultado(t["id"], "R1-P2", {"a": 0, "b": 1}, "admin-2")
        detalle = torneos.detalle(t["id"])["auditoria"]
        self.assertEqual([e["accion"] for e in detalle], ["corregir_resultado", "resultado", "crear"])
        self.assertEqual((detalle[0]["por"], detalle[1]["por"], detalle[2]["por"]), ("admin-2", "organizador-9", ADMIN))
        self.assertEqual(detalle[0]["detalle"]["antes"]["ganador"], "mecanica")
        self.assertEqual(detalle[0]["detalle"]["despues"]["ganador"], "dpg")

    def test_dos_personas_a_la_vez_no_se_pisan(self):
        t = self.jugar_tres()
        copia_vieja = torneos._cargar(t["id"])
        torneos.registrar_resultado(t["id"], "R1-P2", {"a": 2, "b": 1}, ADMIN)
        with self.assertRaises(torneos.Conflicto):
            torneos._guardar(copia_vieja)  # tenía la versión anterior


class HorariosTests(BaseTorneos):
    def setUp(self):
        super().setUp()
        self.futbol = self.crear(["td", "mecanica"], deporte="Fútbol")
        self.voley = self.crear(["td", "dpg"], deporte="Vóley")
        torneos.programar(self.futbol["id"], "R1-P1", {"fecha": "2026-11-17", "hora": "15:00", "lugar": "Cancha 1"}, ADMIN)

    def prog(self, hora, lugar, fecha="2026-11-17"):
        return torneos.programar(self.voley["id"], "R1-P1", {"fecha": fecha, "hora": hora, "lugar": lugar}, ADMIN)

    def test_un_departamento_no_juega_dos_partidos_a_la_vez(self):
        for hora in ("15:00", "15:30", "14:30"):
            with self.assertRaises(DatosInvalidos, msg=hora) as e:
                self.prog(hora, "Cancha 2")
            self.assertIn("TD", e.exception.errores["hora"])
        self.prog("16:00", "Cancha 2")  # una hora después: ya no se cruza
        self.prog("15:00", "Cancha 2", fecha="2026-11-18")  # otro día

    def test_una_cancha_no_se_usa_dos_veces_a_la_vez(self):
        otro = self.crear(["quimica", "electricidad"], deporte="Básquet")
        with self.assertRaises(DatosInvalidos) as e:
            torneos.programar(otro["id"], "R1-P1", {"fecha": "2026-11-17", "hora": "15:20", "lugar": " cancha 1 "}, ADMIN)
        self.assertIn("ocupada", e.exception.errores["hora"])
        torneos.programar(otro["id"], "R1-P1", {"fecha": "2026-11-17", "hora": "15:20", "lugar": "Cancha 3"}, ADMIN)

    def test_un_partido_suspendido_libera_el_horario(self):
        torneos.cambiar_estado(self.futbol["id"], "R1-P1", "suspendido", ADMIN)
        self.prog("15:00", "Cancha 1")

    def test_reprogramar_el_mismo_partido_no_choca_consigo_mismo(self):
        torneos.programar(self.futbol["id"], "R1-P1", {"fecha": "2026-11-17", "hora": "15:10", "lugar": "Cancha 1"}, ADMIN)

    def test_formatos(self):
        for datos in ({"fecha": "17/11/2026", "hora": "15:00", "lugar": "C1"}, {"fecha": "2026-11-17", "hora": "25:00", "lugar": "C1"},
                      {"fecha": "2026-11-17", "hora": "15:00", "lugar": ""}):
            with self.assertRaises(DatosInvalidos):
                torneos.programar(self.voley["id"], "R1-P1", datos, ADMIN)

    def test_estados(self):
        t = self.futbol
        with self.assertRaises(torneos.Conflicto):  # no se finaliza a mano
            torneos.cambiar_estado(t["id"], "R1-P1", "finalizado", ADMIN)
        torneos.cambiar_estado(t["id"], "R1-P1", "en_curso", ADMIN)
        with self.assertRaises(torneos.Conflicto):  # en curso no se reprograma
            torneos.programar(t["id"], "R1-P1", {"fecha": "2026-11-18", "hora": "10:00", "lugar": "C1"}, ADMIN)
        torneos.cambiar_estado(t["id"], "R1-P1", "suspendido", ADMIN)
        t = torneos.cambiar_estado(t["id"], "R1-P1", "programado", ADMIN)
        self.assertEqual(self.partido(t, "R1-P1")["estado"], "programado")
        with self.assertRaises(DatosInvalidos):
            torneos.cambiar_estado(t["id"], "R1-P1", "volando", ADMIN)


class SeguridadTests(SimpleTestCase):
    VISTAS = ["lista_departamentos", "torneos", "detalle_torneo", "enviar_puestos_torneo", "participantes_torneo",
              "programar_partido", "estado_partido", "resultado_partido", "validar_partido"]

    def test_ninguna_ruta_responde_sin_token(self):
        fabrica = RequestFactory()
        for nombre in self.VISTAS:
            vista = getattr(views, nombre)
            for metodo in ("get", "post"):
                respuesta = vista(getattr(fabrica, metodo)("/x/"), *(["t1", "R1-P1"][: vista.__code__.co_argcount - 1] if False else self._args(vista)))
                self.assertIn(respuesta.status_code, (401, 405), f"{nombre} {metodo}")

    @staticmethod
    def _args(vista):
        inner = vista
        while hasattr(inner, "__wrapped__"):
            inner = inner.__wrapped__
        return ["t1", "R1-P1"][: max(0, inner.__code__.co_argcount - 1)]

    def test_los_permisos_nuevos_existen_y_el_admin_los_tiene_por_defecto(self):
        self.assertIn("torneos.gestionar", permisos.CATALOGO)
        self.assertIn("torneos.validar", permisos.CATALOGO)
        sin_registro = mock.Mock(find_one=mock.Mock(return_value=None))
        with mock.patch("usuarios.permisos.mongo.permisos_usuarios", return_value=sin_registro):
            self.assertTrue(permisos.permisos_de("a1", "administrador")["torneos.gestionar"])
            self.assertFalse(permisos.permisos_de("u1", "usuario")["torneos.gestionar"])

    def test_sin_el_permiso_el_backend_rechaza_aunque_el_boton_se_vea(self):
        quitado = mock.Mock(find_one=mock.Mock(return_value={"permisos": {"torneos.validar": False}}))
        vista = permisos.exigir_permiso("torneos.validar")(lambda request: JsonResponse({"ok": True}))
        peticion = RequestFactory().post("/x/")
        peticion.admin_id = "a1"
        with mock.patch("usuarios.permisos.mongo.permisos_usuarios", return_value=quitado):
            self.assertEqual(vista(peticion).status_code, 403)

    def test_departamentos_y_colores_definidos(self):
        colores = {d["id"]: d["color"] for d in departamentos.DEPARTAMENTOS}
        self.assertEqual(colores, {"quimica": "verde", "mecanica": "amarillo", "td": "rojo", "electricidad": "azul", "dpg": "morado"})
        self.assertTrue(departamentos.obtener("dpg")["denominacionPorConfirmar"])
        self.assertEqual(departamentos.por_area_tecsup("Tecnología Digital")["id"], "td")
