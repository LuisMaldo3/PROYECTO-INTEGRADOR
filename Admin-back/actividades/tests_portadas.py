from django.test import SimpleTestCase
from .portadas import normalizar_portada

class PortadasTest(SimpleTestCase):
    def test_gemini(self):
        p = normalizar_portada({"tipo": "gemini", "archivoId": "a" * 24})
        self.assertEqual(p["url"], "/api/portadas/archivo/" + "a" * 24)
    def test_no_acepta_web_arbitraria(self):
        with self.assertRaises(ValueError):
            normalizar_portada({"tipo": "commons", "url": "https://example.com/img.jpg",
                                "creditoUrl": "https://commons.wikimedia.org/wiki/File:foo.jpg"})
    def test_comunes(self):
        p = normalizar_portada({"tipo": "commons", "url": "https://upload.wikimedia.org/a.jpg",
              "creditoUrl": "https://commons.wikimedia.org/wiki/File:A.jpg", "autor": "Test", "licencia": "CC-BY-SA"})
        self.assertEqual(p["autor"], "Test")
