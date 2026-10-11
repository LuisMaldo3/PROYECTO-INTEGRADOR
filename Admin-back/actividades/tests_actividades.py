"""Pruebas de la creación de actividades (US-25 · tarea 6, parte de Luis). Ejecutar:  python manage.py test"""
import json
from datetime import datetime, timedelta, timezone
from unittest import mock

from bson import ObjectId
from django.test import RequestFactory, SimpleTestCase, override_settings

from usuarios import tests as base

from . import tests_tablas as tablas
from . import views

CATEGORIAS = [
    {"_id": "deportes", "nombre": "Deportes"},
    {"_id": "cultura", "nombre": "Cultura"},
]
VALIDA = {
    "nombre": "Torneo de fútbol",
    "descripcion": "Campeonato abierto para todos los ciclos.",
    "categoriaId": "deportes",
    "fecha": "2026-10-19",
    "horaInicio": "11:00",
    "horaFin": "14:30",
}


@override_settings(JWT_SECRET=base.SECRETO)
class ActividadesTests(SimpleTestCase):
    def setUp(self):
        self.categorias = tablas.Coleccion(CATEGORIAS)
        self.actividades = tablas.Coleccion()
        parches = {
            "actividades.servicio.mongo.categorias": self.categorias,
            "actividades.servicio.mongo.actividades": self.actividades,
            "usuarios.auth.mongo.usuarios": base.ColeccionFalsa(base.USUARIOS),
            "usuarios.auth.mongo.tokens_revocados": base.ColeccionFalsa([]),
        }
        for objetivo, coleccion in parches.items():
            p = mock.patch(objetivo, return_value=coleccion)
            p.start()
            self.addCleanup(p.stop)
        self.rf = RequestFactory()

    def _auth(self, quien):
        return {"HTTP_AUTHORIZATION": f"Bearer {base.token(quien)}"} if quien else {}

    def _post(self, cuerpo, quien=base.ID_ADMIN, crudo=None):
        datos = crudo if crudo is not None else json.dumps(cuerpo)
        return views.actividades(self.rf.post("/", data=datos, content_type="application/json", **self._auth(quien)))

    def _errores(self, respuesta):
        self.assertEqual(respuesta.status_code, 400)
        datos = json.loads(respuesta.content)
        self.assertEqual(datos["codigo"], "DATOS_INVALIDOS")
        return datos["errores"]

    # --- crear -----------------------------------------------------------
    def test_el_admin_crea_una_actividad(self):
        r = self._post(VALIDA)
        self.assertEqual(r.status_code, 201)
        datos = json.loads(r.content)
        self.assertEqual(datos["nombre"], "Torneo de fútbol")
        self.assertEqual(datos["categoria"], {"id": "deportes", "nombre": "Deportes"})
        self.assertEqual(datos["creadaPor"], str(base.ID_ADMIN))
        self.assertTrue(datos["creadaEn"].endswith("+00:00"))
        guardada = self.actividades.docs[0]
        self.assertEqual(guardada["categoriaId"], "deportes")
        self.assertIsInstance(guardada["creadaEn"], datetime)

    def test_quita_espacios_sobrantes(self):
        r = self._post({**VALIDA, "nombre": "   Torneo   ", "descripcion": "  " + VALIDA["descripcion"] + "  "})
        self.assertEqual(r.status_code, 201)
        self.assertEqual(self.actividades.docs[0]["nombre"], "Torneo")
        self.assertEqual(self.actividades.docs[0]["descripcion"], VALIDA["descripcion"])

    def test_limites_exactos_se_aceptan(self):
        self.assertEqual(self._post({**VALIDA, "nombre": "abc", "descripcion": "x" * 10}).status_code, 201)
        self.assertEqual(self._post({**VALIDA, "nombre": "n" * 80, "descripcion": "x" * 500}).status_code, 201)

    def test_sin_token_da_401_y_no_guarda(self):
        self.assertEqual(self._post(VALIDA, quien=None).status_code, 401)
        self.assertEqual(self.actividades.docs, [])

    def test_usuario_normal_da_403_y_no_guarda(self):
        self.assertEqual(self._post(VALIDA, quien=base.ID_NORMAL).status_code, 403)
        self.assertEqual(self.actividades.docs, [])

    # --- revisiones ------------------------------------------------------
    def test_todo_vacio_marca_todos_los_campos(self):
        errores = self._errores(self._post({}))
        self.assertEqual(sorted(errores), ["categoriaId", "descripcion", "fecha", "horaFin", "horaInicio", "nombre"])
        self.assertEqual(self.actividades.docs, [])

    def test_solo_espacios_cuenta_como_vacio(self):
        errores = self._errores(self._post({**VALIDA, "nombre": "    "}))
        self.assertEqual(errores, {"nombre": "Escribe el nombre de la actividad."})

    def test_nombre_demasiado_corto_o_largo(self):
        self.assertIn("al menos 3", self._errores(self._post({**VALIDA, "nombre": "ab"}))["nombre"])
        self.assertIn("pasar de 80", self._errores(self._post({**VALIDA, "nombre": "n" * 81}))["nombre"])

    def test_descripcion_demasiado_corta_o_larga(self):
        self.assertIn("al menos 10", self._errores(self._post({**VALIDA, "descripcion": "corta"}))["descripcion"])
        self.assertIn("pasar de 500", self._errores(self._post({**VALIDA, "descripcion": "x" * 501}))["descripcion"])

    def test_categoria_que_no_existe(self):
        errores = self._errores(self._post({**VALIDA, "categoriaId": "inventada"}))
        self.assertEqual(errores, {"categoriaId": "Elige una categoría válida."})

    def test_tipos_raros_se_rechazan_sin_romper(self):
        self._errores(self._post({"nombre": 5, "descripcion": ["x"], "categoriaId": {"a": 1}}))
        self._errores(self._post([1, 2, 3]))

    def test_json_malo_da_400(self):
        r = self._post(None, crudo="{no es json")
        self.assertEqual(r.status_code, 400)
        self.assertEqual(json.loads(r.content)["codigo"], "JSON_INVALIDO")

    def test_metodo_no_permitido(self):
        r = views.actividades(self.rf.delete("/", **self._auth(base.ID_ADMIN)))
        self.assertEqual(r.status_code, 405)

    # --- fecha y horario (US-26 · tarea 6) -------------------------------
    def test_guarda_y_devuelve_la_fecha_y_las_horas(self):
        r = self._post(VALIDA)
        self.assertEqual(r.status_code, 201)
        datos = json.loads(r.content)
        self.assertEqual((datos["fecha"], datos["horaInicio"], datos["horaFin"]), ("2026-10-19", "11:00", "14:30"))
        guardada = self.actividades.docs[0]
        self.assertEqual((guardada["fecha"], guardada["horaInicio"], guardada["horaFin"]), ("2026-10-19", "11:00", "14:30"))

    def test_quita_espacios_de_la_fecha_y_las_horas(self):
        r = self._post({**VALIDA, "fecha": " 2026-10-19 ", "horaInicio": " 11:00", "horaFin": "14:30 "})
        self.assertEqual(r.status_code, 201)
        self.assertEqual(self.actividades.docs[0]["fecha"], "2026-10-19")

    def test_fecha_y_horas_son_obligatorias(self):
        for campo in ("fecha", "horaInicio", "horaFin"):
            sin = {k: v for k, v in VALIDA.items() if k != campo}
            self.assertEqual(list(self._errores(self._post(sin))), [campo])
        self.assertEqual(self.actividades.docs, [])

    def test_fecha_con_formato_malo_o_que_no_existe(self):
        for mala in ("19/10/2026", "2026-13-01", "2026-02-30", "2026-1-5", "mañana", "2026-10-19T10:00"):
            errores = self._errores(self._post({**VALIDA, "fecha": mala}))
            self.assertEqual(errores, {"fecha": "La fecha no es válida."}, mala)

    def test_fecha_bisiesta(self):
        self.assertEqual(self._post({**VALIDA, "fecha": "2028-02-29"}).status_code, 201)
        self.assertIn("fecha", self._errores(self._post({**VALIDA, "fecha": "2026-02-29"})))

    def test_hora_con_formato_malo(self):
        for mala in ("25:00", "11:60", "9:00", "11-00", "abc", "11:00:30"):
            errores = self._errores(self._post({**VALIDA, "horaInicio": mala}))
            self.assertEqual(errores, {"horaInicio": "La hora de inicio no es válida."}, mala)
            errores = self._errores(self._post({**VALIDA, "horaFin": mala}))
            self.assertEqual(errores, {"horaFin": "La hora de fin no es válida."}, mala)

    def test_horas_limite_del_dia_se_aceptan(self):
        self.assertEqual(self._post({**VALIDA, "horaInicio": "00:00", "horaFin": "23:59"}).status_code, 201)

    def test_la_hora_de_fin_debe_ser_despues_de_la_de_inicio(self):
        esperado = {"horaFin": "La hora de fin debe ser después de la hora de inicio."}
        self.assertEqual(self._errores(self._post({**VALIDA, "horaInicio": "14:00", "horaFin": "11:00"})), esperado)
        self.assertEqual(self._errores(self._post({**VALIDA, "horaInicio": "11:00", "horaFin": "11:00"})), esperado)
        self.assertEqual(self.actividades.docs, [])

    def test_un_minuto_despues_ya_es_valido(self):
        self.assertEqual(self._post({**VALIDA, "horaInicio": "11:00", "horaFin": "11:01"}).status_code, 201)
        self.assertEqual(self._post({**VALIDA, "horaInicio": "09:30", "horaFin": "10:00"}).status_code, 201)

    def test_no_compara_horas_si_una_no_es_valida(self):
        errores = self._errores(self._post({**VALIDA, "horaInicio": "25:00", "horaFin": "10:00"}))
        self.assertEqual(errores, {"horaInicio": "La hora de inicio no es válida."})

    def test_todos_los_errores_de_horario_llegan_juntos(self):
        errores = self._errores(self._post({**VALIDA, "fecha": "", "horaInicio": "14:00", "horaFin": "11:00"}))
        self.assertEqual(sorted(errores), ["fecha", "horaFin"])

    def test_tipos_raros_en_fecha_y_horas(self):
        errores = self._errores(self._post({**VALIDA, "fecha": 20261019, "horaInicio": ["11:00"], "horaFin": None}))
        self.assertEqual(sorted(errores), ["fecha", "horaFin", "horaInicio"])

    def test_la_lista_trae_el_horario_y_las_antiguas_salen_null(self):
        ahora = datetime.now(timezone.utc)
        self.actividades.docs = [
            {"_id": ObjectId(), "nombre": "Con horario", "descripcion": "d" * 10, "categoriaId": "cultura", "creadaPor": "x",
             "creadaEn": ahora, "fecha": "2026-10-20", "horaInicio": "10:00", "horaFin": "11:00"},
            {"_id": ObjectId(), "nombre": "Antigua", "descripcion": "d" * 10, "categoriaId": "cultura", "creadaPor": "x",
             "creadaEn": ahora - timedelta(days=5)},
        ]
        datos = json.loads(views.actividades(self.rf.get("/", **self._auth(base.ID_ADMIN))).content)
        nueva, antigua = datos["actividades"]
        self.assertEqual((nueva["fecha"], nueva["horaInicio"], nueva["horaFin"]), ("2026-10-20", "10:00", "11:00"))
        self.assertEqual((antigua["fecha"], antigua["horaInicio"], antigua["horaFin"]), (None, None, None))

    # --- listas ----------------------------------------------------------
    def test_lista_de_la_mas_nueva_a_la_mas_antigua(self):
        ahora = datetime.now(timezone.utc)
        self.actividades.docs = [
            {"_id": ObjectId(), "nombre": "Vieja", "descripcion": "d" * 10, "categoriaId": "cultura", "creadaPor": "x", "creadaEn": ahora - timedelta(days=2)},
            {"_id": ObjectId(), "nombre": "Nueva", "descripcion": "d" * 10, "categoriaId": "deportes", "creadaPor": "x", "creadaEn": ahora},
            {"_id": ObjectId(), "nombre": "Huérfana", "descripcion": "d" * 10, "categoriaId": "borrada", "creadaPor": "x", "creadaEn": ahora - timedelta(days=1)},
        ]
        r = views.actividades(self.rf.get("/", **self._auth(base.ID_ADMIN)))
        datos = json.loads(r.content)
        self.assertEqual(datos["total"], 3)
        self.assertEqual([a["nombre"] for a in datos["actividades"]], ["Nueva", "Huérfana", "Vieja"])
        self.assertEqual(datos["actividades"][0]["categoria"]["nombre"], "Deportes")
        self.assertEqual(datos["actividades"][1]["categoria"]["nombre"], "Sin categoría")

    def test_lista_requiere_admin(self):
        self.assertEqual(views.actividades(self.rf.get("/", **self._auth(base.ID_NORMAL))).status_code, 403)

    def test_categorias_ordenadas_por_nombre(self):
        r = views.categorias(self.rf.get("/", **self._auth(base.ID_ADMIN)))
        self.assertEqual(json.loads(r.content)["categorias"], [
            {"id": "cultura", "nombre": "Cultura"},
            {"id": "deportes", "nombre": "Deportes"},
        ])

    def test_categorias_requiere_admin(self):
        self.assertEqual(views.categorias(self.rf.get("/", **self._auth(None))).status_code, 401)
