"""US-38: pruebas de cálculo sin conexión a MongoDB, Django ni Gemini.

Ejecutar: python -m unittest actividades.tests_intereses -v
"""
import json
import types
import unittest
from unittest import mock

from . import intereses


CATALOGO = [
    {'_id': 'musica', 'nombre': 'Música', 'orden': 1},
    {'_id': 'tecnologia', 'nombre': 'Tecnología e innovación', 'orden': 2},
    {'_id': 'danza', 'nombre': 'Danza', 'orden': 3},
]
USUARIOS = [
    {'_id': 'u1', 'activo': True, 'rol': 'usuario'},
    {'_id': 'u2', 'activo': True, 'rol': 'usuario'},
    {'_id': 'u3', 'activo': True, 'rol': 'organizador'},
    {'_id': 'u4', 'activo': False, 'rol': 'usuario'},
    {'_id': 'admin', 'activo': True, 'rol': 'administrador'},
]
ELECCIONES = [
    {'_id': 'u1', 'intereses': ['musica', 'tecnologia', 'musica']},
    {'_id': 'u2', 'intereses': ['musica']},
    {'_id': 'u3', 'intereses': []},
    {'_id': 'u4', 'intereses': ['danza']},
    {'_id': 'admin', 'intereses': ['danza']},
]


class ResumenInteresesTests(unittest.TestCase):
    def resumen(self):
        return intereses.preparar_resumen(CATALOGO, ELECCIONES, USUARIOS)

    def test_cuenta_personas_sin_repetir_categoria_por_usuario(self):
        datos = self.resumen()
        conteos = {i['id']: i['estudiantes'] for i in datos['intereses']}
        self.assertEqual(conteos, {'musica': 2, 'tecnologia': 1, 'danza': 0})
        self.assertEqual(datos['totales']['selecciones'], 3)

    def test_excluye_inactivos_y_admin_y_respeta_estudiante_sin_intereses(self):
        totales = self.resumen()['totales']
        self.assertEqual(totales['estudiantesActivos'], 3)
        self.assertEqual(totales['conIntereses'], 2)
        self.assertEqual(totales['sinIntereses'], 1)

    def test_porcentajes_son_por_estudiante_que_guardo_seleccion(self):
        datos = self.resumen()
        porcentajes = {i['id']: i['porcentaje'] for i in datos['intereses']}
        self.assertEqual(porcentajes, {'musica': 100.0, 'tecnologia': 50.0, 'danza': 0.0})
        self.assertGreater(sum(porcentajes.values()), 100)  # Selección múltiple.

    def test_omite_categorias_inexistentes_y_no_inventa_valores(self):
        datos = intereses.preparar_resumen(CATALOGO, [
            {'_id': 'u1', 'intereses': ['no-existe', 'musica', 'musica']},
            {'_id': 'u2', 'intereses': ['otro']},
        ], USUARIOS)
        self.assertEqual(datos['totales']['conIntereses'], 1)
        self.assertEqual(datos['totales']['selecciones'], 1)
        self.assertEqual(datos['intereses'][0]['nombre'], 'Música')

    def test_sin_elecciones_no_divide_entre_cero(self):
        datos = intereses.preparar_resumen(CATALOGO, [], USUARIOS)
        self.assertEqual(datos['totales']['conIntereses'], 0)
        self.assertTrue(all(x['porcentaje'] == 0 for x in datos['intereses']))

    def test_prompt_gemini_solo_contiene_agregados_y_no_datos_privados(self):
        respuesta = {'candidates': [{'content': {'parts': [{'text': 'La música predomina.'}]}}]}
        fake = mock.MagicMock()
        fake.read.return_value = json.dumps(respuesta).encode('utf-8')
        fake.__enter__.return_value = fake
        with mock.patch.object(intereses.request, 'urlopen', return_value=fake) as llamada:
            texto = intereses._solicitar_gemini(self.resumen(), 'test-secreto')
        self.assertEqual(texto, 'La música predomina.')
        envio = json.loads(llamada.call_args[0][0].data.decode('utf-8'))
        contenido = envio['contents'][0]['parts'][0]['text']
        self.assertIn('Música', contenido)
        self.assertNotIn('u1', contenido)
        self.assertNotIn('correo', contenido.lower())

    def test_gemini_sin_contenido_es_error_no_resumen_ficticio(self):
        fake = mock.MagicMock()
        fake.read.return_value = json.dumps({'candidates': []}).encode('utf-8')
        fake.__enter__.return_value = fake
        with mock.patch.object(intereses.request, 'urlopen', return_value=fake):
            with self.assertRaises(ValueError):
                intereses._solicitar_gemini(self.resumen(), 'test')


if __name__ == '__main__':
    unittest.main()
