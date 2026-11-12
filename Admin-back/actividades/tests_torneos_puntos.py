"""T&C ARENA: tercer puesto, puestos finales hacia el flujo oficial de resultados y eliminar un torneo."""
from unittest import mock

from django.test import RequestFactory, SimpleTestCase

from . import reporte_evento, resultados_competencias, torneos, views
from .servicio import DatosInvalidos
from .tests_torneos import ADMIN, TODOS, BaseTorneos

PUNTOS = [10, 7, 5, 3, 1]  # los que el administrador configura en la competencia


class TercerPuestoTests(BaseTorneos):
    def test_cuatro_y_cinco_participantes_tienen_partido_por_el_tercer_puesto(self):
        for n in (4, 5):
            t = self.crear(TODOS[:n], deporte=f"Deporte{n}")
            self.assertEqual([p["nombreRonda"] for p in t["partidos"] if p["tercerPuesto"]], ["Tercer puesto"], f"n={n}")

    def test_sin_tercer_puesto_con_tres_o_si_se_desactiva(self):
        self.assertFalse(self.crear(["td", "mecanica", "dpg"], deporte="Tres")["terceroPuesto"])
        self.assertFalse(self.crear(TODOS[:4], deporte="Cuatro", tercerPuesto=False)["terceroPuesto"])

    def test_los_perdedores_de_semifinales_van_al_tercer_puesto_y_el_campeon_no_cambia(self):
        t = self.crear(TODOS[:4])
        s1, s2 = self.partido(t, "R1-P1"), self.partido(t, "R1-P2")
        torneos.registrar_resultado(t["id"], "R1-P1", {"a": 2, "b": 0}, ADMIN)
        t = torneos.registrar_resultado(t["id"], "R1-P2", {"a": 0, "b": 1}, ADMIN)
        tercero = self.partido(t, "R2-P2")
        self.assertEqual((tercero["a"]["id"], tercero["b"]["id"]), (s1["b"]["id"], s2["a"]["id"]))
        t = torneos.registrar_resultado(t["id"], "R2-P1", {"a": 1, "b": 0}, ADMIN)
        campeon = t["campeon"]["id"]
        t = torneos.registrar_resultado(t["id"], "R2-P2", {"a": 3, "b": 1}, ADMIN)
        self.assertEqual(t["campeon"]["id"], campeon)


class PuestosAlFlujoOficialTests(BaseTorneos):
    """Los puestos del torneo se preparan en «Resultados» de la competencia; solo el administrador publica."""

    def setUp(self):
        super().setUp()
        registros = {d: {"_id": d, "nombreEquipo": f"Equipo {d}", "areaTecsup": n["area"], "areaVerificada": True}
                     for d, n in ((x["id"], x) for x in self.inscritos.get("act-Fútbol", []))}
        self.parches = [mock.patch("actividades.resultados_competencias._actividad", side_effect=lambda aid: self.actividades[aid]),
                        mock.patch("actividades.resultados_competencias.participantes_confirmados",
                                   side_effect=lambda act: {i["id"]: {"_id": i["id"], "nombreEquipo": f"Equipo {i['id']}", "areaTecsup": i["area"], "areaVerificada": True}
                                                            for i in self.inscritos[act["_id"]]})]
        for p in self.parches:
            p.start()
            self.addCleanup(p.stop)

    def jugar_cinco(self):
        """td campeón, mecánica 2.º, electricidad 3.º, química 4.º, dpg 5.º."""
        aid = self.competencia("Fútbol", ["td", "quimica", "mecanica", "electricidad", "dpg"], puntos=PUNTOS)
        t = torneos.crear_torneo({"tipo": "arena", "actividadId": aid, "participantes": ["td", "quimica", "mecanica", "electricidad", "dpg"], "sorteo": False,
                                  "modoParticipantes": "inscritos"}, ADMIN)
        for pid, m in (("R1-P2", (1, 0)), ("R2-P1", (2, 0)), ("R2-P2", (0, 1)), ("R3-P1", (2, 1)), ("R3-P2", (1, 0))):
            t = torneos.registrar_resultado(t["id"], pid, dict(zip("ab", m)), ADMIN)
        return t

    def validar_todo(self, t):
        for pid in ("R1-P2", "R2-P1", "R2-P2", "R3-P1", "R3-P2"):
            torneos.validar_resultado(t["id"], pid, ADMIN)

    def test_sin_la_final_validada_no_se_pueden_enviar_puestos(self):
        t = self.jugar_cinco()
        with self.assertRaises(torneos.Conflicto):
            torneos.enviar_puestos(t["id"], ADMIN)
        self.assertEqual(self.col_resultados.docs, [])

    def test_los_puestos_llegan_como_propuesta_pendiente_con_los_puntos_de_la_competencia(self):
        t = self.jugar_cinco()
        self.validar_todo(t)
        self.assertEqual([(p["puesto"], p["participante"]["id"]) for p in torneos.detalle(t["id"])["posiciones"]],
                         [(1, "td"), (2, "mecanica"), (3, "electricidad"), (4, "quimica"), (5, "dpg")])
        r = torneos.enviar_puestos(t["id"], ADMIN)
        self.assertTrue(r["puestosEnviados"])
        doc = self.col_resultados.docs[0]
        self.assertEqual(doc["estado"], "pendiente", "nunca se publica solo: lo publica el administrador")
        self.assertEqual([(p["puesto"], p["inscripcionId"], p["puntos"]) for p in doc["posiciones"]],
                         [(1, "td", 10), (2, "mecanica", 7), (3, "electricidad", 5), (4, "quimica", 3), (5, "dpg", 1)])
        self.assertEqual(doc["posiciones"][0]["areaTecsup"], "Tecnología Digital")

    def test_enviar_de_nuevo_actualiza_la_misma_propuesta(self):
        t = self.jugar_cinco()
        self.validar_todo(t)
        torneos.enviar_puestos(t["id"], ADMIN)
        torneos.enviar_puestos(t["id"], ADMIN)
        self.assertEqual(len(self.col_resultados.docs), 1)

    def test_si_ya_se_publicaron_no_se_pisan(self):
        t = self.jugar_cinco()
        self.validar_todo(t)
        torneos.enviar_puestos(t["id"], ADMIN)
        self.col_resultados.docs[0]["estado"] = "publicado"
        with self.assertRaises(DatosInvalidos):
            torneos.enviar_puestos(t["id"], ADMIN)

    def test_con_la_final_validada_pero_sin_el_resto_van_solo_los_dos_primeros(self):
        t = self.jugar_cinco()
        torneos.validar_resultado(t["id"], "R3-P1", ADMIN)
        torneos.enviar_puestos(t["id"], ADMIN)
        self.assertEqual([p["puesto"] for p in self.col_resultados.docs[0]["posiciones"]], [1, 2])


