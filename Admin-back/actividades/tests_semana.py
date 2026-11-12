"""Organizador por competencia, propuesta de sorteo con IA, gymkanas y concursos por puntos, categorías y cierre de la semana."""
import json
import time
from unittest import mock

import jwt
from django.test import RequestFactory, SimpleTestCase, override_settings

from . import cierre, competencias, puntajes, torneos, views
from .gemini_config import GeminiError
from .servicio import DatosInvalidos
from .tests_torneos import ADMIN, TODOS, BaseTorneos, ColeccionEnMemoria

SECRETO = "s" * 40
USUARIOS = {"org1": {"rol": "organizador", "activo": True}, "org2": {"rol": "organizador", "activo": True},
            "enc1": {"rol": "organizador", "activo": True}, "est1": {"rol": "usuario", "activo": True}, "adm": {"rol": "administrador", "activo": True}}


def respuesta_ia(texto):
    return lambda cuerpo, clave, modelo, timeout=None: {"candidates": [{"content": {"parts": [{"text": texto}]}}]}


class ConUsuarios(BaseTorneos):
    def setUp(self):
        super().setUp()
        for destino, valor in (("usuarios.auth.buscar_por_id", mock.Mock(side_effect=lambda c, i, p=None: USUARIOS.get(i))),
                               ("usuarios.auth.mongo.tokens_revocados", lambda: mock.Mock(find_one=mock.Mock(return_value=None)))):
            p = mock.patch(destino, new=valor)
            p.start()
            self.addCleanup(p.stop)
        o = override_settings(JWT_SECRET=SECRETO)
        o.enable()
        self.addCleanup(o.disable)
        self.competencia("Fútbol", TODOS)
        self.competencia("Vóley", ["td", "dpg"])
        p = mock.patch("actividades.mongo.actividades", side_effect=self._coleccion_actividades)
        p.start()
        self.addCleanup(p.stop)

    # Fútbol: lo organiza org1 y, por Mecánica, enc1. Vóley: lo organiza org2.
    ORGANIZA = {"act-Fútbol": {"organizadorId": "org1", "encargadosPorArea": {"Mecánica": {"id": "enc1", "nombre": "E"}}},
                "act-Vóley": {"organizadorId": "org2"}}

    def competencia(self, deporte, participantes, puntos=None):
        aid = super().competencia(deporte, participantes, puntos)
        self.actividades[aid].update(self.ORGANIZA.get(aid, {}))
        return aid

    def _coleccion_actividades(self):
        col = ColeccionEnMemoria()
        col.docs.extend(self.actividades.values())
        return col

    def llamar(self, vista, usuario, *args, metodo="get", cuerpo=None):
        token = jwt.encode({"sub": usuario, "exp": int(time.time()) + 600}, SECRETO, algorithm="HS256")
        f = RequestFactory()
        if metodo == "get":
            req = f.get("/x/", HTTP_AUTHORIZATION=f"Bearer {token}")
        else:
            req = getattr(f, metodo)("/x/", data=json.dumps(cuerpo or {}), content_type="application/json", HTTP_AUTHORIZATION=f"Bearer {token}")
        r = vista(req, *args)
        return r.status_code, json.loads(r.content)


