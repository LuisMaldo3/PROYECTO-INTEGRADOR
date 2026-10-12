from django.urls import path

from . import views

urlpatterns = [
    path("actividades/", views.actividades, name="actividades"),
    path("categorias/", views.categorias, name="categorias"),
    path("ubicaciones/", views.ubicaciones, name="ubicaciones"),
    path("organizadores/", views.listar_organizadores, name="organizadores"),
]