class ParticipantesTests(SimpleTestCase):
    def test_funcion_real_de_participantes(self):
        confirmados = {"e1": {"_id": "e1", "nombreEquipo": "Los Rayos", "areaTecsup": "Mecánica"},
                       "a2": {"_id": "a2", "nombreEstudiante": "Ana Torres", "areaTecsup": "Tecnología Digital"},
                       "x3": {"_id": "x3", "nombreEstudiante": "Sin área", "areaTecsup": ""}}
        with mock.patch("actividades.resultados_competencias.participantes_confirmados", return_value=confirmados):
            lista = torneos._participantes_de_actividad({"_id": "a"})
        self.assertEqual([(p["nombre"], p["departamento"]) for p in lista], [("Ana Torres", "td"), ("Los Rayos", "mecanica"), ("Sin área", None)])


class EliminarTests(BaseTorneos):
    def test_eliminar_borra_el_torneo_y_deja_una_copia(self):
        t = self.crear(["td", "mecanica"])
        r = torneos.eliminar_torneo(t["id"], "admin-2")
        self.assertEqual((r["eliminado"], r["deporte"]), (True, "Fútbol"))
        self.assertEqual(self.col.docs, [])
        copia = self.col_eliminados.docs[0]
        self.assertEqual((copia["torneoId"], copia["eliminadoPor"]), (t["id"], "admin-2"))
        self.assertEqual(torneos.listar("arena"), [])

    def test_eliminar_uno_no_toca_los_demas(self):
        a, b = self.crear(["td", "mecanica"], deporte="Fútbol"), self.crear(["td", "dpg"], deporte="Vóley")
        torneos.eliminar_torneo(a["id"], ADMIN)
        self.assertEqual([t["id"] for t in torneos.listar("arena")], [b["id"]])

    def test_eliminar_un_torneo_que_no_existe_avisa(self):
        with self.assertRaises(torneos.NoEncontrado):
            torneos.eliminar_torneo("no-existe", ADMIN)

    def test_las_rutas_nuevas_exigen_administrador(self):
        fabrica = RequestFactory()
        self.assertEqual(views.detalle_torneo(fabrica.delete("/x/"), "t1").status_code, 401)
        self.assertEqual(views.enviar_puestos_torneo(fabrica.post("/x/"), "t1").status_code, 401)
        self.assertEqual(views.participantes_torneo(fabrica.get("/x/")).status_code, 401)


