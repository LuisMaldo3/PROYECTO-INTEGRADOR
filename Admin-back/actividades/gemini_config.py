"""Configuración local segura de Gemini, sin duplicar ni publicar claves.

Django lee primero su variable de entorno. Si falta, en desarrollo local puede
leer el .env del backend de usuario, siempre que sea una carpeta hermana.
No modifica .env, no incluye claves en respuestas HTTP, logs ni archivos ZIP.
"""
import os
import re
from pathlib import Path
from dotenv import dotenv_values

_RAIZ = Path(__file__).resolve().parents[2]
_MODELO_POR_DEFECTO = 'gemini-2.5-flash'


def _configuracion_usuario():
    ruta = _RAIZ / 'Usuario-back' / '.env'
    if not ruta.is_file():
        return {}
    try:
        return dotenv_values(ruta)
    except (OSError, UnicodeError):
        return {}


def credenciales_gemini():
    """Devuelve (clave, modelo); la clave NUNCA sale del proceso Python."""
    ajustes = _configuracion_usuario()
    clave = (os.environ.get('GEMINI_API_KEY') or ajustes.get('GEMINI_API_KEY') or '').strip()
    modelo = (os.environ.get('GEMINI_MODEL') or ajustes.get('GEMINI_MODEL') or _MODELO_POR_DEFECTO).strip()
    if not re.fullmatch(r'[a-zA-Z0-9._-]{3,80}', modelo):
        modelo = _MODELO_POR_DEFECTO
    return clave, modelo
