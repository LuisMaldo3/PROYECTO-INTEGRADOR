from django.urls import path

from . import views

urlpatterns = [
    path("avisos/", views.avisos, name="avisos"),
    path("actividades/<str:actividad_id>/cancelar/", views.cancelar_actividad, name="cancelar_actividad"),
    path("actividades/<str:actividad_id>/", views.editar_actividad, name="editar_actividad"),
    path("actividades/", views.actividades, name="actividades"),
    path("categorias/", views.categorias, name="categorias"),
    path("ubicaciones/", views.ubicaciones, name="ubicaciones"),
    path("organizadores/", views.listar_organizadores, name="organizadores"),
]