class OrganizadorTests(ConUsuarios):
    def test_el_organizador_arma_el_torneo_de_su_competencia_y_no_el_de_otra(self):
        estado, t = self.llamar(views.organizador_torneos, "org1", metodo="post", cuerpo={"actividadId": "act-Fútbol", "sorteo": False})
        self.assertEqual(estado, 201)
        self.assertEqual(len(t["participantes"]), 5)
        estado, cuerpo = self.llamar(views.organizador_torneos, "org1", metodo="post", cuerpo={"actividadId": "act-Vóley"})
        self.assertEqual((estado, cuerpo["codigo"]), (403, "PERMISO_DENEGADO"))

    def test_registra_marcadores_y_el_ganador_avanza_pero_no_valida(self):
        t = self.crear(TODOS, deporte="Fútbol")
        estado, cuerpo = self.llamar(views.organizador_accion_partido, "org1", t["id"], "R1-P2", "resultado", metodo="post", cuerpo={"a": 2, "b": 1})
        self.assertEqual(estado, 200)
        self.assertIsNotNone(next(p for p in cuerpo["partidos"] if p["id"] == "R2-P1")["a"])
        self.assertFalse(next(p for p in cuerpo["partidos"] if p["id"] == "R1-P2")["validado"], "queda por validar")
        self.assertEqual(self.llamar(views.organizador_accion_partido, "org1", t["id"], "R1-P2", "validar", metodo="post")[0], 404)
        self.assertEqual(torneos.detalle(t["id"])["auditoria"][0]["por"], "org1")

    def test_el_encargado_por_area_tambien_organiza(self):
        t = self.crear(TODOS, deporte="Fútbol")
        self.assertEqual(self.llamar(views.organizador_accion_partido, "enc1", t["id"], "R1-P2", "resultado", metodo="post", cuerpo={"a": 0, "b": 1})[0], 200)

    def test_otro_organizador_y_un_estudiante_no_pueden(self):
        t = self.crear(TODOS, deporte="Fútbol")
        estado, cuerpo = self.llamar(views.organizador_accion_partido, "org2", t["id"], "R1-P2", "resultado", metodo="post", cuerpo={"a": 2, "b": 1})
        self.assertEqual((estado, cuerpo["codigo"]), (403, "PERMISO_DENEGADO"))
        estado, cuerpo = self.llamar(views.organizador_torneos, "est1")
        self.assertEqual((estado, cuerpo["codigo"]), (403, "SOLO_ORGANIZADOR"))

    def test_cada_organizador_ve_solo_lo_suyo_y_sin_historial(self):
        a = self.crear(TODOS, deporte="Fútbol")
        self.crear(["td", "dpg"], deporte="Vóley")
        self.assertEqual([t["id"] for t in self.llamar(views.organizador_torneos, "org1")[1]["torneos"]], [a["id"]])
        self.assertEqual([c["id"] for c in self.llamar(views.organizador_competencias, "org1")[1]["competencias"]], ["act-Fútbol"])
        self.assertNotIn("auditoria", self.llamar(views.organizador_torneo, "org1", a["id"])[1])
        self.assertEqual(self.llamar(views.organizador_torneo, "org2", a["id"])[0], 403)

    def test_las_rutas_de_administracion_le_siguen_cerradas(self):
        t = self.crear(TODOS, deporte="Fútbol")
        for vista, args, metodo in ((views.validar_partido, (t["id"], "R1-P2"), "post"), (views.detalle_torneo, (t["id"],), "delete"),
                                    (views.enviar_puestos_torneo, (t["id"],), "post"), (views.cierre_semana, (), "post"),
                                    (views.enviar_puestos_puntajes, ("act-Fútbol",), "post")):
            self.assertEqual(self.llamar(vista, "org1", *args, metodo=metodo)[1]["codigo"], "SOLO_ADMIN", vista.__name__)


