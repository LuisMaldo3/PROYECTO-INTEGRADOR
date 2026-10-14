package pe.tecsup.vynk.service;

import org.junit.jupiter.api.Test;
import pe.tecsup.vynk.config.InteresesIniciales;
import pe.tecsup.vynk.model.Interes;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InteresesServiceTest {

    private static final Set<String> VALIDOS = Set.of("teatro", "musica", "tecnologia", "deportes");

    @Test
    void aceptaUnaSeleccionCorrectaYRespetaElOrden() {
        assertEquals(List.of("musica", "teatro"), InteresesService.validarSeleccion(List.of("musica", "teatro"), VALIDOS));
    }

    @Test
    void conUnSoloInteresBasta() {
        assertEquals(List.of("deportes"), InteresesService.validarSeleccion(List.of("deportes"), VALIDOS));
    }

    @Test
    void quitaRepetidosYNormalizaMayusculasYEspacios() {
        assertEquals(List.of("teatro", "tecnologia"),
                InteresesService.validarSeleccion(List.of(" Teatro ", "teatro", "TECNOLOGIA", "tecnologia"), VALIDOS));
    }

    @Test
    void ignoraLosVaciosPeroNoLaListaVacia() {
        assertEquals(List.of("musica"), InteresesService.validarSeleccion(Arrays.asList("", "  ", null, "musica"), VALIDOS));
        SeleccionInvalidaException vacia = assertThrows(SeleccionInvalidaException.class,
                () -> InteresesService.validarSeleccion(List.of(), VALIDOS));
        assertEquals("Elige al menos un interés.", vacia.getMessage());
        assertThrows(SeleccionInvalidaException.class, () -> InteresesService.validarSeleccion(Arrays.asList("", null), VALIDOS));
    }

    @Test
    void sinListaSeRechaza() {
        SeleccionInvalidaException e = assertThrows(SeleccionInvalidaException.class, () -> InteresesService.validarSeleccion(null, VALIDOS));
        assertEquals("Envía la lista de intereses.", e.getMessage());
    }

    @Test
    void unInteresQueNoExisteSeRechazaAunqueLosDemasEstenBien() {
        SeleccionInvalidaException e = assertThrows(SeleccionInvalidaException.class,
                () -> InteresesService.validarSeleccion(List.of("teatro", "astrologia"), VALIDOS));
        assertEquals("El interés «astrologia» no existe.", e.getMessage());
    }

    @Test
    void elCatalogoInicialTieneIdsUnicosYLegibles() {
        List<String> ids = InteresesIniciales.CATALOGO.stream().map(Interes::getId).toList();
        assertEquals(ids.size(), ids.stream().distinct().count());
        assertTrue(ids.stream().allMatch(id -> id.matches("[a-z]+")), "los ids son minúsculas sin espacios");
        assertEquals(ids.size(), InteresesIniciales.CATALOGO.stream().map(Interes::getOrden).distinct().count());
    }

    @Test
    void elCatalogoInicialIncluyeLosTemasDelTcWeek() {
        Set<String> ids = InteresesIniciales.CATALOGO.stream().map(Interes::getId).collect(Collectors.toSet());
        assertTrue(ids.containsAll(Set.of("teatro", "musica", "tecnologia", "ia", "videojuegos", "deportes", "gastronomia")));
    }
}
