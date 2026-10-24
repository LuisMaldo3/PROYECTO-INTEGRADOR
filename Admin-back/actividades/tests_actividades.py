"""Pruebas de la creación de actividades (US-25 · tarea 6, parte de Luis). Ejecutar:  python manage.py test"""
import json
from datetime import datetime, timezone
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
UBICACIONES = [
    {"_id": "polideportivo", "nombre": "Polideportivo"},
    {"_id": "auditorio", "nombre": "Auditorio"},
]
ORGANIZADOR_ID = ObjectId("111111111111111111111111")
ORGANIZADOR = {"_id": ORGANIZADOR_ID, "nombre": "Rita Organizadora", "rol": "organizador", "activo": True}
VALIDA = {
    "nombre": "Torneo de fútbol",
    "descripcion": "Campeonato abierto para todos los ciclos.",
    "categoriaId": "deportes",
    "ubicacionId": "polideportivo",
    "organizadorId": str(ORGANIZADOR_ID),
    "maximoParticipantes": 30,
    "fecha": "2026-10-19",
    "horaInicio": "15:00",
    "horaFin": "16:30",
}


@override_settings(JWT_SECRET=base.SECRETO)
class ActividadesTests(SimpleTestCase):
    def setUp(self):
        self.categorias = tablas.Coleccion(CATEGORIAS)
        self.actividades = tablas.Coleccion()
        parches = {
            "actividades.servicio.mongo.categorias": self.categorias,
            "actividades.servicio.mongo.actividades": self.actividades,
            "actividades.servicio.mongo.ubicaciones": tablas.Coleccion(UBICACIONES),
            "usuarios.auth.mongo.usuarios": base.ColeccionFalsa(base.USUARIOS + [ORGANIZADOR]),
            "usuarios.auth.mongo.tokens_revocados": base.ColeccionFalsa([]),
            "usuarios.permisos.mongo.permisos_usuarios": base.ColeccionFalsa([]),
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
    def test_todo_vacio_marca_los_campos_obligatorios(self):
        errores = self._errores(self._post({}))
        self.assertEqual(sorted(errores), ["categoriaId", "descripcion", "fecha", "horaInicio", "maximoParticipantes", "nombre", "organizadorId", "ubicacionId"])
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

    # --- listas ----------------------------------------------------------
    def test_lista_ordenada_por_fecha_y_hora(self):
        ahora = datetime.now(timezone.utc)

        def doc(nombre, categoria, fecha, hora):
            return {"_id": ObjectId(), "nombre": nombre, "descripcion": "d" * 10, "categoriaId": categoria,
                    "fecha": fecha, "horaInicio": hora, "horaFin": None, "creadaPor": "x", "creadaEn": ahora}

        self.actividades.docs = [
            doc("Tarde del martes", "cultura", "2026-10-20", "15:00"),
            doc("Lunes", "deportes", "2026-10-19", "11:00"),
            doc("Mañana del martes", "borrada", "2026-10-20", "10:00"),
            doc("Sin fecha", "cultura", None, None),
        ]
        r = views.actividades(self.rf.get("/", **self._auth(base.ID_ADMIN)))
        datos = json.loads(r.content)
        self.assertEqual(datos["total"], 4)
        self.assertEqual([a["nombre"] for a in datos["actividades"]], ["Lunes", "Mañana del martes", "Tarde del martes", "Sin fecha"])
        self.assertEqual(datos["actividades"][0]["categoria"]["nombre"], "Deportes")
        self.assertEqual(datos["actividades"][1]["categoria"]["nombre"], "Sin categoría")
        self.assertEqual(datos["actividades"][0]["fecha"], "2026-10-19")
        self.assertIsNone(datos["actividades"][3]["horaInicio"])

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
