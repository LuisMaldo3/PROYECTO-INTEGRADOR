import importlib.util
import pathlib
import unittest

ruta = pathlib.Path(__file__).with_name('portadas.py')
spec = importlib.util.spec_from_file_location('portadas_aislado', ruta)
mod = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)

class LicenciasVynkTest(unittest.TestCase):
    def test_acepta_cc_by_con_autor_y_atribucion(self):
        portada = mod.normalizar_portada({'tipo': 'commons',
            'url': 'https://upload.wikimedia.org/wikipedia/commons/a/a0/foto.jpg',
            'creditoUrl': 'https://commons.wikimedia.org/wiki/File:foto.jpg',
            'autor': 'Autora', 'licencia': 'CC BY-SA 4.0'})
        self.assertEqual(portada['licencia'], 'CC BY-SA 4.0')

    def test_no_acepta_licencia_nc(self):
        with self.assertRaisesRegex(ValueError, 'licencia'):
            mod.normalizar_portada({'tipo':'commons',
                'url':'https://upload.wikimedia.org/wikipedia/commons/a/a0/foto.jpg',
                'creditoUrl':'https://commons.wikimedia.org/wiki/File:foto.jpg',
                'licencia':'CC BY-NC 4.0'})

    def test_no_acepta_licencia_omitida(self):
        with self.assertRaisesRegex(ValueError, 'licencia'):
            mod.normalizar_portada({'tipo':'commons',
                'url':'https://upload.wikimedia.org/wikipedia/commons/a/a0/foto.jpg',
                'creditoUrl':'https://commons.wikimedia.org/wiki/File:foto.jpg'})

if __name__ == '__main__': unittest.main()
