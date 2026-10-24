from django.urls import include, path

urlpatterns = [
    path("api/usuarios/", include("usuarios.urls")),
    path("api/", include("actividades.urls")),
]
