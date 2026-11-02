from django.urls import path

from . import views

urlpatterns = [
    path("clasificacion/", views.clasificacion_general, name="clasificacion_general"),
    path("actividades/<str:actividad_id>/participantes/", views.participantes_competencia, name="participantes_competencia"),
    path("actividades/<str:actividad_id>/resultados/", views.resultados_competencia, name="resultados_competencia"),
    path("actividades/<str:actividad_id>/resultados/publicar/", views.publicar_resultados, name="publicar_resultados"),
    path("actividades/<str:actividad_id>/resultados/anunciar/", views.anunciar_resultados, name="anunciar_resultados"),
    path("catalogo/", views.catalogo_estudiantes, name="catalogo_estudiantes"),
    path("avisos/publicados/", views.avisos_publicados, name="avisos_publicados"),
    path("avisos/", views.avisos, name="avisos"),
    path("actividades/<str:actividad_id>/cancelar/", views.cancelar_actividad, name="cancelar_actividad"),
    path("actividades/<str:actividad_id>/", views.editar_actividad, name="editar_actividad"),
    path("actividades/", views.actividades, name="actividades"),
    path("categorias/", views.categorias, name="categorias"),
    path("ubicaciones/", views.ubicaciones, name="ubicaciones"),
    path("organizadores/", views.listar_organizadores, name="organizadores"),
]
