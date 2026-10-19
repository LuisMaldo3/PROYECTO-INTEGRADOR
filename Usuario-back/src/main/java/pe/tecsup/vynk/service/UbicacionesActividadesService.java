package pe.tecsup.vynk.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

/** Consulta ubicaciones gestionadas por Tecsup, sin convertir identificadores en direcciones ficticias. */
@Service
public class UbicacionesActividadesService {
    private final MongoTemplate mongo;

    public UbicacionesActividadesService(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    /** Datos publicables para el chat: nunca se devuelven IDs de MongoDB como si fueran un lugar. */
    public List<Map<String, Object>> resolver(List<Map<String, Object>> actividades) {
        List<Map<String, Object>> resultados = new ArrayList<>();
        Map<String, Document> ubicaciones = new LinkedHashMap<>();
        // El catálogo ya limita las actividades que se envían al chat (máx. 7).
        for (Map<String, Object> actividad : actividades) {
            Object idUbicacion = actividad.get("ubicacionId");
            String identificador = texto(idUbicacion);
            if (!identificador.isBlank() && !ubicaciones.containsKey(identificador)) {
                Document encontrada = buscarUbicacion(idUbicacion);
                ubicaciones.put(identificador, encontrada);
            }
        }
        for (Map<String, Object> actividad : actividades) {
            Document referencia = ubicaciones.get(texto(actividad.get("ubicacionId")));
            Map<String, Object> resultado = construir(actividad, referencia);
            resultados.add(resultado);
        }
        return resultados;
    }

    private Document buscarUbicacion(Object identificador) {
        if (identificador == null) return null;
        try {
            // findById acepta el ObjectId del esquema MongoDB o las claves de texto.
            String id = texto(identificador);
            if (id.isBlank()) return null;
            Document encontrado = mongo.findById(id, Document.class, "ubicaciones");
            if (encontrado == null && ObjectId.isValid(id)) {
                encontrado = mongo.findById(new ObjectId(id), Document.class, "ubicaciones");
            }
            return encontrado;
        } catch (RuntimeException ex) {
            // Si faltan ubicaciones antiguas, el asistente funciona con el dato disponible.
            return null;
        }
    }

    static Map<String, Object> construir(Map<String, Object> actividad, Document ubicacion) {
        String lugar = primerValor(ubicacion, "nombre", "nombreUbicacion", "lugar");
        if (lugar.isBlank()) lugar = primerValor(actividad, "ubicacion", "lugar");
        if (lugar.isBlank() && actividad.get("ubicacionId") instanceof Map<?, ?> anidada) {
            lugar = texto(anidada.get("nombre"));
        }
        // Una etiqueta descriptiva antigua es aceptable, pero NO un UUID/ObjectId/código numérico.
        if (lugar.isBlank()) {
            String id = texto(actividad.get("ubicacionId"));
            if (esNombreLegible(id)) lugar = id;
        }
        String sede = primerValor(ubicacion, "sede");
        String pabellon = primerValor(ubicacion, "pabellon", "edificio");
        String aula = primerValor(ubicacion, "aula", "salon", "ambiente");
        String direccion = primerValor(ubicacion, "direccion");
        String referencia = primerValor(ubicacion, "referencia", "indicaciones");
        if (lugar.isBlank()) lugar = !aula.isBlank() ? aula : !pabellon.isBlank() ? pabellon : !sede.isBlank() ? sede : direccion;
        boolean confirmado = !lugar.isBlank() || !sede.isBlank() || !pabellon.isBlank() || !aula.isBlank() || !direccion.isBlank();
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("actividadId", texto(actividad.get("id")));
        datos.put("nombre", texto(actividad.get("nombre")));
        datos.put("fecha", texto(actividad.get("fecha")));
        datos.put("horaInicio", texto(actividad.get("horaInicio")));
        datos.put("lugar", confirmado ? (lugar.isBlank() ? "Ubicación por confirmar" : lugar) : "Ubicación por confirmar");
        datos.put("sede", sede);
        datos.put("pabellon", pabellon);
        datos.put("aula", aula);
        datos.put("direccion", direccion);
        datos.put("referencia", referencia);
        datos.put("confirmada", confirmado);
        return datos;
    }

    private static boolean esNombreLegible(String valor) {
        String limpio = valor.trim();
        return limpio.length() >= 4 && !limpio.matches("[0-9a-fA-F]{24}")
                && !limpio.matches("[0-9a-fA-F-]{32,36}") && !limpio.matches("[0-9]+")
                && !limpio.matches("[A-Za-z0-9_-]{4,}");
    }

    private static String primerValor(Map<String, ?> mapa, String... nombres) {
        if (mapa == null) return "";
        for (String nombre : nombres) {
            String valor = texto(mapa.get(nombre));
            if (!valor.isBlank() && !"null".equalsIgnoreCase(valor)) return valor;
        }
        return "";
    }

    private static String texto(Object valor) { return valor == null ? "" : String.valueOf(valor).trim(); }
}
