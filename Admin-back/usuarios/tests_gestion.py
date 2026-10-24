"""Cobertura sin MongoDB para búsqueda, permisos y desactivación."""
import json
import re
from unittest import mock
from bson import ObjectId
from django.test import SimpleTestCase, RequestFactory, override_settings
from . import views, tests as base


class ColeccionUsuario(base.ColeccionFalsa):
    def find(self, filtro=None, proyeccion=None):
        filtro = filtro or {}
        docs = self.docs
        if "$or" in filtro:
            condiciones = filtro["$or"]
            docs = [d for d in docs if any(re.search(reg["$regex"], str(d.get(campo, "")), re.IGNORECASE)
                    for condicion in condiciones for campo, reg in condicion.items())]
        return base.Cursor([{k: v for k, v in d.items()
                            if k == "_id" or k in (proyeccion or d)} for d in docs])

    def update_one(self, filtro, cambio, **kwargs):
        doc = next((d for d in self.docs if d["_id"] == filtro["_id"]), None)
        if doc is not None:
            doc.update(cambio["$set"])


class ColeccionPermisos:
    def __init__(self):
        self.docs = {}

    def find_one(self, filtro, proyeccion=None):
        return self.docs.get(filtro["_id"])

    def update_one(self, filtro, cambio, upsert=False):
        doc = self.docs.setdefault(filtro["_id"], {"_id": filtro["_id"], "permisos": {}})
        for clave, valor in cambio["$set"].items():
            if clave.startswith("permisos."):
                doc["permisos"][clave.split(".", 1)[1]] = valor
            else:
                doc[clave] = valor


@override_settings(JWT_SECRET=base.SECRETO)
class GestionAdministrativaTests(SimpleTestCase):
    def setUp(self):
        self.usuarios = ColeccionUsuario([dict(d) for d in base.USUARIOS])
        self.permisos = ColeccionPermisos()
        parches = {
            "usuarios.auth.mongo.usuarios": self.usuarios,
            "usuarios.views.mongo.usuarios": self.usuarios,
            "usuarios.servicio.mongo.usuarios": self.usuarios,
            "usuarios.permisos.mongo.permisos_usuarios": self.permisos,
            "usuarios.auth.mongo.tokens_revocados": base.ColeccionFalsa([]),
        }
        for nombre, coleccion in parches.items():
            p = mock.patch(nombre, return_value=coleccion)
            p.start()
            self.addCleanup(p.stop)
        self.rf = RequestFactory()
        self.auth = {"HTTP_AUTHORIZATION": f"Bearer {base.token(base.ID_ADMIN)}"}

    def test_busqueda_nombre_y_correo_segura(self):
        r = views.listar_usuarios(self.rf.get("/api/usuarios/?q=beto", **self.auth))
        self.assertEqual(r.status_code, 200)
        self.assertEqual(json.loads(r.content)["total"], 1)
        r = views.listar_usuarios(self.rf.get("/api/usuarios/?q=tecsup.edu.pe", **self.auth))
        self.assertEqual(json.loads(r.content)["total"], 3)
        r = views.listar_usuarios(self.rf.get("/api/usuarios/?q=.*", **self.auth))
        self.assertEqual(json.loads(r.content)["total"], 0)

    def test_permisos_se_guardan_y_restringen_la_api(self):
        ruta = str(base.ID_ADMIN)
        body = json.dumps({"permisos": {"usuarios.ver": False}})
        respuesta = views.permisos_usuario(self.rf.put("/", body, content_type="application/json", **self.auth), usuario_id=ruta)
        self.assertEqual(respuesta.status_code, 200)
        respuesta = views.listar_usuarios(self.rf.get("/", **self.auth))
        self.assertEqual(respuesta.status_code, 403)
        self.assertEqual(json.loads(respuesta.content)["codigo"], "PERMISO_DENEGADO")

    def test_no_permita_revocar_su_propio_gestor(self):
        cuerpo = json.dumps({"permisos": {"usuarios.gestionar_permisos": False}})
        r = views.permisos_usuario(self.rf.put("/", cuerpo, content_type="application/json", **self.auth), usuario_id=str(base.ID_ADMIN))
        self.assertEqual(r.status_code, 400)

    def test_inactivar_rechaza_token_preexistente_en_django(self):
        cuerpo = json.dumps({"activo": False})
        respuesta = views.cambiar_estado_usuario(self.rf.patch("/", cuerpo, content_type="application/json", **self.auth), usuario_id=str(base.ID_NORMAL))
        self.assertEqual(respuesta.status_code, 200)
        self.assertFalse(json.loads(respuesta.content)["activo"])
        token = base.token(base.ID_NORMAL)
        respuesta = views.listar_usuarios(self.rf.get("/", HTTP_AUTHORIZATION=f"Bearer {token}"))
        self.assertEqual(respuesta.status_code, 401)

    def test_admin_no_se_desactiva_a_si_mismo(self):
        cuerpo = json.dumps({"activo": False})
        respuesta = views.cambiar_estado_usuario(self.rf.patch("/", cuerpo, content_type="application/json", **self.auth), usuario_id=str(base.ID_ADMIN))
        self.assertEqual(respuesta.status_code, 400)
