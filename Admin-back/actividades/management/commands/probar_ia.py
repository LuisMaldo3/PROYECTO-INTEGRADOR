"""Comprueba en un paso si la IA (Gemini) del proyecto funciona y, si no, dice por qué.

Uso (desde la carpeta Admin-back):  python manage.py probar_ia
"""
import os

from django.core.management.base import BaseCommand

from actividades.gemini_config import GeminiError, MENSAJES, _configuracion_usuario, credenciales_gemini, llamar_gemini


class Command(BaseCommand):
    help = "Prueba la conexión con Gemini y explica cualquier falla en español."

    def handle(self, *args, **opciones):
        clave, modelo = credenciales_gemini()
        origen = ("Admin-back/.env" if os.environ.get("GEMINI_API_KEY") else "Usuario-back/.env" if _configuracion_usuario().get("GEMINI_API_KEY") else None)
        self.stdout.write(f"Modelo configurado: {modelo}")
        if not clave:
            self.stdout.write(self.style.ERROR("✗ " + MENSAJES["sin_clave"]))
            return
        self.stdout.write(f"Clave encontrada en {origen or 'el entorno'} ({len(clave)} caracteres).")
        try:
            r = llamar_gemini({"contents": [{"parts": [{"text": "Responde solo con la palabra: listo"}]}], "generationConfig": {"maxOutputTokens": 20}}, clave, modelo, timeout=15)
        except GeminiError as e:
            self.stdout.write(self.style.ERROR(f"✗ {e}  [{e.tipo}]"))
            return
        texto = " ".join(p.get("text", "") for c in r.get("candidates", []) for p in c.get("content", {}).get("parts", []) if isinstance(p, dict)).strip()
        self.stdout.write(self.style.SUCCESS(f"✓ La IA responde: «{texto[:60]}»"))
