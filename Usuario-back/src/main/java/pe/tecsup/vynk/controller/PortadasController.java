package pe.tecsup.vynk.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Portadas de actividades: fotos Commons o imágenes generadas por Gemini.
 * Solo el administrador busca/genera; las imágenes aprobadas pueden verse públicamente.
 * GEMINI_API_KEY permanece exclusivamente en el backend.
 */
@RestController
@RequestMapping("/api/portadas")
public class PortadasController {
    private static final Pattern ID = Pattern.compile("[a-fA-F0-9]{24}");
    private final MongoTemplate mongo;
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    @Value("${GEMINI_API_KEY:}") private String geminiKey;
    @Value("${GEMINI_IMAGE_MODEL:gemini-2.5-flash-image}") private String geminiModel;

    public PortadasController(MongoTemplate mongo, ObjectMapper mapper) {
        this.mongo = mongo;
        this.mapper = mapper;
    }

    private static String nombre(Map<String, String> cuerpo) {
        String texto = cuerpo == null ? "" : cuerpo.getOrDefault("nombre", "").trim();
        if (texto.length() < 3 || texto.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escribe el nombre de una actividad (3 a 100 caracteres).");
        }
        return texto;
    }

    private static void soloAdmin(Jwt jwt) {
        if (jwt == null || !"administrador".equalsIgnoreCase(jwt.getClaimAsString("rol"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el administrador puede preparar portadas.");
        }
    }

    /** Buscar fotografías con licencia abierta, sin claves externas. */
    @GetMapping("/buscar")
    public Map<String, Object> buscar(@AuthenticationPrincipal Jwt jwt, @RequestParam("q") String q) {
        soloAdmin(jwt);
        String termino = nombre(Map.of("nombre", q));
        // Buscar también los nombres deportivos habituales en inglés para obtener más resultados.
        String clave = termino.toLowerCase(Locale.ROOT);
        String consulta = clave.contains("fútbol") || clave.contains("futbol") ? "association football match" :
                clave.contains("vóley") || clave.contains("voley") ? "volleyball players" :
                clave.contains("ajedrez") ? "chess tournament" :
                clave.contains("programaci") ? "hackathon computers" :
                clave.contains("robótica") || clave.contains("robotica") ? "robotics competition" : termino;
        String url = "https://commons.wikimedia.org/w/api.php?action=query&generator=search&gsrnamespace=6" +
                "&gsrsearch=" + URLEncoder.encode(consulta, StandardCharsets.UTF_8) +
                "&gsrlimit=12&prop=imageinfo&iiprop=url%7Cextmetadata&iiurlwidth=1000&format=json&formatversion=2";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(17))
                .header("User-Agent", "VYNK-educational/1.0 (contact: admin@tecsup.edu.pe)")
                .header("Accept", "application/json").GET().build();
        try {
            HttpResponse<String> r = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() != 200) throw new IllegalStateException("HTTP " + r.statusCode());
            JsonNode pages = mapper.readTree(r.body()).path("query").path("pages");
            List<Map<String, Object>> fotos = new ArrayList<>();
            for (JsonNode page : pages) {
                JsonNode info = page.path("imageinfo").path(0);
                String imagen = info.path("thumburl").asText(info.path("url").asText(""));
                // Mostrar únicamente contenido alojado en Wikimedia Commons.
                if (!imagen.startsWith("https://upload.wikimedia.org/")) continue;
                JsonNode meta = info.path("extmetadata");
                String autor = quitarHtml(meta.path("Artist").path("value").asText("Wikimedia Commons"));
                String licencia = quitarHtml(meta.path("LicenseShortName").path("value").asText("Licencia de Commons"));
                String creditUrl = info.path("descriptionurl").asText("https://commons.wikimedia.org");
                if (!creditUrl.startsWith("https://commons.wikimedia.org/")) continue;
                // Evitar imágenes con restricciones no comerciales, sin derivados o sin licencia verificable.
                if (!licenciaAbierta(licencia)) continue;
                if (licencia.length() > 80) licencia = licencia.substring(0, 80);
                Map<String, Object> portada = new LinkedHashMap<>();
                portada.put("tipo", "commons");
                portada.put("url", imagen);
                portada.put("autor", autor.length() > 100 ? autor.substring(0, 100) : autor);
                portada.put("licencia", licencia);
                portada.put("creditoUrl", creditUrl);
                fotos.add(portada);
                if (fotos.size() >= 8) break;
            }
            return Map.of("fotos", fotos, "total", fotos.size(), "proveedor", "Wikimedia Commons");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Se interrumpió la búsqueda de fotografías.");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudieron buscar fotografías. Revisa internet o prueba con Gemini.");
        }
    }

    private static boolean licenciaAbierta(String value) {
        String licencia = value == null ? "" : value.toUpperCase(Locale.ROOT).trim();
        if (licencia.contains("-NC") || licencia.contains("-ND") || licencia.contains("NONCOMMERCIAL")
                || licencia.contains("NO DERIV") || licencia.contains("COPYRIGHT")
                || licencia.contains("ALL RIGHTS")) return false;
        return licencia.startsWith("CC BY") || licencia.startsWith("CC-BY") || licencia.startsWith("CC0")
                || licencia.startsWith("PUBLIC DOMAIN") || licencia.startsWith("PD-")
                || licencia.equals("PD") || licencia.contains("DOMINIO PÚBLICO");
    }

    private static String quitarHtml(String value) {
        return value.replaceAll("<[^>]*>", " ").replace("&amp;", "&")
                .replace("&quot;", "\"").replace("&#39;", "'").replaceAll("\\s+", " ").trim();
    }

    /** Genera la portada con la misma clave Gemini del asistente de VYNK. */
    @PostMapping("/generar")
    public Map<String, Object> generar(@AuthenticationPrincipal Jwt jwt, @RequestBody Map<String, String> cuerpo) {
        soloAdmin(jwt);
        String titulo = nombre(cuerpo);
        if (geminiKey == null || geminiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Configura GEMINI_API_KEY en Usuario-back. También puedes buscar una fotografía.");
        }
        String modelo = geminiModel.matches("[A-Za-z0-9._-]{3,80}") ? geminiModel : "gemini-2.5-flash-image";
        String prompt = "Crea una fotografía editorial REALISTA y profesional para la portada horizontal 16:9 " +
                "de una actividad de un instituto tecnológico en Perú llamada: '" + titulo + "'. " +
                "Escena auténtica y contextual al nombre, con estudiantes jóvenes adultos participando " +
                "en esa actividad, luz natural y composición moderna. SIN letras, SIN logotipos, " +
                "SIN marcas de agua añadidas. Fotografía apta para un sitio académico.";
        try {
            String body = mapper.writeValueAsString(Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                    "generationConfig", Map.of("responseModalities", List.of("TEXT", "IMAGE"))));
            HttpRequest request = HttpRequest.newBuilder(URI.create(
                    "https://generativelanguage.googleapis.com/v1beta/models/" + modelo + ":generateContent"))
                    .timeout(Duration.ofSeconds(100)).header("x-goog-api-key", geminiKey)
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> r = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() != 200) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Gemini no generó la imagen (HTTP " + r.statusCode() + "). Revisa el acceso, créditos o modelo; usa buscar fotografía como alternativa.");
            }
            JsonNode candidatos = mapper.readTree(r.body()).path("candidates");
            for (JsonNode candidato : candidatos) {
                for (JsonNode parte : candidato.path("content").path("parts")) {
                    JsonNode inline = parte.path("inlineData");
                    if (inline.isMissingNode()) inline = parte.path("inline_data");
                    String encoded = inline.path("data").asText("");
                    String mime = inline.path("mimeType").asText(inline.path("mime_type").asText("image/png"));
                    if (encoded.isBlank()) continue;
                    if (!List.of("image/png", "image/jpeg", "image/webp").contains(mime)) continue;
                    if (encoded.length() > 11_000_000) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "La imagen generada es demasiado grande.");
                    byte[] bytes = Base64.getDecoder().decode(encoded);
                    if (bytes.length == 0 || bytes.length > 8 * 1024 * 1024)
                        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "La imagen generada es demasiado grande.");
                    GridFSBucket gridfs = GridFSBuckets.create(mongo.getDb());
                    ObjectId archivo = gridfs.uploadFromStream("vynk-portada-" + System.currentTimeMillis(),
                            new ByteArrayInputStream(bytes), new GridFSUploadOptions().metadata(new Document("contentType", mime)));
                    return Map.of("portada", Map.of("tipo", "gemini", "archivoId", archivo.toHexString(),
                            "url", "/api/portadas/archivo/" + archivo.toHexString(), "autor", "Gemini", "licencia", "Imagen generada con IA"));
                }
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini respondió sin imagen. Intenta otra vez o busca una fotografía.");
        } catch (ResponseStatusException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "La generación de imagen fue interrumpida.");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Error al generar la portada con Gemini. Comprueba conexión y API.");
        }
    }

    /** Portada subida por administrador. Nunca se conserva el nombre original del usuario. */
    @PostMapping(value = "/subir", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> subir(@AuthenticationPrincipal Jwt jwt,
                                      @RequestPart("archivo") MultipartFile archivo) {
        soloAdmin(jwt);
        if (archivo == null || archivo.isEmpty() || archivo.getSize() > 5L * 1024L * 1024L)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona una imagen JPG o PNG de hasta 5 MB.");
        try {
            byte[] bytes = archivo.getBytes();
            String tipo;
            if (bytes.length >= 8 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 &&
                    bytes[2] == 0x4E && bytes[3] == 0x47 && bytes[4] == 0x0D &&
                    bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A) tipo = "image/png";
            else if (bytes.length >= 4 && (bytes[0] & 0xFF) == 0xFF &&
                    (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) tipo = "image/jpeg";
            else throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El contenido no es una imagen PNG o JPG válida.");
            GridFSBucket gridfs = GridFSBuckets.create(mongo.getDb());
            ObjectId id = gridfs.uploadFromStream("vynk-imagen-propia-" + System.currentTimeMillis(),
                new ByteArrayInputStream(bytes), new GridFSUploadOptions().metadata(new Document("contentType", tipo)));
            return Map.of("portada", Map.of("tipo", "subida", "archivoId", id.toHexString(),
                "url", "/api/portadas/archivo/" + id.toHexString(),
                "autor", "Administración VYNK", "licencia", "Material autorizado por administración"));
        } catch (ResponseStatusException e) { throw e; }
        catch (Exception e) { throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar la imagen."); }
    }

    @GetMapping("/archivo/{id}")
    public ResponseEntity<byte[]> archivo(@PathVariable("id") String id) {
        if (!ID.matcher(id).matches()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        try {
            GridFSBucket gridfs = GridFSBuckets.create(mongo.getDb());
            GridFSFile archivo = gridfs.find(new Document("_id", new ObjectId(id))).first();
            if (archivo == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            String mime = archivo.getMetadata() == null ? "image/png" : archivo.getMetadata().getString("contentType");
            if (!List.of("image/png", "image/jpeg", "image/webp").contains(mime))
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            try (GridFSDownloadStream input = gridfs.openDownloadStream(new ObjectId(id));
                 ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[16 * 1024];
                int leidos;
                while ((leidos = input.read(buffer)) != -1) {
                    salida.write(buffer, 0, leidos);
                    if (salida.size() > 8 * 1024 * 1024) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
                }
                return ResponseEntity.ok().contentType(MediaType.parseMediaType(mime))
                        .cacheControl(CacheControl.noCache()).body(salida.toByteArray());
            }
        } catch (ResponseStatusException e) { throw e; }
        catch (Exception e) { throw new ResponseStatusException(HttpStatus.NOT_FOUND); }
    }
}