class PropuestaIATests(ConUsuarios):
    def test_la_ia_propone_un_orden_con_todos_y_se_muestran_los_cruces(self):
        orden = ["dpg", "td", "quimica", "electricidad", "mecanica"]
        r = torneos.proponer_sorteo("act-Fútbol", None, llamar=respuesta_ia(json.dumps({"orden": orden, "explicacion": "Separé áreas."})))
        self.assertEqual((r["orden"], r["explicacion"]), (orden, "Separé áreas."))
        self.assertEqual(len(r["cruces"]), 4)
        self.assertEqual(sum(1 for c in r["cruces"] if c["descanso"]), 3)

    def test_una_propuesta_que_inventa_u_omite_participantes_se_descarta(self):
        for orden in (["td", "quimica"], TODOS + ["inventado"], ["td", "td", "quimica", "electricidad", "mecanica"]):
            with self.assertRaises(GeminiError, msg=str(orden)):
                torneos.proponer_sorteo("act-Fútbol", None, llamar=respuesta_ia(json.dumps({"orden": orden})))
        with self.assertRaises(GeminiError):
            torneos.proponer_sorteo("act-Fútbol", None, llamar=respuesta_ia("no es json"))

    def test_si_la_ia_no_responde_se_avisa_con_503(self):
        with mock.patch("actividades.gemini_config.llamar_gemini", side_effect=GeminiError("cuota")):
            estado, cuerpo = self.llamar(views.organizador_propuesta_ia, "org1", metodo="post", cuerpo={"actividadId": "act-Fútbol"})
        self.assertEqual((estado, cuerpo["codigo"], cuerpo["tipo"]), (503, "IA_NO_DISPONIBLE", "cuota"))

    def test_el_organizador_solo_pide_propuestas_de_sus_competencias(self):
        self.assertEqual(self.llamar(views.organizador_propuesta_ia, "org1", metodo="post", cuerpo={"actividadId": "act-Vóley"})[0], 403)


