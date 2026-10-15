package pe.tecsup.vynk.controller;

import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import pe.tecsup.vynk.service.GeminiRecomendacionesService;
import pe.tecsup.vynk.service.InteresesService;

@RestController
@RequestMapping("/api/recomendaciones")
public class RecomendacionesController {
    private final InteresesService intereses;
    private final CatalogoController catalogo;
    private final GeminiRecomendacionesService gemini;
    public RecomendacionesController(InteresesService intereses, CatalogoController catalogo, GeminiRecomendacionesService gemini) {
        this.intereses=intereses; this.catalogo=catalogo; this.gemini=gemini;
    }
    @GetMapping
    public Map<String,Object> listar(@AuthenticationPrincipal Jwt jwt) {
        List<String> preferencias = intereses.deUsuario(jwt.getSubject()).getIntereses();
        if(preferencias == null || preferencias.isEmpty()) return Map.of("origen","sin_intereses","aviso","Selecciona tus intereses para recibir recomendaciones.","actividades",List.of());
        @SuppressWarnings("unchecked") List<Map<String,Object>> todas = (List<Map<String,Object>>) catalogo.listar().get("actividades");
        List<Map<String,Object>> candidatas = todas.stream().filter(a -> {
            String categoria = String.valueOf(a.getOrDefault("categoriaId", "")).toLowerCase(Locale.ROOT);
            String texto = (String.valueOf(a.getOrDefault("nombre", "")) + " " + String.valueOf(a.getOrDefault("descripcion", ""))).toLowerCase(Locale.ROOT);
            return preferencias.stream().anyMatch(p -> categoria.equalsIgnoreCase(p) || texto.contains(p.toLowerCase(Locale.ROOT)));
        }).limit(35).toList();
        // Sin coincidencias de categoría, Gemini puede evaluar el catálogo completo.
        if(candidatas.isEmpty()) candidatas=todas.stream().limit(35).toList();
        GeminiRecomendacionesService.Resultado resultado=gemini.ordenar(preferencias,candidatas);
        Map<String,Map<String,Object>> porId=new HashMap<>(); for(var a:candidatas) porId.put(String.valueOf(a.get("id")),a);
        List<Map<String,Object>> salida=new ArrayList<>();
        if("ia".equals(resultado.origen())) {
            for(var d:resultado.decisiones()) {
                var a=porId.get(d.id()); if(a==null) continue;
                var item=new LinkedHashMap<String,Object>(a); item.put("motivo",d.motivo()); salida.add(item);
            }
        } else if(!"sin_actividades".equals(resultado.origen())) {
            // Respaldo explícito: nunca se presenta como recomendación de IA.
            for(var a:candidatas) { var item=new LinkedHashMap<String,Object>(a); item.put("motivo","Actividad sugerida por coincidencia con tus intereses (sin IA)."); salida.add(item); if(salida.size()==12) break; }
        }
        return Map.of("origen",resultado.origen(),"aviso",resultado.aviso(),"intereses",preferencias,"actividades",salida);
    }
}