class MotivoSinParticipantesTests(SimpleTestCase):
    """El formulario del torneo explica por qué una competencia no tiene participantes confirmados."""

    def motivo(self, registros, confirmados):
        col = mock.Mock(find=mock.Mock(return_value=registros))
        with mock.patch("actividades.mongo.inscripciones", return_value=col):
            return torneos._motivo_sin_participantes({"_id": "a1"}, confirmados)

    def test_nadie_inscrito(self):
        self.assertIn("Nadie se ha inscrito", self.motivo([], set())["motivo"])

    def test_equipos_a_los_que_les_falta_confirmar(self):
        r = self.motivo([{"_id": "e1", "nombreEquipo": "Los Rayos", "rolEquipo": "capitan"}, {"_id": "m1", "rolEquipo": "integrante"},
                         {"_id": "e2", "nombreEquipo": "Bits", "rolEquipo": "capitan"}], set())
        self.assertEqual(r["equiposPendientes"], ["Los Rayos", "Bits"])
        self.assertIn("les falta que todos sus integrantes confirmen", r["motivo"])

    def test_un_solo_confirmado(self):
        self.assertIn("al menos dos", self.motivo([{"_id": "e1", "nombreEstudiante": "Ana"}], {"e1"})["motivo"])

    def test_con_dos_o_mas_confirmados_no_hay_motivo(self):
        self.assertIsNone(self.motivo([{"_id": "e1"}, {"_id": "e2"}], {"e1", "e2"})["motivo"])


class PuntosAlFinalTests(PuestosAlFlujoOficialTests):
    """Los puntos se escriben a mano cuando termina el torneo y se publican como resultado oficial."""

    def test_los_puntos_escritos_a_mano_reemplazan_a_los_de_la_competencia(self):
        t = self.jugar_cinco()
        self.validar_todo(t)
        torneos.enviar_puestos(t["id"], ADMIN, {"1": 20, "2": 15, "3": 10, "4": 5, "5": 2})
        doc = self.col_resultados.docs[0]
        self.assertEqual([p["puntos"] for p in doc["posiciones"]], [20, 15, 10, 5, 2])
        self.assertEqual(doc["estado"], "pendiente")

    def test_falta_un_puesto_o_un_valor_invalido(self):
        t = self.jugar_cinco()
        self.validar_todo(t)
        for puntos in ({"1": 20, "2": 15}, {"1": 20, "2": 15, "3": 10, "4": 5, "5": -1}, {"1": "20", "2": 1, "3": 1, "4": 1, "5": 1}, [20, 15]):
            with self.assertRaises(DatosInvalidos, msg=str(puntos)):
                torneos.enviar_puestos(t["id"], ADMIN, puntos)
        self.assertEqual(self.col_resultados.docs, [], "no se guarda nada a medias")

    def test_publicar_deja_el_resultado_oficial_para_la_tabla_acumulada(self):
        t = self.jugar_cinco()
        self.validar_todo(t)
        with mock.patch("actividades.mongo.inscripciones", return_value=mock.Mock(find=mock.Mock(return_value=[]))):
            r = torneos.enviar_puestos(t["id"], ADMIN, {"1": 20, "2": 15, "3": 10, "4": 5, "5": 2}, publicar=True)
        self.assertTrue(r["resultadosPublicados"])
        doc = self.col_resultados.docs[0]
        self.assertEqual((doc["estado"], doc["posiciones"][0]["puntos"]), ("publicado", 20))
        with self.assertRaises(DatosInvalidos):  # una vez publicado no se pisa
            torneos.enviar_puestos(t["id"], ADMIN, {"1": 1, "2": 1, "3": 1, "4": 1, "5": 1})