class PuntajesTests(ConUsuarios):
    PRUEBAS = [{"id": "carrera", "nombre": "Carrera"}, {"id": "acertijo", "nombre": "Acertijo"}]

    def setUp(self):
        super().setUp()
        self.actividades["act-Gymkana"] = {"_id": "act-Gymkana", "nombre": "Gymkana", "tipoActividad": "competencia", "estado": "programada",
                                           "puntosPorPuesto": [10, 7, 5, 3, 1], "organizadorId": "org1"}
        self.inscritos["act-Gymkana"] = [{"id": d, "nombre": d.upper(), "area": "", "departamento": d} for d in TODOS]
        for destino, efecto in (("actividades.resultados_competencias._actividad", lambda aid: self.actividades[aid]),
                                ("actividades.resultados_competencias.participantes_confirmados",
                                 lambda act: {i["id"]: {"_id": i["id"], "nombreEquipo": i["nombre"], "areaTecsup": "Mecánica", "areaVerificada": True}
                                              for i in self.inscritos[act["_id"]]})):
            p = mock.patch(destino, side_effect=efecto)
            p.start()
            self.addCleanup(p.stop)

    def guardar(self, puntajes, version=0, usuario=ADMIN):
        return puntajes_mod.guardar("act-Gymkana", {"pruebas": self.PRUEBAS, "puntajes": puntajes, "version": version, "modoParticipantes": "inscritos"}, usuario)

    def test_con_el_mismo_total_gana_quien_gano_mas_pruebas(self):
        h = self.guardar({"td": {"carrera": 7, "acertijo": 5}, "dpg": {"carrera": 6, "acertijo": 6}, "mecanica": {"acertijo": 9}})
        orden = [(f["participante"]["id"], f["total"], f["pruebasGanadas"], f["puesto"]) for f in h["tabla"][:3]]
        self.assertEqual(orden, [("td", 12, 1, 1), ("dpg", 12, 0, 2), ("mecanica", 9, 1, 3)])
        self.assertFalse(h["tabla"][0]["empate"])
        self.assertEqual(h["version"], 1)

    def test_un_empate_total_se_marca_para_que_el_administrador_lo_revise(self):
        h = self.guardar({"td": {"carrera": 10, "acertijo": 5}, "dpg": {"carrera": 6, "acertijo": 9}})
        self.assertEqual([f["total"] for f in h["tabla"][:2]], [15, 15])
        self.assertTrue(h["tabla"][0]["empate"] and h["tabla"][1]["empate"])

    def test_validaciones_de_la_hoja(self):
        casos = [({"pruebas": [], "puntajes": {}}, "pruebas"), ({"pruebas": [{"id": "x", "nombre": "A"}], "puntajes": {}}, "pruebas"),
                 ({"pruebas": self.PRUEBAS + [{"id": "carrera", "nombre": "Otra"}], "puntajes": {}}, "pruebas"),
                 ({"pruebas": self.PRUEBAS, "puntajes": {"intruso": {"carrera": 1}}}, "puntajes"),
                 ({"pruebas": self.PRUEBAS, "puntajes": {"td": {"carrera": -1}}}, "puntajes"),
                 ({"pruebas": self.PRUEBAS, "puntajes": {"td": {"carrera": "10"}}}, "puntajes")]
        for datos, campo in casos:
            with self.assertRaises(DatosInvalidos, msg=campo) as e:
                puntajes_mod.guardar("act-Gymkana", datos, ADMIN)
            self.assertIn(campo, e.exception.errores)

    def test_no_se_pisa_lo_que_otra_persona_guardo(self):
        self.guardar({"td": {"carrera": 1}})
        with self.assertRaises(puntajes_mod.Conflicto):
            self.guardar({"td": {"carrera": 2}}, version=0)
        self.assertEqual(self.guardar({"td": {"carrera": 2}}, version=1)["version"], 2)

    def test_torneo_y_hoja_de_puntajes_no_se_mezclan(self):
        self.guardar({"td": {"carrera": 1}})
        with self.assertRaises(DatosInvalidos):
            torneos.crear_torneo({"tipo": "arena", "actividadId": "act-Gymkana", "sorteo": False}, ADMIN)
        t = self.crear(TODOS, deporte="Fútbol")
        with self.assertRaises(puntajes_mod.Conflicto):
            puntajes_mod.guardar("act-Fútbol", {"pruebas": self.PRUEBAS, "puntajes": {}}, ADMIN)
        self.assertTrue(t["id"])

    def test_los_puestos_llegan_a_resultados_con_los_puntos_de_la_competencia_y_no_se_publican(self):
        self.guardar({"td": {"carrera": 10}, "dpg": {"carrera": 7}, "mecanica": {"carrera": 3}})
        h = puntajes_mod.enviar_puestos("act-Gymkana", ADMIN)
        self.assertTrue(h["puestosEnviados"])
        doc = self.col_resultados.docs[0]
        self.assertEqual(doc["estado"], "pendiente")
        self.assertEqual([(p["inscripcionId"], p["puesto"], p["puntos"]) for p in doc["posiciones"]], [("td", 1, 10), ("dpg", 2, 7), ("mecanica", 3, 5)])

    def test_los_puntos_de_la_gymkana_se_escriben_al_final(self):
        self.guardar({"td": {"carrera": 10}, "dpg": {"carrera": 7}})
        puntajes_mod.enviar_puestos("act-Gymkana", ADMIN, {"1": 12, "2": 8})
        self.assertEqual([p["puntos"] for p in self.col_resultados.docs[0]["posiciones"]], [12, 8])
        with self.assertRaises(DatosInvalidos):
            puntajes_mod.enviar_puestos("act-Gymkana", ADMIN, {"1": 12})

    def test_sin_puntajes_no_hay_puestos_que_enviar(self):
        with self.assertRaises(puntajes_mod.Conflicto):
            puntajes_mod.enviar_puestos("act-Gymkana", ADMIN)

    def test_el_organizador_llena_la_hoja_de_su_competencia_y_no_la_de_otra(self):
        estado, h = self.llamar(views.organizador_puntajes, "org1", "act-Gymkana", metodo="put", cuerpo={"pruebas": self.PRUEBAS, "puntajes": {"td": {"carrera": 4}}})
        self.assertEqual((estado, h["tabla"][0]["participante"]["id"]), (200, "td"))
        self.assertEqual(self.llamar(views.organizador_puntajes, "org2", "act-Gymkana")[0], 403)


puntajes_mod = puntajes


