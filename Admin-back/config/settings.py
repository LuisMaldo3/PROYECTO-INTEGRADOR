"""Backend de administracion de VYNK (Django).

No tiene modelos propios: lee la MISMA base MongoDB que Usuario-back (Spring Boot)
y confia en los JWT que emite Spring Boot (mismo JWT_SECRET).
"""
import os
from pathlib import Path

from dotenv import load_dotenv

BASE_DIR = Path(__file__).resolve().parent.parent
load_dotenv(BASE_DIR / ".env")

SECRET_KEY = os.environ.get("DJANGO_SECRET_KEY", "solo-desarrollo")
DEBUG = os.environ.get("DJANGO_DEBUG", "true").lower() == "true"
ALLOWED_HOSTS = ["localhost", "127.0.0.1"]

INSTALLED_APPS = [
    "corsheaders",
    "usuarios",
]

MIDDLEWARE = [
    "corsheaders.middleware.CorsMiddleware",  # siempre primero
    "django.middleware.common.CommonMiddleware",
]

ROOT_URLCONF = "config.urls"
WSGI_APPLICATION = "config.wsgi.application"

# Django no usa base propia aqui (los usuarios viven en MongoDB).
DATABASES = {}

LANGUAGE_CODE = "es"
TIME_ZONE = "America/Lima"
USE_TZ = True
DEFAULT_AUTO_FIELD = "django.db.models.BigAutoField"

# --- Configuracion propia de VYNK -------------------------------------------
MONGODB_URI = os.environ.get("MONGODB_URI", "mongodb://localhost:27017/vynk")
MONGODB_DB_POR_DEFECTO = "vynk"

JWT_SECRET = os.environ.get("JWT_SECRET", "")
JWT_ALGORITMO = "HS256"
ROL_ADMIN = "administrador"

CORS_ALLOWED_ORIGINS = [
    o.strip()
    for o in os.environ.get("CORS_ORIGINS", "http://localhost:5174").split(",")
    if o.strip()
]
