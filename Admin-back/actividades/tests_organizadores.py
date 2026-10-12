from unittest import mock
from django.test import SimpleTestCase
from . import organizadores

class OrganizadoresTests(SimpleTestCase):
    def test_lista_solo_activos_y_ordena(self):
        docs = [{"_id": "2", "nombre": "Zoe", "rol": "organizador"}, {"_id": "1", "nombre": "Ana", "rol": "organizador"}]
        with mock.patch("actividades.organizadores.mongo.usuarios") as usuarios:
            usuarios.return_value.find.return_value = docs
            resultado = organizadores.listar_organizadores()
            self.assertEqual([o["nombre"] for o in resultado], ["Ana", "Zoe"])
            usuarios.return_value.find.assert_called_once_with({"rol": "organizador", "activo": {"$ne": False}})