class _ColeccionConId(ColeccionEnMemoria):
    """Como MongoDB: si el documento trae su `_id`, se respeta."""
    def insert_one(self, doc):
        self.docs.append(dict(doc))
        return mock.Mock(inserted_id=doc["_id"])


class CierreTests(SimpleTestCase):
    def setUp(self):
        self.col = _ColeccionConId()
        self.avisos = []
        self.tabla = [{"area": "Mecánica", "puntos": 40, "primerosPuestos": 2}, {"area": "Tecnología Digital", "puntos": 31, "primerosPuestos": 1},
                      {"area": "Gestión y Producción", "puntos": 10, "primerosPuestos": 0}]
        for destino, valor in (("actividades.mongo.cierre", mock.Mock(return_value=self.col)),
                               ("actividades.resultados_competencias.clasificacion_general", mock.Mock(side_effect=lambda: {"areas": self.tabla})),
                               ("actividades.avisos.crear_aviso", mock.Mock(side_effect=lambda d, a: self.avisos.append(d) or {"id": "av1"}))):
            p = mock.patch(destino, new=valor)
            p.start()
            self.addCleanup(p.stop)

    def test_el_lider_sale_de_la_tabla_oficial(self):
        e = cierre.estado()
        self.assertEqual((e["cerrada"], e["lider"]["area"], e["lider"]["departamento"], e["empate"]), (False, "Mecánica", "mecanica", False))

    def test_cerrar_publica_al_ganador_con_un_aviso_y_no_se_cierra_dos_veces(self):
        e = cierre.cerrar(ADMIN)
        self.assertEqual((e["cerrada"], e["ganador"]["area"], e["ganador"]["puntos"]), (True, "Mecánica", 40))
        self.assertIn("Mecánica gana la T&C Week", self.avisos[0]["titulo"])
        self.assertIn("2.º Tecnología Digital (31 pts)", self.avisos[0]["mensaje"])
        with self.assertRaises(cierre.Conflicto):
            cierre.cerrar(ADMIN)

    def test_con_empate_en_el_primer_lugar_no_se_cierra(self):
        self.tabla[1] = {"area": "Tecnología Digital", "puntos": 40, "primerosPuestos": 2}
        with self.assertRaises(cierre.Conflicto) as e:
            cierre.cerrar(ADMIN)
        self.assertIn("empate", str(e.exception))
        self.assertEqual(self.avisos, [])

    def test_sin_puntos_no_hay_ganador(self):
        self.tabla = [{"area": a, "puntos": 0, "primerosPuestos": 0} for a in ("Mecánica", "Tecnología Digital")]
        with self.assertRaises(DatosInvalidos):
            cierre.cerrar(ADMIN)

    def test_reabrir_si_hubo_un_error(self):
        cierre.cerrar(ADMIN)
        self.assertFalse(cierre.reabrir(ADMIN)["cerrada"])
        with self.assertRaises(cierre.Conflicto):
            cierre.reabrir(ADMIN)

    def test_la_ruta_exige_administrador(self):
        for metodo in ("get", "post", "delete"):
            self.assertEqual(views.cierre_semana(getattr(RequestFactory(), metodo)("/x/")).status_code, 401)


class CategoriaTests(SimpleTestCase):
    BASE = {"tipoActividad": "competencia", "modalidad": "individual", "puntosPorPuesto": [5, 3]}

    def test_varones_damas_mixto_o_sin_categoria(self):
        for categoria in ("varones", "damas", "mixto", ""):
            self.assertEqual(competencias.validar_configuracion({**self.BASE, "categoriaGenero": categoria})["categoriaGenero"], categoria)
        with self.assertRaises(DatosInvalidos) as e:
            competencias.validar_configuracion({**self.BASE, "categoriaGenero": "otra"})
        self.assertIn("categoriaGenero", e.exception.errores)
        self.assertEqual(competencias.validar_configuracion({"tipoActividad": "actividad"})["categoriaGenero"], "")