class DepartamentosObligatoriosTests(BaseTorneos):
    """En la T&C Week los cinco departamentos participan siempre: no hace falta que nadie se inscriba."""
    ORDEN = ["td", "quimica", "mecanica", "electricidad", "dpg"]

    def setUp(self):
        super().setUp()
        p = mock.patch("actividades.mongo.inscripciones", return_value=mock.Mock(find=mock.Mock(return_value=[])))
        p.start()
        self.addCleanup(p.stop)
        self.aid = self.competencia("Fútbol", [])  # nadie inscrito
        self.actividades[self.aid]["puntosPorPuesto"] = [10, 7, 5, 3, 1]

    def jugar(self):
        t = torneos.crear_torneo({"tipo": "arena", "actividadId": self.aid, "participantes": self.ORDEN, "sorteo": False}, ADMIN)
        for pid, m in (("R1-P2", (1, 0)), ("R2-P1", (2, 0)), ("R2-P2", (0, 1)), ("R3-P1", (2, 1)), ("R3-P2", (1, 0))):
            t = torneos.registrar_resultado(t["id"], pid, dict(zip("ab", m)), ADMIN)
            torneos.validar_resultado(t["id"], pid, ADMIN)
        return t

    def test_sin_inscritos_entran_los_cinco_departamentos(self):
        t = torneos.crear_torneo({"tipo": "arena", "actividadId": self.aid, "sorteo": False}, ADMIN)
        self.assertEqual(t["modoParticipantes"], "departamentos")
        self.assertEqual(len(t["participantes"]), 5)
        self.assertIn({"id": "td", "nombre": "Tecnología Digital", "area": "Tecnología Digital", "departamento": "td"}, t["participantes"])
        disponibles = torneos.participantes_disponibles(self.aid)
        self.assertEqual((disponibles["modoPorDefecto"], len(disponibles["departamentos"])), ("departamentos", 5))

    def test_se_puede_quitar_un_departamento_pero_no_inventar_uno(self):
        t = torneos.crear_torneo({"tipo": "arena", "actividadId": self.aid, "participantes": ["td", "mecanica", "dpg", "quimica"]}, ADMIN)
        self.assertEqual(len(t["participantes"]), 4)
        torneos.eliminar_torneo(t["id"], ADMIN)
        with self.assertRaises(DatosInvalidos) as e:
            torneos.crear_torneo({"tipo": "arena", "actividadId": self.aid, "participantes": ["td", "inventado"]}, ADMIN)
        self.assertIn("no existe", e.exception.errores["participantes"])

    def test_los_puntos_escritos_a_mano_se_publican_por_departamento_y_suman_a_la_tabla(self):
        t = self.jugar()
        r = torneos.enviar_puestos(t["id"], ADMIN, {"1": 20, "2": 15, "3": 10, "4": 5, "5": 2}, publicar=True)
        self.assertTrue(r["resultadosPublicados"])
        doc = self.col_resultados.docs[0]
        self.assertEqual((doc["estado"], doc["origen"]), ("publicado", "departamentos"))
        self.assertEqual([(p["departamentoId"], p["puntos"]) for p in doc["posiciones"]], [("td", 20), ("mecanica", 15), ("electricidad", 10), ("quimica", 5), ("dpg", 2)])
        reporte = reporte_evento.preparar_reporte([{"_id": self.aid, "nombre": "Fútbol", "tipoActividad": "competencia", "estado": "programada"}],
                                                  self.col_resultados.find({}), {"actividades": []})
        puntos = {a["area"]: a["puntos"] for a in reporte["areas"]}
        self.assertEqual((puntos["Tecnología Digital"], puntos["Mecánica"], puntos["Gestión y Producción"]), (20, 15, 2))

    def test_guardar_sin_publicar_y_luego_publicar_pero_nunca_pisar_lo_publicado(self):
        t = self.jugar()
        torneos.enviar_puestos(t["id"], ADMIN, {"1": 1, "2": 1, "3": 1, "4": 1, "5": 1})
        self.assertEqual(self.col_resultados.docs[0]["estado"], "pendiente")
        torneos.enviar_puestos(t["id"], ADMIN, {"1": 20, "2": 15, "3": 10, "4": 5, "5": 2}, publicar=True)
        self.assertEqual((len(self.col_resultados.docs), self.col_resultados.docs[0]["estado"]), (1, "publicado"))
        with self.assertRaises(DatosInvalidos):
            torneos.enviar_puestos(t["id"], ADMIN, {"1": 9, "2": 9, "3": 9, "4": 9, "5": 9}, publicar=True)

    def test_la_gymkana_tambien_es_de_los_cinco_departamentos(self):
        from . import puntajes
        h = puntajes.hoja(self.aid)
        self.assertEqual((h["modoParticipantes"], len(h["participantes"])), ("departamentos", 5))
        puntajes.guardar(self.aid, {"pruebas": [{"id": "carrera", "nombre": "Carrera"}], "puntajes": {"mecanica": {"carrera": 9}, "td": {"carrera": 4}}}, ADMIN)
        puntajes.enviar_puestos(self.aid, ADMIN, {"1": 12, "2": 8}, publicar=True)
        doc = self.col_resultados.docs[0]
        self.assertEqual((doc["estado"], [(p["departamentoId"], p["puntos"]) for p in doc["posiciones"]]), ("publicado", [("mecanica", 12), ("td", 8)]))

    def test_el_torneo_guarda_como_participan_aunque_se_mida_por_sets(self):
        t = torneos.crear_torneo({"tipo": "arena", "actividadId": self.aid, "modoResultado": "sets", "setsParaGanar": 2, "sorteo": False}, ADMIN)
        self.assertEqual((t["modoParticipantes"], t["modoResultado"]), ("departamentos", "sets"))
        self.assertEqual(torneos._cargar(t["id"])["modoParticipantes"], "departamentos")

    def test_los_torneos_antiguos_se_reconocen_por_sus_participantes(self):
        self.assertEqual(torneos.modo_de({"participantes": ["td", "dpg"]}), "departamentos")
        self.assertEqual(torneos.modo_de({"participantes": [{"id": "e1"}, {"id": "e2"}]}), "inscritos")
        self.assertEqual(torneos.modo_de({"modoParticipantes": "inscritos", "participantes": ["td"]}), "inscritos")
