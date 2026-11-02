"""Validación conservadora de portadas seleccionadas por el administrador.
Las imágenes de Gemini se almacenan en GridFS desde Usuario-back (Spring Boot).
Las fotografías deben provenir de Wikimedia Commons para preservar procedencia.
"""
import re
from urllib.parse import urlparse

ID = re.compile(r"^[0-9a-fA-F]{24}$")


def normalizar_portada(valor):
    if valor in (None, ""):
        return None
    if not isinstance(valor, dict):
        raise ValueError("Selecciona una portada válida.")
    tipo = valor.get("tipo")
    if tipo in ("gemini", "subida"):
        archivo_id = str(valor.get("archivoId") or "")
        if not ID.fullmatch(archivo_id):
            raise ValueError("La portada generada no tiene un archivo válido.")
        return {"tipo": tipo, "archivoId": archivo_id,
                "url": f"/api/portadas/archivo/{archivo_id}",
                "autor": "Gemini" if tipo == "gemini" else "Administración VYNK",
                "licencia": "Imagen generada con IA" if tipo == "gemini" else "Material autorizado por administración"}
    if tipo == "commons":
        url = str(valor.get("url") or "")
        enlace = str(valor.get("creditoUrl") or "")
        p = urlparse(url)
        c = urlparse(enlace)
        if p.scheme != "https" or p.hostname != "upload.wikimedia.org" or p.username or p.password:
            raise ValueError("La fotografía debe provenir de Wikimedia Commons.")
        if c.scheme != "https" or c.hostname != "commons.wikimedia.org" or c.username or c.password:
            raise ValueError("Falta el enlace de atribución de Wikimedia Commons.")
        if len(url) > 1500 or len(enlace) > 1500:
            raise ValueError("La URL de la portada es demasiado larga.")
        autor = str(valor.get("autor") or "Colaborador de Wikimedia Commons")[:120]
        licencia = str(valor.get("licencia") or "").strip()[:100]
        permitida = licencia.upper().startswith(("CC BY", "CC-BY", "CC0", "PUBLIC DOMAIN", "PD-")) or licencia.upper() == "PD"
        prohibida = any(p in licencia.upper() for p in ("-NC", "-ND", "COPYRIGHT", "ALL RIGHTS", "NONCOMMERCIAL"))
        if not permitida or prohibida:
            raise ValueError("La fotografía debe tener una licencia reutilizable verificable.")
        return {"tipo": "commons", "url": url, "creditoUrl": enlace,
                "autor": autor, "licencia": licencia}
    if tipo == "url":
        url = str(valor.get("url") or "").strip()
        p = urlparse(url)
        try:
            puerto = p.port
        except ValueError as exc:
            raise ValueError("La URL tiene un puerto inválido.") from exc
        if (p.scheme != "https" or not p.hostname or p.username or p.password or
            len(url) > 1500 or puerto not in (None, 443) or
            p.hostname.lower() in ("localhost", "127.0.0.1", "::1") or
            p.hostname.lower().endswith((".localhost", ".local", ".internal"))):
            raise ValueError("La imagen debe utilizar una URL pública HTTPS válida.")
        return {"tipo": "url", "url": url, "autor": "Imagen proporcionada por administración", "licencia": "Verificar permisos de uso"}
    raise ValueError("Origen de portada no permitido.")
