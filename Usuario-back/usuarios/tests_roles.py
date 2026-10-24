"""Pruebas de los roles válidos (US-24 · tarea 6, parte de Jordi). Ejecutar:  python manage.py test usuarios"""
import json
from unittest import mock

from django.test import RequestFactory, SimpleTestCase, override_settings

from . import tests as base
from . import tests_cambio_rol as luis
from . import views
from .roles import ROLES_VALIDOS, RolInvalido, validar_rol


class ValidarRolTests(SimpleTestCase):
    def test_los_tres_roles_son_validos(self):
        for rol in ("usuario", "organizador", "administrador"):
            self.assertEqual(validar_rol(rol), rol)

    def test_normaliza_espacios_y_mayusculas(self):
        self.assertEqual(validar_rol("  ORGANIZADOR "), "organizador")

    def test_rol_desconocido_se_rechaza(self):
        for malo in ("superadmin", "admin", "root", "usuario;drop"):
            with self.assertRaises(RolInvalido):
                validar_rol(malo)

    def test_vacio_o_sin_texto_se_rechaza(self):
        for malo in (None, "", "   ", 5, ["administrador"], {"rol": "usuario"}):
            with self.assertRaises(RolInvalido):
                validar_rol(malo)

    def test_el_mensaje_lista_los_roles_permitidos(self):
        with self.assertRaises(RolInvalido) as e:
            validar_rol("jefe")
        for rol in ROLES_VALIDOS:
            self.assertIn(rol, str(e.exception))


@override_settings(JWT_SECRET=base.SECRETO)
class EndpointRechazaRolesInvalidosTests(SimpleTestCase):
    def setUp(self):
        self.coleccion = luis.ColeccionEditable([dict(d) for d in base.USUARIOS])
        for objetivo in ("usuarios.views.mongo.usuarios", "usuarios.auth.mongo.usuarios", "usuarios.servicio.mongo.usuarios"):
            p = mock.patch(objetivo, return_value=self.coleccion)
            p.start()
            self.addCleanup(p.stop)
        p = mock.patch("usuarios.auth.mongo.tokens_revocados", return_value=base.ColeccionFalsa([]))
        p.start()
        self.addCleanup(p.stop)
        self.rf = RequestFactory()

    def _patch(self, cuerpo):
        peticion = self.rf.patch(
            "/", data=json.dumps(cuerpo), content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {base.token(base.ID_ADMIN)}",
        )
        return views.cambiar_rol_usuario(peticion, usuario_id=str(base.ID_NORMAL))

    def test_rol_inventado_da_400_y_no_se_guarda(self):
        r = self._patch({"rol": "superadmin"})
        self.assertEqual(r.status_code, 400)
        self.assertEqual(json.loads(r.content)["codigo"], "ROL_INVALIDO")
        self.assertEqual(next(d["rol"] for d in self.coleccion.docs if d["_id"] == base.ID_NORMAL), "usuario")

    def test_sin_campo_rol_da_400(self):
        self.assertEqual(self._patch({}).status_code, 400)

    def test_rol_con_mayusculas_se_guarda_normalizado(self):
        r = self._patch({"rol": "Organizador"})
        self.assertEqual(r.status_code, 200)
        self.assertEqual(json.loads(r.content)["rol"], "organizador")
